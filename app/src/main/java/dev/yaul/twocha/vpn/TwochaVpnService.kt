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
import dev.yaul.twocha.config.ConfigParser
import dev.yaul.twocha.config.VpnConfig
import dev.yaul.twocha.security.KeyManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import uniffi.twocha_mobile.SocketProtector
import uniffi.twocha_mobile.TunnelObserver
import uniffi.twocha_mobile.TwochaTunnel
import uniffi.twocha_mobile.initLogging

/**
 * 2cha v4 VPN Service.
 *
 * Owns the data-plane setup (VpnService.Builder: addresses/routes/DNS/MTU) and
 * delegates the protocol — Noise_IK handshake, obfuscation transport, packet
 * pump — to the native engine via [TwochaTunnel]. The engine's `start` blocks,
 * so it runs on a dedicated thread; `stop` flips a shared atomic in Rust.
 */
class TwochaVpnService : VpnService() {

    companion object {
        private const val TAG = "TwochaVpnService"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "twocha_vpn_channel"

        const val ACTION_CONNECT = "dev.yaul.twocha.CONNECT"
        const val ACTION_DISCONNECT = "dev.yaul.twocha.DISCONNECT"
        const val EXTRA_CONFIG_JSON = "config_json"

        private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
        val connectionState: StateFlow<ConnectionState> = _connectionState

        private val _stats = MutableStateFlow(VpnStats())
        val stats: StateFlow<VpnStats> = _stats

        @Volatile
        var isRunning = false
            private set
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private var tunnel: TwochaTunnel? = null
    private var engineThread: Thread? = null

    // Bumped on every startVpn/stopVpn. A spawned engine thread captures the
    // generation it belongs to and only runs cleanup if it is still current —
    // so a stale (slow-to-exit) engine can't tear down a newer session.
    @Volatile
    private var generation = 0

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        initLogging()
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
            ACTION_DISCONNECT -> stopVpn()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "Service destroyed")
        stopVpn()
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

        val config: VpnConfig
        val configToml: String
        val tunFd: ParcelFileDescriptor
        val privateKeyB64: String
        try {
            config = ConfigParser.parseJson(configJson)
            val errors = config.validate()
            require(errors.isEmpty()) { "Invalid config: ${errors.joinToString(", ")}" }

            configToml = config.toToml()
            privateKeyB64 = KeyManager(this).privateKeyB64()

            _connectionState.value = ConnectionState.CONNECTING
            isRunning = true
            startForeground(NOTIFICATION_ID, createNotification(ConnectionState.CONNECTING))

            tunFd = establishVpnInterface(config)
                ?: throw IllegalStateException("Failed to establish VPN interface")
            vpnInterface = tunFd
        } catch (e: Exception) {
            Log.e(TAG, "VPN setup failed", e)
            _connectionState.value = ConnectionState.ERROR
            startForeground(NOTIFICATION_ID, createNotification(ConnectionState.ERROR))
            cleanup(preserveError = true)
            return
        }

        val protector = object : SocketProtector {
            override fun protect(fd: Int): Boolean = this@TwochaVpnService.protect(fd)
        }

        // The engine fires `onConnected` only after the Noise_IK handshake lands,
        // so CONNECTED reflects a genuinely established tunnel rather than an
        // optimistic guess made before any network I/O.
        val observer = object : TunnelObserver {
            override fun onConnected() {
                _connectionState.value = ConnectionState.CONNECTED
                startForeground(NOTIFICATION_ID, createNotification(ConnectionState.CONNECTED))
                Log.i(TAG, "VPN handshake complete; connected")
            }
        }

        val engine = TwochaTunnel()
        tunnel = engine

        // `start` transfers fd ownership to the engine and blocks until `stop`.
        val detachedFd = tunFd.detachFd()

        val gen = ++generation
        engineThread = Thread({
            try {
                _stats.value = VpnStats()
                Log.i(TAG, "VPN engine starting (${config.client.transport.wire})")

                engine.start(configToml, privateKeyB64, detachedFd, protector, observer)
                Log.i(TAG, "VPN engine returned")
            } catch (e: Exception) {
                Log.e(TAG, "VPN engine error", e)
                // A deliberate stop also surfaces here as an error (the handshake
                // aborts); only report ERROR if we're still the current session.
                if (gen == generation) {
                    _connectionState.value = ConnectionState.ERROR
                    startForeground(NOTIFICATION_ID, createNotification(ConnectionState.ERROR))
                }
            } finally {
                // Skip if a newer session has superseded us (stale orphan thread).
                if (gen == generation) {
                    val keepError = _connectionState.value == ConnectionState.ERROR
                    cleanup(preserveError = keepError)
                }
            }
        }, "twocha-engine").also { it.start() }
    }

    private fun stopVpn() {
        Log.d(TAG, "Stopping VPN")
        // Supersede the current session: a slow engine thread that outlives the
        // join below must not run cleanup and clobber a later re-connect.
        generation++
        _connectionState.value = ConnectionState.DISCONNECTING
        tunnel?.stop()
        engineThread?.join(2000)
        cleanup(preserveError = false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun cleanup(preserveError: Boolean) {
        tunnel = null
        engineThread = null
        // The engine owns the detached fd; only close ours if it was never handed off.
        vpnInterface?.close()
        vpnInterface = null
        isRunning = false
        if (!preserveError || _connectionState.value != ConnectionState.ERROR) {
            _connectionState.value = ConnectionState.DISCONNECTED
        }
        _stats.value = VpnStats()
        Log.d(TAG, "VPN cleaned up")
    }

    private fun establishVpnInterface(config: VpnConfig): ParcelFileDescriptor? {
        val builder = Builder()
            .setSession("2cha VPN")
            .setMtu(config.tun.mtu)

        if (config.ipv4.enable && !config.ipv4.address.isNullOrBlank()) {
            builder.addAddress(config.ipv4.address, config.ipv4.prefix)
            if (config.ipv4.routeAll) {
                builder.addRoute("0.0.0.0", 0)
            } else {
                config.ipv4.routes.forEach { route ->
                    val parts = route.split("/")
                    if (parts.size == 2) builder.addRoute(parts[0], parts[1].toInt())
                }
                val subnet = config.ipv4.address.substringBeforeLast(".") + ".0"
                builder.addRoute(subnet, config.ipv4.prefix)
            }
        }

        if (config.ipv6.enable && !config.ipv6.address.isNullOrBlank()) {
            builder.addAddress(config.ipv6.address, config.ipv6.prefix)
            if (config.ipv6.routeAll) {
                builder.addRoute("::", 0)
            } else {
                config.ipv6.routes.forEach { route ->
                    val parts = route.split("/")
                    if (parts.size == 2) builder.addRoute(parts[0], parts[1].toInt())
                }
            }
        }

        config.dns.serversV4.forEach { builder.addDnsServer(it) }
        config.dns.serversV6.forEach { builder.addDnsServer(it) }
        config.dns.search.forEach { builder.addSearchDomain(it) }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            config.ipv4.excludeIps.forEach { route ->
                val parts = route.split("/")
                if (parts.size == 2) {
                    builder.excludeRoute(
                        android.net.IpPrefix(
                            java.net.InetAddress.getByName(parts[0]),
                            parts[1].toInt()
                        )
                    )
                }
            }
        }

        // Keep this app (and the engine's carrier sockets) out of the tunnel so
        // server-address resolution and the handshake reach the network.
        try {
            builder.addDisallowedApplication(packageName)
        } catch (e: Exception) {
            Log.w(TAG, "Could not exclude self from VPN", e)
        }

        builder.setMetered(false)
        return builder.establish()
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
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun createNotification(state: ConnectionState): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val disconnectIntent = PendingIntent.getService(
            this, 1,
            Intent(this, TwochaVpnService::class.java).apply { action = ACTION_DISCONNECT },
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

/** VPN connection states. */
enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    DISCONNECTING,
    ERROR
}

/**
 * VPN traffic statistics. Live byte/packet counters are not yet surfaced by the
 * native engine (run_mobile blocks without a stats callback); only duration is
 * meaningful until a stats hook is added to the FFI.
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
