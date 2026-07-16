/******************************************************************************
 * Copyright (C) 2022 by nekohasekai <contact-git@sekai.icu>                  *
 *                                                                            *
 * This program is free software: you can redistribute it and/or modify       *
 * it under the terms of the GNU General Public License as published by       *
 * the Free Software Foundation, either version 3 of the License, or          *
 *  (at your option) any later version.                                       *
 *                                                                            *
 * This program is distributed in the hope that it will be useful,            *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of             *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the              *
 * GNU General Public License for more details.                               *
 *                                                                            *
 * You should have received a copy of the GNU General Public License          *
 * along with this program. If not, see <http://www.gnu.org/licenses/>.       *
 *                                                                            *
 ******************************************************************************/

package io.nekohasekai.sagernet.fmt.mieru

import io.nekohasekai.sagernet.ktx.linkBuilder
import io.nekohasekai.sagernet.ktx.toLink
import io.nekohasekai.sagernet.ktx.urlSafe
import io.nekohasekai.sagernet.ktx.toStringPretty
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONArray
import org.json.JSONObject

fun parseMieru(server: String): MieruBean {
    val link = server.replace("mierus://", "https://").toHttpUrlOrNull()
        ?: error("invalid mieru simple link $server")
    val ports = link.queryParameterValues("port").filterNotNull().filter { it.isNotBlank() }
    require(ports.isNotEmpty()) { "missing mieru port" }
    return MieruBean().apply {
        serverAddress = link.host
        // The first port/protocol pair is the primary; the rest ride along in
        // serverPorts sharing the same transport.
        serverPort = ports.first().substringBefore("-").toIntOrNull() ?: error("bad mieru port")
        serverPorts = ports.drop(1).joinToString(",")
        username = link.username
        password = link.password
        protocol = link.queryParameterValues("protocol").firstOrNull()?.uppercase() ?: "TCP"
        mtu = link.queryParameter("mtu")?.toIntOrNull() ?: 1400
        multiplexing = link.queryParameter("multiplexing").orEmpty()
        trafficPattern = link.queryParameter("traffic-pattern").orEmpty()
        name = link.fragment ?: link.queryParameter("profile")
    }
}

fun MieruBean.toUri(): String {
    val builder = linkBuilder()
        .host(serverAddress)
        .username(username)
        .password(password)
        .addQueryParameter("profile", name.ifBlank { "default" })
        .addQueryParameter("mtu", mtu.toString())
    // Emit port/protocol positionally: the primary first, then each extra range,
    // all sharing this profile's transport (the simple link pairs them by index).
    builder.addQueryParameter("port", serverPort.toString())
    builder.addQueryParameter("protocol", protocol.uppercase())
    serverPorts.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach {
        builder.addQueryParameter("port", it)
        builder.addQueryParameter("protocol", protocol.uppercase())
    }
    if (multiplexing.isNotBlank()) builder.addQueryParameter("multiplexing", multiplexing)
    if (trafficPattern.isNotBlank()) builder.addQueryParameter("traffic-pattern", trafficPattern)
    if (name.isNotBlank()) builder.encodedFragment(name.urlSafe())
    return builder.toLink("mierus", appendDefaultPort = false)
}

fun MieruBean.buildMieruConfig(port: Int): String {
    val serverInfo = JSONArray().apply {
        put(JSONObject().apply {
            put("ipAddress", finalAddress)
            put("portBindings", JSONArray().apply {
                put(JSONObject().apply {
                    put("port", finalPort)
                    put("protocol", protocol)
                })
            })
        })
    }
    return JSONObject().apply {
        put("activeProfile", "default")
        put("socks5Port", port)
        // TODO: follow NekoBox logging level.
        put("loggingLevel", "INFO")
        put("profiles", JSONArray().apply {
            put(JSONObject().apply {
                put("profileName", "default")
                put("user", JSONObject().apply {
                    put("name", username)
                    put("password", password)
                })
                put("servers", serverInfo)
                put("mtu", mtu)
            })
        })
    }.toStringPretty()
}
