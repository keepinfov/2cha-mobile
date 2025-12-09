package dev.yaul.twocha.config

import dev.yaul.twocha.crypto.CryptoUtils
import dev.yaul.twocha.protocol.Constants
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Complete VPN configuration
 */
@Serializable
data class VpnConfig(
    val client: ClientSection,
    val tun: TunSection = TunSection(),
    val crypto: CryptoSection,
    val ipv4: Ipv4Section = Ipv4Section(),
    val ipv6: Ipv6Section = Ipv6Section(),
    val dns: DnsSection = DnsSection(),
    val performance: PerformanceSection = PerformanceSection(),
    val timeouts: TimeoutsSection = TimeoutsSection(),
    val logging: LoggingSection = LoggingSection()
) {
    /**
     * Get the encryption key as bytes
     */
    fun getKeyBytes(): ByteArray {
        val hexKey = crypto.key ?: throw IllegalStateException("No encryption key configured")
        return CryptoUtils.hexToBytes(hexKey.trim())
    }

    /**
     * Get the cipher suite
     */
    fun getCipherSuite(): CipherSuite {
        return crypto.cipher
    }

    /**
     * Parse server address into host and port
     */
    fun parseServerAddress(): Pair<String, Int> {
        val parts = client.server.split(":")
        return if (parts.size == 2) {
            Pair(parts[0], parts[1].toIntOrNull() ?: Constants.DEFAULT_PORT)
        } else {
            Pair(client.server, Constants.DEFAULT_PORT)
        }
    }

    /**
     * Validate the configuration
     */
    fun validate(): List<String> {
        val errors = mutableListOf<String>()

        if (client.server.isBlank()) {
            errors.add("Server address is required")
        }

        if (crypto.key.isNullOrBlank()) {
            errors.add("Encryption key is required")
        } else if (crypto.key.length != 64) {
            errors.add("Encryption key must be 64 hex characters")
        } else {
            try {
                CryptoUtils.hexToBytes(crypto.key)
            } catch (e: Exception) {
                errors.add("Encryption key contains invalid hex characters")
            }
        }

        if (ipv4.enable && ipv4.address.isNullOrBlank()) {
            errors.add("IPv4 address is required when IPv4 is enabled")
        }

        if (ipv6.enable && ipv6.address.isNullOrBlank()) {
            errors.add("IPv6 address is required when IPv6 is enabled")
        }

        if (!ipv4.enable && !ipv6.enable) {
            errors.add("At least one of IPv4 or IPv6 must be enabled")
        }

        return errors
    }

    fun isValid(): Boolean = validate().isEmpty()
}

@Serializable
data class ClientSection(
    val server: String,
    @SerialName("prefer_ipv6")
    val preferIpv6: Boolean = false,
    @SerialName("dns_lookup")
    val dnsLookup: DnsLookupMode = DnsLookupMode.AUTO
)

@Serializable
enum class DnsLookupMode {
    @SerialName("auto")
    AUTO,
    @SerialName("always")
    ALWAYS,
    @SerialName("never")
    NEVER
}

@Serializable
data class TunSection(
    val name: String = "tun0",
    val mtu: Int = Constants.DEFAULT_MTU,
    @SerialName("queue_len")
    val queueLen: Int = 500
)

@Serializable
data class CryptoSection(
    val cipher: CipherSuite = CipherSuite.CHACHA20_POLY1305,
    val key: String? = null,
    @SerialName("key_file")
    val keyFile: String? = null
)

@Serializable
data class Ipv4Section(
    val enable: Boolean = true,
    val address: String? = "10.0.0.2",
    val prefix: Int = 24,
    @SerialName("route_all")
    val routeAll: Boolean = false,
    val routes: List<String> = emptyList(),
    @SerialName("exclude_ips")
    val excludeIps: List<String> = emptyList()
)

@Serializable
data class Ipv6Section(
    val enable: Boolean = false,
    val address: String? = null,
    val prefix: Int = 64,
    @SerialName("route_all")
    val routeAll: Boolean = false,
    val routes: List<String> = emptyList(),
    @SerialName("exclude_ips")
    val excludeIps: List<String> = emptyList()
)

@Serializable
data class DnsSection(
    @SerialName("servers_v4")
    val serversV4: List<String> = emptyList(),
    @SerialName("servers_v6")
    val serversV6: List<String> = emptyList(),
    val search: List<String> = emptyList()
)

@Serializable
data class PerformanceSection(
    @SerialName("socket_recv_buffer")
    val socketRecvBuffer: Int = Constants.SOCKET_BUFFER_SIZE,
    @SerialName("socket_send_buffer")
    val socketSendBuffer: Int = Constants.SOCKET_BUFFER_SIZE,
    @SerialName("batch_size")
    val batchSize: Int = 32,
    @SerialName("multi_queue")
    val multiQueue: Boolean = false,
    @SerialName("cpu_affinity")
    val cpuAffinity: List<Int> = emptyList()
)

@Serializable
data class TimeoutsSection(
    val keepalive: Long = Constants.DEFAULT_KEEPALIVE_SECONDS,
    val session: Long = 180,
    val handshake: Long = 10
)

@Serializable
data class LoggingSection(
    val level: String = "info",
    val file: String? = null
)

/**
 * Custom serializer for CipherSuite
 */
@Serializable
enum class CipherSuite {
    @SerialName("chacha20-poly1305")
    CHACHA20_POLY1305,
    @SerialName("aes-256-gcm")
    AES_256_GCM;

    fun toCryptoSuite(): dev.yaul.twocha.crypto.CipherSuite {
        return when (this) {
            CHACHA20_POLY1305 -> dev.yaul.twocha.crypto.CipherSuite.CHACHA20_POLY1305
            AES_256_GCM -> dev.yaul.twocha.crypto.CipherSuite.AES_256_GCM
        }
    }
}