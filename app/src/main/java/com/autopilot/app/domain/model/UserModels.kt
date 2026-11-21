package com.autopilot.app.domain.model

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val lastLogin: Long = System.currentTimeMillis()
)

data class UserSettings(
    val defaultTabs: Int = 5,
    val defaultRefreshSec: Int = 30,
    val defaultScrollSec: Int = 10,
    val theme: String = "system",
    val preferredProxyCountry: String = "Any"
)

data class ScheduledTask(
    val id: Long = 0L,
    val title: String,
    val targetUrl: String,
    val mode: AutomationMode,
    val tabCount: Int,
    val intervalMinutes: Int,
    val enabled: Boolean,
    val lastRunAt: Long = 0L,
    val nextRunAt: Long = System.currentTimeMillis()
)
