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
        if (!bean.multiplexing.isNullOrBlank()) {
            multiplexing = bean.multiplexing
        }
        if (!bean.trafficPattern.isNullOrBlank()) {
            traffic_pattern = bean.trafficPattern
        }
        if (!bean.serverPorts.isNullOrBlank()) {
            // The core parses each entry strictly as "begin-end" (fmt.Sscanf
            // "%d-%d"), so a lone port has to be widened to a one-wide range.
            server_ports = bean.serverPorts.split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .map { if (it.contains("-")) it else "$it-$it" }
        }
        // sing-box mieru has no MTU option; it lives only in the mieru-native
        // config, so bean.mtu is intentionally not forwarded here.
    }
}
