package io.nekohasekai.sagernet.fmt.trusttunnel

import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.ktx.Logs
import io.nekohasekai.sagernet.ktx.applyDefaultValues
import moe.matsuri.nb4a.SingBoxOptions
import moe.matsuri.nb4a.utils.Util
import java.io.ByteArrayInputStream
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

fun buildSingBoxOutboundTrustTunnelBean(bean: TrustTunnelBean): SingBoxOptions.SingBoxOption {
    return SingBoxOptions.SingBoxOption().apply {
        _hack_config_map["type"] = "trusttunnel"
        _hack_config_map["server"] = bean.serverAddress
        _hack_config_map["server_port"] = bean.serverPort
        _hack_config_map["username"] = bean.username
        _hack_config_map["password"] = bean.password
        if (bean.quic) _hack_config_map["quic"] = true
        if (bean.healthCheck) _hack_config_map["health_check"] = true

        // The core requires TLS and does not pick an ALPN itself, so the upstream
        // protocol has to be spelled out here.
        val tls = linkedMapOf<String, Any>(
            "enabled" to true,
            "alpn" to listOf(if (bean.quic) "h3" else "h2"),
            "insecure" to (bean.allowInsecure || DataStore.globalAllowInsecure),
        )
        if (bean.sni.isNotBlank()) tls["server_name"] = bean.sni
        if (bean.certificates.isNotBlank()) tls["certificate"] = bean.certificates
        _hack_config_map["tls"] = tls
    }
}

/** RFC 9000 §16 variable-length integer, as the deep link spec requires. */
private class TLVReader(private val buf: ByteArray) {

    var pos = 0
        private set

    fun hasRemaining() = pos < buf.size

    fun readVarInt(): Long {
        require(pos < buf.size) { "truncated varint" }
        val first = buf[pos].toInt() and 0xFF
        val size = 1 shl (first ushr 6)
        require(pos + size <= buf.size) { "truncated varint" }
        var value = (first and 0x3F).toLong()
        for (i in 1 until size) {
            value = (value shl 8) or (buf[pos + i].toLong() and 0xFF)
        }
        pos += size
        return value
    }

    fun readBytes(length: Int): ByteArray {
        require(length >= 0 && pos + length <= buf.size) { "truncated value" }
        return buf.copyOfRange(pos, pos + length).also { pos += length }
    }
}

private fun derChainToPem(der: ByteArray): String {
    val factory = CertificateFactory.getInstance("X.509")
    val certificates = factory.generateCertificates(ByteArrayInputStream(der))
    return certificates.filterIsInstance<X509Certificate>().joinToString("\n") {
        buildString {
            append("-----BEGIN CERTIFICATE-----\n")
            append(Util.b64EncodeDefault(it.encoded).trim())
            append("\n-----END CERTIFICATE-----")
        }
    }
}

/**
 * TrustTunnel's documented deep link:
 * <https://github.com/TrustTunnel/TrustTunnel/blob/master/DEEP_LINK.md>
 *
 *     tt://?<base64url payload, unpadded>
 *
 * The payload is binary TLV -- tag and length are RFC 9000 varints -- not a query
 * string. Unknown tags are skipped, as the spec demands, so newer links keep
 * importing.
 */
fun parseTrustTunnel(url: String): TrustTunnelBean {
    val payload = url.substringAfter("tt://").removePrefix("?").substringBefore("#").trim()
    require(payload.isNotBlank()) { "empty tt:// payload" }
    val reader = TLVReader(Util.b64Decode(payload))

    var hostname = ""
    val addresses = mutableListOf<String>()
    var customSni = ""
    var username: String? = null
    var password: String? = null
    var skipVerification = false
    var certificate = ""
    var http3 = false
    var displayName = ""

    while (reader.hasRemaining()) {
        val tag = reader.readVarInt()
        val length = reader.readVarInt()
        require(length <= Int.MAX_VALUE) { "value too large" }
        val value = reader.readBytes(length.toInt())
        when (tag) {
            0x01L -> hostname = value.toString(Charsets.UTF_8)
            0x02L -> addresses.add(value.toString(Charsets.UTF_8))
            0x03L -> customSni = value.toString(Charsets.UTF_8)
            0x05L -> username = value.toString(Charsets.UTF_8)
            0x06L -> password = value.toString(Charsets.UTF_8)
            0x07L -> skipVerification = value.firstOrNull() == 1.toByte()
            0x08L -> certificate = derChainToPem(value)
            0x09L -> http3 = value.firstOrNull() == 2.toByte()

            // Carried by the link but absent from the core's trusttunnel
            // outbound, so importing them would be a lie.
            0x0AL -> if (value.firstOrNull() == 1.toByte()) {
                Logs.w("tt://: anti_dpi is set but the core cannot honour it")
            }

            0x0BL -> Logs.w("tt://: ignoring client_random_prefix, unsupported by the core")
            0x0DL -> Logs.w("tt://: ignoring dns_upstreams, unsupported by the core")

            0x0CL -> displayName = value.toString(Charsets.UTF_8)

            // 0x00 version and 0x04 has_ipv6 carry nothing we act on; anything
            // else is a forward-compatible extension.
            else -> {}
        }
    }

    require(hostname.isNotBlank()) { "tt:// link is missing hostname" }
    require(addresses.isNotEmpty()) { "tt:// link is missing addresses" }
    require(!username.isNullOrBlank()) { "tt:// link is missing username" }
    require(!password.isNullOrBlank()) { "tt:// link is missing password" }

    if (addresses.size > 1) {
        // A profile maps to one sing-box outbound, which dials a single server.
        Logs.w("tt://: using ${addresses[0]}, ignoring ${addresses.size - 1} more address(es)")
    }

    return TrustTunnelBean().applyDefaultValues().apply {
        name = displayName.ifBlank { hostname }
        serverAddress = addresses[0].substringBeforeLast(":", addresses[0])
        serverPort = addresses[0].substringAfterLast(":", "").toIntOrNull()
            ?: error("tt:// address has no port: ${addresses[0]}")
        this.username = username
        this.password = password
        sni = customSni.ifBlank { hostname }
        certificates = certificate
        allowInsecure = skipVerification
        quic = http3
    }
}
