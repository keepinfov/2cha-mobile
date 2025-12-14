package dev.yaul.twocha.config

import kotlinx.serialization.json.Json
import dev.yaul.twocha.crypto.CipherSuite as CryptoCipherSuite

/**
 * Configuration parser supporting TOML and JSON formats
 */
object ConfigParser {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        encodeDefaults = true
    }

    /**
     * Parse configuration from TOML string
     */
    fun parseToml(tomlContent: String): VpnConfig {
        // Simple TOML parser - for production, use ktoml library
        val lines = tomlContent.lines()
        var currentSection = ""
        val values = mutableMapOf<String, String>()

        for (line in lines) {
            val trimmed = line.trim()

            // Skip comments and empty lines
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue

            val cleaned = trimmed.substringBefore("#").trim()
            if (cleaned.isEmpty()) continue

            // Section header
            if (cleaned.startsWith("[") && cleaned.endsWith("]")) {
                currentSection = cleaned.substring(1, cleaned.length - 1)
                continue
            }

            // Key-value pair
            if (cleaned.contains("=")) {
                val (key, value) = cleaned.split("=", limit = 2)
                val fullKey = if (currentSection.isNotEmpty()) "$currentSection.${key.trim()}" else key.trim()
                values[fullKey] = value.trim().removeSurrounding("\"")
            }
        }

        return buildConfigFromMap(values)
    }

    /**
     * Parse configuration from JSON string
     */
    fun parseJson(jsonContent: String): VpnConfig {
        return json.decodeFromString(jsonContent)
    }

    /**
     * Serialize configuration to JSON
     */
    fun toJson(config: VpnConfig): String {
        return json.encodeToString(VpnConfig.serializer(), config)
    }

    /**
     * Create a default configuration
     */
    fun createDefault(): VpnConfig {
        return VpnConfig(
            client = ClientSection(server = ""),
            crypto = CryptoSection(key = "")
        )
    }

    /**
     * Create a sample configuration for testing
     */
    fun createSample(): VpnConfig {
        return VpnConfig(
            client = ClientSection(
                server = "vpn.example.com:51820",
                preferIpv6 = false,
                dnsLookup = DnsLookupMode.AUTO
            ),
            tun = TunSection(
                name = "tun0",
                mtu = 1420,
                queueLen = 500
            ),
            crypto = CryptoSection(
                cipher = CipherSuite.CHACHA20_POLY1305,
                key = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
            ),
            ipv4 = Ipv4Section(
                enable = true,
                address = "10.0.0.2",
                prefix = 24,
                routeAll = false
            ),
            ipv6 = Ipv6Section(
                enable = false
            ),
            dns = DnsSection(
                serversV4 = listOf("1.1.1.1", "8.8.8.8")
            ),
            timeouts = TimeoutsSection(
                keepalive = 25
            )
        )
    }

    private fun buildConfigFromMap(values: Map<String, String>): VpnConfig {
        return VpnConfig(
            client = ClientSection(
                server = values["client.server"] ?: "",
                preferIpv6 = values["client.prefer_ipv6"]?.toBoolean() ?: false,
                dnsLookup = when (values["client.dns_lookup"]?.lowercase()) {
                    "always", "true" -> DnsLookupMode.ALWAYS
                    "never", "false" -> DnsLookupMode.NEVER
                    else -> DnsLookupMode.AUTO
                }
            ),
            tun = TunSection(
                name = values["tun.name"] ?: "tun0",
                mtu = values["tun.mtu"]?.toIntOrNull() ?: 1420,
                queueLen = values["tun.queue_len"]?.toIntOrNull() ?: 500
            ),
            crypto = CryptoSection(
                cipher = when (values["crypto.cipher"]?.lowercase()) {
                    "aes-256-gcm" -> CipherSuite.AES_256_GCM
                    else -> CipherSuite.CHACHA20_POLY1305
                },
                key = values["crypto.key"],
                keyFile = values["crypto.key_file"]
            ),
            ipv4 = Ipv4Section(
                enable = values["ipv4.enable"]?.toBoolean() ?: true,
                address = values["ipv4.address"] ?: "10.0.0.2",
                prefix = values["ipv4.prefix"]?.toIntOrNull() ?: 24,
                routeAll = values["ipv4.route_all"]?.toBoolean() ?: false,
                routes = parseStringList(values["ipv4.routes"]),
                excludeIps = parseStringList(values["ipv4.exclude_ips"])
            ),
            ipv6 = Ipv6Section(
                enable = values["ipv6.enable"]?.toBoolean() ?: false,
                address = values["ipv6.address"],
                prefix = values["ipv6.prefix"]?.toIntOrNull() ?: 64,
                routeAll = values["ipv6.route_all"]?.toBoolean() ?: false,
                routes = parseStringList(values["ipv6.routes"]),
                excludeIps = parseStringList(values["ipv6.exclude_ips"])
            ),
            dns = DnsSection(
                serversV4 = parseStringList(values["dns.servers_v4"]),
                serversV6 = parseStringList(values["dns.servers_v6"]),
                search = parseStringList(values["dns.search"])
            ),
            performance = PerformanceSection(
                socketRecvBuffer = values["performance.socket_recv_buffer"]?.toIntOrNull() ?: 2097152,
                socketSendBuffer = values["performance.socket_send_buffer"]?.toIntOrNull() ?: 2097152,
                batchSize = values["performance.batch_size"]?.toIntOrNull() ?: 32
            ),
            timeouts = TimeoutsSection(
                keepalive = values["timeouts.keepalive"]?.toLongOrNull() ?: 25
            ),
            logging = LoggingSection(
                level = values["logging.level"] ?: "info"
            )
        )
    }

    private fun parseStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return value
            .removeSurrounding("[", "]")
            .split(",")
            .map { it.trim().removeSurrounding("\"") }
            .filter { it.isNotBlank() }
    }
}