package com.autopilot.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AutoPilotDao {

    // Session history
    @Query("SELECT * FROM session_history ORDER BY endedAt DESC LIMIT 100")
    fun observeSessionHistory(): Flow<List<SessionHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSessionHistory(session: SessionHistoryEntity)

    @Query("DELETE FROM session_history WHERE id = :id")
    suspend fun deleteSessionHistory(id: String)

    @Query("DELETE FROM session_history")
    suspend fun clearSessionHistory()

    // Proxies
    @Query("SELECT * FROM proxies ORDER BY isAlive DESC, latencyMs ASC, host ASC")
    fun observeProxies(): Flow<List<ProxyEntity>>

    @Query("SELECT * FROM proxies")
    suspend fun getAllProxiesOnce(): List<ProxyEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProxy(proxy: ProxyEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProxies(proxies: List<ProxyEntity>)

    @Query("DELETE FROM proxies WHERE host = :host AND port = :port")
    suspend fun deleteProxy(host: String, port: Int)

    @Query("DELETE FROM proxies")
    suspend fun clearProxies()

    // Fingerprints
    @Query("SELECT * FROM fingerprints ORDER BY buildManufacturer ASC, buildModel ASC")
    fun observeFingerprints(): Flow<List<FingerprintEntity>>

    @Query("SELECT * FROM fingerprints")
    suspend fun getAllFingerprintsOnce(): List<FingerprintEntity>

    @Query("SELECT COUNT(*) FROM fingerprints")
    suspend fun getFingerprintCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFingerprint(fingerprint: FingerprintEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFingerprints(fingerprints: List<FingerprintEntity>)

    @Query("DELETE FROM fingerprints WHERE id = :id")
    suspend fun deleteFingerprint(id: String)

    // Scheduled tasks
    @Query("SELECT * FROM scheduled_tasks ORDER BY nextRunAt ASC")
    fun observeScheduledTasks(): Flow<List<ScheduledTaskEntity>>

    @Query("SELECT * FROM scheduled_tasks WHERE enabled = 1")
    suspend fun getEnabledScheduledTasksOnce(): List<ScheduledTaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertScheduledTask(task: ScheduledTaskEntity): Long

    @Query("DELETE FROM scheduled_tasks WHERE id = :id")
    suspend fun deleteScheduledTask(id: Long)
}
