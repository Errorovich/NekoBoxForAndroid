package io.nekohasekai.sagernet.fmt.wireguard

import io.nekohasekai.sagernet.ktx.linkBuilder
import io.nekohasekai.sagernet.ktx.toLink
import io.nekohasekai.sagernet.ktx.urlSafe
import moe.matsuri.nb4a.SingBoxOptions
import moe.matsuri.nb4a.utils.Util
import moe.matsuri.nb4a.utils.listByLineOrComma
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

fun parseWireGuardLink(server: String): WireGuardBean {
    val link = server.replace("wireguard://", "https://").replace("wg://", "https://")
        .toHttpUrlOrNull() ?: error("invalid WireGuard link $server")
    return WireGuardBean().apply {
        serverAddress = link.host
        serverPort = link.port
        privateKey = link.username
        localAddress = link.queryParameterValues("address").joinToString("\n")
        peerPublicKey = link.queryParameter("public_key") ?: error("missing WireGuard public key")
        peerPreSharedKey = link.queryParameter("pre_shared_key") ?: ""
        mtu = link.queryParameter("mtu")?.toIntOrNull() ?: 1420
        reserved = link.queryParameter("reserved") ?: ""
        name = link.fragment
    }
}

fun WireGuardBean.toUri(): String {
    val builder = linkBuilder().host(serverAddress).port(serverPort).username(privateKey)
    localAddress.listByLineOrComma().forEach { builder.addQueryParameter("address", it) }
    builder.addQueryParameter("public_key", peerPublicKey)
    if (peerPreSharedKey.isNotBlank()) builder.addQueryParameter("pre_shared_key", peerPreSharedKey)
    if (mtu > 0) builder.addQueryParameter("mtu", mtu.toString())
    if (reserved.isNotBlank()) builder.addQueryParameter("reserved", reserved)
    if (name.isNotBlank()) builder.encodedFragment(name.urlSafe())
    return builder.toLink("wireguard")
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
        _hack_config_map["peers"] = listOf(peer)
    }
}
