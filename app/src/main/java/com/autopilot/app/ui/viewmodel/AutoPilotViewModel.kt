package com.autopilot.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.autopilot.app.automation.AutomationEngine
import com.autopilot.app.automation.AutomationForegroundService
import com.autopilot.app.automation.CountryRegionCatalog
import com.autopilot.app.automation.ProxyHealthChecker
import com.autopilot.app.automation.PublicProxyFetcher
import com.autopilot.app.automation.RootEnvironmentDetector
import com.autopilot.app.automation.RootSpoofer
import com.autopilot.app.data.local.AutoPilotRepository
import com.autopilot.app.data.local.ConfigCodec
import com.autopilot.app.data.local.SessionHistoryEntity
import com.autopilot.app.domain.model.AutomationConfig
import com.autopilot.app.domain.model.AutomationEvent
import com.autopilot.app.domain.model.AutomationMode
import com.autopilot.app.domain.model.DeviceFingerprint
import com.autopilot.app.domain.model.EngineState
import com.autopilot.app.domain.model.ExitIpResult
import com.autopilot.app.domain.model.ProxyCountryStats
import com.autopilot.app.domain.model.ProxyFetchSummary
import com.autopilot.app.domain.model.ProxyInfo
import com.autopilot.app.domain.model.ProxyProtocol
import com.autopilot.app.domain.model.RootEnvironment
import com.autopilot.app.domain.model.RootVerificationResult
import com.autopilot.app.domain.model.ScheduledTask
import com.autopilot.app.domain.model.User
import com.autopilot.app.domain.model.UserSettings
import com.autopilot.app.domain.model.WebRtcPolicy
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.random.Random

class AutoPilotViewModel(
    application: Application,
    private val repository: AutoPilotRepository,
    val engine: AutomationEngine
) : AndroidViewModel(application) {

    val engineState: StateFlow<EngineState> = engine.engineState
    val events: StateFlow<List<AutomationEvent>> = engine.events
    val activeTabId: StateFlow<String?> = engine.activeTabId

    val sessionHistory: StateFlow<List<SessionHistoryEntity>> =
        repository.sessionHistoryFlow.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    val proxies: StateFlow<List<ProxyInfo>> =
        repository.proxiesFlow.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    val countryStats: StateFlow<List<ProxyCountryStats>> =
        repository.proxyCountryStatsFlow.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    val fingerprints: StateFlow<List<DeviceFingerprint>> =
        repository.fingerprintsFlow.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    val scheduledTasks: StateFlow<List<ScheduledTask>> =
        repository.scheduledTasksFlow.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    val userSettings: StateFlow<UserSettings> =
        repository.settingsRepo.settingsFlow.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            UserSettings()
        )

    val user: StateFlow<User> =
        repository.settingsRepo.userFlow.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            User()
        )

    val activeFingerprintId: StateFlow<String> =
        repository.settingsRepo.activeFingerprintIdFlow.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            "fp_pixel_9_pro"
        )

    val activeProxyKey: StateFlow<String> =
        repository.settingsRepo.activeProxyKeyFlow.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            ""
        )

    val lastSyncAt: StateFlow<Long> =
        repository.settingsRepo.lastSyncAtFlow.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            0L
        )

    private val _rootEnvironment = MutableStateFlow(RootEnvironment())
    val rootEnvironment: StateFlow<RootEnvironment> = _rootEnvironment.asStateFlow()

    private val _rootVerification = MutableStateFlow(RootVerificationResult())
    val rootVerification: StateFlow<RootVerificationResult> = _rootVerification.asStateFlow()

    private val _isProbingProxies = MutableStateFlow(false)
    val isProbingProxies: StateFlow<Boolean> = _isProbingProxies.asStateFlow()

    private val _isFetchingProxies = MutableStateFlow(false)
    val isFetchingProxies: StateFlow<Boolean> = _isFetchingProxies.asStateFlow()

    private val _proxyFetchSummary = MutableStateFlow<ProxyFetchSummary?>(null)
    val proxyFetchSummary: StateFlow<ProxyFetchSummary?> = _proxyFetchSummary.asStateFlow()

    private val _isCheckingIp = MutableStateFlow(false)
    val isCheckingIp: StateFlow<Boolean> = _isCheckingIp.asStateFlow()

    private val _exitIpResult = MutableStateFlow<ExitIpResult?>(null)
    val exitIpResult: StateFlow<ExitIpResult?> = _exitIpResult.asStateFlow()

    private val _draftUrl = MutableStateFlow("https://example.com")
    val draftUrl: StateFlow<String> = _draftUrl.asStateFlow()

    private val _draftExtraUrlsText = MutableStateFlow("")
    val draftExtraUrlsText: StateFlow<String> = _draftExtraUrlsText.asStateFlow()

    private val _draftConfig = MutableStateFlow(AutomationConfig())
    val draftConfig: StateFlow<AutomationConfig> = _draftConfig.asStateFlow()

    private val _showLivePreview = MutableStateFlow(true)
    val showLivePreview: StateFlow<Boolean> = _showLivePreview.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureSeededFingerprints()
            val defaults = repository.settingsRepo.settingsFlow.first()
            _draftConfig.update {
                it.copy(
                    tabCount = defaults.defaultTabs,
                    refreshIntervalSec = defaults.defaultRefreshSec,
                    scrollIntervalSec = defaults.defaultScrollSec
                )
            }
            val env = RootEnvironmentDetector.detect()
            _rootEnvironment.value = env
            _rootVerification.value = RootSpoofer.readObservedDeviceState(env.isRooted)
        }
    }

    fun updateDraftUrl(url: String) {
        _draftUrl.value = url
    }

    fun updateDraftExtraUrlsText(text: String) {
        _draftExtraUrlsText.value = text
        val parsed = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        _draftConfig.update { it.copy(urls = parsed) }
    }

    fun updateDraftConfig(transform: (AutomationConfig) -> AutomationConfig) {
        _draftConfig.update(transform)
    }

    fun toggleLivePreview() {
        _showLivePreview.value = !_showLivePreview.value
    }

    fun startAutomation() {
        val url = _draftUrl.value.trim().ifBlank { "https://example.com" }
        val sessionId = UUID.randomUUID().toString()
        val cfg = _draftConfig.value
        AutomationForegroundService.startSession(
            context = getApplication(),
            sessionId = sessionId,
            url = url,
            config = cfg
        )
    }

    fun pauseAutomation() {
        AutomationForegroundService.sendCommand(
            getApplication(),
            AutomationForegroundService.ACTION_PAUSE
        )
    }

    fun resumeAutomation() {
        AutomationForegroundService.sendCommand(
            getApplication(),
            AutomationForegroundService.ACTION_RESUME
        )
    }

    fun stopAutomation() {
        AutomationForegroundService.sendCommand(
            getApplication(),
            AutomationForegroundService.ACTION_STOP
        )
    }

    fun addLiveTab() {
        AutomationForegroundService.sendCommand(
            getApplication(),
            AutomationForegroundService.ACTION_ADD_TAB,
            _draftUrl.value.trim().ifBlank { "https://example.com" }
        )
    }

    fun removeLiveTab() {
        AutomationForegroundService.sendCommand(
            getApplication(),
            AutomationForegroundService.ACTION_REMOVE_TAB
        )
    }

    fun selectActiveTab(tabId: String) {
        engine.selectActiveTab(tabId)
    }

    fun loadSessionIntoDraft(entity: SessionHistoryEntity) {
        val decoded = ConfigCodec.decode(entity.configJson)
        _draftUrl.value = entity.url
        _draftExtraUrlsText.value = decoded.urls.joinToString("\n")
        _draftConfig.value = decoded
    }

    fun deleteSessionHistory(id: String) {
        viewModelScope.launch { repository.deleteSessionHistory(id) }
    }

    fun clearSessionHistory() {
        viewModelScope.launch { repository.clearSessionHistory() }
    }

    fun clearEvents() {
        engine.clearEvents()
    }

    // Fingerprint operations
    fun selectFingerprint(id: String) {
        viewModelScope.launch {
            repository.settingsRepo.setActiveFingerprintId(id)
            val fp = fingerprints.value.firstOrNull { it.id == id }
            if (fp != null && engineState.value.status.name != "STOPPED") {
                engine.rotateFingerprint(fp)
            }
        }
    }

    fun rotateFingerprint() {
        val list = fingerprints.value
        if (list.isEmpty()) return
        val currentIdx = list.indexOfFirst { it.id == activeFingerprintId.value }
        val next = list[(currentIdx + 1) % list.size]
        selectFingerprint(next.id)
    }

    fun addCustomFingerprint(
        manufacturer: String,
        model: String,
        androidVersion: Int,
        screenWidth: Int,
        screenHeight: Int,
        cpuCores: Int,
        deviceMemory: Int,
        webrtcPolicy: WebRtcPolicy,
        timezone: String,
        userAgent: String
    ) {
        val cleanModel = model.trim().ifBlank { "Custom Device" }
        val cleanMfg = manufacturer.trim().ifBlank { "Generic" }
        val ua = userAgent.trim().ifBlank {
            "Mozilla/5.0 (Linux; Android $androidVersion; $cleanModel) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Mobile Safari/537.36"
        }
        val fp = DeviceFingerprint(
            id = "fp_${UUID.randomUUID().toString().take(8)}",
            androidId = UUID.randomUUID().toString().replace("-", "").take(16),
            advertisingId = UUID.randomUUID().toString(),
            buildModel = cleanModel,
            buildManufacturer = cleanMfg,
            buildFingerprint = "$cleanMfg/$cleanModel/$cleanModel:$androidVersion/AP3A.241105.008:user/release-keys",
            fingerprintAndroidVersion = androidVersion,
            buildProduct = cleanModel.lowercase().replace(" ", "_"),
            buildDevice = cleanModel.lowercase().replace(" ", "_"),
            buildBoard = "arm64",
            buildBrand = cleanMfg.lowercase(),
            serialNumber = "AP${Random.nextInt(100000, 999999)}",
            macAddress = "02:${(1..5).joinToString(":") { "%02X".format(Random.nextInt(256)) }}",
            bluetoothAddress = "22:22:${(1..4).joinToString(":") { "%02X".format(Random.nextInt(256)) }}",
            widevineId = UUID.randomUUID().toString().replace("-", "").take(20).uppercase(),
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            screenDpi = 440,
            userAgent = ua,
            timezone = timezone.ifBlank { "America/New_York" },
            locale = "en-US",
            glRenderer = "Adreno (TM) 750",
            glVendor = "Qualcomm",
            canvasNoiseSeed = Random.nextLong(100_000_000L, 999_999_999L),
            audioNoiseSeed = Random.nextLong(100_000_000L, 999_999_999L),
            webrtcPolicy = webrtcPolicy,
            platform = "Linux armv8l",
            cpuCores = cpuCores.coerceIn(2, 16),
            deviceMemory = deviceMemory.coerceIn(2, 24),
            maxTouchPoints = 5,
            languages = listOf("en-US", "en")
        )
        viewModelScope.launch {
            repository.upsertFingerprint(fp)
            repository.settingsRepo.setActiveFingerprintId(fp.id)
        }
    }

    fun deleteFingerprint(id: String) {
        viewModelScope.launch { repository.deleteFingerprint(id) }
    }

    // Proxy operations
    fun addProxy(host: String, port: Int, protocol: ProxyProtocol, country: String) {
        val cleanHost = host.trim()
        if (cleanHost.isEmpty() || port !in 1..65535) return
        val resolvedCode = CountryRegionCatalog.resolveCountryCode(country)
        val proxy = ProxyInfo(
            host = cleanHost,
            port = port,
            protocol = protocol,
            country = resolvedCode.ifBlank { country.trim() }
        )
        viewModelScope.launch {
            repository.upsertProxy(proxy)
            val probed = ProxyHealthChecker.probe(proxy)
            repository.upsertProxy(probed)
        }
    }

    fun selectActiveProxy(host: String, port: Int) {
        val key = "$host:$port"
        viewModelScope.launch {
            val next = if (activeProxyKey.value == key) "" else key
            repository.settingsRepo.setActiveProxyKey(next)
        }
    }

    fun deleteProxy(host: String, port: Int) {
        viewModelScope.launch {
            if (activeProxyKey.value == "$host:$port") {
                repository.settingsRepo.setActiveProxyKey("")
            }
            repository.deleteProxy(host, port)
        }
    }

    fun clearAllProxies() {
        viewModelScope.launch {
            repository.settingsRepo.setActiveProxyKey("")
            repository.clearProxies()
            _proxyFetchSummary.value = null
            _exitIpResult.value = null
        }
    }

    fun fetchPublicProxies() {
        if (_isFetchingProxies.value) return
        viewModelScope.launch {
            _isFetchingProxies.value = true
            try {
                val outcome = PublicProxyFetcher.fetchAll()
                _proxyFetchSummary.value = outcome.summary
                if (outcome.proxies.isNotEmpty()) {
                    repository.upsertProxies(outcome.proxies)
                }
            } finally {
                _isFetchingProxies.value = false
            }
        }
    }

    fun checkCurrentExitIp() {
        if (_isCheckingIp.value) return
        viewModelScope.launch {
            _isCheckingIp.value = true
            try {
                val selectedKey = activeProxyKey.value
                val selectedProxy = if (selectedKey.isNotBlank()) {
                    proxies.value.firstOrNull { "${it.host}:${it.port}" == selectedKey }
                } else {
                    null
                }
                _exitIpResult.value = ProxyHealthChecker.verifyExitIp(selectedProxy)
            } finally {
                _isCheckingIp.value = false
            }
        }
    }

    fun probeSingleProxy(proxy: ProxyInfo) {
        viewModelScope.launch {
            val updated = ProxyHealthChecker.probe(proxy)
            repository.upsertProxy(updated)
        }
    }

    fun probeAllProxies() {
        if (_isProbingProxies.value) return
        viewModelScope.launch {
            _isProbingProxies.value = true
            try {
                val all = repository.getAllProxiesOnce()
                all.chunked(10).forEach { batch ->
                    val results = coroutineScope {
                        batch.map { proxy ->
                            async { ProxyHealthChecker.probe(proxy) }
                        }.awaitAll()
                    }
                    repository.upsertProxies(results)
                }
            } finally {
                _isProbingProxies.value = false
            }
        }
    }

    // Schedule operations
    fun addScheduledTask(
        title: String,
        url: String,
        mode: AutomationMode,
        tabCount: Int,
        intervalMinutes: Int
    ) {
        val now = System.currentTimeMillis()
        val task = ScheduledTask(
            title = title.trim().ifBlank { "Scheduled Session" },
            targetUrl = url.trim().ifBlank { "https://example.com" },
            mode = mode,
            tabCount = tabCount.coerceIn(1, 12),
            intervalMinutes = intervalMinutes.coerceAtLeast(15),
            enabled = true,
            lastRunAt = 0L,
            nextRunAt = now + intervalMinutes.coerceAtLeast(15) * 60_000L
        )
        viewModelScope.launch { repository.upsertScheduledTask(task) }
    }

    fun toggleScheduledTask(task: ScheduledTask) {
        viewModelScope.launch {
            repository.upsertScheduledTask(task.copy(enabled = !task.enabled))
        }
    }

    fun deleteScheduledTask(id: Long) {
        viewModelScope.launch { repository.deleteScheduledTask(id) }
    }

    // Settings & diagnostics
    fun updateUserSettings(settings: UserSettings) {
        viewModelScope.launch {
            repository.settingsRepo.updateSettings(settings)
        }
    }

    fun updateUserProfile(name: String, email: String) {
        viewModelScope.launch {
            repository.settingsRepo.updateUser(name, email)
        }
    }

    fun rescanRootEnvironment() {
        viewModelScope.launch {
            val env = RootEnvironmentDetector.detect()
            _rootEnvironment.value = env
            _rootVerification.value = RootSpoofer.readObservedDeviceState(env.isRooted)
        }
    }

    fun applyActiveFingerprintToRootSystem() {
        viewModelScope.launch {
            val env = RootEnvironmentDetector.detect()
            _rootEnvironment.value = env
            val activeFp = fingerprints.value.firstOrNull { it.id == activeFingerprintId.value }
                ?: fingerprints.value.firstOrNull()
            if (activeFp != null) {
                _rootVerification.value = RootSpoofer.applyRootFingerprint(activeFp, env)
            } else {
                _rootVerification.value = RootSpoofer.readObservedDeviceState(env.isRooted)
            }
        }
    }

    fun triggerManualSync() {
        viewModelScope.launch {
            repository.settingsRepo.recordSyncTimestamp()
        }
    }

    class Factory(
        private val application: Application,
        private val repository: AutoPilotRepository,
        private val engine: AutomationEngine
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AutoPilotViewModel(application, repository, engine) as T
        }
    }
}
