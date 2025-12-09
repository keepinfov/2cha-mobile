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
import dev.yaul.twocha.vpn.ConnectionState
import dev.yaul.twocha.vpn.TwochaVpnService
import dev.yaul.twocha.vpn.VpnStats
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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

    // Logs
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    // Settings
    val darkMode: StateFlow<Boolean> = preferencesManager.darkMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val dynamicColor: StateFlow<Boolean> = preferencesManager.dynamicColor
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val autoConnect: StateFlow<Boolean> = preferencesManager.autoConnect
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

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
                    addLog("Configuration saved")
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
                addLog("Configuration imported")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to import config", e)
                _errorMessage.value = "Failed to import configuration: ${e.message}"
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
                addLog("Connecting to ${config.client.server}...")

                val configJson = ConfigParser.toJson(config)

                val intent = Intent(context, TwochaVpnService::class.java).apply {
                    action = TwochaVpnService.ACTION_CONNECT
                    putExtra(TwochaVpnService.EXTRA_CONFIG_JSON, configJson)
                }

                context.startForegroundService(intent)

            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect", e)
                _errorMessage.value = "Failed to connect: ${e.message}"
                addLog("Connection failed: ${e.message}")
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
                addLog("Disconnecting...")

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
     * Update auto-connect setting
     */
    fun setAutoConnect(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setAutoConnect(enabled)
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Add log entry
     */
    private fun addLog(message: String) {
        val entry = LogEntry(System.currentTimeMillis(), message)
        _logs.value = (_logs.value + entry).takeLast(100)
    }

    /**
     * Clear logs
     */
    fun clearLogs() {
        _logs.value = emptyList()
    }
}

/**
 * Log entry data class
 */
data class LogEntry(
    val timestamp: Long,
    val message: String
)