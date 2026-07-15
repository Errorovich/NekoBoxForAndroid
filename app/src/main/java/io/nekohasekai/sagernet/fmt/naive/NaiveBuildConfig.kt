package io.nekohasekai.sagernet.fmt.naive

import io.nekohasekai.sagernet.ktx.blankAsNull
import moe.matsuri.nb4a.SingBoxOptions

fun buildSingBoxOutboundNaiveBean(bean: NaiveBean): SingBoxOptions.Outbound_NaiveOptions {
    return SingBoxOptions.Outbound_NaiveOptions().apply {
        type = "naive"
        server = bean.serverAddress
        server_port = bean.serverPort
        username = bean.username
        password = bean.password

        if (bean.insecureConcurrency != null && bean.insecureConcurrency > 0) {
            insecure_concurrency = bean.insecureConcurrency
        }

        // NaiveBean.proto is "https" or "quic"
        if (bean.proto == "quic") {
            quic = true
        }

        bean.extraHeaders.blankAsNull()?.let { raw ->
            val headers = raw.split("\n").mapNotNull { line ->
                val idx = line.indexOf(':')
                if (idx <= 0) null else line.substring(0, idx).trim() to line.substring(idx + 1).trim()
            }.toMap()
            if (headers.isNotEmpty()) extra_headers = headers
        }

        tls = SingBoxOptions.OutboundTLSOptions().apply {
            enabled = true
            server_name = bean.sni.blankAsNull()
            bean.certificates.blankAsNull()?.let {
                certificate = it
            }
        }
    }
}
