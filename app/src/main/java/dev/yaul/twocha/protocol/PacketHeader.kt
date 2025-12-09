package dev.yaul.twocha.protocol

import java.security.SecureRandom

/**
 * VPN Protocol Packet Header (24 bytes)
 *
 * Structure:
 * ```
 * ┌──────────┬──────────┬────────────┬──────────────────────────┬─────────────┐
 * │ Version  │   Type   │  Counter   │          Nonce           │  Reserved   │
 * │  (1 B)   │  (1 B)   │   (4 B)    │         (12 B)           │    (6 B)    │
 * │          │          │ Little-end │                          │   Unused    │
 * └──────────┴──────────┴────────────┴──────────────────────────┴─────────────┘
 *    Byte 0     Byte 1    Bytes 2-5      Bytes 6-17              Bytes 18-23
 * ```
 */
data class PacketHeader(
    val version: Byte = Constants.PROTOCOL_VERSION,
    val packetType: PacketType,
    val counter: UInt,
    val nonce: ByteArray
) {
    init {
        require(nonce.size == Constants.NONCE_SIZE) {
            "Nonce must be ${Constants.NONCE_SIZE} bytes, got ${nonce.size}"
        }
    }

    /**
     * Serialize header to 24-byte array
     * Used both for transmission and as AAD for AEAD encryption
     */
    fun serialize(): ByteArray {
        val buffer = ByteArray(Constants.PROTOCOL_HEADER_SIZE)

        // Byte 0: Version
        buffer[0] = version

        // Byte 1: Packet type
        buffer[1] = packetType.value

        // Bytes 2-5: Counter (little-endian)
        buffer[2] = (counter and 0xFFu).toByte()
        buffer[3] = ((counter shr 8) and 0xFFu).toByte()
        buffer[4] = ((counter shr 16) and 0xFFu).toByte()
        buffer[5] = ((counter shr 24) and 0xFFu).toByte()

        // Bytes 6-17: Nonce
        System.arraycopy(nonce, 0, buffer, 6, Constants.NONCE_SIZE)

        // Bytes 18-23: Reserved (zeros)
        return buffer
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PacketHeader

        if (version != other.version) return false
        if (packetType != other.packetType) return false
        if (counter != other.counter) return false
        if (!nonce.contentEquals(other.nonce)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = version.toInt()
        result = 31 * result + packetType.hashCode()
        result = 31 * result + counter.hashCode()
        result = 31 * result + nonce.contentHashCode()
        return result
    }

    companion object {
        private val secureRandom = SecureRandom()

        /**
         * Deserialize header from byte array
         * @return PacketHeader or null if invalid
         */
        fun deserialize(buffer: ByteArray): PacketHeader? {
            if (buffer.size < Constants.PROTOCOL_HEADER_SIZE) {
                return null
            }

            // Check version
            val version = buffer[0]
            if (version != Constants.PROTOCOL_VERSION) {
                return null
            }

            // Parse packet type
            val packetType = PacketType.fromByte(buffer[1]) ?: return null

            // Parse counter (little-endian)
            val counter = (buffer[2].toUInt() and 0xFFu) or
                    ((buffer[3].toUInt() and 0xFFu) shl 8) or
                    ((buffer[4].toUInt() and 0xFFu) shl 16) or
                    ((buffer[5].toUInt() and 0xFFu) shl 24)

            // Extract nonce
            val nonce = buffer.copyOfRange(6, 6 + Constants.NONCE_SIZE)

            return PacketHeader(version, packetType, counter, nonce)
        }

        /**
         * Generate a new nonce for the given counter
         * Nonce structure: counter (4 bytes, LE) + random (8 bytes)
         */
        fun generateNonce(counter: UInt): ByteArray {
            val nonce = ByteArray(Constants.NONCE_SIZE)

            // First 4 bytes: counter (little-endian)
            nonce[0] = (counter and 0xFFu).toByte()
            nonce[1] = ((counter shr 8) and 0xFFu).toByte()
            nonce[2] = ((counter shr 16) and 0xFFu).toByte()
            nonce[3] = ((counter shr 24) and 0xFFu).toByte()

            // Last 8 bytes: random
            val randomBytes = ByteArray(8)
            secureRandom.nextBytes(randomBytes)
            System.arraycopy(randomBytes, 0, nonce, 4, 8)

            return nonce
        }

        /**
         * Create a new header with auto-generated nonce
         */
        fun create(packetType: PacketType, counter: UInt): PacketHeader {
            return PacketHeader(
                packetType = packetType,
                counter = counter,
                nonce = generateNonce(counter)
            )
        }
    }
}