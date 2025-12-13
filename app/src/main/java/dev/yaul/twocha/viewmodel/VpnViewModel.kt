package dev.yaul.twocha.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.yaul.twocha.config.ConfigParser
import dev.yaul.twocha.config.VpnConfig
import dev.yaul.twocha.data.PreferencesManager
import dev.yaul.twocha.ui.theme.ThemeStyle
import dev.yaul.twocha.vpn.ConnectionState
import dev.yaul.twocha.vpn.TwochaVpnService
import dev.yaul.twocha.vpn.VpnStats
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class VpnViewModel @Inject constructor(
    application: Application,
    private val preferencesManager: PreferencesManager
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "VpnViewModel"
    }

    private val context: Context = application.applicationContext

    // Connection state from service
    val connectionState: StateFlow<ConnectionState> = TwochaVpnService.connectionState

    // Stats from service
    val stats: StateFlow<VpnStats> = TwochaVpnService.stats

    // Current configuration
    private val _config = MutableStateFlow<VpnConfig?>(null)
    val config: StateFlow<VpnConfig?> = _config.asStateFlow()

    // Config validation errors
    private val _configErrors = MutableStateFlow<List<String>>(emptyList())
    val configErrors: StateFlow<List<String>> = _configErrors.asStateFlow()

    // UI state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Logs with proper LogItem format
    private val _logs = MutableStateFlow<List<LogItem>>(emptyList())
    val logs: StateFlow<List<LogItem>> = _logs.asStateFlow()

    // Individual settings flows
    val darkMode: StateFlow<Boolean> = preferencesManager.darkMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val dynamicColor: StateFlow<Boolean> = preferencesManager.dynamicColor
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val themeStyle: StateFlow<ThemeStyle> = preferencesManager.themeStyle
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeStyle.CYBER)

    val autoConnect: StateFlow<Boolean> = preferencesManager.autoConnect
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // Combined settings state for UI
    private val _showNotifications = MutableStateFlow(true)
    private val _keepAliveOnBattery = MutableStateFlow(true)

    val settings: StateFlow<Settings> = combine(
        darkMode,
        dynamicColor,
        themeStyle,
        autoConnect,
        _showNotifications,
        _keepAliveOnBattery
    ) { values: Array<Any> ->
        Settings(
            darkMode = values[0] as Boolean,
            dynamicColor = values[1] as Boolean,
            themeStyle = values[2] as ThemeStyle,
            autoConnect = values[3] as Boolean,
            showNotifications = values[4] as Boolean,
            keepAliveOnBattery = values[5] as Boolean
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        Settings()
    )

    init {
        loadConfig()
    }

    /**
     * Load configuration from storage
     */
    private fun loadConfig() {
        viewModelScope.launch {
            try {
                val configJson = preferencesManager.getConfigJson()
                if (configJson != null) {
                    val config = ConfigParser.parseJson(configJson)
                    _config.value = config
                    validateConfig(config)
                } else {
                    // Load default/sample config
                    _config.value = ConfigParser.createDefault()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load config", e)
                _config.value = ConfigParser.createDefault()
            }
        }
    }

    /**
     * Save configuration
     */
    fun saveConfig(config: VpnConfig) {
        viewModelScope.launch {
            try {
                _config.value = config
                validateConfig(config)

                if (_configErrors.value.isEmpty()) {
                    val json = ConfigParser.toJson(config)
                    preferencesManager.saveConfigJson(json)
                    addLog(LogLevel.INFO, "Configuration saved")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save config", e)
                _errorMessage.value = "Failed to save configuration: ${e.message}"
            }
        }
    }

    /**
     * Import configuration from TOML string
     */
    fun importConfig(tomlContent: String) {
        viewModelScope.launch {
            try {
                val config = ConfigParser.parseToml(tomlContent)
                saveConfig(config)
                addLog(LogLevel.INFO, "Configuration imported")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to import config", e)
                _errorMessage.value = "Failed to import configuration: ${e.message}"
            }
        }
    }

    /**
     * Import configuration from file (Context overload for SettingsScreen)
     */
    @Suppress("UNUSED_PARAMETER")
    fun importConfig(context: Context) {
        // TODO: Implement file picker for config import
        addLog(LogLevel.INFO, "Import config from file not yet implemented")
        _errorMessage.value = "Import from file not yet implemented"
    }

    /**
     * Export configuration to file
     */
    @Suppress("UNUSED_PARAMETER")
    fun exportConfig(context: Context) {
        viewModelScope.launch {
            try {
                val config = _config.value
                if (config != null) {
                    val json = ConfigParser.toJson(config)
                    // TODO: Implement file saving
                    addLog(LogLevel.INFO, "Export config not yet implemented")
                    _errorMessage.value = "Export to file not yet implemented"
                } else {
                    _errorMessage.value = "No configuration to export"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to export config", e)
                _errorMessage.value = "Failed to export configuration: ${e.message}"
            }
        }
    }

    /**
     * Reset configuration to defaults
     */
    fun resetConfig() {
        viewModelScope.launch {
            try {
                val defaultConfig = ConfigParser.createDefault()
                _config.value = defaultConfig
                preferencesManager.saveConfigJson(ConfigParser.toJson(defaultConfig))
                addLog(LogLevel.INFO, "Configuration reset to defaults")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reset config", e)
                _errorMessage.value = "Failed to reset configuration: ${e.message}"
            }
        }
    }

    /**
     * Validate configuration
     */
    private fun validateConfig(config: VpnConfig) {
        _configErrors.value = config.validate()
    }

    /**
     * Check if VPN permission is granted
     */
    fun prepareVpn(): Intent? {
        return VpnService.prepare(context)
    }

    /**
     * Connect to VPN
     */
    fun connect() {
        val config = _config.value

        if (config == null) {
            _errorMessage.value = "No configuration loaded"
            return
        }

        val errors = config.validate()
        if (errors.isNotEmpty()) {
            _errorMessage.value = errors.first()
            return
        }

        viewModelScope.launch {
            try {
                _isLoading.value = true
                addLog(LogLevel.INFO, "Connecting to ${config.client.server}...")

                val configJson = ConfigParser.toJson(config)

                val intent = Intent(context, TwochaVpnService::class.java).apply {
                    action = TwochaVpnService.ACTION_CONNECT
                    putExtra(TwochaVpnService.EXTRA_CONFIG_JSON, configJson)
                }

                context.startForegroundService(intent)

            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect", e)
                _errorMessage.value = "Failed to connect: ${e.message}"
                addLog(LogLevel.ERROR, "Connection failed: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Disconnect from VPN
     */
    fun disconnect() {
        viewModelScope.launch {
            try {
                addLog(LogLevel.INFO, "Disconnecting...")

                val intent = Intent(context, TwochaVpnService::class.java).apply {
                    action = TwochaVpnService.ACTION_DISCONNECT
                }

                context.startService(intent)

            } catch (e: Exception) {
                Log.e(TAG, "Failed to disconnect", e)
                _errorMessage.value = "Failed to disconnect: ${e.message}"
            }
        }
    }

    /**
     * Toggle VPN connection
     */
    fun toggle() {
        when (connectionState.value) {
            ConnectionState.DISCONNECTED -> connect()
            ConnectionState.CONNECTED -> disconnect()
            else -> { /* Ignore during transition states */ }
        }
    }

    /**
     * Called when VPN permission is denied
     */
    fun onVpnPermissionDenied() {
        _errorMessage.value = "VPN permission denied"
        addLog(LogLevel.WARN, "VPN permission denied by user")
    }

    /**
     * Called when VPN service is started
     */
    fun onVpnStarted() {
        addLog(LogLevel.INFO, "VPN service started")
    }

    /**
     * Update dark mode setting
     */
    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setDarkMode(enabled)
        }
    }

    /**
     * Update dynamic color setting
     */
    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setDynamicColor(enabled)
        }
    }

    /**
     * Update selected theme style
     */
    fun setThemeStyle(style: ThemeStyle) {
        viewModelScope.launch {
            preferencesManager.setThemeStyle(style)
        }
    }

    /**
     * Update auto-connect setting
     */
    fun setAutoConnect(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setAutoConnect(enabled)
        }
    }

    /**
     * Update show notifications setting
     */
    fun setShowNotifications(enabled: Boolean) {
        _showNotifications.value = enabled
    }

    /**
     * Update keep alive on battery setting
     */
    fun setKeepAliveOnBattery(enabled: Boolean) {
        _keepAliveOnBattery.value = enabled
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Add log entry with level
     */
    private fun addLog(level: LogLevel, message: String) {
        val entry = LogItem(
            id = System.currentTimeMillis(),
            level = level,
            message = message,
            timestamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        )
        _logs.value = (_logs.value + entry).takeLast(100)
    }

    /**
     * Add simple log entry (defaults to INFO)
     */
    private fun addLog(message: String) {
        addLog(LogLevel.INFO, message)
    }

    /**
     * Clear logs
     */
    fun clearLogs() {
        _logs.value = emptyList()
    }
}

/**
 * Combined settings data class
 */
data class Settings(
    val darkMode: Boolean = true,
    val dynamicColor: Boolean = false,
    val themeStyle: ThemeStyle = ThemeStyle.CYBER,
    val autoConnect: Boolean = false,
    val showNotifications: Boolean = true,
    val keepAliveOnBattery: Boolean = true
)

/**
 * Log levels matching LogsScreen expectations
 */
enum class LogLevel {
    DEBUG,
    INFO,
    WARN,
    ERROR,
    VERBOSE
}

/**
 * Log item data class matching LogsScreen expectations
 */
data class LogItem(
    val id: Long = System.currentTimeMillis(),
    val level: LogLevel,
    val message: String,
    val timestamp: String = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
)
