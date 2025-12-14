package dev.yaul.twocha.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.yaul.twocha.ui.theme.ThemeStyle
import dev.yaul.twocha.ui.theme.isDark
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "twocha_preferences")

@Singleton
class PreferencesManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        private val KEY_CONFIG_JSON = stringPreferencesKey("config_json")
        private val KEY_DARK_MODE = booleanPreferencesKey("dark_mode")
        private val KEY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        private val KEY_AUTO_CONNECT = booleanPreferencesKey("auto_connect")
        private val KEY_AUTO_RECONNECT = booleanPreferencesKey("auto_reconnect")
        private val KEY_LAST_SERVER = stringPreferencesKey("last_server")
        private val KEY_THEME_STYLE = stringPreferencesKey("theme_style")
        private val KEY_SHOW_NOTIFICATIONS = booleanPreferencesKey("show_notifications")
        private val KEY_KEEP_ALIVE_ON_BATTERY = booleanPreferencesKey("keep_alive_on_battery")
        private val KEY_TOTAL_BYTES_RECEIVED = longPreferencesKey("total_bytes_received")
        private val KEY_TOTAL_BYTES_SENT = longPreferencesKey("total_bytes_sent")
    }

    // Dark mode preference
    val darkMode: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[KEY_DARK_MODE] ?: true // Default to dark mode
        }

    suspend fun setDarkMode(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_DARK_MODE] = enabled
            preferences[KEY_THEME_STYLE] = if (enabled) ThemeStyle.CYBER.name else ThemeStyle.LIGHT.name
        }
    }

    // Dynamic color preference
    val dynamicColor: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[KEY_DYNAMIC_COLOR] ?: false
        }

    suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_DYNAMIC_COLOR] = enabled
        }
    }

    // Theme style preference
    val themeStyle: Flow<ThemeStyle> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val stored = preferences[KEY_THEME_STYLE]
            stored?.let { runCatching { ThemeStyle.valueOf(it) }.getOrNull() } ?: ThemeStyle.CYBER
        }

    suspend fun setThemeStyle(style: ThemeStyle) {
        dataStore.edit { preferences ->
            preferences[KEY_THEME_STYLE] = style.name
            preferences[KEY_DARK_MODE] = style.isDark()
        }
    }

    // Auto-connect preference
    val autoConnect: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[KEY_AUTO_CONNECT] ?: false
        }

    suspend fun setAutoConnect(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_AUTO_CONNECT] = enabled
        }
    }

    // Auto-reconnect preference
    val autoReconnect: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[KEY_AUTO_RECONNECT] ?: true
        }

    suspend fun setAutoReconnect(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_AUTO_RECONNECT] = enabled
        }
    }

    // Configuration JSON
    suspend fun getConfigJson(): String? {
        return dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[KEY_CONFIG_JSON]
            }
            .firstOrNull()
    }

    suspend fun saveConfigJson(json: String) {
        dataStore.edit { preferences ->
            preferences[KEY_CONFIG_JSON] = json
        }
    }

    // Last server
    val lastServer: Flow<String?> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[KEY_LAST_SERVER]
        }

    suspend fun setLastServer(server: String) {
        dataStore.edit { preferences ->
            preferences[KEY_LAST_SERVER] = server
        }
    }

    // Show notifications preference
    val showNotifications: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[KEY_SHOW_NOTIFICATIONS] ?: true
        }

    suspend fun setShowNotifications(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_SHOW_NOTIFICATIONS] = enabled
        }
    }

    // Keep alive on battery preference
    val keepAliveOnBattery: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[KEY_KEEP_ALIVE_ON_BATTERY] ?: true
        }

    suspend fun setKeepAliveOnBattery(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_KEEP_ALIVE_ON_BATTERY] = enabled
        }
    }

    // Total traffic statistics
    val totalBytesReceived: Flow<Long> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[KEY_TOTAL_BYTES_RECEIVED] ?: 0L
        }

    val totalBytesSent: Flow<Long> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[KEY_TOTAL_BYTES_SENT] ?: 0L
        }

    suspend fun addTrafficStats(bytesReceived: Long, bytesSent: Long) {
        dataStore.edit { preferences ->
            val currentReceived = preferences[KEY_TOTAL_BYTES_RECEIVED] ?: 0L
            val currentSent = preferences[KEY_TOTAL_BYTES_SENT] ?: 0L
            preferences[KEY_TOTAL_BYTES_RECEIVED] = currentReceived + bytesReceived
            preferences[KEY_TOTAL_BYTES_SENT] = currentSent + bytesSent
        }
    }

    suspend fun resetTrafficStats() {
        dataStore.edit { preferences ->
            preferences[KEY_TOTAL_BYTES_RECEIVED] = 0L
            preferences[KEY_TOTAL_BYTES_SENT] = 0L
        }
    }

    // Clear all preferences
    suspend fun clearAll() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}