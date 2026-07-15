package moe.matsuri.nb4a.proxy.config

import okio.ByteString.Companion.decodeBase64
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/** Imports one arbitrary sing-box outbound or endpoint from a compact link. */
fun parseSingBoxLink(server: String): ConfigBean {
    val link = server.replace("sing-box://", "https://").toHttpUrlOrNull()
        ?: error("invalid sing-box link $server")
    val profileType = when (link.host) {
        "outbound" -> 1
        "endpoint" -> 2
        else -> error("unsupported sing-box profile type ${link.host}")
    }
    val payload = link.pathSegments.firstOrNull()?.takeIf { it.isNotBlank() }
        ?: error("missing sing-box link payload")
    return ConfigBean().apply {
        initializeDefaultValues()
        type = profileType
        config = payload.decodeBase64()?.utf8() ?: error("invalid sing-box link payload")
        name = link.fragment
    }
}
