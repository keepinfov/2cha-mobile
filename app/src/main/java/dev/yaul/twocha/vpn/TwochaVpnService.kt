package dev.yaul.twocha.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import dev.yaul.twocha.MainActivity
import dev.yaul.twocha.R
import dev.yaul.twocha.config.VpnConfig
import dev.yaul.twocha.crypto.AeadCipher
import dev.yaul.twocha.crypto.CipherFactory
import dev.yaul.twocha.protocol.Constants
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.net.InetSocketAddress

/**
 * 2cha VPN Service
 *
 * Handles VPN tunnel creation and packet forwarding
 */
class TwochaVpnService : VpnService() {

    companion object {
        private const val TAG = "TwochaVpnService"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "twocha_vpn_channel"

        // Actions
        const val ACTION_CONNECT = "dev.yaul.twocha.CONNECT"
        const val ACTION_DISCONNECT = "dev.yaul.twocha.DISCONNECT"

        // Extras
        const val EXTRA_CONFIG_JSON = "config_json"

        // Service state
        private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
        val connectionState: StateFlow<ConnectionState> = _connectionState

        private val _stats = MutableStateFlow(VpnStats())
        val stats: StateFlow<VpnStats> = _stats

        var isRunning = false
            private set
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private var connection: VpnConnection? = null
    private var serviceJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: ${intent?.action}")

        when (intent?.action) {
            ACTION_CONNECT -> {
                val configJson = intent.getStringExtra(EXTRA_CONFIG_JSON)
                if (configJson != null) {
                    startVpn(configJson)
                } else {
                    Log.e(TAG, "No config provided")
                    stopSelf()
                }
            }
            ACTION_DISCONNECT -> {
                stopVpn()
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "Service destroyed")
        stopVpn()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onRevoke() {
        Log.d(TAG, "VPN revoked")
        stopVpn()
        super.onRevoke()
    }

    private fun startVpn(configJson: String) {
        if (isRunning) {
            Log.w(TAG, "VPN already running")
            return
        }

        serviceJob = serviceScope.launch {
            try {
                _connectionState.value = ConnectionState.CONNECTING
                isRunning = true

                // Parse config
                val config = dev.yaul.twocha.config.ConfigParser.parseJson(configJson)

                // Validate config
                val errors = config.validate()
                if (errors.isNotEmpty()) {
                    throw IllegalArgumentException("Invalid config: ${errors.joinToString(", ")}")
                }

                // Create cipher
                val cipher = CipherFactory.create(
                    config.getCipherSuite().toCryptoSuite(),
                    config.getKeyBytes()
                )

                // Establish VPN interface
                val tunFd = establishVpnInterface(config)
                vpnInterface = tunFd

                // Parse server address
                val (host, port) = config.parseServerAddress()
                val serverAddress = resolveServerAddress(host, port, config.client.preferIpv6)

                // Create UDP socket
                val udpSocket = VpnUdpSocket(this@TwochaVpnService)
                udpSocket.connect(serverAddress)

                // Create connection handler
                connection = VpnConnection(
                    tunFd = tunFd,
                    udpSocket = udpSocket,
                    cipher = cipher,
                    config = config
                )

                // Start foreground service with notification
                startForeground(NOTIFICATION_ID, createNotification(ConnectionState.CONNECTED))

                _connectionState.value = ConnectionState.CONNECTED
                Log.i(TAG, "VPN connected to $serverAddress")

                // Run the VPN connection loop
                connection?.run(
                    onStatsUpdate = { bytesRx, bytesTx, packetsRx, packetsTx ->
                        _stats.value = VpnStats(
                            bytesReceived = bytesRx,
                            bytesSent = bytesTx,
                            packetsReceived = packetsRx,
                            packetsSent = packetsTx,
                            connectedAt = _stats.value.connectedAt
                        )
                    }
                )

            } catch (e: CancellationException) {
                Log.d(TAG, "VPN job cancelled")
            } catch (e: Exception) {
                Log.e(TAG, "VPN error", e)
                _connectionState.value = ConnectionState.ERROR
            } finally {
                cleanup()
            }
        }
    }

    private fun stopVpn() {
        Log.d(TAG, "Stopping VPN")
        _connectionState.value = ConnectionState.DISCONNECTING

        serviceJob?.cancel()
        connection?.stop()
        cleanup()

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun cleanup() {
        connection?.cleanup()
        connection = null

        vpnInterface?.close()
        vpnInterface = null

        isRunning = false
        _connectionState.value = ConnectionState.DISCONNECTED
        _stats.value = VpnStats()

        Log.d(TAG, "VPN cleaned up")
    }

    private fun establishVpnInterface(config: VpnConfig): ParcelFileDescriptor {
        val builder = Builder()
            .setSession("2cha VPN")
            .setMtu(config.tun.mtu)

        // IPv4 configuration
        if (config.ipv4.enable && !config.ipv4.address.isNullOrBlank()) {
            builder.addAddress(config.ipv4.address, config.ipv4.prefix)

            if (config.ipv4.routeAll) {
                // Route all IPv4 traffic
                builder.addRoute("0.0.0.0", 0)
            } else {
                // Add specific routes
                config.ipv4.routes.forEach { route ->
                    val parts = route.split("/")
                    if (parts.size == 2) {
                        builder.addRoute(parts[0], parts[1].toInt())
                    }
                }
                // Always add VPN subnet route
                val subnet = config.ipv4.address.substringBeforeLast(".") + ".0"
                builder.addRoute(subnet, config.ipv4.prefix)
            }
        }

        // IPv6 configuration
        if (config.ipv6.enable && !config.ipv6.address.isNullOrBlank()) {
            builder.addAddress(config.ipv6.address, config.ipv6.prefix)

            if (config.ipv6.routeAll) {
                builder.addRoute("::", 0)
            } else {
                config.ipv6.routes.forEach { route ->
                    val parts = route.split("/")
                    if (parts.size == 2) {
                        builder.addRoute(parts[0], parts[1].toInt())
                    }
                }
            }
        }

        // DNS servers
        config.dns.serversV4.forEach { builder.addDnsServer(it) }
        config.dns.serversV6.forEach { builder.addDnsServer(it) }

        // Search domains
        config.dns.search.forEach { builder.addSearchDomain(it) }

        // Exclude IPs from VPN (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            config.ipv4.excludeIps.forEach { route ->
                val parts = route.split("/")
                if (parts.size == 2) {
                    val ipPrefix = android.net.IpPrefix(
                        java.net.InetAddress.getByName(parts[0]),
                        parts[1].toInt()
                    )
                    builder.excludeRoute(ipPrefix)
                }
            }
        }

        // Don't route this app's traffic through VPN
        try {
            builder.addDisallowedApplication(packageName)
        } catch (e: Exception) {
            Log.w(TAG, "Could not exclude self from VPN", e)
        }

        // Allow metered network usage
        builder.setMetered(false)

        return builder.establish()
            ?: throw IllegalStateException("Failed to establish VPN interface")
    }

    private suspend fun resolveServerAddress(
        host: String,
        port: Int,
        preferIpv6: Boolean
    ): InetSocketAddress = withContext(Dispatchers.IO) {
        try {
            val addresses = java.net.InetAddress.getAllByName(host)

            val selected = if (preferIpv6) {
                addresses.firstOrNull { it is java.net.Inet6Address }
                    ?: addresses.firstOrNull { it is java.net.Inet4Address }
            } else {
                addresses.firstOrNull { it is java.net.Inet4Address }
                    ?: addresses.firstOrNull { it is java.net.Inet6Address }
            }

            selected?.let { InetSocketAddress(it, port) }
                ?: throw IllegalArgumentException("Could not resolve server address: $host")
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to resolve server: $host", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_vpn),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(state: ConnectionState): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val disconnectIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, TwochaVpnService::class.java).apply {
                action = ACTION_DISCONNECT
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val title = when (state) {
            ConnectionState.CONNECTED -> getString(R.string.notification_connected)
            ConnectionState.CONNECTING -> getString(R.string.notification_connecting)
            else -> getString(R.string.app_name)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(getString(R.string.notification_tap_disconnect))
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_delete,
                getString(R.string.btn_disconnect),
                disconnectIntent
            )
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}

/**
 * VPN connection states
 */
enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    DISCONNECTING,
    ERROR
}

/**
 * VPN traffic statistics
 */
data class VpnStats(
    val bytesReceived: Long = 0,
    val bytesSent: Long = 0,
    val packetsReceived: Long = 0,
    val packetsSent: Long = 0,
    val connectedAt: Long = System.currentTimeMillis()
) {
    val duration: Long
        get() = System.currentTimeMillis() - connectedAt
}