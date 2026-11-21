package com.autopilot.app.automation

import com.autopilot.app.domain.model.ProxyFetchSummary
import com.autopilot.app.domain.model.ProxyInfo
import com.autopilot.app.domain.model.ProxyProtocol
import com.autopilot.app.domain.model.ProxySourceStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Ported from original APK proxy fetcher classes (ox0.java and jx0.java).
 * Queries only the verified public proxy endpoints present in original source.
 * Failures, rate limits, timeouts, and malformed responses are reported honestly.
 */
object PublicProxyFetcher {

    const val ENDPOINT_GEONODE =
        "https://proxylist.geonode.com/api/proxy-list?limit=40&page=1&sort_by=lastChecked&sort_type=desc&protocols=http%2Chttps"
    const val ENDPOINT_MONOSANS =
        "https://raw.githubusercontent.com/monosans/proxy-list/main/proxies/http.txt"
    const val ENDPOINT_PROXYSCRAPE =
        "https://api.proxyscrape.com/v2/?request=displayproxies&protocol=http&timeout=5000&country=all&ssl=yes&anonymity=all"
    const val ENDPOINT_THESPEEDX =
        "https://raw.githubusercontent.com/TheSpeedX/PROXY-List/master/http.txt"

    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 15; Pixel 9) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.7444.68 Mobile Safari/537.36"

    private val json = Json { ignoreUnknownKeys = true }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(6L, TimeUnit.SECONDS)
        .readTimeout(6L, TimeUnit.SECONDS)
        .callTimeout(10L, TimeUnit.SECONDS)
        .build()

    data class FetchOutcome(
        val proxies: List<ProxyInfo>,
        val summary: ProxyFetchSummary
    )

    suspend fun fetchAll(): FetchOutcome = coroutineScope {
        val geonodeJob = async(Dispatchers.IO) { fetchGeoNode() }
        val monosansJob = async(Dispatchers.IO) {
            fetchPlainTextSource("Monosans", ENDPOINT_MONOSANS, cap = 30)
        }
        val proxyScrapeJob = async(Dispatchers.IO) {
            fetchPlainTextSource("ProxyScrape", ENDPOINT_PROXYSCRAPE, cap = 40)
        }
        val speedXJob = async(Dispatchers.IO) {
            fetchPlainTextSource("TheSpeedX", ENDPOINT_THESPEEDX, cap = 30)
        }

        val results = listOf(
            geonodeJob.await(),
            monosansJob.await(),
            proxyScrapeJob.await(),
            speedXJob.await()
        )

        val merged = LinkedHashMap<String, ProxyInfo>()
        var totalFetched = 0
        results.forEach { (list, status) ->
            totalFetched += status.fetchedCount
            list.forEach { proxy ->
                val key = "${proxy.host}:${proxy.port}"
                val existing = merged[key]
                if (existing == null || (existing.country.isBlank() && proxy.country.isNotBlank())) {
                    merged[key] = proxy
                }
            }
        }

        val summary = ProxyFetchSummary(
            totalFetched = totalFetched,
            totalUnique = merged.size,
            sources = results.map { it.second },
            fetchedAt = System.currentTimeMillis()
        )
        FetchOutcome(
            proxies = merged.values.toList(),
            summary = summary
        )
    }

    fun parseProxyLine(rawLine: String): ProxyInfo? {
        val trimmed = rawLine.trim()
        if (trimmed.isEmpty() || trimmed.startsWith("#")) return null

        val protocol = when {
            trimmed.startsWith("socks5://", ignoreCase = true) -> ProxyProtocol.SOCKS5
            trimmed.startsWith("socks4://", ignoreCase = true) -> ProxyProtocol.SOCKS4
            trimmed.startsWith("https://", ignoreCase = true) -> ProxyProtocol.HTTPS
            else -> ProxyProtocol.HTTP
        }

        val cleaned = trimmed
            .removePrefix("http://")
            .removePrefix("https://")
            .removePrefix("socks4://")
            .removePrefix("socks5://")
            .substringBefore("/")
            .trim()

        val parts = cleaned.split(":")
        if (parts.size != 2) return null
        val host = parts[0].trim()
        val port = parts[1].trim().toIntOrNull() ?: return null
        if (host.isEmpty() || port !in 1..65535) return null

        return ProxyInfo(
            host = host,
            port = port,
            protocol = protocol,
            country = "",
            latencyMs = -1L,
            isAlive = false,
            lastChecked = 0L,
            failureCount = 0
        )
    }

    private suspend fun fetchGeoNode(): Pair<List<ProxyInfo>, ProxySourceStatus> =
        withContext(Dispatchers.IO) {
            val start = System.currentTimeMillis()
            try {
                client.newCall(buildRequest(ENDPOINT_GEONODE)).execute().use { response ->
                    val elapsed = System.currentTimeMillis() - start
                    if (!response.isSuccessful) {
                        val msg = if (response.code == 429) {
                            "Rate limited (HTTP 429)"
                        } else {
                            "HTTP ${response.code}"
                        }
                        return@withContext emptyList<ProxyInfo>() to ProxySourceStatus(
                            sourceName = "GeoNode",
                            endpointUrl = ENDPOINT_GEONODE,
                            isSuccess = false,
                            errorMessage = msg,
                            durationMs = elapsed
                        )
                    }
                    val body = response.body?.string().orEmpty()
                    if (body.isBlank()) {
                        return@withContext emptyList<ProxyInfo>() to ProxySourceStatus(
                            sourceName = "GeoNode",
                            endpointUrl = ENDPOINT_GEONODE,
                            isSuccess = false,
                            errorMessage = "Empty response",
                            durationMs = elapsed
                        )
                    }
                    val parsed = parseGeoNodeJson(body).take(30)
                    parsed to ProxySourceStatus(
                        sourceName = "GeoNode",
                        endpointUrl = ENDPOINT_GEONODE,
                        fetchedCount = parsed.size,
                        isSuccess = true,
                        errorMessage = if (parsed.isEmpty()) "No valid entries in response" else null,
                        durationMs = elapsed
                    )
                }
            } catch (e: Exception) {
                val elapsed = System.currentTimeMillis() - start
                emptyList<ProxyInfo>() to ProxySourceStatus(
                    sourceName = "GeoNode",
                    endpointUrl = ENDPOINT_GEONODE,
                    isSuccess = false,
                    errorMessage = e.message?.take(60) ?: e::class.java.simpleName,
                    durationMs = elapsed
                )
            }
        }

    fun parseGeoNodeJson(rawJson: String): List<ProxyInfo> {
        val root = json.parseToJsonElement(rawJson).jsonObject
        val dataArray = root["data"]?.jsonArray ?: return emptyList()
        val list = ArrayList<ProxyInfo>(dataArray.size)
        for (element in dataArray) {
            val obj = runCatching { element.jsonObject }.getOrNull() ?: continue
            val ip = obj["ip"]?.jsonPrimitive?.content?.trim().orEmpty()
            val port = obj["port"]?.jsonPrimitive?.content?.trim()?.toIntOrNull() ?: continue
            val countryCode = obj["country"]?.jsonPrimitive?.content?.trim().orEmpty()
            if (ip.isNotEmpty() && port in 1..65535) {
                list.add(
                    ProxyInfo(
                        host = ip,
                        port = port,
                        protocol = ProxyProtocol.HTTP,
                        country = CountryRegionCatalog.resolveCountryCode(countryCode)
                    )
                )
            }
        }
        return list
    }

    private suspend fun fetchPlainTextSource(
        sourceName: String,
        url: String,
        cap: Int
    ): Pair<List<ProxyInfo>, ProxySourceStatus> = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        try {
            client.newCall(buildRequest(url)).execute().use { response ->
                val elapsed = System.currentTimeMillis() - start
                if (!response.isSuccessful) {
                    val msg = if (response.code == 429) {
                        "Rate limited (HTTP 429)"
                    } else {
                        "HTTP ${response.code}"
                    }
                    return@withContext emptyList<ProxyInfo>() to ProxySourceStatus(
                        sourceName = sourceName,
                        endpointUrl = url,
                        isSuccess = false,
                        errorMessage = msg,
                        durationMs = elapsed
                    )
                }
                val body = response.body?.string().orEmpty()
                val parsed = body.lineSequence()
                    .mapNotNull { parseProxyLine(it) }
                    .take(cap)
                    .toList()
                parsed to ProxySourceStatus(
                    sourceName = sourceName,
                    endpointUrl = url,
                    fetchedCount = parsed.size,
                    isSuccess = true,
                    errorMessage = if (parsed.isEmpty()) "No valid host:port lines" else null,
                    durationMs = elapsed
                )
            }
        } catch (e: Exception) {
            val elapsed = System.currentTimeMillis() - start
            emptyList<ProxyInfo>() to ProxySourceStatus(
                sourceName = sourceName,
                endpointUrl = url,
                isSuccess = false,
                errorMessage = e.message?.take(60) ?: e::class.java.simpleName,
                durationMs = elapsed
            )
        }
    }

    private fun buildRequest(url: String): Request = Request.Builder()
        .url(url)
        .header("User-Agent", USER_AGENT)
        .header("Accept", "*/*")
        .build()
}
