package io.nekohasekai.sagernet.fmt.wireguard

import moe.matsuri.nb4a.SingBoxOptions
import moe.matsuri.nb4a.utils.Util
import moe.matsuri.nb4a.utils.listByLineOrComma

// WireGuard has no agreed share-link format -- every client invents its own, so
// none is emitted or parsed here. Import a profile manually instead.

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
