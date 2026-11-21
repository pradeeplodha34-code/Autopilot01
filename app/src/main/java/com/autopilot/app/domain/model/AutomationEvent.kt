package com.autopilot.app.domain.model

sealed class AutomationEvent(
    val timestamp: Long = System.currentTimeMillis(),
    val tabId: String = ""
) {
    class SessionStarted(
        val sessionId: String,
        val url: String,
        val tabCount: Int
    ) : AutomationEvent()

    class SessionPaused(val sessionId: String) : AutomationEvent()
    class SessionResumed(val sessionId: String) : AutomationEvent()
    class SessionStopped(val sessionId: String) : AutomationEvent()

    class TabCreated(tabId: String, val index: Int, val url: String) : AutomationEvent(tabId = tabId)
    class TabLoading(tabId: String, val url: String) : AutomationEvent(tabId = tabId)
    class TabReady(tabId: String) : AutomationEvent(tabId = tabId)

    class ScrollPerformed(
        tabId: String,
        val pixels: Int,
        val direction: String = "down"
    ) : AutomationEvent(tabId = tabId)

    class RefreshTriggered(tabId: String, val count: Int) : AutomationEvent(tabId = tabId)
    class ReadingPause(tabId: String, val durationMs: Long) : AutomationEvent(tabId = tabId)
    class TabSwitched(val fromTabId: String, val toTabId: String) : AutomationEvent(tabId = toTabId)
    class StealthInjected(tabId: String, val scriptCount: Int) : AutomationEvent(tabId = tabId)
    class ProxyApplied(tabId: String, val proxyHost: String, val proxyPort: Int) : AutomationEvent(tabId = tabId)
    class SpoofApplied(val model: String, val manufacturer: String) : AutomationEvent()
    class FingerprintRotated(tabId: String, val newModel: String) : AutomationEvent(tabId = tabId)
    class CustomJsExecuted(tabId: String) : AutomationEvent(tabId = tabId)
    class TabError(tabId: String, val error: String) : AutomationEvent(tabId = tabId)
    class TabRecovery(tabId: String, val attempt: Int) : AutomationEvent(tabId = tabId)
    class MemoryWarning(val percentUsed: Int) : AutomationEvent()
    class Info(val message: String) : AutomationEvent()

    fun describe(): String = when (this) {
        is SessionStarted -> "Session started: $tabCount tabs \u2192 $url"
        is SessionPaused -> "Session paused"
        is SessionResumed -> "Session resumed"
        is SessionStopped -> "Session stopped"
        is TabCreated -> "Tab ${index + 1} created"
        is TabLoading -> "Tab loading: ${url.take(40)}"
        is TabReady -> "Tab ready"
        is ScrollPerformed -> "Scrolled ${pixels}px $direction"
        is RefreshTriggered -> "Page refreshed (#$count)"
        is ReadingPause -> "Reading pause ${durationMs / 1000}s"
        is TabSwitched -> "Switched to tab"
        is StealthInjected -> "$scriptCount stealth scripts injected"
        is ProxyApplied -> "Proxy applied: $proxyHost:$proxyPort"
        is SpoofApplied -> "Device spoofed: $manufacturer $model"
        is FingerprintRotated -> "Fingerprint rotated \u2192 $newModel"
        is CustomJsExecuted -> "Custom JS executed"
        is TabError -> "Error: $error"
        is TabRecovery -> "Recovery attempt #$attempt"
        is MemoryWarning -> "Memory warning: $percentUsed% used"
        is Info -> message
    }

    fun shortLabel(): String = when (this) {
        is ScrollPerformed -> "Scroll"
        is RefreshTriggered -> "Refresh"
        is ReadingPause -> "Pause"
        is TabSwitched -> "Switch"
        is StealthInjected -> "Stealth"
        is ProxyApplied -> "Proxy"
        is SpoofApplied, is FingerprintRotated -> "Spoof"
        is TabError -> "Error"
        is TabRecovery -> "Recovery"
        is MemoryWarning -> "Memory"
        is CustomJsExecuted -> "JS"
        else -> "Info"
    }
}
