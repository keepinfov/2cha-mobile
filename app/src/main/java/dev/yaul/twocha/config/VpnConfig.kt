package dev.yaul.twocha.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 2cha v4 client configuration.
 *
 * Mirrors the Rust `ClientConfig` schema (twocha-core::config::client) so it can
 * be serialized to TOML and handed to the native engine via
 * `TwochaTunnel.start(configToml, ...)`. The client's own X25519 private key is
 * NOT part of this config — it is generated/stored on device and passed to the
 * engine separately; the engine never reads `crypto.private_key_file`, but the
 * Rust deserializer requires the field to be present, so [toToml] emits a
 * placeholder for it.
 */
@Serializable
data class VpnConfig(
    val client: ClientSection,
    val tls: TlsSection = TlsSection(),
    val reality: RealitySection = RealitySection(),
    val tun: TunSection = TunSection(),
    val crypto: CryptoSection,
    val ipv4: Ipv4Section = Ipv4Section(),
    val ipv6: Ipv6Section = Ipv6Section(),
    val dns: DnsSection = DnsSection()
) {
    /** Split `host:port` (or bare host) into a host/port pair. */
    fun parseServerAddress(): Pair<String, Int> {
        val idx = client.server.lastIndexOf(':')
        return if (idx > 0 && idx < client.server.length - 1) {
            val host = client.server.substring(0, idx)
            val port = client.server.substring(idx + 1).toIntOrNull() ?: DEFAULT_PORT
            host to port
        } else {
            client.server to DEFAULT_PORT
        }
    }

    fun validate(): List<String> {
        val errors = mutableListOf<String>()

        if (client.server.isBlank()) {
            errors.add("Server address is required")
        }
        if (crypto.serverPublicKey.isBlank()) {
            errors.add("Server public key is required")
        }
        if (client.transport == Transport.TLS && tls.sni.isBlank()) {
            errors.add("TLS transport requires an SNI host")
        }
        if (client.transport == Transport.REALITY) {
            if (reality.publicKey.isBlank()) {
                errors.add("REALITY transport requires the server's public key")
            }
            if (reality.shortId.isBlank()) {
                errors.add("REALITY transport requires a short id")
            }
            if (reality.serverName.isBlank()) {
                errors.add("REALITY transport requires a server name (SNI to mimic)")
            }
        }
        if (tun.mtu !in MIN_MTU..MAX_MTU) {
            // Same policy as the Rust config validation: each packet gains
            // 35 bytes of tunnel overhead and must fit a 1500-byte datagram.
            errors.add("MTU must be in $MIN_MTU..$MAX_MTU")
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

    /**
     * Serialize to the Rust v4 client TOML schema. The native engine parses this
     * with `ClientConfig::parse`; routing/DNS/MTU are applied by the Android
     * VpnService.Builder, so those fields are informational for the engine.
     */
    fun toToml(): String = buildString {
        appendLine("[client]")
        appendLine("server = ${q(client.server)}")
        appendLine("prefer_ipv6 = ${client.preferIpv6}")
        appendLine("dns_lookup = ${q(client.dnsLookup.wire)}")
        appendLine("transport = ${q(client.transport.wire)}")
        appendLine()

        appendLine("[tls]")
        appendLine("sni = ${q(tls.sni)}")
        appendLine()

        if (client.transport == Transport.REALITY) {
            appendLine("[reality]")
            appendLine("public_key = ${q(reality.publicKey)}")
            appendLine("short_id = ${q(reality.shortId)}")
            appendLine("server_name = ${q(reality.serverName)}")
            appendLine("fingerprint = ${q(reality.fingerprint)}")
            appendLine()
        }

        appendLine("[tun]")
        appendLine("name = ${q(tun.name)}")
        appendLine("mtu = ${tun.mtu}")
        appendLine()

        appendLine("[crypto]")
        appendLine("cipher = ${q(crypto.cipher.wire)}")
        // Required by the Rust deserializer; never read on the mobile path.
        appendLine("private_key_file = ${q("managed-by-host")}")
        appendLine("server_public_key = ${q(crypto.serverPublicKey)}")
        appendLine()

        appendLine("[ipv4]")
        appendLine("enable = ${ipv4.enable}")
        ipv4.address?.let { appendLine("address = ${q(it)}") }
        appendLine("prefix = ${ipv4.prefix}")
        appendLine("route_all = ${ipv4.routeAll}")
        appendLine("routes = ${arr(ipv4.routes)}")
        appendLine("exclude_ips = ${arr(ipv4.excludeIps)}")
        appendLine()

        appendLine("[ipv6]")
        appendLine("enable = ${ipv6.enable}")
        ipv6.address?.let { appendLine("address = ${q(it)}") }
        appendLine("prefix = ${ipv6.prefix}")
        appendLine("route_all = ${ipv6.routeAll}")
        appendLine("routes = ${arr(ipv6.routes)}")
        appendLine("exclude_ips = ${arr(ipv6.excludeIps)}")
        appendLine()

        appendLine("[dns]")
        appendLine("servers_v4 = ${arr(dns.serversV4)}")
        appendLine("servers_v6 = ${arr(dns.serversV6)}")
        appendLine("search = ${arr(dns.search)}")
    }

    private fun q(s: String): String = "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

    private fun arr(items: List<String>): String =
        items.joinToString(prefix = "[", postfix = "]") { q(it) }

    companion object {
        const val DEFAULT_PORT = 51820
        const val DEFAULT_MTU = 1420

        /** IPv4 minimum reassembly size — smaller MTUs break DNS/TCP. */
        const val MIN_MTU = 576

        /** 1500 (wire datagram) − 35 (header 17 + inner length 2 + AEAD tag 16). */
        const val MAX_MTU = 1465
    }
}

@Serializable
data class ClientSection(
    val server: String,
    val transport: Transport = Transport.QUIC,
    @SerialName("prefer_ipv6")
    val preferIpv6: Boolean = false,
    @SerialName("dns_lookup")
    val dnsLookup: DnsLookupMode = DnsLookupMode.AUTO
)

@Serializable
enum class Transport(val wire: String) {
    @SerialName("quic")
    QUIC("quic"),
    @SerialName("tls")
    TLS("tls"),
    @SerialName("reality")
    REALITY("reality")
}

@Serializable
enum class DnsLookupMode(val wire: String) {
    @SerialName("auto")
    AUTO("auto"),
    @SerialName("always")
    ALWAYS("always"),
    @SerialName("never")
    NEVER("never")
}

@Serializable
data class TlsSection(
    val sni: String = "www.cloudflare.com"
)

@Serializable
data class RealitySection(
    @SerialName("public_key")
    val publicKey: String = "",
    @SerialName("short_id")
    val shortId: String = "",
    @SerialName("server_name")
    val serverName: String = "",
    val fingerprint: String = "chrome"
)

@Serializable
data class TunSection(
    val name: String = "tun0",
    val mtu: Int = VpnConfig.DEFAULT_MTU
)

@Serializable
data class CryptoSection(
    val cipher: CipherSuite = CipherSuite.CHACHA20_POLY1305,
    @SerialName("server_public_key")
    val serverPublicKey: String = ""
)

@Serializable
enum class CipherSuite(val wire: String) {
    @SerialName("chacha20-poly1305")
    CHACHA20_POLY1305("chacha20-poly1305"),
    @SerialName("aes-256-gcm")
    AES_256_GCM("aes-256-gcm")
}

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
