package com.autopilot.app.automation

import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.autopilot.app.AutoPilotApplication
import com.autopilot.app.domain.model.SessionStatus

class SessionResumeWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? AutoPilotApplication ?: return Result.success()
        val engine = app.container.automationEngine

        if (engine.engineState.value.status == SessionStatus.RUNNING) {
            return Result.success()
        }

        return try {
            applicationContext.startForegroundService(
                Intent(applicationContext, AutomationForegroundService::class.java)
            )
            Result.success()
        } catch (_: Exception) {
            if (engine.tryResumeFromSaved()) Result.success() else Result.retry()
        }
    }
}
