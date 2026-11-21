package com.autopilot.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.autopilot.app.domain.model.AutomationConfig
import com.autopilot.app.domain.model.AutomationMode
import com.autopilot.app.domain.model.DeviceFingerprint
import com.autopilot.app.domain.model.ProxyInfo
import com.autopilot.app.domain.model.ProxyProtocol
import com.autopilot.app.domain.model.ScheduledTask
import com.autopilot.app.domain.model.SessionStatus
import com.autopilot.app.domain.model.WebRtcPolicy
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(tableName = "session_history")
data class SessionHistoryEntity(
    @PrimaryKey val id: String,
    val url: String,
    val mode: String,
    val status: String,
    val tabCount: Int,
    val totalRefreshes: Int,
    val totalScrolls: Int,
    val runtimeSeconds: Long,
    val spoofedModel: String,
    val proxyHost: String,
    val startedAt: Long,
    val endedAt: Long,
    val configJson: String
)

@Entity(tableName = "proxies", primaryKeys = ["host", "port"])
data class ProxyEntity(
    val host: String,
    val port: Int,
    val protocol: String,
    val country: String,
    val latencyMs: Long,
    val isAlive: Boolean,
    val lastChecked: Long,
    val failureCount: Int
)

@Entity(tableName = "fingerprints")
data class FingerprintEntity(
    @PrimaryKey val id: String,
    val androidId: String,
    val advertisingId: String,
    val buildModel: String,
    val buildManufacturer: String,
    val buildFingerprint: String,
    val fingerprintAndroidVersion: Int,
    val buildProduct: String,
    val buildDevice: String,
    val buildBoard: String,
    val buildBrand: String,
    val serialNumber: String,
    val macAddress: String,
    val bluetoothAddress: String,
    val widevineId: String,
    val screenWidth: Int,
    val screenHeight: Int,
    val screenDpi: Int,
    val userAgent: String,
    val timezone: String,
    val locale: String,
    val glRenderer: String,
    val glVendor: String,
    val canvasNoiseSeed: Long,
    val audioNoiseSeed: Long,
    val webrtcPolicy: String,
    val platform: String,
    val cpuCores: Int,
    val deviceMemory: Int,
    val maxTouchPoints: Int,
    val languagesCsv: String
)

@Entity(tableName = "scheduled_tasks")
data class ScheduledTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val targetUrl: String,
    val mode: String,
    val tabCount: Int,
    val intervalMinutes: Int,
    val enabled: Boolean,
    val lastRunAt: Long,
    val nextRunAt: Long
)

fun ProxyEntity.toDomain(): ProxyInfo = ProxyInfo(
    host = host,
    port = port,
    protocol = ProxyProtocol.entries.firstOrNull { it.name == protocol } ?: ProxyProtocol.HTTP,
    country = country,
    latencyMs = latencyMs,
    isAlive = isAlive,
    lastChecked = lastChecked,
    failureCount = failureCount
)

fun ProxyInfo.toEntity(): ProxyEntity = ProxyEntity(
    host = host,
    port = port,
    protocol = protocol.name,
    country = country,
    latencyMs = latencyMs,
    isAlive = isAlive,
    lastChecked = lastChecked,
    failureCount = failureCount
)

fun FingerprintEntity.toDomain(): DeviceFingerprint = DeviceFingerprint(
    id = id,
    androidId = androidId,
    advertisingId = advertisingId,
    buildModel = buildModel,
    buildManufacturer = buildManufacturer,
    buildFingerprint = buildFingerprint,
    fingerprintAndroidVersion = fingerprintAndroidVersion,
    buildProduct = buildProduct,
    buildDevice = buildDevice,
    buildBoard = buildBoard,
    buildBrand = buildBrand,
    serialNumber = serialNumber,
    macAddress = macAddress,
    bluetoothAddress = bluetoothAddress,
    widevineId = widevineId,
    screenWidth = screenWidth,
    screenHeight = screenHeight,
    screenDpi = screenDpi,
    userAgent = userAgent,
    timezone = timezone,
    locale = locale,
    glRenderer = glRenderer,
    glVendor = glVendor,
    canvasNoiseSeed = canvasNoiseSeed,
    audioNoiseSeed = audioNoiseSeed,
    webrtcPolicy = WebRtcPolicy.entries.firstOrNull { it.name == webrtcPolicy } ?: WebRtcPolicy.BLOCK,
    platform = platform,
    cpuCores = cpuCores,
    deviceMemory = deviceMemory,
    maxTouchPoints = maxTouchPoints,
    languages = languagesCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
)

fun DeviceFingerprint.toEntity(): FingerprintEntity = FingerprintEntity(
    id = id,
    androidId = androidId,
    advertisingId = advertisingId,
    buildModel = buildModel,
    buildManufacturer = buildManufacturer,
    buildFingerprint = buildFingerprint,
    fingerprintAndroidVersion = fingerprintAndroidVersion,
    buildProduct = buildProduct,
    buildDevice = buildDevice,
    buildBoard = buildBoard,
    buildBrand = buildBrand,
    serialNumber = serialNumber,
    macAddress = macAddress,
    bluetoothAddress = bluetoothAddress,
    widevineId = widevineId,
    screenWidth = screenWidth,
    screenHeight = screenHeight,
    screenDpi = screenDpi,
    userAgent = userAgent,
    timezone = timezone,
    locale = locale,
    glRenderer = glRenderer,
    glVendor = glVendor,
    canvasNoiseSeed = canvasNoiseSeed,
    audioNoiseSeed = audioNoiseSeed,
    webrtcPolicy = webrtcPolicy.name,
    platform = platform,
    cpuCores = cpuCores,
    deviceMemory = deviceMemory,
    maxTouchPoints = maxTouchPoints,
    languagesCsv = languages.joinToString(",")
)

fun ScheduledTaskEntity.toDomain(): ScheduledTask = ScheduledTask(
    id = id,
    title = title,
    targetUrl = targetUrl,
    mode = AutomationMode.fromString(mode),
    tabCount = tabCount,
    intervalMinutes = intervalMinutes,
    enabled = enabled,
    lastRunAt = lastRunAt,
    nextRunAt = nextRunAt
)

fun ScheduledTask.toEntity(): ScheduledTaskEntity = ScheduledTaskEntity(
    id = id,
    title = title,
    targetUrl = targetUrl,
    mode = mode.name,
    tabCount = tabCount,
    intervalMinutes = intervalMinutes,
    enabled = enabled,
    lastRunAt = lastRunAt,
    nextRunAt = nextRunAt
)

object ConfigCodec {
    private val json = Json { ignoreUnknownKeys = true }

    fun encode(config: AutomationConfig): String = json.encodeToString(config)

    fun decode(raw: String): AutomationConfig = runCatching {
        json.decodeFromString<AutomationConfig>(raw)
    }.getOrDefault(AutomationConfig())

    fun statusFromString(raw: String): SessionStatus = SessionStatus.fromString(raw)
}
