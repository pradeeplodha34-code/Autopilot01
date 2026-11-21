package com.autopilot.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.autopilot.app.automation.AutomationForegroundService
import com.autopilot.app.automation.ScheduleWorker
import com.autopilot.app.automation.SessionSyncWorker
import com.autopilot.app.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class AutoPilotApplication : Application() {

    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        createNotificationChannels()

        appScope.launch {
            container.repository.ensureSeededFingerprints()
        }

        scheduleBackgroundWorkers()
    }

    private fun createNotificationChannels() {
        val nm = getSystemService(NotificationManager::class.java) ?: return
        val automationChannel = NotificationChannel(
            AutomationForegroundService.CHANNEL_AUTOMATION,
            getString(R.string.notification_channel_automation),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_channel_automation_desc)
            setShowBadge(false)
        }

        val statusChannel = NotificationChannel(
            AutomationForegroundService.CHANNEL_STATUS,
            getString(R.string.notification_channel_status),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = getString(R.string.notification_channel_status_desc)
        }

        nm.createNotificationChannels(listOf(automationChannel, statusChannel))
    }

    private fun scheduleBackgroundWorkers() {
        try {
            val wm = WorkManager.getInstance(this)
            val syncRequest = PeriodicWorkRequestBuilder<SessionSyncWorker>(
                15, TimeUnit.MINUTES
            ).build()
            wm.enqueueUniquePeriodicWork(
                "autopilot_session_sync",
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )

            val scheduleRequest = PeriodicWorkRequestBuilder<ScheduleWorker>(
                15, TimeUnit.MINUTES
            ).build()
            wm.enqueueUniquePeriodicWork(
                "autopilot_schedule_check",
                ExistingPeriodicWorkPolicy.KEEP,
                scheduleRequest
            )
        } catch (e: Exception) {
            Log.w("AutoPilotApp", "WorkManager scheduling failed (non-fatal)", e)
        }
    }
}
