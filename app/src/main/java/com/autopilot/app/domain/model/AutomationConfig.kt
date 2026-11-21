package com.autopilot.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class AutomationConfig(
    val tabCount: Int = 5,
    val refreshIntervalSec: Int = 30,
    val scrollIntervalSec: Int = 10,
    val mode: AutomationMode = AutomationMode.SIMPLE_SCROLL,
    val enableSpoofing: Boolean = false,
    val randomizeIntervals: Boolean = true,
    val enableProxy: Boolean = false,
    val customJs: String = "",
    val autoPauseOnLowMemory: Boolean = true,
    val urls: List<String> = emptyList()
)
