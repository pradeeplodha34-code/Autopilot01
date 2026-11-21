package com.autopilot.app.automation

import android.annotation.SuppressLint
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature
import com.autopilot.app.domain.model.ExitIpResult
import com.autopilot.app.domain.model.ProxyInfo
import com.autopilot.app.domain.model.ProxyProtocol
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.Socket
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

object ProxyHealthChecker {

    private val json = Json { ignoreUnknownKeys = true }

    private val ipLookupEndpoints = listOf(
        "https://ipwho.is/",
        "https://api.ipify.org?format=json",
        "https://ipapi.co/json/"
    )

    suspend fun probe(proxy: ProxyInfo, timeoutMs: Int = 3500): ProxyInfo = withContext(Dispatchers.IO) {
        val start = System.nanoTime()
        val now = System.currentTimeMillis()
        var socket: Socket? = null
        try {
            socket = Socket()
            socket.connect(InetSocketAddress(proxy.host, proxy.port), timeoutMs)
            val elapsedMs = ((System.nanoTime() - start) / 1_000_000L).coerceAtLeast(1L)
            proxy.copy(
                latencyMs = elapsedMs,
                isAlive = true,
                lastChecked = now,
                failureCount = 0
            )
        } catch (_: Exception) {
            proxy.copy(
                latencyMs = -1L,
                isAlive = false,
                lastChecked = now,
                failureCount = proxy.failureCount + 1
            )
        } finally {
            runCatching { socket?.close() }
        }
    }

    /**
     * Verifies real external IP and country via the original APK endpoints
     * (ipwho.is, api.ipify.org, ipapi.co). Never fabricates IP or location data.
     */
    suspend fun verifyExitIp(proxy: ProxyInfo? = null, timeoutMs: Long = 6000L): ExitIpResult =
        withContext(Dispatchers.IO) {
            val clientBuilder = OkHttpClient.Builder()
                .connectTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .readTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .callTimeout(timeoutMs + 2000L, TimeUnit.MILLISECONDS)

            val viaLabel = if (proxy != null) {
                val javaProxyType = when (proxy.protocol) {
                    ProxyProtocol.SOCKS4, ProxyProtocol.SOCKS5 -> Proxy.Type.SOCKS
                    ProxyProtocol.HTTP, ProxyProtocol.HTTPS -> Proxy.Type.HTTP
                }
                clientBuilder.proxy(Proxy(javaProxyType, InetSocketAddress(proxy.host, proxy.port)))
                "${proxy.protocol.name} ${proxy.host}:${proxy.port}"
            } else {
                "Direct connection"
            }

            val client = clientBuilder.build()
            var lastError: String? = null

            for (endpoint in ipLookupEndpoints) {
                try {
                    val request = Request.Builder()
                        .url(endpoint)
                        .header("User-Agent", "Mozilla/5.0 (Linux; Android 15; Pixel 9) AppleWebKit/537.36")
                        .header("Accept", "application/json")
                        .build()

                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            lastError = "$endpoint -> HTTP ${response.code}"
                            return@use
                        }
                        val body = response.body?.string().orEmpty()
                        val obj = json.parseToJsonElement(body).jsonObject
                        val ip = obj["ip"]?.jsonPrimitive?.content?.trim().orEmpty()
                        if (ip.isBlank()) {
                            lastError = "$endpoint -> missing IP field"
                            return@use
                        }
                        val countryCode = (obj["country_code"] ?: obj["country"])
                            ?.jsonPrimitive?.content?.trim().orEmpty()
                        val countryName = (obj["country_name"] ?: obj["country"])
                            ?.jsonPrimitive?.content?.trim().orEmpty()
                        return@withContext ExitIpResult(
                            ip = ip,
                            countryCode = CountryRegionCatalog.resolveCountryCode(countryCode),
                            countryName = CountryRegionCatalog.resolveCountryName(
                                countryName.ifBlank { countryCode }
                            ),
                            provider = endpoint.substringAfter("https://").substringBefore("/"),
                            viaProxy = viaLabel,
                            checkedAt = System.currentTimeMillis(),
                            errorMessage = null
                        )
                    }
                } catch (e: Exception) {
                    lastError = e.message?.take(80) ?: e::class.java.simpleName
                }
            }

            ExitIpResult(
                viaProxy = viaLabel,
                checkedAt = System.currentTimeMillis(),
                errorMessage = lastError ?: "All IP verification endpoints unreachable"
            )
        }

    fun isWebViewProxySupported(): Boolean =
        WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)

    @SuppressLint("RequiresFeature")
    fun applyWebViewProxy(proxy: ProxyInfo?): Boolean {
        if (!isWebViewProxySupported()) {
            return false
        }
        val controller = ProxyController.getInstance()
        val executor = Executor { it.run() }
        return if (proxy == null) {
            runCatching { controller.clearProxyOverride(executor) {} }.isSuccess
        } else {
            val scheme = when (proxy.protocol) {
                ProxyProtocol.HTTP -> "http"
                ProxyProtocol.HTTPS -> "https"
                ProxyProtocol.SOCKS4 -> "socks4"
                ProxyProtocol.SOCKS5 -> "socks5"
            }
            val config = ProxyConfig.Builder()
                .addProxyRule("$scheme://${proxy.host}:${proxy.port}")
                .build()
            runCatching { controller.setProxyOverride(config, executor) {} }.isSuccess
        }
    }
}
