package dev.yaul.twocha.vpn

import android.os.ParcelFileDescriptor
import android.util.Log
import dev.yaul.twocha.config.VpnConfig
import dev.yaul.twocha.crypto.AeadCipher
import dev.yaul.twocha.protocol.*
import kotlinx.coroutines.*
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * VPN Connection Handler
 *
 * Manages the bidirectional packet forwarding between TUN interface and UDP socket.
 * Handles encryption, decryption, keepalive, and replay protection.
 */
class VpnConnection(
    private val tunFd: ParcelFileDescriptor,
    private val udpSocket: VpnUdpSocket,
    private val cipher: AeadCipher,
    private val config: VpnConfig
) {
    companion object {
        private const val TAG = "VpnConnection"
    }

    // Packet counter for outgoing packets
    private val txCounter = AtomicLong(0)

    // Replay protection for incoming packets
    private val replayWindow = ReplayWindow()

    // Traffic statistics
    private val bytesRx = AtomicLong(0)
    private val bytesTx = AtomicLong(0)
    private val packetsRx = AtomicLong(0)
    private val packetsTx = AtomicLong(0)

    // Running flag
    private val running = AtomicBoolean(false)

    // IO streams
    private val tunInput = FileInputStream(tunFd.fileDescriptor)
    private val tunOutput = FileOutputStream(tunFd.fileDescriptor)

    // Buffers
    private val tunReadBuffer = ByteArray(config.tun.mtu + 100)

    /**
     * Run the VPN connection loop
     */
    suspend fun run(
        onStatsUpdate: ((Long, Long, Long, Long) -> Unit)? = null
    ) = coroutineScope {
        running.set(true)
        Log.i(TAG, "VPN connection started")

        // Send initial keepalive to establish connection
        sendKeepalive()

        // Launch TUN → UDP forwarder (reads from TUN, sends to server)
        val tunToUdpJob = launch(Dispatchers.IO) {
            tunToUdpLoop()
        }

        // Launch UDP → TUN forwarder (receives from server, writes to TUN)
        val udpToTunJob = launch(Dispatchers.IO) {
            udpToTunLoop()
        }

        // Launch keepalive sender
        val keepaliveJob = launch {
            keepaliveLoop()
        }

        // Launch stats reporter
        val statsJob = launch {
            while (isActive && running.get()) {
                delay(1000)
                onStatsUpdate?.invoke(
                    bytesRx.get(),
                    bytesTx.get(),
                    packetsRx.get(),
                    packetsTx.get()
                )
            }
        }

        // Wait for any to complete (error or cancellation)
        try {
            listOf(tunToUdpJob, udpToTunJob, keepaliveJob, statsJob).joinAll()
        } catch (e: CancellationException) {
            Log.d(TAG, "Connection cancelled")
        }
    }

    /**
     * Stop the connection
     */
    fun stop() {
        Log.d(TAG, "Stopping connection")
        running.set(false)

        // Send disconnect notification (best effort)
        try {
            sendDisconnect()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to send disconnect", e)
        }
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        running.set(false)

        try {
            tunInput.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing TUN input", e)
        }

        try {
            tunOutput.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing TUN output", e)
        }

        try {
            udpSocket.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing UDP socket", e)
        }

        cipher.destroy()

        Log.d(TAG, "Connection cleaned up")
    }

    /**
     * TUN to UDP forwarding loop
     * Reads IP packets from TUN, encrypts, and sends to server
     */
    private suspend fun tunToUdpLoop() {
        Log.d(TAG, "TUN→UDP loop started")

        while (running.get()) {
            try {
                val length = tunInput.read(tunReadBuffer)

                if (length > 0) {
                    val ipPacket = tunReadBuffer.copyOf(length)
                    sendEncryptedPacket(ipPacket)
                } else if (length < 0) {
                    Log.w(TAG, "TUN read returned -1, stopping")
                    break
                }
            } catch (e: Exception) {
                if (running.get()) {
                    Log.e(TAG, "TUN read error", e)
                }
                break
            }

            yield() // Allow other coroutines to run
        }

        Log.d(TAG, "TUN→UDP loop stopped")
    }

    /**
     * UDP to TUN forwarding loop
     * Receives encrypted packets from server, decrypts, and writes to TUN
     */
    private suspend fun udpToTunLoop() {
        Log.d(TAG, "UDP→TUN loop started")

        while (running.get()) {
            try {
                val data = udpSocket.receive()

                if (data != null && data.size >= Constants.PROTOCOL_HEADER_SIZE) {
                    processReceivedPacket(data)
                }
            } catch (e: Exception) {
                if (running.get()) {
                    Log.e(TAG, "UDP receive error", e)
                }
                break
            }

            yield() // Allow other coroutines to run
        }

        Log.d(TAG, "UDP→TUN loop stopped")
    }

    /**
     * Keepalive sending loop
     */
    private suspend fun keepaliveLoop() {
        val intervalMs = config.timeouts.keepalive * 1000
        Log.d(TAG, "Keepalive loop started (interval: ${intervalMs}ms)")

        while (running.get()) {
            delay(intervalMs)

            if (running.get()) {
                try {
                    sendKeepalive()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to send keepalive", e)
                }
            }
        }

        Log.d(TAG, "Keepalive loop stopped")
    }

    /**
     * Send encrypted data packet
     */
    private fun sendEncryptedPacket(ipPacket: ByteArray) {
        // Get next counter
        val counter = txCounter.incrementAndGet().toUInt()

        // Create header with nonce
        val header = PacketHeader.create(PacketType.DATA, counter)
        val headerBytes = header.serialize()

        // Encrypt payload with header as AAD
        val encrypted = cipher.encrypt(header.nonce, ipPacket, headerBytes)

        // Build complete packet
        val packet = ByteArray(Constants.PROTOCOL_HEADER_SIZE + encrypted.size)
        System.arraycopy(headerBytes, 0, packet, 0, Constants.PROTOCOL_HEADER_SIZE)
        System.arraycopy(encrypted, 0, packet, Constants.PROTOCOL_HEADER_SIZE, encrypted.size)

        // Send via UDP
        udpSocket.send(packet)

        // Update stats
        bytesTx.addAndGet(packet.size.toLong())
        packetsTx.incrementAndGet()

        Log.v(TAG, "Sent DATA packet #$counter (${ipPacket.size} bytes)")
    }

    /**
     * Send keepalive packet
     */
    private fun sendKeepalive() {
        val counter = txCounter.incrementAndGet().toUInt()
        val header = PacketHeader.create(PacketType.KEEPALIVE, counter)
        val headerBytes = header.serialize()

        // Encrypt empty payload
        val encrypted = cipher.encrypt(header.nonce, ByteArray(0), headerBytes)

        // Build packet
        val packet = ByteArray(Constants.PROTOCOL_HEADER_SIZE + encrypted.size)
        System.arraycopy(headerBytes, 0, packet, 0, Constants.PROTOCOL_HEADER_SIZE)
        System.arraycopy(encrypted, 0, packet, Constants.PROTOCOL_HEADER_SIZE, encrypted.size)

        udpSocket.send(packet)

        Log.v(TAG, "Sent KEEPALIVE #$counter")
    }

    /**
     * Send disconnect packet
     */
    private fun sendDisconnect() {
        val counter = txCounter.incrementAndGet().toUInt()
        val header = PacketHeader.create(PacketType.DISCONNECT, counter)
        val headerBytes = header.serialize()

        // Encrypt empty payload
        val encrypted = cipher.encrypt(header.nonce, ByteArray(0), headerBytes)

        // Build packet
        val packet = ByteArray(Constants.PROTOCOL_HEADER_SIZE + encrypted.size)
        System.arraycopy(headerBytes, 0, packet, 0, Constants.PROTOCOL_HEADER_SIZE)
        System.arraycopy(encrypted, 0, packet, Constants.PROTOCOL_HEADER_SIZE, encrypted.size)

        udpSocket.send(packet)

        Log.d(TAG, "Sent DISCONNECT")
    }

    /**
     * Process received encrypted packet
     */
    private fun processReceivedPacket(data: ByteArray) {
        // Parse header
        val header = PacketHeader.deserialize(data)
        if (header == null) {
            Log.w(TAG, "Invalid packet header")
            return
        }

        // Check replay protection
        if (!replayWindow.checkAndUpdate(header.counter.toLong())) {
            Log.w(TAG, "Replay detected for packet #${header.counter}")
            return
        }

        // Get encrypted payload
        val encrypted = data.copyOfRange(Constants.PROTOCOL_HEADER_SIZE, data.size)
        val headerBytes = header.serialize()

        // Update stats
        bytesRx.addAndGet(data.size.toLong())
        packetsRx.incrementAndGet()

        when (header.packetType) {
            PacketType.DATA -> {
                // Decrypt payload
                val decrypted = cipher.tryDecrypt(header.nonce, encrypted, headerBytes)
                if (decrypted != null) {
                    // Write to TUN
                    tunOutput.write(decrypted)
                    Log.v(TAG, "Received DATA packet #${header.counter} (${decrypted.size} bytes)")
                } else {
                    Log.w(TAG, "Failed to decrypt packet #${header.counter}")
                }
            }

            PacketType.KEEPALIVE -> {
                Log.v(TAG, "Received KEEPALIVE #${header.counter}")
            }

            PacketType.DISCONNECT -> {
                Log.i(TAG, "Received DISCONNECT from server")
                running.set(false)
            }

            PacketType.HANDSHAKE_RESPONSE -> {
                Log.d(TAG, "Received HANDSHAKE_RESPONSE #${header.counter}")
            }

            else -> {
                Log.w(TAG, "Unknown packet type: ${header.packetType}")
            }
        }
    }
}