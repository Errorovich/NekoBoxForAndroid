package io.nekohasekai.sagernet.bg.proto

import io.nekohasekai.sagernet.bg.AbstractInstance
import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.database.ProxyEntity
import io.nekohasekai.sagernet.fmt.ConfigBuildResult
import io.nekohasekai.sagernet.fmt.buildConfig
import io.nekohasekai.sagernet.ktx.*
import libcore.BoxInstance
import libcore.Libcore
import moe.matsuri.nb4a.net.LocalResolverImpl

abstract class BoxInstance(
    val profile: ProxyEntity
) : AbstractInstance {

    lateinit var config: ConfigBuildResult
    lateinit var box: BoxInstance

    fun isInitialized(): Boolean {
        return ::config.isInitialized && ::box.isInitialized
    }

    protected open fun buildConfig() {
        config = buildConfig(profile)
        DataStore.mixedInboundAuthed = DataStore.mixedInboundNeedsAuth
    }

    protected open suspend fun loadConfig() {
        box = Libcore.newSingBoxInstance(config.config, LocalResolverImpl)
    }

    open suspend fun init() {
        buildConfig()
        loadConfig()
    }

    override fun launch() {
        box.start()
    }

    override fun close() {
        if (::box.isInitialized) {
            box.close()
        }
    }

}
