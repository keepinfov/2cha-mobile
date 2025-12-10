package dev.yaul.twocha.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
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
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        private val KEY_CONFIG_JSON = stringPreferencesKey("config_json")
        private val KEY_DARK_MODE = booleanPreferencesKey("dark_mode")
        private val KEY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        private val KEY_AUTO_CONNECT = booleanPreferencesKey("auto_connect")
        private val KEY_AUTO_RECONNECT = booleanPreferencesKey("auto_reconnect")
        private val KEY_LAST_SERVER = stringPreferencesKey("last_server")
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

    // Clear all preferences
    suspend fun clearAll() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}