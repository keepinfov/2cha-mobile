package dev.yaul.twocha.config

import kotlinx.serialization.json.Json

/**
 * (De)serialization for the persisted v4 [VpnConfig]. The app stores config as
 * JSON (DataStore / intent extras); the TOML the native engine consumes is
 * produced by [VpnConfig.toToml].
 */
object ConfigParser {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        encodeDefaults = true
    }

    fun parseJson(jsonContent: String): VpnConfig =
        json.decodeFromString(VpnConfig.serializer(), jsonContent)

    fun toJson(config: VpnConfig): String =
        json.encodeToString(VpnConfig.serializer(), config)

    fun createDefault(): VpnConfig = VpnConfig(
        client = ClientSection(server = ""),
        crypto = CryptoSection(serverPublicKey = "")
    )

    fun createSample(): VpnConfig = VpnConfig(
        client = ClientSection(
            server = "vpn.example.com:51820",
            transport = Transport.QUIC,
            preferIpv6 = false,
            dnsLookup = DnsLookupMode.AUTO
        ),
        tun = TunSection(name = "tun0", mtu = 1420),
        crypto = CryptoSection(
            cipher = CipherSuite.CHACHA20_POLY1305,
            serverPublicKey = ""
        ),
        ipv4 = Ipv4Section(enable = true, address = "10.0.0.2", prefix = 24, routeAll = true),
        ipv6 = Ipv6Section(enable = false),
        dns = DnsSection(serversV4 = listOf("1.1.1.1", "8.8.8.8"))
    )
}
