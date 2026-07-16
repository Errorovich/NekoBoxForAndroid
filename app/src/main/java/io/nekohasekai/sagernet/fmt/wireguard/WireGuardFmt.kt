package io.nekohasekai.sagernet.fmt.wireguard

import io.nekohasekai.sagernet.fmt.AbstractBean
import io.nekohasekai.sagernet.fmt.awg.AWGBean
import io.nekohasekai.sagernet.ktx.linkBuilder
import io.nekohasekai.sagernet.ktx.toLink
import io.nekohasekai.sagernet.ktx.urlSafe
import moe.matsuri.nb4a.SingBoxOptions
import moe.matsuri.nb4a.utils.Util
import moe.matsuri.nb4a.utils.listByLineOrComma
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

// wg:// is the share link the desktop NekoBox (qr243vbi) lineage uses for both
// WireGuard and AmneziaWG -- the same scheme, with enable_amnezia=true plus the
// obfuscation parameters marking the AWG variant. local_address is joined with
// "-" because a comma would break the query. Parsing routes to the right bean.

/** Multiple addresses in one query value, "-"-joined like the desktop does. */
private fun packAddresses(localAddress: String): String =
    localAddress.listByLineOrComma().joinToString("-")

private fun unpackAddresses(value: String): String =
    value.split("-").filter { it.isNotBlank() }.joinToString("\n")

fun WireGuardBean.toUri(): String {
    val builder = linkBuilder().host(serverAddress).port(serverPort)
    builder.addQueryParameter("private_key", privateKey)
    builder.addQueryParameter("peer_public_key", peerPublicKey)
    if (peerPreSharedKey.isNotBlank()) builder.addQueryParameter("pre_shared_key", peerPreSharedKey)
    if (reserved.isNotBlank()) {
        builder.addQueryParameter("reserved", reserved.listByLineOrComma().joinToString("-"))
    }
    builder.addQueryParameter("mtu", mtu.toString())
    if (persistentKeepalive > 0) {
        builder.addQueryParameter("persistent_keepalive", persistentKeepalive.toString())
    }
    if (localAddress.isNotBlank()) builder.addQueryParameter("local_address", packAddresses(localAddress))
    if (name.isNotBlank()) builder.encodedFragment(name.urlSafe())
    return builder.toLink("wg")
}

fun AWGBean.toUri(): String {
    val builder = linkBuilder().host(serverAddress).port(serverPort)
    builder.addQueryParameter("private_key", privateKey)
    builder.addQueryParameter("peer_public_key", peerPublicKey)
    if (peerPreSharedKey.isNotBlank()) builder.addQueryParameter("pre_shared_key", peerPreSharedKey)
    builder.addQueryParameter("mtu", mtu.toString())
    if (persistentKeepalive > 0) {
        builder.addQueryParameter("persistent_keepalive", persistentKeepalive.toString())
    }
    if (localAddress.isNotBlank()) builder.addQueryParameter("local_address", packAddresses(localAddress))

    builder.addQueryParameter("enable_amnezia", "true")
    if (jc > 0) builder.addQueryParameter("junk_packet_count", jc.toString())
    if (jmin > 0) builder.addQueryParameter("junk_packet_min_size", jmin.toString())
    if (jmax > 0) builder.addQueryParameter("junk_packet_max_size", jmax.toString())
    if (s1 > 0) builder.addQueryParameter("init_packet_junk_size", s1.toString())
    if (s2 > 0) builder.addQueryParameter("response_packet_junk_size", s2.toString())
    if (s3 > 0) builder.addQueryParameter("cookie_reply_junk_size", s3.toString())
    if (s4 > 0) builder.addQueryParameter("transport_packet_junk_size", s4.toString())
    if (h1.isNotBlank()) builder.addQueryParameter("init_packet_magic_header", h1)
    if (h2.isNotBlank()) builder.addQueryParameter("response_packet_magic_header", h2)
    if (h3.isNotBlank()) builder.addQueryParameter("cookie_reply_magic_header", h3)
    if (h4.isNotBlank()) builder.addQueryParameter("transport_packet_magic_header", h4)
    if (i1.isNotBlank()) builder.addQueryParameter("i1", i1)
    if (i2.isNotBlank()) builder.addQueryParameter("i2", i2)
    if (i3.isNotBlank()) builder.addQueryParameter("i3", i3)
    if (i4.isNotBlank()) builder.addQueryParameter("i4", i4)
    if (i5.isNotBlank()) builder.addQueryParameter("i5", i5)

    if (name.isNotBlank()) builder.encodedFragment(name.urlSafe())
    return builder.toLink("wg")
}

/**
 * wg:// carries WireGuard or AmneziaWG; enable_amnezia=true selects the latter,
 * which is a different bean here (its obfuscation parameters are not a WireGuard
 * client preference). Returns whichever the link describes.
 */
fun parseWireGuard(server: String): AbstractBean {
    val url = server.replace("wg://", "https://").toHttpUrlOrNull()
        ?: error("invalid wireguard link $server")
    val isAmnezia = url.queryParameter("enable_amnezia").let { it == "true" || it == "1" }
    return if (isAmnezia) parseAwg(url) else parseWg(url)
}

private fun parseWg(url: HttpUrl) = WireGuardBean().apply {
    serverAddress = url.host
    serverPort = url.port
    name = url.fragment
    privateKey = url.queryParameter("private_key").orEmpty()
    peerPublicKey = url.queryParameter("peer_public_key").orEmpty()
    peerPreSharedKey = url.queryParameter("pre_shared_key").orEmpty()
    url.queryParameter("reserved")?.let { reserved = it.replace("-", ",") }
    url.queryParameter("mtu")?.toIntOrNull()?.let { mtu = it }
    url.queryParameter("persistent_keepalive")?.toIntOrNull()?.let { persistentKeepalive = it }
    url.queryParameter("local_address")?.let { localAddress = unpackAddresses(it) }
}

private fun parseAwg(url: HttpUrl) = AWGBean().apply {
    serverAddress = url.host
    serverPort = url.port
    name = url.fragment
    privateKey = url.queryParameter("private_key").orEmpty()
    peerPublicKey = url.queryParameter("peer_public_key").orEmpty()
    peerPreSharedKey = url.queryParameter("pre_shared_key").orEmpty()
    url.queryParameter("mtu")?.toIntOrNull()?.let { mtu = it }
    url.queryParameter("persistent_keepalive")?.toIntOrNull()?.let { persistentKeepalive = it }
    url.queryParameter("local_address")?.let { localAddress = unpackAddresses(it) }
    url.queryParameter("junk_packet_count")?.toIntOrNull()?.let { jc = it }
    url.queryParameter("junk_packet_min_size")?.toIntOrNull()?.let { jmin = it }
    url.queryParameter("junk_packet_max_size")?.toIntOrNull()?.let { jmax = it }
    url.queryParameter("init_packet_junk_size")?.toIntOrNull()?.let { s1 = it }
    url.queryParameter("response_packet_junk_size")?.toIntOrNull()?.let { s2 = it }
    url.queryParameter("cookie_reply_junk_size")?.toIntOrNull()?.let { s3 = it }
    url.queryParameter("transport_packet_junk_size")?.toIntOrNull()?.let { s4 = it }
    url.queryParameter("init_packet_magic_header")?.let { h1 = it }
    url.queryParameter("response_packet_magic_header")?.let { h2 = it }
    url.queryParameter("cookie_reply_magic_header")?.let { h3 = it }
    url.queryParameter("transport_packet_magic_header")?.let { h4 = it }
    url.queryParameter("i1")?.let { i1 = it }
    url.queryParameter("i2")?.let { i2 = it }
    url.queryParameter("i3")?.let { i3 = it }
    url.queryParameter("i4")?.let { i4 = it }
    url.queryParameter("i5")?.let { i5 = it }
}

fun genReserved(anyStr: String): String {
    try {
        val list = anyStr.listByLineOrComma()
        val ba = ByteArray(3)
        if (list.size == 3) {
            list.forEachIndexed { index, s ->
                val i = s
                    .replace("[", "")
                    .replace("]", "")
                    .replace(" ", "")
                    .toIntOrNull() ?: return anyStr
                ba[index] = i.toByte()
            }
            return Util.b64EncodeOneLine(ba)
        } else {
            return anyStr
        }
    } catch (e: Exception) {
        return anyStr
    }
}

fun buildSingBoxEndpointWireguardBean(bean: WireGuardBean): SingBoxOptions.SingBoxOption {
    return SingBoxOptions.SingBoxOption().apply {
        _hack_config_map["type"] = "wireguard"
        _hack_config_map["system"] = false
        _hack_config_map["address"] = bean.localAddress.listByLineOrComma()
        _hack_config_map["private_key"] = bean.privateKey
        _hack_config_map["mtu"] = bean.mtu

        val peer = linkedMapOf<String, Any>(
            "address" to bean.serverAddress,
            "port" to bean.serverPort,
            "public_key" to bean.peerPublicKey,
            "allowed_ips" to listOf("0.0.0.0/0", "::/0"),
        )
        if (bean.peerPreSharedKey.isNotBlank()) {
            peer["pre_shared_key"] = bean.peerPreSharedKey
        }
        if (bean.reserved.isNotBlank()) {
            peer["reserved"] = Util.b64Decode(genReserved(bean.reserved)).map { it.toInt() and 0xff }
        }
        if ((bean.persistentKeepalive ?: 0) > 0) {
            peer["persistent_keepalive_interval"] = bean.persistentKeepalive
        }
        _hack_config_map["peers"] = listOf(peer)
    }
}
