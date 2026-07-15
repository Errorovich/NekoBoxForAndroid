package io.nekohasekai.sagernet.fmt.ssh

import io.nekohasekai.sagernet.ktx.linkBuilder
import io.nekohasekai.sagernet.ktx.toLink
import io.nekohasekai.sagernet.ktx.urlSafe
import moe.matsuri.nb4a.SingBoxOptions
import moe.matsuri.nb4a.utils.listByLineOrComma
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.net.URI

fun parseSSH(server: String): SSHBean {
    val link = server.replace("ssh://", "https://").toHttpUrlOrNull()
        ?: error("invalid SSH link $server")
    return SSHBean().apply {
        serverAddress = link.host
        serverPort = URI(server).port.takeIf { it > 0 } ?: 22
        username = link.username.ifBlank { "root" }
        password = link.password
        authType = if (password.isBlank()) SSHBean.AUTH_TYPE_NONE else SSHBean.AUTH_TYPE_PASSWORD
        link.queryParameter("private_key")?.let {
            privateKey = it
            privateKeyPassphrase = link.queryParameter("private_key_passphrase") ?: ""
            authType = SSHBean.AUTH_TYPE_PRIVATE_KEY
        }
        publicKey = link.queryParameterValues("host_key").joinToString("\n")
        name = link.fragment
    }
}

fun SSHBean.toUri(): String {
    val builder = linkBuilder().host(serverAddress).port(serverPort).username(username)
    when (authType) {
        SSHBean.AUTH_TYPE_PASSWORD -> builder.password(password)
        SSHBean.AUTH_TYPE_PRIVATE_KEY -> {
            builder.addQueryParameter("private_key", privateKey)
            if (privateKeyPassphrase.isNotBlank()) {
                builder.addQueryParameter("private_key_passphrase", privateKeyPassphrase)
            }
        }
    }
    publicKey.listByLineOrComma().forEach { builder.addQueryParameter("host_key", it) }
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
