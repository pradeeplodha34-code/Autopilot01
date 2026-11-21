package com.autopilot.app.data.local

import com.autopilot.app.domain.model.DeviceFingerprint
import com.autopilot.app.domain.model.ProxyCountryStats
import com.autopilot.app.domain.model.ProxyInfo
import com.autopilot.app.domain.model.ScheduledTask
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AutoPilotRepository(
    private val dao: AutoPilotDao,
    val settingsRepo: UserSettingsRepository
) {
    val sessionHistoryFlow: Flow<List<SessionHistoryEntity>> = dao.observeSessionHistory()

    val proxiesFlow: Flow<List<ProxyInfo>> = dao.observeProxies().map { list ->
        list.map { it.toDomain() }
    }

    val proxyCountryStatsFlow: Flow<List<ProxyCountryStats>> = proxiesFlow.map { proxies ->
        proxies.groupBy { proxy ->
            com.autopilot.app.automation.CountryRegionCatalog.resolveCountryCode(proxy.country)
                .ifBlank { "UNKNOWN" }
        }
            .map { (code, group) ->
                val resolvedName = if (code == "UNKNOWN") {
                    "Unknown"
                } else {
                    com.autopilot.app.automation.CountryRegionCatalog.resolveCountryName(code)
                }
                ProxyCountryStats(
                    countryCode = code,
                    countryName = resolvedName,
                    proxyCount = group.size
                )
            }
            .sortedByDescending { it.proxyCount }
    }

    val fingerprintsFlow: Flow<List<DeviceFingerprint>> = dao.observeFingerprints().map { list ->
        list.map { it.toDomain() }
    }

    val scheduledTasksFlow: Flow<List<ScheduledTask>> = dao.observeScheduledTasks().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun ensureSeededFingerprints() {
        if (dao.getFingerprintCount() == 0) {
            dao.upsertFingerprints(AppDatabase.defaultFingerprintProfiles().map { it.toEntity() })
        }
    }

    suspend fun saveSessionHistory(entity: SessionHistoryEntity) {
        dao.upsertSessionHistory(entity)
    }

    suspend fun deleteSessionHistory(id: String) {
        dao.deleteSessionHistory(id)
    }

    suspend fun clearSessionHistory() {
        dao.clearSessionHistory()
    }

    suspend fun upsertProxy(proxy: ProxyInfo) {
        dao.upsertProxy(proxy.toEntity())
    }

    suspend fun upsertProxies(proxies: List<ProxyInfo>) {
        if (proxies.isNotEmpty()) {
            dao.upsertProxies(proxies.map { it.toEntity() })
        }
    }

    suspend fun getAllProxiesOnce(): List<ProxyInfo> =
        dao.getAllProxiesOnce().map { it.toDomain() }

    suspend fun deleteProxy(host: String, port: Int) {
        dao.deleteProxy(host, port)
    }

    suspend fun clearProxies() {
        dao.clearProxies()
    }

    suspend fun upsertFingerprint(fingerprint: DeviceFingerprint) {
        dao.upsertFingerprint(fingerprint.toEntity())
    }

    suspend fun getAllFingerprintsOnce(): List<DeviceFingerprint> =
        dao.getAllFingerprintsOnce().map { it.toDomain() }

    suspend fun deleteFingerprint(id: String) {
        dao.deleteFingerprint(id)
    }

    suspend fun upsertScheduledTask(task: ScheduledTask): Long =
        dao.upsertScheduledTask(task.toEntity())

    suspend fun getEnabledScheduledTasksOnce(): List<ScheduledTask> =
        dao.getEnabledScheduledTasksOnce().map { it.toDomain() }

    suspend fun deleteScheduledTask(id: Long) {
        dao.deleteScheduledTask(id)
    }
}
