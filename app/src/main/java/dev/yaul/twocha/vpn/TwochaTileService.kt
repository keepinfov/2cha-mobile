package dev.yaul.twocha.vpn

import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import dev.yaul.twocha.MainActivity
import dev.yaul.twocha.R
import dev.yaul.twocha.data.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Quick Settings tile: toggle the VPN straight from the notification shade.
 *
 * The tile mirrors [TwochaVpnService.connectionState] while the shade is open
 * and, on tap, starts or stops the service. Connecting needs both VPN consent
 * and a saved config — neither of which a tile can obtain on its own — so when
 * either is missing it opens the app instead of failing silently.
 */
class TwochaTileService : TileService() {

    private var scope: CoroutineScope? = null

    override fun onStartListening() {
        super.onStartListening()
        // A fresh scope per listening window; cancelled in onStopListening.
        val s = CoroutineScope(Dispatchers.Main.immediate)
        scope = s
        s.launch {
            TwochaVpnService.connectionState.collect { renderTile(it) }
        }
    }

    override fun onStopListening() {
        scope?.cancel()
        scope = null
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        when (TwochaVpnService.connectionState.value) {
            ConnectionState.CONNECTED, ConnectionState.CONNECTING -> disconnect()
            ConnectionState.DISCONNECTED, ConnectionState.ERROR -> connect()
            ConnectionState.DISCONNECTING -> { /* mid-teardown; ignore */ }
        }
    }

    private fun disconnect() {
        startService(
            Intent(this, TwochaVpnService::class.java).apply {
                action = TwochaVpnService.ACTION_DISCONNECT
            }
        )
    }

    private fun connect() {
        // VPN consent (a one-time system dialog) can only be granted from an
        // activity — hand off to the app when it isn't in place yet.
        if (VpnService.prepare(this) != null) {
            openApp()
            return
        }
        val s = scope ?: CoroutineScope(Dispatchers.Main.immediate).also { scope = it }
        s.launch {
            val json = withContext(Dispatchers.IO) {
                runCatching { PreferencesManager(applicationContext).getConfigJson() }.getOrNull()
            }
            if (json.isNullOrBlank()) {
                openApp()
                return@launch
            }
            try {
                startForegroundService(
                    Intent(this@TwochaTileService, TwochaVpnService::class.java).apply {
                        action = TwochaVpnService.ACTION_CONNECT
                        putExtra(TwochaVpnService.EXTRA_CONFIG_JSON, json)
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start VPN from tile", e)
                openApp()
            }
        }
    }

    private fun openApp() {
        val activity = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pi = PendingIntent.getActivity(this, 0, activity, PendingIntent.FLAG_IMMUTABLE)
            startActivityAndCollapse(pi)
        } else {
            @Suppress("DEPRECATION", "StartActivityAndCollapseDeprecated")
            startActivityAndCollapse(activity)
        }
    }

    private fun renderTile(state: ConnectionState) {
        val tile = qsTile ?: return
        tile.label = getString(R.string.tile_label)
        when (state) {
            ConnectionState.CONNECTED -> {
                tile.state = Tile.STATE_ACTIVE
                tile.subtitle = getString(R.string.tile_connected)
            }
            ConnectionState.CONNECTING, ConnectionState.DISCONNECTING -> {
                tile.state = Tile.STATE_ACTIVE
                tile.subtitle = getString(R.string.tile_working)
            }
            ConnectionState.DISCONNECTED, ConnectionState.ERROR -> {
                tile.state = Tile.STATE_INACTIVE
                tile.subtitle = getString(R.string.tile_disconnected)
            }
        }
        tile.updateTile()
    }

    private companion object {
        const val TAG = "TwochaTileService"
    }
}
