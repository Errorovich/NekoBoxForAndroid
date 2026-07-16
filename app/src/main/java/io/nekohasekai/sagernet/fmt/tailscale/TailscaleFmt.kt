package io.nekohasekai.sagernet.fmt.tailscale

import io.nekohasekai.sagernet.ktx.urlSafe
import moe.matsuri.nb4a.SingBoxOptions
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

// ts:// is the share link the desktop NekoBox (qr243vbi) lineage uses. It is not
// a normal authority URL -- the host is the literal "tailscale" and everything
// lives in the query -- so it is built and parsed by hand rather than through
// HttpUrl's authority machinery. state_directory is deliberately omitted: it is
// a local path scoped per profile, meaningless to another device.

fun TailscaleBean.toUri(): String {
    val q = StringBuilder()
    fun add(key: String, value: String) {
        if (q.isNotEmpty()) q.append('&')
        q.append(key).append('=').append(value.urlSafe())
    }
    if (authKey.isNotBlank()) add("auth_key", authKey)
    if (controlURL.isNotBlank()) add("control_url", controlURL)
    if (hostname.isNotBlank()) add("hostname", hostname)
    add("ephemeral", if (ephemeral) "true" else "false")
    add("accept_routes", if (acceptRoutes) "true" else "false")
    if (exitNode.isNotBlank()) add("exit_node", exitNode)
    add("exit_node_allow_lan_access", if (exitNodeAllowLANAccess) "true" else "false")

    val fragment = if (name.isNotBlank()) "#" + name.urlSafe() else ""
    return "ts://tailscale?$q$fragment"
}

fun parseTailscale(server: String): TailscaleBean {
    val url = server.replace("ts://", "https://").toHttpUrlOrNull()
        ?: error("invalid tailscale link $server")
    require(url.host == "tailscale") { "unexpected tailscale link host ${url.host}" }
    return TailscaleBean().apply {
        name = url.fragment.orEmpty()
        authKey = url.queryParameter("auth_key").orEmpty()
        controlURL = url.queryParameter("control_url").orEmpty()
        hostname = url.queryParameter("hostname").orEmpty()
        ephemeral = url.queryParameter("ephemeral") == "true"
        // accept_routes defaults on, so a link that omits it should stay on.
        acceptRoutes = url.queryParameter("accept_routes") != "false"
        exitNode = url.queryParameter("exit_node").orEmpty()
        exitNodeAllowLANAccess = url.queryParameter("exit_node_allow_lan_access") == "true"
    }
}

/**
 * @param profileId scopes the node state. Two profiles sharing one state
 * directory would fight over a single node identity, so each gets its own.
 */
fun buildSingBoxEndpointTailscaleBean(
    bean: TailscaleBean,
    profileId: Long,
): SingBoxOptions.SingBoxOption {
    if (bean.controlURL.isNotBlank()) {
        // The core reads the host out of a parsed URL and silently treats a
        // scheme-less one as having no host, which fails much later.
        require(bean.controlURL.startsWith("http://") || bean.controlURL.startsWith("https://")) {
            "control URL must start with http:// or https://"
        }
    }
    return SingBoxOptions.SingBoxOption().apply {
        _hack_config_map["type"] = "tailscale"
        _hack_config_map["state_directory"] = "tailscale/$profileId"
        if (bean.authKey.isNotBlank()) _hack_config_map["auth_key"] = bean.authKey
        if (bean.controlURL.isNotBlank()) _hack_config_map["control_url"] = bean.controlURL
        if (bean.hostname.isNotBlank()) _hack_config_map["hostname"] = bean.hostname
        if (bean.ephemeral) _hack_config_map["ephemeral"] = true
        if (bean.acceptRoutes) _hack_config_map["accept_routes"] = true
        if (bean.exitNode.isNotBlank()) {
            _hack_config_map["exit_node"] = bean.exitNode
            if (bean.exitNodeAllowLANAccess) {
                _hack_config_map["exit_node_allow_lan_access"] = true
            }
        }
    }
}
