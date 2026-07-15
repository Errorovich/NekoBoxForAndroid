package moe.matsuri.nb4a.proxy.shadowtls

import io.nekohasekai.sagernet.fmt.v2ray.buildSingBoxOutboundTLS
import io.nekohasekai.sagernet.fmt.v2ray.setTLS
import io.nekohasekai.sagernet.ktx.linkBuilder
import io.nekohasekai.sagernet.ktx.toLink
import io.nekohasekai.sagernet.ktx.urlSafe
import moe.matsuri.nb4a.SingBoxOptions
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

fun parseShadowTLS(server: String): ShadowTLSBean {
    val link = server.replace("shadowtls://", "https://").toHttpUrlOrNull()
        ?: error("invalid ShadowTLS link $server")
    return ShadowTLSBean().apply {
        serverAddress = link.host
        serverPort = link.port
        password = link.username
        version = link.queryParameter("version")?.toIntOrNull() ?: 3
        sni = link.queryParameter("sni") ?: ""
        utlsFingerprint = link.queryParameter("fp") ?: ""
        allowInsecure = link.queryParameter("insecure") in listOf("1", "true")
        name = link.fragment
        setTLS(true)
    }
}

fun ShadowTLSBean.toUri(): String {
    val builder = linkBuilder().host(serverAddress).port(serverPort).username(password)
        .addQueryParameter("version", version.toString())
    if (sni.isNotBlank()) builder.addQueryParameter("sni", sni)
    if (utlsFingerprint.isNotBlank()) builder.addQueryParameter("fp", utlsFingerprint)
    if (allowInsecure) builder.addQueryParameter("insecure", "1")
    if (name.isNotBlank()) builder.encodedFragment(name.urlSafe())
    return builder.toLink("shadowtls")
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
