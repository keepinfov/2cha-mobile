package dev.yaul.twocha.protocol

/**
 * Complete VPN Packet
 *
 * Structure:
 * ```
 * ┌─────────────────────────────┬─────────────────────────────────────────────┐
 * │      Header (24 bytes)      │      Encrypted Payload + Tag                │
 * │                             │      (variable + 16 bytes)                  │
 * └─────────────────────────────┴─────────────────────────────────────────────┘
 * ```
 */
data class Packet(
    val header: PacketHeader,
    val payload: ByteArray // Encrypted for outgoing, decrypted for incoming
) {
    /**
     * Serialize complete packet for transmission
     */
    fun serialize(): ByteArray {
        val headerBytes = header.serialize()
        val result = ByteArray(headerBytes.size + payload.size)
        System.arraycopy(headerBytes, 0, result, 0, headerBytes.size)
        System.arraycopy(payload, 0, result, headerBytes.size, payload.size)
        return result
    }

    /**
     * Total packet size
     */
    val size: Int
        get() = Constants.PROTOCOL_HEADER_SIZE + payload.size

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Packet

        if (header != other.header) return false
        if (!payload.contentEquals(other.payload)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = header.hashCode()
        result = 31 * result + payload.contentHashCode()
        return result
    }

    companion object {
        /**
         * Deserialize packet from byte array
         * @return Packet or null if invalid
         */
        fun deserialize(buffer: ByteArray): Packet? {
            val header = PacketHeader.deserialize(buffer) ?: return null
            val payload = buffer.copyOfRange(Constants.PROTOCOL_HEADER_SIZE, buffer.size)
            return Packet(header, payload)
        }

        /**
         * Create a DATA packet
         */
        fun createData(counter: UInt, encryptedPayload: ByteArray): Packet {
            val header = PacketHeader.create(PacketType.DATA, counter)
            return Packet(header, encryptedPayload)
        }

        /**
         * Create a KEEPALIVE packet
         */
        fun createKeepalive(counter: UInt): Packet {
            val header = PacketHeader.create(PacketType.KEEPALIVE, counter)
            return Packet(header, ByteArray(0))
        }

        /**
         * Create a DISCONNECT packet
         */
        fun createDisconnect(counter: UInt): Packet {
            val header = PacketHeader.create(PacketType.DISCONNECT, counter)
            return Packet(header, ByteArray(0))
        }

        /**
         * Create a HANDSHAKE_INIT packet
         */
        fun createHandshakeInit(counter: UInt, clientInfo: ByteArray = ByteArray(0)): Packet {
            val header = PacketHeader.create(PacketType.HANDSHAKE_INIT, counter)
            return Packet(header, clientInfo)
        }
    }
}