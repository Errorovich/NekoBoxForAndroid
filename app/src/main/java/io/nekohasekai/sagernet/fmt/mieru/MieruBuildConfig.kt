package io.nekohasekai.sagernet.fmt.mieru

import moe.matsuri.nb4a.SingBoxOptions

fun buildSingBoxOutboundMieruBean(bean: MieruBean): SingBoxOptions.Outbound_MieruOptions {
    return SingBoxOptions.Outbound_MieruOptions().apply {
        type = "mieru"
        server = bean.serverAddress
        server_port = bean.serverPort
        username = bean.username
        password = bean.password
        // MieruBean.protocol is "TCP" / "UDP"; sing-box mieru uses the same "transport" values.
        if (!bean.protocol.isNullOrBlank()) {
            transport = bean.protocol
        }
    }
}
