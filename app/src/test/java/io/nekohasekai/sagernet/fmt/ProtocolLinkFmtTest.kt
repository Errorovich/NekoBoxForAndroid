package io.nekohasekai.sagernet.fmt

import io.nekohasekai.sagernet.fmt.mieru.parseMieru
import io.nekohasekai.sagernet.fmt.ssh.SSHBean
import io.nekohasekai.sagernet.fmt.ssh.parseSSH
import io.nekohasekai.sagernet.fmt.wireguard.buildSingBoxEndpointWireguardBean
import io.nekohasekai.sagernet.fmt.wireguard.parseWireGuardLink
import moe.matsuri.nb4a.proxy.shadowtls.parseShadowTLS
import moe.matsuri.nb4a.proxy.config.parseSingBoxLink
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.util.Base64

class ProtocolLinkFmtTest {

    @Test
    fun parsesSshPasswordLink() {
        val bean = parseSSH("ssh://user:pass@example.com:2222#test-ssh")

        assertEquals("example.com", bean.serverAddress)
        assertEquals(2222, bean.serverPort)
        assertEquals("user", bean.username)
        assertEquals("pass", bean.password)
        assertEquals(SSHBean.AUTH_TYPE_PASSWORD, bean.authType)
        assertEquals("test-ssh", bean.name)
    }

    @Test
    fun parsesOfficialMieruSimpleLink() {
        val bean = parseMieru(
            "mierus://user:pass@example.com?profile=test&mtu=1400&port=49165&protocol=TCP#test-mieru"
        )

        assertEquals("example.com", bean.serverAddress)
        assertEquals(49165, bean.serverPort)
        assertEquals("TCP", bean.protocol)
        assertEquals(1400, bean.mtu)
        assertEquals("test-mieru", bean.name)
    }

    @Test
    fun parsesWireGuardLinkAndBuildsEndpoint() {
        val bean = parseWireGuardLink(
            "wireguard://private%3D@example.com:51820" +
                "?address=10.7.0.2%2F32&public_key=public%3D&mtu=1420#test-wireguard"
        )
        val endpoint = buildSingBoxEndpointWireguardBean(bean).asMap()

        assertEquals("wireguard", endpoint["type"])
        assertEquals(false, endpoint["system"])
        assertEquals(listOf("10.7.0.2/32"), endpoint["address"])
        assertEquals("private=", endpoint["private_key"])
        assertEquals(1420L, endpoint["mtu"])
        val peer = (endpoint["peers"] as List<*>).single() as Map<*, *>
        assertEquals("example.com", peer["address"])
        assertEquals(51820L, peer["port"])
        assertEquals("public=", peer["public_key"])
    }

    @Test
    fun parsesShadowTlsTransportLink() {
        val bean = parseShadowTLS(
            "shadowtls://secret@example.com:443?version=3&sni=www.microsoft.com&fp=chrome#transport"
        )

        assertEquals("example.com", bean.serverAddress)
        assertEquals(443, bean.serverPort)
        assertEquals(3, bean.version)
        assertEquals("secret", bean.password)
        assertEquals("www.microsoft.com", bean.sni)
        assertEquals("chrome", bean.utlsFingerprint)
        assertFalse(bean.allowInsecure)
        assertEquals("transport", bean.name)
    }

    @Test
    fun parsesGenericSingBoxEndpointLink() {
        val json = """{"type":"awg","address":["10.8.0.2/32"]}"""
        val payload = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(json.toByteArray())
        val bean = parseSingBoxLink("sing-box://endpoint/$payload#test-awg")

        assertEquals(2, bean.type)
        assertEquals(json, bean.config)
        assertEquals("test-awg", bean.name)
    }
}
