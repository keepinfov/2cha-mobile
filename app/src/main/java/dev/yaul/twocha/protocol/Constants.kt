package dev.yaul.twocha.protocol

/**
 * Protocol constants for 2cha VPN
 * Based on Protocol Version 3
 */
object Constants {
    /** Protocol version - must match server */
    const val PROTOCOL_VERSION: Byte = 3

    /** ChaCha20 key size in bytes (256-bit) */
    const val CHACHA20_KEY_SIZE = 32

    /** ChaCha20/AES-GCM nonce size in bytes (96-bit) */
    const val NONCE_SIZE = 12

    /** Poly1305/GCM authentication tag size in bytes (128-bit) */
    const val TAG_SIZE = 16

    /** Protocol header size in bytes */
    const val PROTOCOL_HEADER_SIZE = 24

    /** Maximum packet size */
    const val MAX_PACKET_SIZE = 1500

    /** Default MTU */
    const val DEFAULT_MTU = 1420

    /** Default server port */
    const val DEFAULT_PORT = 51820

    /** Default keepalive interval in seconds */
    const val DEFAULT_KEEPALIVE_SECONDS = 25L

    /** Replay window size */
    const val REPLAY_WINDOW_SIZE = 64L

    /** Socket buffer size (2MB) */
    const val SOCKET_BUFFER_SIZE = 2 * 1024 * 1024

    /** Read timeout in milliseconds */
    const val READ_TIMEOUT_MS = 100
}