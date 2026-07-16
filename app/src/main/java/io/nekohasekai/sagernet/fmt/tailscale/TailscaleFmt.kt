package io.nekohasekai.sagernet.fmt.tailscale

import moe.matsuri.nb4a.SingBoxOptions

// Tailscale has no share link: a profile is an authenticated node in someone's
// tailnet, not a server anyone can hand out.

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
