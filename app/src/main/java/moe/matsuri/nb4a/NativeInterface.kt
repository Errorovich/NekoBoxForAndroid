package moe.matsuri.nb4a

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Build.VERSION_CODES
import androidx.annotation.RequiresApi
import io.nekohasekai.sagernet.SagerNet
import org.json.JSONArray
import org.json.JSONObject
import java.net.NetworkInterface
import io.nekohasekai.sagernet.bg.ServiceNotification
import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.database.SagerDatabase
import io.nekohasekai.sagernet.ktx.Logs
import io.nekohasekai.sagernet.ktx.app
import io.nekohasekai.sagernet.ktx.runOnDefaultDispatcher
import io.nekohasekai.sagernet.utils.PackageCache
import libcore.BoxPlatformInterface
import libcore.Libcore
import libcore.NB4AInterface
import java.net.InetSocketAddress

class NativeInterface : BoxPlatformInterface, NB4AInterface {

    //  libbox interface

    override fun autoDetectInterfaceControl(fd: Int) {
        DataStore.vpnService?.protect(fd)
    }

    override fun openTun(singTunOptionsJson: String, tunPlatformOptionsJson: String): Long {
        if (DataStore.vpnService == null) {
            throw Exception("no VpnService")
        }
        return DataStore.vpnService!!.startVpn(singTunOptionsJson, tunPlatformOptionsJson).toLong()
    }

    override fun useProcFS(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun findConnectionOwner(
        ipProto: Int, srcIp: String, srcPort: Int, destIp: String, destPort: Int
    ): Int {
        return SagerNet.connectivity.getConnectionOwnerUid(
            ipProto, InetSocketAddress(srcIp, srcPort), InetSocketAddress(destIp, destPort)
        )
    }

    override fun packageNameByUid(uid: Int): String {
        PackageCache.awaitLoadSync()

        if (uid <= 1000L) {
            return "android"
        }

        val packageNames = PackageCache.uidMap[uid]
        if (!packageNames.isNullOrEmpty()) for (packageName in packageNames) {
            return packageName
        }

        error("unknown uid $uid")
    }

    override fun uidByPackageName(packageName: String): Int {
        PackageCache.awaitLoadSync()
        return PackageCache[packageName] ?: 0
    }

    // TODO: 'getter for connectionInfo: WifiInfo!' is deprecated
    override fun wifiState(): String {
        val wifiManager =
            app.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val connectionInfo = wifiManager.connectionInfo
        return "${connectionInfo.ssid},${connectionInfo.bssid}"
    }

    // Enumerate network interfaces via ConnectivityManager. Go's net.Interfaces()
    // is blocked on Android 11+, so sing-box 1.12+ relies on the platform for this.
    override fun getInterfaces(): String {
        val cm = SagerNet.connectivity
        val arr = JSONArray()
        // The underlying (non-VPN) network is what the dialer must bind to as its
        // default; DefaultNetworkListener tracks it, avoiding our own tun interface.
        val underlyingName = SagerNet.underlyingNetwork?.let {
            try { cm.getLinkProperties(it)?.interfaceName } catch (_: Exception) { null }
        }
        for (network in cm.allNetworks) {
            try {
                val lp = cm.getLinkProperties(network) ?: continue
                val name = lp.interfaceName ?: continue
                val caps = cm.getNetworkCapabilities(network)
                // Skip our own VPN tunnel: it must never be a dial candidate.
                if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) continue
                val metered = caps == null ||
                    !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                val type = when {
                    caps == null -> 3
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> 0
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> 1
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> 2
                    else -> 3
                }
                var index = 0
                var mtu = 0
                try {
                    val ni = NetworkInterface.getByName(name)
                    if (ni != null) {
                        index = ni.index
                        mtu = ni.mtu
                    }
                } catch (_: Exception) {
                }
                if (index == 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    try {
                        index = android.system.Os.if_nametoindex(name)
                    } catch (_: Exception) {
                    }
                }
                if (index == 0) continue
                val addresses = JSONArray()
                for (la in lp.linkAddresses) {
                    val host = la.address.hostAddress ?: continue
                    addresses.put("$host/${la.prefixLength}")
                }
                arr.put(JSONObject().apply {
                    put("name", name)
                    put("index", index)
                    put("mtu", if (mtu > 0) mtu else 1500)
                    put("addresses", addresses)
                    put("type", type)
                    put("metered", metered)
                    put("default", underlyingName != null && name == underlyingName)
                })
            } catch (_: Exception) {
            }
        }
        return arr.toString()
    }

    // nb4a interface

    override fun useOfficialAssets(): Boolean {
        return DataStore.rulesProvider == 0
    }

    override fun selector_OnProxySelected(selectorTag: String, tag: String) {
        if (selectorTag != "proxy") {
            Logs.d("other selector: $selectorTag")
            return
        }
        Libcore.resetAllConnections(true)
        DataStore.baseService?.apply {
            runOnDefaultDispatcher {
                val id = data.proxy!!.config.profileTagMap
                    .filterValues { it == tag }.keys.firstOrNull() ?: -1
                val ent = SagerDatabase.proxyDao.getById(id) ?: return@runOnDefaultDispatcher
                // traffic & title
                data.proxy?.apply {
                    looper?.selectMain(id)
                    displayProfileName = ServiceNotification.genTitle(ent)
                    data.notification?.postNotificationTitle(displayProfileName)
                }
                // post binder
                data.binder.broadcast { b ->
                    b.cbSelectorUpdate(id)
                }
            }
        }
    }

}
