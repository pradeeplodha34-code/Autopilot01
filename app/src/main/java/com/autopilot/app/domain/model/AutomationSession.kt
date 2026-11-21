package com.autopilot.app.domain.model

import java.util.Locale

data class TabState(
    val id: String = "",
    val index: Int = 0,
    val status: SessionStatus = SessionStatus.STOPPED,
    val url: String = "",
    val refreshCount: Int = 0,
    val scrollCount: Int = 0,
    val userAgent: String = "",
    val lastRefresh: Long = 0L,
    val startedAt: Long = 0L,
    val error: String? = null
)

data class TabRealTimeState(
    val tabId: String = "",
    val index: Int = 0,
    val status: SessionStatus = SessionStatus.RUNNING,
    val refreshCount: Int = 0,
    val scrollCount: Int = 0,
    val lastAction: String = "Initializing",
    val lastActionTime: Long = System.currentTimeMillis()
)

data class SessionStats(
    val activeTabs: Int = 0,
    val totalRefreshes: Int = 0,
    val totalScrolls: Int = 0,
    val currentDeviceId: String = "",
    val lastUpdate: Long = 0L,
    val startedAt: Long = 0L,
    val runtime: String = "00:00:00"
)

data class SessionCommand(
    val id: String = "",
    val type: CommandType = CommandType.PAUSE,
    val processed: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

data class EngineState(
    val sessionId: String = "",
    val status: SessionStatus = SessionStatus.STOPPED,
    val url: String = "",
    val totalTabs: Int = 0,
    val activeTabs: Int = 0,
    val totalRefreshes: Int = 0,
    val totalScrolls: Int = 0,
    val startedAt: Long = 0L,
    val currentActivity: String = "Idle",
    val tabStates: Map<String, TabRealTimeState> = emptyMap(),
    val proxyActive: Boolean = false,
    val proxyHost: String = "",
    val proxyCountry: String = "",
    val spoofingActive: Boolean = false,
    val spoofedModel: String = "",
    val memoryPercent: Int = 0,
    val runtimeSeconds: Long = 0L
) {
    val runtime: String
        get() = String.format(
            Locale.US,
            "%02d:%02d:%02d",
            runtimeSeconds / 3600,
            (runtimeSeconds % 3600) / 60,
            runtimeSeconds % 60
        )
}

data class AutomationSession(
    val id: String = "",
    val userId: String = "",
    val status: SessionStatus = SessionStatus.STOPPED,
    val url: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val config: AutomationConfig = AutomationConfig(),
    val stats: SessionStats = SessionStats(),
    val rootStatus: RootStatus = RootStatus(),
    val proxyStatus: ProxyStatus = ProxyStatus(),
    val fingerprint: FingerprintInfo = FingerprintInfo(),
    val tabs: Map<String, TabState> = emptyMap(),
    val lastHeartbeat: Long = 0L
)
