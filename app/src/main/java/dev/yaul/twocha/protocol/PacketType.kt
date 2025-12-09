package dev.yaul.twocha.protocol

/**
 * VPN protocol packet types
 */
enum class PacketType(val value: Byte) {
    /** Client initiates connection */
    HANDSHAKE_INIT(1),

    /** Server responds to handshake */
    HANDSHAKE_RESPONSE(2),

    /** Encrypted IP packet payload */
    DATA(3),

    /** Empty payload, maintains connection */
    KEEPALIVE(4),

    /** Graceful disconnection notification */
    DISCONNECT(5);

    companion object {
        /**
         * Parse packet type from byte value
         * @return PacketType or null if invalid
         */
        fun fromByte(value: Byte): PacketType? = entries.find { it.value == value }
    }
}