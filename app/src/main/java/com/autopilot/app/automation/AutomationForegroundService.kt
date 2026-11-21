package com.autopilot.app.automation

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.autopilot.app.AutoPilotApplication
import com.autopilot.app.MainActivity
import com.autopilot.app.R
import com.autopilot.app.domain.model.AutomationConfig
import com.autopilot.app.domain.model.AutomationMode
import com.autopilot.app.domain.model.SessionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AutomationForegroundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var observerJob: Job? = null

    private val engine: AutomationEngine
        get() = (application as AutoPilotApplication).container.automationEngine

    private val repository
        get() = (application as AutoPilotApplication).container.repository

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == null) {
            startForegroundCompat(buildNotification("AutoPilot", "Recovering session...", isPaused = false))
            serviceScope.launch {
                val resumed = engine.tryResumeFromSaved()
                if (!resumed) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                } else {
                    observeEngineForNotification()
                }
            }
            return START_STICKY
        }

        when (action) {
            ACTION_START -> {
                val sessionId = intent.getStringExtra(EXTRA_SESSION_ID) ?: return START_NOT_STICKY
                val url = intent.getStringExtra(EXTRA_URL) ?: return START_NOT_STICKY
                val tabCount = intent.getIntExtra(EXTRA_TAB_COUNT, 5)
                val refreshSec = intent.getIntExtra(EXTRA_REFRESH_SEC, 30)
                val scrollSec = intent.getIntExtra(EXTRA_SCROLL_SEC, 10)
                val mode = AutomationMode.fromString(intent.getStringExtra(EXTRA_MODE).orEmpty())
                val randomize = intent.getBooleanExtra(EXTRA_RANDOMIZE, true)
                val spoofing = intent.getBooleanExtra(EXTRA_SPOOFING, false)
                val proxyEnabled = intent.getBooleanExtra(EXTRA_PROXY, false)
                val customJs = intent.getStringExtra(EXTRA_CUSTOM_JS).orEmpty()
                val autoPauseLowMem = intent.getBooleanExtra(EXTRA_AUTO_PAUSE_LOW_MEMORY, true)
                val extraUrls = intent.getStringExtra(EXTRA_URLS_MULTI).orEmpty()
                    .split("||")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }

                val config = AutomationConfig(
                    tabCount = tabCount,
                    refreshIntervalSec = refreshSec,
                    scrollIntervalSec = scrollSec,
                    mode = mode,
                    enableSpoofing = spoofing,
                    randomizeIntervals = randomize,
                    enableProxy = proxyEnabled,
                    customJs = customJs,
                    autoPauseOnLowMemory = autoPauseLowMem,
                    urls = extraUrls
                )

                startForegroundCompat(buildNotification("AutoPilot Running", "Starting \u2022 $url", isPaused = false))
                serviceScope.launch {
                    val activeFpId = repository.settingsRepo.activeFingerprintIdFlow.first()
                    val fp = repository.getAllFingerprintsOnce().firstOrNull { it.id == activeFpId }
                        ?: repository.getAllFingerprintsOnce().firstOrNull()
                    val activeProxyKey = repository.settingsRepo.activeProxyKeyFlow.first()
                    val preferredCountry = repository.settingsRepo.settingsFlow.first().preferredProxyCountry
                    val preferredCode = CountryRegionCatalog.resolveCountryCode(preferredCountry)
                    val proxies = repository.getAllProxiesOnce()
                    val proxy = proxies.firstOrNull { "${it.host}:${it.port}" == activeProxyKey }
                        ?: if (preferredCode.isNotEmpty() && !preferredCountry.equals("Any", ignoreCase = true)) {
                            proxies.firstOrNull {
                                it.isAlive && CountryRegionCatalog.resolveCountryCode(it.country)
                                    .equals(preferredCode, ignoreCase = true)
                            } ?: proxies.firstOrNull { it.isAlive }
                        } else {
                            proxies.firstOrNull { it.isAlive }
                        }
                    engine.start(config, sessionId, url, fp, proxy)
                    observeEngineForNotification()
                }
            }

            ACTION_PAUSE -> {
                engine.pause()
                updateNotification("AutoPilot Paused", "Automation is paused", isPaused = true)
            }

            ACTION_RESUME -> {
                val sessionId = intent.getStringExtra(EXTRA_SESSION_ID)
                val url = intent.getStringExtra(EXTRA_URL)
                engine.resume(sessionId, url)
                updateNotification("AutoPilot Running", "Automation resumed", isPaused = false)
                observeEngineForNotification()
            }

            ACTION_ADD_TAB -> {
                val url = intent.getStringExtra(EXTRA_URL)
                engine.addTab(url)
            }

            ACTION_REMOVE_TAB -> {
                engine.removeTab()
            }

            ACTION_STOP -> {
                observerJob?.cancel()
                engine.stop()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun observeEngineForNotification() {
        observerJob?.cancel()
        observerJob = serviceScope.launch {
            engine.engineState.collect { state ->
                when (state.status) {
                    SessionStatus.RUNNING -> updateNotification(
                        "AutoPilot Running",
                        "${state.activeTabs}/${state.totalTabs} tabs \u2022 ${state.runtime} \u2022 ${state.url}",
                        isPaused = false
                    )
                    SessionStatus.PAUSED -> updateNotification(
                        "AutoPilot Paused",
                        "${state.totalTabs} tabs \u2022 ${state.runtime} \u2022 ${state.url}",
                        isPaused = true
                    )
                    SessionStatus.STOPPED, SessionStatus.ERROR -> {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
                }
            }
        }
    }

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(title: String, content: String, isPaused: Boolean) {
        val nm = getSystemService(NotificationManager::class.java)
        nm?.notify(NOTIFICATION_ID, buildNotification(title, content, isPaused))
    }

    private fun buildNotification(title: String, content: String, isPaused: Boolean): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleAction = if (isPaused) {
            NotificationCompat.Action(
                0,
                "Resume",
                servicePendingIntent(ACTION_RESUME)
            )
        } else {
            NotificationCompat.Action(
                0,
                "Pause",
                servicePendingIntent(ACTION_PAUSE)
            )
        }

        val stopAction = NotificationCompat.Action(
            0,
            "Stop",
            servicePendingIntent(ACTION_STOP)
        )

        return NotificationCompat.Builder(this, CHANNEL_AUTOMATION)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_stat_autopilot)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openAppIntent)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(toggleAction)
            .addAction(stopAction)
            .build()
    }

    private fun servicePendingIntent(action: String): PendingIntent {
        val intent = Intent(this, AutomationForegroundService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            this,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    override fun onDestroy() {
        observerJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_AUTOMATION = "autopilot_automation"
        const val CHANNEL_STATUS = "autopilot_status"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.autopilot.app.ACTION_START"
        const val ACTION_PAUSE = "com.autopilot.app.ACTION_PAUSE"
        const val ACTION_RESUME = "com.autopilot.app.ACTION_RESUME"
        const val ACTION_STOP = "com.autopilot.app.ACTION_STOP"
        const val ACTION_ADD_TAB = "com.autopilot.app.ACTION_ADD_TAB"
        const val ACTION_REMOVE_TAB = "com.autopilot.app.ACTION_REMOVE_TAB"

        const val EXTRA_SESSION_ID = "session_id"
        const val EXTRA_URL = "url"
        const val EXTRA_TAB_COUNT = "tab_count"
        const val EXTRA_REFRESH_SEC = "refresh_sec"
        const val EXTRA_SCROLL_SEC = "scroll_sec"
        const val EXTRA_MODE = "mode"
        const val EXTRA_RANDOMIZE = "randomize"
        const val EXTRA_SPOOFING = "spoofing"
        const val EXTRA_PROXY = "proxy"
        const val EXTRA_CUSTOM_JS = "custom_js"
        const val EXTRA_AUTO_PAUSE_LOW_MEMORY = "auto_pause_low_memory"
        const val EXTRA_URLS_MULTI = "urls_multi"

        fun startSession(
            context: Context,
            sessionId: String,
            url: String,
            config: AutomationConfig
        ) {
            val intent = Intent(context, AutomationForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_SESSION_ID, sessionId)
                putExtra(EXTRA_URL, url)
                putExtra(EXTRA_TAB_COUNT, config.tabCount)
                putExtra(EXTRA_REFRESH_SEC, config.refreshIntervalSec)
                putExtra(EXTRA_SCROLL_SEC, config.scrollIntervalSec)
                putExtra(EXTRA_MODE, config.mode.name)
                putExtra(EXTRA_RANDOMIZE, config.randomizeIntervals)
                putExtra(EXTRA_SPOOFING, config.enableSpoofing)
                putExtra(EXTRA_PROXY, config.enableProxy)
                putExtra(EXTRA_CUSTOM_JS, config.customJs)
                putExtra(EXTRA_AUTO_PAUSE_LOW_MEMORY, config.autoPauseOnLowMemory)
                putExtra(EXTRA_URLS_MULTI, config.urls.joinToString("||"))
            }
            context.startForegroundService(intent)
        }

        fun sendCommand(context: Context, action: String, url: String? = null) {
            val intent = Intent(context, AutomationForegroundService::class.java).apply {
                this.action = action
                if (url != null) putExtra(EXTRA_URL, url)
            }
            context.startService(intent)
        }
    }
}
