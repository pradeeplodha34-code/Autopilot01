package com.autopilot.app.automation

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.autopilot.app.AutoPilotApplication
import com.autopilot.app.domain.model.AutomationConfig
import com.autopilot.app.domain.model.SessionStatus
import java.util.UUID

class ScheduleWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? AutoPilotApplication ?: return Result.success()
        val repo = app.container.repository
        val engine = app.container.automationEngine

        if (engine.engineState.value.status == SessionStatus.RUNNING) {
            return Result.success()
        }

        val now = System.currentTimeMillis()
        val dueTask = repo.getEnabledScheduledTasksOnce()
            .firstOrNull { it.nextRunAt <= now } ?: return Result.success()

        val config = AutomationConfig(
            tabCount = dueTask.tabCount,
            mode = dueTask.mode
        )
        val sessionId = UUID.randomUUID().toString()
        runCatching {
            AutomationForegroundService.startSession(
                context = applicationContext,
                sessionId = sessionId,
                url = dueTask.targetUrl,
                config = config
            )
        }

        repo.upsertScheduledTask(
            dueTask.copy(
                lastRunAt = now,
                nextRunAt = now + (dueTask.intervalMinutes.coerceAtLeast(15) * 60_000L)
            )
        )
        return Result.success()
    }
}
