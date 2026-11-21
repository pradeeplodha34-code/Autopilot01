package com.autopilot.app.automation

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.autopilot.app.AutoPilotApplication

class SessionSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? AutoPilotApplication ?: return Result.success()
        val repo = app.container.repository
        repo.settingsRepo.recordSyncTimestamp()
        return Result.success()
    }
}
