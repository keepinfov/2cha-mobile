package dev.yaul.twocha.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The AWG transport's wire params must survive the server -> app -> engine trip
 * unchanged. These tests pin the JSON schema the server's mobile export emits
 * (`mobile_config_json`) and the `[awg]` TOML the native engine consumes.
 */
class AwgConfigTest {

    // Mirrors the JSON produced by `2cha` (init_wizard/mobile.rs): snake_case
    // keys, `transport = "awg"`, and an `awg` object with the wire params.
    private val serverExportJson = """
        {
          "client": { "server": "vpn.example.com:51820", "transport": "awg",
                      "prefer_ipv6": false, "dns_lookup": "auto" },
          "tun": { "name": "tun0", "mtu": 1420 },
          "crypto": { "cipher": "chacha20-poly1305", "server_public_key": "AAAABBBBCCCCDDDDEEEEFFFFGGGGHHHHIIIIJJJJKKK=" },
          "ipv4": { "enable": true, "address": "10.0.0.2", "prefix": 24, "route_all": true },
          "ipv6": { "enable": false },
          "dns": { "servers_v4": ["1.1.1.1"], "servers_v6": [], "search": [] },
          "awg": { "h1": 10, "h2": 1073741834, "h3": 2147483658, "h4": 3221225482,
                   "header_span": 16777215,
                   "s1": 24, "s2": 40, "s3": 24, "s4": 16,
                   "jc": 4, "jmin": 64, "jmax": 1024 }
        }
    """.trimIndent()

    @Test
    fun parsesAwgTransportAndParams() {
        val config = ConfigParser.parseJson(serverExportJson)
        assertEquals(Transport.AWG, config.client.transport)
        assertNotNull("awg section must survive import", config.awg)
        val awg = config.awg!!
        assertEquals(3221225482L, awg.h4) // > Int.MAX_VALUE — held as Long
        assertEquals(16777215L, awg.headerSpan)
        assertEquals(1024, awg.jmax)
    }

    @Test
    fun emitsAwgTomlSection() {
        val toml = ConfigParser.parseJson(serverExportJson).toToml()
        assertTrue(toml.contains("transport = \"awg\""))
        assertTrue(toml.contains("[awg]"))
        assertTrue(toml.contains("h4 = 3221225482"))
        assertTrue(toml.contains("header_span = 16777215"))
        assertTrue(toml.contains("jmin = 64"))
        assertTrue(toml.contains("jmax = 1024"))
    }

    @Test
    fun validAwgConfigPasses() {
        assertTrue(ConfigParser.parseJson(serverExportJson).isValid())
    }

    @Test
    fun awgWithoutParamsFailsValidation() {
        val config = VpnConfig(
            client = ClientSection(server = "vpn.example.com:51820", transport = Transport.AWG),
            crypto = CryptoSection(serverPublicKey = "AAAABBBBCCCCDDDDEEEEFFFFGGGGHHHHIIIIJJJJKKK=")
        )
        assertTrue(
            "AWG with no [awg] block must be rejected",
            config.validate().any { it.contains("AWG transport requires") }
        )
    }
}
