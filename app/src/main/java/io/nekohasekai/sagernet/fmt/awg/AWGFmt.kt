package io.nekohasekai.sagernet.fmt.awg

import io.nekohasekai.sagernet.ktx.applyDefaultValues
import moe.matsuri.nb4a.SingBoxOptions
import moe.matsuri.nb4a.utils.Util
import moe.matsuri.nb4a.utils.listByLineOrComma
import org.ini4j.Ini
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.StringReader
import java.util.zip.Inflater

// AmneziaWG is imported from Amnezia's own vpn:// link; it has no share link of
// its own. See parseAmneziaVpnLink below.

/** A magic header spec: either "5" or a "5-10" range. Values are uint32. */
private fun parseMagicHeader(spec: String, standard: Long): LongRange {
    if (spec.isBlank()) return standard..standard
    val parts = spec.split("-")
    require(parts.size <= 2) { "invalid header $spec" }
    val start = parts[0].trim().toLongOrNull() ?: error("invalid header $spec")
    val end = if (parts.size == 2) {
        parts[1].trim().toLongOrNull() ?: error("invalid header $spec")
    } else start
    require(start in 0..0xFFFFFFFFL && end in 0..0xFFFFFFFFL) { "header out of range: $spec" }
    require(end >= start) { "inverted header range: $spec" }
    return start..end
}

/**
 * Rejects the profiles the core cannot survive. The junk range is the important
 * one: the device picks each junk size with rand.Int(jmax - jmin + 1), which
 * panics on a non-positive argument and would take the whole core down rather
 * than just failing to connect.
 */
fun validateAWGBean(bean: AWGBean) {
    if (bean.jc > 0) {
        require(bean.jmin > 0 && bean.jmax > 0) { "jc needs a positive jmin and jmax" }
        require(bean.jmin <= bean.jmax) { "jmin must not exceed jmax" }
    }
    require(bean.jmin <= 0 || bean.jmax <= 0 || bean.jmin <= bean.jmax) {
        "jmin must not exceed jmax"
    }

    // Unset headers fall back to WireGuard's standard message types, so a single
    // custom header can still collide with a default one.
    val headers = listOf(
        parseMagicHeader(bean.h1, 1),
        parseMagicHeader(bean.h2, 2),
        parseMagicHeader(bean.h3, 3),
        parseMagicHeader(bean.h4, 4),
    )
    for (i in headers.indices) {
        for (j in i + 1 until headers.size) {
            val left = headers[i]
            val right = headers[j]
            require(left.first > right.last || right.first > left.last) {
                "H${i + 1} and H${j + 1} must not overlap"
            }
        }
    }
}

fun buildSingBoxEndpointAWGBean(bean: AWGBean): SingBoxOptions.SingBoxOption {
    validateAWGBean(bean)
    return SingBoxOptions.SingBoxOption().apply {
        _hack_config_map["type"] = "awg"
        _hack_config_map["useIntegratedTun"] = false
        _hack_config_map["address"] = bean.localAddress.listByLineOrComma()
        _hack_config_map["private_key"] = bean.privateKey
        if (bean.mtu > 0) _hack_config_map["mtu"] = bean.mtu

        // The core omits every zero/blank one of these, leaving the device on its
        // standard WireGuard behaviour for that knob.
        if (bean.jc > 0) _hack_config_map["jc"] = bean.jc
        if (bean.jmin > 0) _hack_config_map["jmin"] = bean.jmin
        if (bean.jmax > 0) _hack_config_map["jmax"] = bean.jmax
        if (bean.s1 > 0) _hack_config_map["s1"] = bean.s1
        if (bean.s2 > 0) _hack_config_map["s2"] = bean.s2
        if (bean.s3 > 0) _hack_config_map["s3"] = bean.s3
        if (bean.s4 > 0) _hack_config_map["s4"] = bean.s4
        if (bean.h1.isNotBlank()) _hack_config_map["h1"] = bean.h1
        if (bean.h2.isNotBlank()) _hack_config_map["h2"] = bean.h2
        if (bean.h3.isNotBlank()) _hack_config_map["h3"] = bean.h3
        if (bean.h4.isNotBlank()) _hack_config_map["h4"] = bean.h4
        if (bean.i1.isNotBlank()) _hack_config_map["i1"] = bean.i1
        if (bean.i2.isNotBlank()) _hack_config_map["i2"] = bean.i2
        if (bean.i3.isNotBlank()) _hack_config_map["i3"] = bean.i3
        if (bean.i4.isNotBlank()) _hack_config_map["i4"] = bean.i4
        if (bean.i5.isNotBlank()) _hack_config_map["i5"] = bean.i5

        val peer = linkedMapOf<String, Any>(
            "address" to bean.serverAddress,
            "port" to bean.serverPort,
            "public_key" to bean.peerPublicKey,
            "allowed_ips" to listOf("0.0.0.0/0", "::/0"),
        )
        if (bean.peerPreSharedKey.isNotBlank()) {
            peer["pre_shared_key"] = bean.peerPreSharedKey
        }
        if (bean.persistentKeepalive > 0) {
            peer["persistent_keepalive_interval"] = bean.persistentKeepalive
        }
        _hack_config_map["peers"] = listOf(peer)
    }
}

/** Qt's qCompress: a 4-byte big-endian uncompressed size, then a zlib stream. */
private fun qUncompress(raw: ByteArray): String {
    require(raw.size > 4) { "payload too short" }
    val inflater = Inflater()
    inflater.setInput(raw, 4, raw.size - 4)
    val out = ByteArrayOutputStream()
    val buf = ByteArray(8192)
    try {
        while (!inflater.finished()) {
            val n = inflater.inflate(buf)
            if (n == 0 && (inflater.needsInput() || inflater.needsDictionary())) break
            out.write(buf, 0, n)
        }
    } finally {
        inflater.end()
    }
    require(out.size() > 0) { "empty payload" }
    return out.toString("UTF-8")
}

/**
 * Amnezia's official share link:
 *
 *     vpn:// + base64url( [4 bytes BE uncompressed length] + zlib(JSON) )
 *
 * The JSON carries a list of containers; the AmneziaWG one holds `last_config`,
 * itself a JSON *string* whose `config` field is the actual WireGuard .conf. That
 * INI is the source of truth here -- the sibling JSON fields duplicate it and are
 * only used to fill what the INI omits.
 */
fun parseAmneziaVpnLink(url: String): AWGBean {
    val payload = url.substringAfter("vpn://").substringBefore("#").trim()
    val json = JSONObject(qUncompress(Util.b64Decode(payload)))

    val containers = json.optJSONArray("containers") ?: error("no containers in vpn:// link")
    val awg = (0 until containers.length())
        .mapNotNull { containers.optJSONObject(it)?.optJSONObject("awg") }
        .firstOrNull() ?: error("vpn:// link carries no AmneziaWG container")

    val lastConfig = awg.optString("last_config").takeIf { it.isNotBlank() }
        ?.let { JSONObject(it) }
    val conf = Ini(StringReader(lastConfig?.optString("config").orEmpty()))
    val iface = conf["Interface"]
    val peer = conf["Peer"]

    // Prefer the INI, fall back to the JSON fields that duplicate it.
    fun pick(vararg candidates: String?): String =
        candidates.firstOrNull { !it.isNullOrBlank() }.orEmpty()

    fun int(key: String): Int? = iface?.get(key)?.toIntOrNull()
        ?: awg.optString(key).toIntOrNull()

    fun str(key: String): String = pick(iface?.get(key), awg.optString(key))

    return AWGBean().applyDefaultValues().apply {
        name = pick(
            json.optString("description"),
            url.substringAfter("#", ""),
        ).ifBlank { "AmneziaWG" }

        val endpoint = peer?.get("Endpoint").orEmpty()
        serverAddress = pick(
            endpoint.substringBeforeLast(":", ""),
            lastConfig?.optString("hostName"),
            json.optString("hostName"),
        )
        serverPort = endpoint.substringAfterLast(":", "").toIntOrNull()
            ?: lastConfig?.optString("port")?.toIntOrNull()
            ?: awg.optString("port").toIntOrNull()
            ?: 51820

        localAddress = iface?.getAll("Address")
            ?.flatMap { it.split(",") }
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?.takeIf { it.isNotEmpty() }
            ?.joinToString("\n")
            ?: lastConfig?.optString("client_ip").orEmpty()
        privateKey = pick(iface?.get("PrivateKey"), lastConfig?.optString("client_priv_key"))
        peerPublicKey = pick(peer?.get("PublicKey"), lastConfig?.optString("server_pub_key"))
        peerPreSharedKey = pick(peer?.get("PresharedKey"), lastConfig?.optString("psk_key"))

        (iface?.get("MTU")?.toIntOrNull() ?: lastConfig?.optString("mtu")?.toIntOrNull())
            ?.also { mtu = it }
        (peer?.get("PersistentKeepalive")?.toIntOrNull()
            ?: lastConfig?.optString("persistent_keep_alive")?.toIntOrNull())
            ?.also { persistentKeepalive = it }

        int("Jc")?.also { jc = it }
        int("Jmin")?.also { jmin = it }
        int("Jmax")?.also { jmax = it }
        int("S1")?.also { s1 = it }
        int("S2")?.also { s2 = it }
        int("S3")?.also { s3 = it }
        int("S4")?.also { s4 = it }
        h1 = str("H1")
        h2 = str("H2")
        h3 = str("H3")
        h4 = str("H4")
        i1 = str("I1")
        i2 = str("I2")
        i3 = str("I3")
        i4 = str("I4")
        i5 = str("I5")

        validateAWGBean(this)
    }
}
