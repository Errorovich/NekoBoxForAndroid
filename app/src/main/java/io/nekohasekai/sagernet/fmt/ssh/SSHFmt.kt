package io.nekohasekai.sagernet.fmt.ssh

import io.nekohasekai.sagernet.ktx.linkBuilder
import io.nekohasekai.sagernet.ktx.toLink
import io.nekohasekai.sagernet.ktx.urlSafe
import moe.matsuri.nb4a.SingBoxOptions
import moe.matsuri.nb4a.utils.listByLineOrComma
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.net.URI

// Only the IANA-registered ssh://user:password@host:port form is handled. Key
// material is deliberately not carried in a link: there is no standard for it,
// and a share link ends up in QR codes, clipboards and logs, so a private key
// must be entered on the profile screen instead.

fun parseSSH(server: String): SSHBean {
    val link = server.replace("ssh://", "https://").toHttpUrlOrNull()
        ?: error("invalid SSH link $server")
    return SSHBean().apply {
        serverAddress = link.host
        serverPort = URI(server).port.takeIf { it > 0 } ?: 22
        username = link.username.ifBlank { "root" }
        password = link.password
        authType = if (password.isBlank()) SSHBean.AUTH_TYPE_NONE else SSHBean.AUTH_TYPE_PASSWORD
        name = link.fragment
    }
}

fun SSHBean.toUri(): String {
    val builder = linkBuilder().host(serverAddress).port(serverPort).username(username)
    if (authType == SSHBean.AUTH_TYPE_PASSWORD) builder.password(password)
    if (name.isNotBlank()) builder.encodedFragment(name.urlSafe())
    return builder.toLink("ssh")
}

fun buildSingBoxOutboundSSHBean(bean: SSHBean): SingBoxOptions.Outbound_SSHOptions {
    return SingBoxOptions.Outbound_SSHOptions().apply {
        type = "ssh"
        server = bean.serverAddress
        server_port = bean.serverPort
        user = bean.username
        if (bean.publicKey.isNotBlank()) {
            host_key = bean.publicKey.listByLineOrComma()
        }
        when (bean.authType) {
            SSHBean.AUTH_TYPE_PRIVATE_KEY -> {
                private_key = bean.privateKey
                private_key_passphrase = bean.privateKeyPassphrase
            }
            else -> {
                password = bean.password
            }
        }
    }
}
