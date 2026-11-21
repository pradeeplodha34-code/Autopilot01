package com.autopilot.app.automation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.autopilot.app.AutoPilotApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            val pendingResult = goAsync()
            val app = context.applicationContext as? AutoPilotApplication
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                try {
                    val saved = app?.container?.repository?.settingsRepo?.getSavedActiveSession()
                    if (saved != null && saved.shouldResume) {
                        val req = OneTimeWorkRequestBuilder<SessionResumeWorker>().build()
                        WorkManager.getInstance(context.applicationContext)
                            .enqueueUniqueWork(
                                "autopilot_session_resume",
                                ExistingWorkPolicy.REPLACE,
                                req
                            )
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
