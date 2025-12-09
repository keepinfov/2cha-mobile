package dev.yaul.twocha.vpn

import android.net.VpnService
import android.util.Log
import dev.yaul.twocha.protocol.Constants
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.SocketTimeoutException

/**
 * Protected UDP socket for VPN traffic
 *
 * The socket is "protected" which means its traffic won't be routed
 * through the VPN tunnel (avoiding infinite loop)
 */
class VpnUdpSocket(private val vpnService: VpnService) {

    companion object {
        private const val TAG = "VpnUdpSocket"
    }

    private lateinit var socket: DatagramSocket
    private lateinit var serverAddress: InetSocketAddress

    private val receiveBuffer = ByteArray(Constants.MAX_PACKET_SIZE)

    var isConnected: Boolean = false
        private set

    /**
     * Connect to VPN server
     */
    fun connect(address: InetSocketAddress) {
        Log.d(TAG, "Connecting to $address")

        socket = DatagramSocket()

        // CRITICAL: Protect socket from VPN routing
        if (!vpnService.protect(socket)) {
            throw IllegalStateException("Failed to protect socket")
        }

        // Set socket options
        socket.soTimeout = Constants.READ_TIMEOUT_MS
        socket.sendBufferSize = Constants.SOCKET_BUFFER_SIZE
        socket.receiveBufferSize = Constants.SOCKET_BUFFER_SIZE

        // Connect to server
        socket.connect(address)
        serverAddress = address
        isConnected = true

        Log.i(TAG, "Connected to $address")
    }

    /**
     * Send data to server
     */
    fun send(data: ByteArray): Int {
        if (!isConnected) {
            throw IllegalStateException("Socket not connected")
        }

        val packet = DatagramPacket(data, data.size)
        socket.send(packet)
        return data.size
    }

    /**
     * Receive data from server
     *
     * @return Received data or null if timeout
     */
    fun receive(): ByteArray? {
        if (!isConnected) {
            throw IllegalStateException("Socket not connected")
        }

        return try {
            val packet = DatagramPacket(receiveBuffer, receiveBuffer.size)
            socket.receive(packet)
            receiveBuffer.copyOf(packet.length)
        } catch (e: SocketTimeoutException) {
            null
        }
    }

    /**
     * Set socket timeout
     */
    fun setTimeout(timeoutMs: Int) {
        if (::socket.isInitialized) {
            socket.soTimeout = timeoutMs
        }
    }

    /**
     * Close the socket
     */
    fun close() {
        if (::socket.isInitialized && !socket.isClosed) {
            Log.d(TAG, "Closing socket")
            socket.close()
            isConnected = false
        }
    }

    /**
     * Check if socket is bound and connected
     */
    fun isReady(): Boolean {
        return isConnected && ::socket.isInitialized && !socket.isClosed && socket.isConnected
    }

    /**
     * Get local port
     */
    fun getLocalPort(): Int {
        return if (::socket.isInitialized) socket.localPort else -1
    }
}