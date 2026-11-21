package com.autopilot.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class AutomationMode(val label: String, val description: String) {
    SIMPLE_SCROLL("Simple Scroll + Refresh", "Basic page scrolling with periodic refresh"),
    DEEP_SCROLL("Deep Scroll", "Full page traversal with human-like interactions"),
    TAB_SWITCHING("Tab Switching", "Simulate multi-tab browsing behavior"),
    CUSTOM_JS("Custom JavaScript", "Execute user-defined scripts at intervals");

    companion object {
        fun fromString(value: String): AutomationMode =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: SIMPLE_SCROLL
    }
}

@Serializable
enum class SessionStatus(val label: String) {
    RUNNING("Running"),
    PAUSED("Paused"),
    STOPPED("Stopped"),
    ERROR("Error");

    companion object {
        fun fromString(value: String): SessionStatus =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: STOPPED
    }
}

@Serializable
enum class CommandType {
    PAUSE,
    RESUME,
    STOP,
    ADD_TAB,
    REMOVE_TAB;

    companion object {
        fun fromString(value: String): CommandType =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: PAUSE
    }
}

@Serializable
enum class ProxyProtocol {
    HTTP,
    HTTPS,
    SOCKS4,
    SOCKS5
}

@Serializable
enum class WebRtcPolicy {
    BLOCK,
    SPOOF,
    ALLOW
}
