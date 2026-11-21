package com.autopilot.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.autopilot.app.domain.model.AutomationConfig
import com.autopilot.app.domain.model.User
import com.autopilot.app.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.autoPilotDataStore: DataStore<Preferences> by preferencesDataStore(name = "autopilot_prefs")

data class SavedActiveSession(
    val sessionId: String,
    val url: String,
    val config: AutomationConfig,
    val shouldResume: Boolean
)

class UserSettingsRepository(private val context: Context) {

    private object Keys {
        val DEFAULT_TABS = intPreferencesKey("default_tabs")
        val DEFAULT_REFRESH_SEC = intPreferencesKey("default_refresh_sec")
        val DEFAULT_SCROLL_SEC = intPreferencesKey("default_scroll_sec")
        val THEME = stringPreferencesKey("theme")
        val PREFERRED_PROXY_COUNTRY = stringPreferencesKey("preferred_proxy_country")

        val ACTIVE_FINGERPRINT_ID = stringPreferencesKey("active_fingerprint_id")
        val ACTIVE_PROXY_KEY = stringPreferencesKey("active_proxy_key")

        val USER_UID = stringPreferencesKey("user_uid")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val USER_CREATED_AT = longPreferencesKey("user_created_at")
        val USER_LAST_LOGIN = longPreferencesKey("user_last_login")

        val LAST_SESSION_ID = stringPreferencesKey("last_session_id")
        val LAST_SESSION_URL = stringPreferencesKey("last_session_url")
        val LAST_SESSION_CONFIG = stringPreferencesKey("last_session_config")
        val LAST_SESSION_SHOULD_RESUME = booleanPreferencesKey("last_session_should_resume")
        val LAST_SYNC_AT = longPreferencesKey("last_sync_at")
    }

    val settingsFlow: Flow<UserSettings> = context.autoPilotDataStore.data.map { prefs ->
        UserSettings(
            defaultTabs = prefs[Keys.DEFAULT_TABS] ?: 5,
            defaultRefreshSec = prefs[Keys.DEFAULT_REFRESH_SEC] ?: 30,
            defaultScrollSec = prefs[Keys.DEFAULT_SCROLL_SEC] ?: 10,
            theme = prefs[Keys.THEME] ?: "system",
            preferredProxyCountry = prefs[Keys.PREFERRED_PROXY_COUNTRY] ?: "Any"
        )
    }

    val userFlow: Flow<User> = context.autoPilotDataStore.data.map { prefs ->
        User(
            uid = prefs[Keys.USER_UID] ?: "local-operator",
            name = prefs[Keys.USER_NAME] ?: "Operator",
            email = prefs[Keys.USER_EMAIL] ?: "operator@autopilot.local",
            photoUrl = "",
            createdAt = prefs[Keys.USER_CREATED_AT] ?: 0L,
            lastLogin = prefs[Keys.USER_LAST_LOGIN] ?: 0L
        )
    }

    val activeFingerprintIdFlow: Flow<String> = context.autoPilotDataStore.data.map { prefs ->
        prefs[Keys.ACTIVE_FINGERPRINT_ID] ?: "fp_pixel_9_pro"
    }

    val activeProxyKeyFlow: Flow<String> = context.autoPilotDataStore.data.map { prefs ->
        prefs[Keys.ACTIVE_PROXY_KEY] ?: ""
    }

    val lastSyncAtFlow: Flow<Long> = context.autoPilotDataStore.data.map { prefs ->
        prefs[Keys.LAST_SYNC_AT] ?: 0L
    }

    suspend fun updateSettings(settings: UserSettings) {
        context.autoPilotDataStore.edit { prefs ->
            prefs[Keys.DEFAULT_TABS] = settings.defaultTabs.coerceIn(1, 12)
            prefs[Keys.DEFAULT_REFRESH_SEC] = settings.defaultRefreshSec.coerceIn(5, 600)
            prefs[Keys.DEFAULT_SCROLL_SEC] = settings.defaultScrollSec.coerceIn(2, 120)
            prefs[Keys.THEME] = settings.theme
            prefs[Keys.PREFERRED_PROXY_COUNTRY] = settings.preferredProxyCountry
        }
    }

    suspend fun updateUser(name: String, email: String) {
        val now = System.currentTimeMillis()
        context.autoPilotDataStore.edit { prefs ->
            if (prefs[Keys.USER_CREATED_AT] == null) {
                prefs[Keys.USER_CREATED_AT] = now
            }
            prefs[Keys.USER_UID] = "local-operator"
            prefs[Keys.USER_NAME] = name.ifBlank { "Operator" }
            prefs[Keys.USER_EMAIL] = email.ifBlank { "operator@autopilot.local" }
            prefs[Keys.USER_LAST_LOGIN] = now
        }
    }

    suspend fun setActiveFingerprintId(id: String) {
        context.autoPilotDataStore.edit { prefs ->
            prefs[Keys.ACTIVE_FINGERPRINT_ID] = id
        }
    }

    suspend fun setActiveProxyKey(key: String) {
        context.autoPilotDataStore.edit { prefs ->
            prefs[Keys.ACTIVE_PROXY_KEY] = key
        }
    }

    suspend fun saveActiveSessionState(
        sessionId: String,
        url: String,
        config: AutomationConfig,
        shouldResume: Boolean
    ) {
        context.autoPilotDataStore.edit { prefs ->
            prefs[Keys.LAST_SESSION_ID] = sessionId
            prefs[Keys.LAST_SESSION_URL] = url
            prefs[Keys.LAST_SESSION_CONFIG] = ConfigCodec.encode(config)
            prefs[Keys.LAST_SESSION_SHOULD_RESUME] = shouldResume
        }
    }

    suspend fun clearResumeFlag() {
        context.autoPilotDataStore.edit { prefs ->
            prefs[Keys.LAST_SESSION_SHOULD_RESUME] = false
        }
    }

    suspend fun getSavedActiveSession(): SavedActiveSession? {
        val prefs = context.autoPilotDataStore.data.first()
        val id = prefs[Keys.LAST_SESSION_ID].orEmpty()
        val url = prefs[Keys.LAST_SESSION_URL].orEmpty()
        val shouldResume = prefs[Keys.LAST_SESSION_SHOULD_RESUME] ?: false
        if (id.isBlank() || url.isBlank()) return null
        val config = prefs[Keys.LAST_SESSION_CONFIG]?.let { ConfigCodec.decode(it) } ?: AutomationConfig()
        return SavedActiveSession(id, url, config, shouldResume)
    }

    suspend fun recordSyncTimestamp() {
        context.autoPilotDataStore.edit { prefs ->
            prefs[Keys.LAST_SYNC_AT] = System.currentTimeMillis()
        }
    }
}
