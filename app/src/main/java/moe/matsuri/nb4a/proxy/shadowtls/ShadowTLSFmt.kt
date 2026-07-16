package moe.matsuri.nb4a.proxy.shadowtls

import io.nekohasekai.sagernet.fmt.v2ray.buildSingBoxOutboundTLS
import io.nekohasekai.sagernet.ktx.linkBuilder
import io.nekohasekai.sagernet.ktx.toLink
import io.nekohasekai.sagernet.ktx.urlSafe
import moe.matsuri.nb4a.SingBoxOptions
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

// shadowtls:// is the share link the desktop NekoBox (qr243vbi) lineage uses. It
// only describes the camouflage layer -- the inner proxy it must wrap is not
// part of the link, exactly as on the desktop; chain it to one after importing.

fun ShadowTLSBean.toUri(): String {
    val builder = linkBuilder().username(password).host(serverAddress).port(serverPort)
    builder.addQueryParameter("version", version.toString())
    builder.addQueryParameter("security", "tls")
    if (!sni.isNullOrBlank()) builder.addQueryParameter("sni", sni)
    if (!alpn.isNullOrBlank()) builder.addQueryParameter("alpn", alpn)
    if (allowInsecure == true) builder.addQueryParameter("insecure", "1")
    if (!utlsFingerprint.isNullOrBlank()) builder.addQueryParameter("fp", utlsFingerprint)
    if (name.isNotBlank()) builder.encodedFragment(name.urlSafe())
    return builder.toLink("shadowtls")
}

fun parseShadowTLS(server: String): ShadowTLSBean {
    val url = server.replace("shadowtls://", "https://").toHttpUrlOrNull()
        ?: error("invalid shadowtls link $server")
    return ShadowTLSBean().apply {
        serverAddress = url.host
        serverPort = url.port
        password = url.username
        name = url.fragment
        url.queryParameter("version")?.toIntOrNull()?.let { version = it }
        url.queryParameter("sni")?.let { if (it.isNotBlank()) sni = it }
        url.queryParameter("alpn")?.let { if (it != "none" && it.isNotBlank()) alpn = it }
        allowInsecure = url.queryParameter("insecure").let { it == "1" || it == "true" }
        url.queryParameter("fp")?.let { if (it.isNotBlank()) utlsFingerprint = it }
    }
}

fun buildSingBoxOutboundShadowTLSBean(bean: ShadowTLSBean): SingBoxOptions.Outbound_ShadowTLSOptions {
    return SingBoxOptions.Outbound_ShadowTLSOptions().apply {
        type = "shadowtls"
        server = bean.serverAddress
        server_port = bean.serverPort
        version = bean.version
        password = bean.password
        tls = buildSingBoxOutboundTLS(bean)
    }
}
