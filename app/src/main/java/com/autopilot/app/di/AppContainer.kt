package com.autopilot.app.di

import android.content.Context
import com.autopilot.app.automation.AutomationEngine
import com.autopilot.app.data.local.AppDatabase
import com.autopilot.app.data.local.AutoPilotRepository
import com.autopilot.app.data.local.UserSettingsRepository

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val database = AppDatabase.create(appContext)
    private val userSettingsRepository = UserSettingsRepository(appContext)

    val repository = AutoPilotRepository(
        dao = database.autoPilotDao(),
        settingsRepo = userSettingsRepository
    )

    val automationEngine = AutomationEngine(
        context = appContext,
        repository = repository
    )
}
