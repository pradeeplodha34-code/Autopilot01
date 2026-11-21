package com.autopilot.app.automation

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import com.autopilot.app.data.local.AutoPilotRepository
import com.autopilot.app.data.local.ConfigCodec
import com.autopilot.app.data.local.SessionHistoryEntity
import com.autopilot.app.domain.model.AutomationConfig
import com.autopilot.app.domain.model.AutomationEvent
import com.autopilot.app.domain.model.AutomationMode
import com.autopilot.app.domain.model.DeviceFingerprint
import com.autopilot.app.domain.model.EngineState
import com.autopilot.app.domain.model.ProxyInfo
import com.autopilot.app.domain.model.SessionStatus
import com.autopilot.app.domain.model.TabRealTimeState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.LinkedHashMap
import java.util.UUID
import kotlin.random.Random

class AutomationEngine(
    private val context: Context,
    private val repository: AutoPilotRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _engineState = MutableStateFlow(EngineState())
    val engineState: StateFlow<EngineState> = _engineState.asStateFlow()

    private val _events = MutableStateFlow<List<AutomationEvent>>(emptyList())
    val events: StateFlow<List<AutomationEvent>> = _events.asStateFlow()

    private val _activeTabId = MutableStateFlow<String?>(null)
    val activeTabId: StateFlow<String?> = _activeTabId.asStateFlow()

    private val webViews = LinkedHashMap<String, WebView>()
    private val tabJobs = LinkedHashMap<String, Job>()
    private var tickerJob: Job? = null
    private var tabSwitcherJob: Job? = null

    private var previewContainer: FrameLayout? = null

    @Volatile
    var currentConfig: AutomationConfig? = null
        private set

    @Volatile
    private var activeFingerprint: DeviceFingerprint? = null

    @Volatile
    private var activeProxy: ProxyInfo? = null

    @Volatile
    private var lastProxyRotationAt: Long = 0L

    private val recoveryAttempts = mutableMapOf<String, Int>()

    fun start(
        config: AutomationConfig,
        sessionId: String,
        url: String,
        fingerprint: DeviceFingerprint?,
        proxy: ProxyInfo?
    ) {
        val normalizedUrl = normalizeUrl(url)
        stopInternal(saveHistory = false)

        currentConfig = config
        activeFingerprint = if (config.enableSpoofing) fingerprint else null
        activeProxy = if (config.enableProxy) proxy else null

        val proxyApplied = if (config.enableProxy && proxy != null) {
            ProxyHealthChecker.applyWebViewProxy(proxy)
        } else {
            ProxyHealthChecker.applyWebViewProxy(null)
            false
        }
        if (config.enableProxy && proxy != null && !proxyApplied) {
            emitEvent(AutomationEvent.Info("WebView ProxyOverride unsupported on this WebView — continuing direct"))
            activeProxy = null
        }

        val now = System.currentTimeMillis()
        _engineState.value = EngineState(
            sessionId = sessionId,
            status = SessionStatus.RUNNING,
            url = normalizedUrl,
            totalTabs = config.tabCount,
            activeTabs = 0,
            totalRefreshes = 0,
            totalScrolls = 0,
            startedAt = now,
            currentActivity = "Initializing ${config.tabCount} tabs...",
            tabStates = emptyMap(),
            proxyActive = config.enableProxy && activeProxy != null,
            proxyHost = if (config.enableProxy && activeProxy != null) "${activeProxy!!.host}:${activeProxy!!.port}" else "",
            proxyCountry = if (config.enableProxy && activeProxy != null) activeProxy!!.country else "",
            spoofingActive = config.enableSpoofing && fingerprint != null,
            spoofedModel = if (config.enableSpoofing && fingerprint != null) {
                "${fingerprint.buildManufacturer} ${fingerprint.buildModel}"
            } else {
                ""
            },
            memoryPercent = readMemoryPercent(),
            runtimeSeconds = 0L
        )

        emitEvent(AutomationEvent.SessionStarted(sessionId, normalizedUrl, config.tabCount))
        if (config.enableSpoofing && fingerprint != null) {
            emitEvent(AutomationEvent.SpoofApplied(fingerprint.buildModel, fingerprint.buildManufacturer))
        }
        if (config.enableProxy && proxy != null) {
            emitEvent(AutomationEvent.ProxyApplied("session", proxy.host, proxy.port))
        }

        scope.launch {
            repository.settingsRepo.saveActiveSessionState(
                sessionId = sessionId,
                url = normalizedUrl,
                config = config,
                shouldResume = true
            )
        }

        startTicker()
        if (config.mode == AutomationMode.TAB_SWITCHING) {
            startTabSwitcher(config)
        }

        val allUrls = (listOf(normalizedUrl) + config.urls.map { normalizeUrl(it) })
            .filter { it.isNotBlank() }
            .distinct()

        scope.launch {
            for (index in 0 until config.tabCount) {
                val tabUrl = allUrls[index % allUrls.size]
                createTabInternal(sessionId, index, tabUrl, config)
                delay(250L)
            }
        }
    }

    fun pause() {
        if (_engineState.value.status != SessionStatus.RUNNING) return
        _engineState.update { state ->
            state.copy(
                status = SessionStatus.PAUSED,
                currentActivity = "Paused by user",
                tabStates = state.tabStates.mapValues { (_, tab) ->
                    tab.copy(status = SessionStatus.PAUSED, lastAction = "Paused")
                }
            )
        }
        emitEvent(AutomationEvent.SessionPaused(_engineState.value.sessionId))
        scope.launch {
            val cfg = currentConfig ?: return@launch
            repository.settingsRepo.saveActiveSessionState(
                sessionId = _engineState.value.sessionId,
                url = _engineState.value.url,
                config = cfg,
                shouldResume = false
            )
        }
    }

    fun resume(sessionId: String? = null, url: String? = null) {
        val state = _engineState.value
        if (state.status == SessionStatus.RUNNING) return
        if (state.tabStates.isEmpty() && sessionId != null && url != null && currentConfig != null) {
            start(currentConfig!!, sessionId, url, activeFingerprint, activeProxy)
            return
        }
        _engineState.update { current ->
            current.copy(
                status = SessionStatus.RUNNING,
                currentActivity = "Automation running",
                tabStates = current.tabStates.mapValues { (_, tab) ->
                    tab.copy(status = SessionStatus.RUNNING, lastAction = "Resumed")
                }
            )
        }
        emitEvent(AutomationEvent.SessionResumed(state.sessionId))
        scope.launch {
            val cfg = currentConfig ?: return@launch
            repository.settingsRepo.saveActiveSessionState(
                sessionId = _engineState.value.sessionId,
                url = _engineState.value.url,
                config = cfg,
                shouldResume = true
            )
        }
    }

    fun stop() {
        stopInternal(saveHistory = true)
    }

    fun addTab(url: String? = null) {
        val cfg = currentConfig ?: return
        val state = _engineState.value
        if (state.status == SessionStatus.STOPPED || state.totalTabs >= 12) return
        val nextIndex = (state.tabStates.values.maxOfOrNull { it.index } ?: -1) + 1
        val targetUrl = normalizeUrl(url ?: state.url)
        scope.launch {
            createTabInternal(state.sessionId, nextIndex, targetUrl, cfg)
        }
    }

    fun removeTab() {
        val state = _engineState.value
        if (state.tabStates.size <= 1) return
        val lastTabId = state.tabStates.values.maxByOrNull { it.index }?.tabId ?: return
        tabJobs.remove(lastTabId)?.cancel()
        scope.launch(Dispatchers.Main) {
            val wv = webViews.remove(lastTabId)
            detachWebViewFromParent(wv)
            wv?.destroy()
            if (_activeTabId.value == lastTabId) {
                val nextActive = webViews.keys.firstOrNull()
                _activeTabId.value = nextActive
                attachActivePreviewOnMain()
            }
        }
        _engineState.update { current ->
            val updated = current.tabStates.toMutableMap().apply { remove(lastTabId) }
            current.copy(
                totalTabs = updated.size,
                activeTabs = updated.count { it.value.status == SessionStatus.RUNNING },
                tabStates = updated
            )
        }
        emitEvent(AutomationEvent.Info("Removed tab ${state.tabStates.size}"))
    }

    fun selectActiveTab(tabId: String) {
        if (!webViews.containsKey(tabId)) return
        val previous = _activeTabId.value.orEmpty()
        _activeTabId.value = tabId
        scope.launch(Dispatchers.Main) {
            attachActivePreviewOnMain()
        }
        if (previous.isNotEmpty() && previous != tabId) {
            emitEvent(AutomationEvent.TabSwitched(previous, tabId))
        }
    }

    fun rotateFingerprint(newFingerprint: DeviceFingerprint) {
        activeFingerprint = newFingerprint
        _engineState.update { it.copy(spoofedModel = "${newFingerprint.buildManufacturer} ${newFingerprint.buildModel}") }
        val payload = StealthScriptGenerator.build(newFingerprint)
        scope.launch(Dispatchers.Main) {
            webViews.forEach { (tabId, wv) ->
                wv.settings.userAgentString = newFingerprint.userAgent
                wv.evaluateJavascript("window.__autopilotStealthApplied = false; ${payload.script}", null)
                emitEvent(AutomationEvent.FingerprintRotated(tabId, "${newFingerprint.buildManufacturer} ${newFingerprint.buildModel}"))
            }
        }
    }

    fun attachPreview(container: FrameLayout) {
        previewContainer = container
        attachActivePreviewOnMain()
    }

    fun detachPreview(container: FrameLayout) {
        if (previewContainer === container) {
            container.removeAllViews()
            previewContainer = null
        }
    }

    fun clearEvents() {
        _events.value = emptyList()
    }

    suspend fun tryResumeFromSaved(): Boolean {
        val saved = repository.settingsRepo.getSavedActiveSession() ?: return false
        if (!saved.shouldResume) return false
        val fingerprints = repository.getAllFingerprintsOnce()
        val activeId = repository.settingsRepo.activeFingerprintIdFlow.firstOrNull()
        val selectedFp = fingerprints.firstOrNull { it.id == activeId } ?: fingerprints.firstOrNull()
        start(saved.config, saved.sessionId, saved.url, selectedFp, null)
        return true
    }

    private fun attachActivePreviewOnMain() {
        val container = previewContainer ?: return
        val tabId = _activeTabId.value ?: webViews.keys.firstOrNull() ?: return
        val wv = webViews[tabId] ?: return
        if (wv.parent === container) return
        detachWebViewFromParent(wv)
        container.removeAllViews()
        container.addView(
            wv,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
    }

    private fun detachWebViewFromParent(wv: WebView?) {
        val parent = wv?.parent as? ViewGroup ?: return
        parent.removeView(wv)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private suspend fun createTabInternal(
        sessionId: String,
        index: Int,
        url: String,
        config: AutomationConfig
    ) {
        val tabId = "tab_${index + 1}_${UUID.randomUUID().toString().take(4)}"
        val fp = activeFingerprint

        withContext(Dispatchers.Main) {
            val webView = WebView(context.applicationContext).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                settings.cacheMode = WebSettings.LOAD_DEFAULT
                if (fp != null && fp.userAgent.isNotBlank()) {
                    settings.userAgentString = fp.userAgent
                }
                webChromeClient = WebChromeClient()
                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, pageUrl: String?, favicon: Bitmap?) {
                        emitEvent(AutomationEvent.TabLoading(tabId, pageUrl ?: url))
                        updateTabState(tabId) { it.copy(lastAction = "Loading page...") }
                    }

                    override fun onPageFinished(view: WebView?, pageUrl: String?) {
                        recoveryAttempts[tabId] = 0
                        emitEvent(AutomationEvent.TabReady(tabId))
                        updateTabState(tabId) { it.copy(lastAction = "Page ready") }
                        if (config.enableSpoofing && fp != null && view != null) {
                            val payload = StealthScriptGenerator.build(fp)
                            view.evaluateJavascript(payload.script, null)
                            emitEvent(AutomationEvent.StealthInjected(tabId, payload.moduleCount))
                        }
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        if (request?.isForMainFrame == true) {
                            val desc = error?.description?.toString() ?: "Load error"
                            emitEvent(AutomationEvent.TabError(tabId, desc))
                            val attempt = (recoveryAttempts[tabId] ?: 0) + 1
                            recoveryAttempts[tabId] = attempt
                            if (attempt <= 3) {
                                emitEvent(AutomationEvent.TabRecovery(tabId, attempt))
                                updateTabState(tabId) { it.copy(lastAction = "Recovering (#$attempt)") }
                                view?.postDelayed({ view.reload() }, 2000L * attempt)
                            } else {
                                updateTabState(tabId) {
                                    it.copy(status = SessionStatus.ERROR, lastAction = "Error: $desc")
                                }
                                if (_engineState.value.proxyActive) {
                                    scope.launch { handleProxyRotationIfNeeded() }
                                }
                            }
                        }
                    }
                }
                loadUrl(url)
            }
            webViews[tabId] = webView
            if (_activeTabId.value == null) {
                _activeTabId.value = tabId
                attachActivePreviewOnMain()
            }
        }

        _engineState.update { state ->
            val updated = state.tabStates.toMutableMap()
            updated[tabId] = TabRealTimeState(
                tabId = tabId,
                index = index,
                status = SessionStatus.RUNNING,
                refreshCount = 0,
                scrollCount = 0,
                lastAction = "Loading $url"
            )
            state.copy(
                totalTabs = updated.size,
                activeTabs = updated.count { it.value.status == SessionStatus.RUNNING },
                tabStates = updated
            )
        }
        emitEvent(AutomationEvent.TabCreated(tabId, index, url))

        tabJobs[tabId] = scope.launch {
            var elapsedSinceRefreshMs = 0L
            while (isActive && _engineState.value.status != SessionStatus.STOPPED) {
                val baseScrollMs = config.scrollIntervalSec.coerceAtLeast(2) * 1000L
                val scrollWait = if (config.randomizeIntervals) {
                    (baseScrollMs * Random.nextDouble(0.75, 1.35)).toLong()
                } else {
                    baseScrollMs
                }
                delay(scrollWait)
                if (_engineState.value.status != SessionStatus.RUNNING) continue

                elapsedSinceRefreshMs += scrollWait

                when (config.mode) {
                    AutomationMode.SIMPLE_SCROLL -> performScroll(tabId, distancePx = Random.nextInt(320, 720))
                    AutomationMode.DEEP_SCROLL -> {
                        val direction = if (Random.nextFloat() < 0.8f) "down" else "up"
                        val px = if (direction == "down") Random.nextInt(450, 950) else -Random.nextInt(180, 420)
                        performScroll(tabId, distancePx = px, direction = direction)
                        if (Random.nextFloat() < 0.35f) {
                            val pauseMs = Random.nextLong(1500L, 3500L)
                            emitEvent(AutomationEvent.ReadingPause(tabId, pauseMs))
                            updateTabState(tabId) { it.copy(lastAction = "Reading pause") }
                            delay(pauseMs)
                        }
                    }
                    AutomationMode.TAB_SWITCHING -> {
                        if (_activeTabId.value == tabId) {
                            performScroll(tabId, distancePx = Random.nextInt(300, 680))
                        }
                    }
                    AutomationMode.CUSTOM_JS -> {
                        if (config.customJs.isNotBlank()) {
                            withContext(Dispatchers.Main) {
                                webViews[tabId]?.evaluateJavascript(config.customJs, null)
                            }
                            emitEvent(AutomationEvent.CustomJsExecuted(tabId))
                            updateTabState(tabId) { it.copy(lastAction = "Executed custom JS") }
                        } else {
                            performScroll(tabId, distancePx = Random.nextInt(300, 600))
                        }
                    }
                }

                val baseRefreshMs = config.refreshIntervalSec.coerceAtLeast(5) * 1000L
                if (elapsedSinceRefreshMs >= baseRefreshMs) {
                    elapsedSinceRefreshMs = 0L
                    withContext(Dispatchers.Main) {
                        webViews[tabId]?.reload()
                    }
                    val newRefreshCount = (_engineState.value.tabStates[tabId]?.refreshCount ?: 0) + 1
                    updateTabState(tabId) {
                        it.copy(
                            refreshCount = newRefreshCount,
                            lastAction = "Refreshed (#$newRefreshCount)",
                            lastActionTime = System.currentTimeMillis()
                        )
                    }
                    _engineState.update {
                        it.copy(
                            totalRefreshes = it.totalRefreshes + 1,
                            currentActivity = "Tab ${index + 1} refreshed"
                        )
                    }
                    emitEvent(AutomationEvent.RefreshTriggered(tabId, newRefreshCount))
                }
            }
        }
    }

    private suspend fun performScroll(tabId: String, distancePx: Int, direction: String = "down") {
        withContext(Dispatchers.Main) {
            webViews[tabId]?.evaluateJavascript(
                "window.scrollBy({ top: $distancePx, behavior: 'smooth' });",
                null
            )
        }
        val absPixels = kotlin.math.abs(distancePx)
        val newScrollCount = (_engineState.value.tabStates[tabId]?.scrollCount ?: 0) + 1
        updateTabState(tabId) {
            it.copy(
                scrollCount = newScrollCount,
                lastAction = "Scrolled ${absPixels}px $direction",
                lastActionTime = System.currentTimeMillis()
            )
        }
        _engineState.update {
            it.copy(
                totalScrolls = it.totalScrolls + 1,
                currentActivity = "Scrolling ($direction ${absPixels}px)"
            )
        }
        emitEvent(AutomationEvent.ScrollPerformed(tabId, absPixels, direction))
    }

    private fun startTabSwitcher(config: AutomationConfig) {
        tabSwitcherJob?.cancel()
        tabSwitcherJob = scope.launch {
            val switchPeriodMs = (config.scrollIntervalSec.coerceAtLeast(3) * 1000L)
            while (isActive && _engineState.value.status != SessionStatus.STOPPED) {
                delay(switchPeriodMs)
                if (_engineState.value.status != SessionStatus.RUNNING) continue
                val ids = _engineState.value.tabStates.keys.toList()
                if (ids.size <= 1) continue
                val currentIdx = ids.indexOf(_activeTabId.value)
                val nextId = ids[(currentIdx + 1) % ids.size]
                selectActiveTab(nextId)
            }
        }
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive && _engineState.value.status != SessionStatus.STOPPED) {
                delay(1000L)
                val current = _engineState.value
                val mem = readMemoryPercent()
                if (current.status == SessionStatus.RUNNING) {
                    val cfg = currentConfig
                    if (cfg?.autoPauseOnLowMemory == true && mem >= 88) {
                        emitEvent(AutomationEvent.MemoryWarning(mem))
                        pause()
                        continue
                    }
                    _engineState.update {
                        it.copy(
                            runtimeSeconds = it.runtimeSeconds + 1L,
                            memoryPercent = mem,
                            activeTabs = it.tabStates.count { entry -> entry.value.status == SessionStatus.RUNNING }
                        )
                    }
                } else {
                    _engineState.update { it.copy(memoryPercent = mem) }
                }
            }
        }
    }

    private fun stopInternal(saveHistory: Boolean) {
        val snapshot = _engineState.value
        val cfg = currentConfig
        if (saveHistory && snapshot.sessionId.isNotBlank() && snapshot.status != SessionStatus.STOPPED) {
            scope.launch {
                repository.saveSessionHistory(
                    SessionHistoryEntity(
                        id = snapshot.sessionId,
                        url = snapshot.url,
                        mode = (cfg?.mode ?: AutomationMode.SIMPLE_SCROLL).name,
                        status = SessionStatus.STOPPED.name,
                        tabCount = snapshot.totalTabs,
                        totalRefreshes = snapshot.totalRefreshes,
                        totalScrolls = snapshot.totalScrolls,
                        runtimeSeconds = snapshot.runtimeSeconds,
                        spoofedModel = snapshot.spoofedModel,
                        proxyHost = snapshot.proxyHost,
                        startedAt = snapshot.startedAt,
                        endedAt = System.currentTimeMillis(),
                        configJson = ConfigCodec.encode(cfg ?: AutomationConfig())
                    )
                )
                repository.settingsRepo.clearResumeFlag()
            }
            emitEvent(AutomationEvent.SessionStopped(snapshot.sessionId))
        }

        tickerJob?.cancel()
        tickerJob = null
        tabSwitcherJob?.cancel()
        tabSwitcherJob = null
        tabJobs.values.forEach { it.cancel() }
        tabJobs.clear()
        recoveryAttempts.clear()
        ProxyHealthChecker.applyWebViewProxy(null)

        scope.launch(Dispatchers.Main) {
            previewContainer?.removeAllViews()
            webViews.values.forEach { wv ->
                detachWebViewFromParent(wv)
                wv.stopLoading()
                wv.destroy()
            }
            webViews.clear()
            _activeTabId.value = null
        }

        _engineState.value = EngineState(memoryPercent = readMemoryPercent())
    }

    private fun updateTabState(tabId: String, transform: (TabRealTimeState) -> TabRealTimeState) {
        _engineState.update { state ->
            val existing = state.tabStates[tabId] ?: return@update state
            val updated = state.tabStates.toMutableMap()
            updated[tabId] = transform(existing)
            state.copy(tabStates = updated)
        }
    }

    private fun emitEvent(event: AutomationEvent) {
        _events.update { list ->
            (listOf(event) + list).take(250)
        }
    }

    private fun readMemoryPercent(): Int {
        return runCatching {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            am.getMemoryInfo(memInfo)
            val used = (memInfo.totalMem - memInfo.availMem).coerceAtLeast(0L)
            ((used * 100L) / memInfo.totalMem.coerceAtLeast(1L)).toInt().coerceIn(0, 100)
        }.getOrDefault(0)
    }

    private suspend fun handleProxyRotationIfNeeded() {
        val currentProxy = activeProxy ?: return
        val now = System.currentTimeMillis()
        if (now - lastProxyRotationAt < 120_000L) return
        lastProxyRotationAt = now

        emitEvent(AutomationEvent.Info("Repeated load failures — rotating proxy..."))
        val updatedFailures = currentProxy.failureCount + 1
        val markedFailed = currentProxy.copy(
            failureCount = updatedFailures,
            isAlive = updatedFailures < 3,
            lastChecked = now
        )
        repository.upsertProxy(markedFailed)

        val preferredCountry = repository.settingsRepo.settingsFlow.firstOrNull()?.preferredProxyCountry.orEmpty()
        val preferredCode = CountryRegionCatalog.resolveCountryCode(preferredCountry)
        val candidates = repository.getAllProxiesOnce()
            .filter { "${it.host}:${it.port}" != "${currentProxy.host}:${currentProxy.port}" }
            .filter { it.isAlive }
            .sortedBy { it.latencyMs.takeIf { l -> l > 0 } ?: Long.MAX_VALUE }

        val nextProxy = if (preferredCode.isNotEmpty() && !preferredCountry.equals("Any", ignoreCase = true)) {
            candidates.firstOrNull {
                CountryRegionCatalog.resolveCountryCode(it.country).equals(preferredCode, ignoreCase = true)
            } ?: candidates.firstOrNull()
        } else {
            candidates.firstOrNull()
        }

        if (nextProxy != null && ProxyHealthChecker.applyWebViewProxy(nextProxy)) {
            activeProxy = nextProxy
            repository.settingsRepo.setActiveProxyKey("${nextProxy.host}:${nextProxy.port}")
            _engineState.update {
                it.copy(
                    proxyActive = true,
                    proxyHost = "${nextProxy.host}:${nextProxy.port}",
                    proxyCountry = nextProxy.country
                )
            }
            emitEvent(AutomationEvent.ProxyApplied("session", nextProxy.host, nextProxy.port))
        } else {
            activeProxy = null
            ProxyHealthChecker.applyWebViewProxy(null)
            _engineState.update {
                it.copy(proxyActive = false, proxyHost = "", proxyCountry = "")
            }
            emitEvent(AutomationEvent.Info("No healthy proxy found — continuing direct"))
        }
    }

    private fun normalizeUrl(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return "https://example.com"
        return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "https://$trimmed"
        }
    }
}
