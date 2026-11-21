package com.autopilot.app.domain.model

data class DeviceFingerprint(
    val id: String = "",
    val androidId: String = "",
    val advertisingId: String = "",
    val buildModel: String = "",
    val buildManufacturer: String = "",
    val buildFingerprint: String = "",
    val fingerprintAndroidVersion: Int = 15,
    val buildProduct: String = "",
    val buildDevice: String = "",
    val buildBoard: String = "",
    val buildBrand: String = "",
    val serialNumber: String = "",
    val macAddress: String = "",
    val bluetoothAddress: String = "",
    val widevineId: String = "",
    val screenWidth: Int = 1080,
    val screenHeight: Int = 2400,
    val screenDpi: Int = 420,
    val userAgent: String = "",
    val timezone: String = "America/New_York",
    val locale: String = "en-US",
    val glRenderer: String = "",
    val glVendor: String = "",
    val canvasNoiseSeed: Long = 0L,
    val audioNoiseSeed: Long = 0L,
    val webrtcPolicy: WebRtcPolicy = WebRtcPolicy.BLOCK,
    val platform: String = "Linux armv8l",
    val cpuCores: Int = 8,
    val deviceMemory: Int = 8,
    val maxTouchPoints: Int = 5,
    val languages: List<String> = listOf("en-US", "en")
)

data class FingerprintInfo(
    val hash: String = "",
    val model: String = "",
    val manufacturer: String = ""
)

data class ProxyInfo(
    val host: String = "",
    val port: Int = 0,
    val protocol: ProxyProtocol = ProxyProtocol.HTTP,
    val country: String = "",
    val latencyMs: Long = -1L,
    val isAlive: Boolean = false,
    val lastChecked: Long = 0L,
    val failureCount: Int = 0
)

data class ProxyStatus(
    val enabled: Boolean = false,
    val proxyCountry: String = "",
    val currentProxy: String = "",
    val proxyPool: Int = 0
)

data class ProxyCountryStats(
    val countryCode: String,
    val countryName: String,
    val proxyCount: Int
)

data class ProxySourceStatus(
    val sourceName: String,
    val endpointUrl: String,
    val fetchedCount: Int = 0,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
    val durationMs: Long = 0L
)

data class ProxyFetchSummary(
    val totalFetched: Int = 0,
    val totalUnique: Int = 0,
    val sources: List<ProxySourceStatus> = emptyList(),
    val fetchedAt: Long = 0L
)

data class ExitIpResult(
    val ip: String = "",
    val countryCode: String = "",
    val countryName: String = "",
    val provider: String = "",
    val viaProxy: String = "",
    val checkedAt: Long = 0L,
    val errorMessage: String? = null
)

data class RootEnvironment(
    val isRooted: Boolean = false,
    val hasMagisk: Boolean = false,
    val magiskVersion: String = "",
    val hasZygisk: Boolean = false,
    val hasLSPosed: Boolean = false,
    val lsposedVersion: String = "",
    val suPath: String = ""
)

data class RootStatus(
    val isRooted: Boolean = false,
    val magisk: Boolean = false,
    val zygisk: Boolean = false,
    val lsposed: Boolean = false,
    val spoofingEnabled: Boolean = false
)

data class RootVerificationResult(
    val observedModel: String = "",
    val observedManufacturer: String = "",
    val observedAndroidId: String = "",
    val systemSpoofApplied: Boolean = false,
    val statusMessage: String = ""
)
