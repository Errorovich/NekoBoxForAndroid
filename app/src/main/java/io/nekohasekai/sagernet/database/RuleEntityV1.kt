package io.nekohasekai.sagernet.database

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * The pre-versioning ([version] < 2) on-wire layout of a route rule, matching the
 * upstream MatsuriDayo [RuleEntity] field order — i.e. without this fork's
 * `ruleset` (inserted before `outbound`) and `gateway` (appended last) fields.
 *
 * `@Parcelize` marshalling is positional, so a version-1 backup/route export must
 * be read back with exactly these fields in this order; reading it as the current
 * [RuleEntity] would misalign at `ruleset` and crash (a bypass rule's `outbound`
 * of -1 lands in the non-null `ruleset` String as a null). This mirror class lets
 * the same codegen decode that layout, then [toRuleEntity] maps it forward with
 * `ruleset` empty and `gateway` off.
 */
@Parcelize
data class RuleEntityV1(
    var id: Long = 0L,
    var name: String = "",
    var config: String = "",
    var userOrder: Long = 0L,
    var enabled: Boolean = false,
    var domains: String = "",
    var ip: String = "",
    var port: String = "",
    var sourcePort: String = "",
    var network: String = "",
    var source: String = "",
    var protocol: String = "",
    var outbound: Long = 0,
    var packages: Set<String> = emptySet(),
) : Parcelable {

    fun toRuleEntity() = RuleEntity(
        id = id,
        name = name,
        config = config,
        userOrder = userOrder,
        enabled = enabled,
        domains = domains,
        ip = ip,
        port = port,
        sourcePort = sourcePort,
        network = network,
        source = source,
        protocol = protocol,
        ruleset = "",
        outbound = outbound,
        packages = packages,
        gateway = false,
    )
}
