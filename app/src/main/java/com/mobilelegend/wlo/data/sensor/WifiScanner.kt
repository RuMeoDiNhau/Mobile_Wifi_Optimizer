package com.mobilelegend.wlo.data.sensor

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import com.mobilelegend.wlo.core.permissions.PermissionHelper
import com.mobilelegend.wlo.data.model.WifiMetric
import com.mobilelegend.wlo.domain.model.WifiAccessState
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.concurrent.atomic.AtomicLong

/** Polls status; Android 12+ identity comes only from a location-bearing callback. */
@SuppressLint("MissingPermission")
class WifiScanner(context: Context) {
    private val appContext = context.applicationContext
    private val permissions = PermissionHelper(appContext)
    private val wifiManager = appContext.getSystemService(WifiManager::class.java)
    private val cm = appContext.getSystemService(ConnectivityManager::class.java)
    private val snapshots = WifiSnapshotCache()
    private val refreshVersion = AtomicLong()

    fun refresh() {
        snapshots.reset()
        refreshVersion.incrementAndGet()
    }

    fun isConnectedToWifi(): Boolean = runCatching {
        val network = cm?.activeNetwork
        network != null && cm?.getNetworkCapabilities(network)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
    }.getOrDefault(false)

    fun getCurrentWifiMetric(): WifiMetric {
        val connected = isConnectedToWifi()
        val empty = WifiMetric("<unknown ssid>", "00:00:00:00:00:00", -127, -1, 0,
            isConnected = connected, isSampleAvailable = false)
        val access = permissions.wifiAccessState()
        if (!connected || !access.canReadWifiIdentity || !access.locationServicesReady) return empty
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                snapshots.read(cm?.activeNetwork?.networkHandle?.toString()) ?: empty
            } else {
                @Suppress("DEPRECATION")
                val info = wifiManager?.connectionInfo
                if (info == null) empty else metricFromWifiInfo(info)
            }
        }.getOrDefault(empty)
    }

    fun getLiveWifiStream(pollIntervalMs: Long = 1000L): Flow<WifiMetric> = flow {
        require(pollIntervalMs > 0)
        var registered: ConnectivityManager.NetworkCallback? = null
        var lastAccess: WifiAccessState? = null
        var observedVersion = -1L
        try {
            while (currentCoroutineContext().isActive) {
                val access = permissions.wifiAccessState()
                val version = refreshVersion.get()
                // Re-register after granting/revoking location or toggling location services,
                // so a previously redacted callback cannot remain cached as a usable sample.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && (access != lastAccess || version != observedVersion)) {
                    registered?.let { cm?.unregisterNetworkCallback(it) }
                    registered = null
                    val token = snapshots.reset()
                    lastAccess = access
                    observedVersion = version
                    if (access.canReadWifiIdentity && access.locationServicesReady && cm != null) {
                        val callback = object : ConnectivityManager.NetworkCallback(
                            ConnectivityManager.NetworkCallback.FLAG_INCLUDE_LOCATION_INFO
                        ) {
                            override fun onAvailable(network: Network) {
                                snapshots.available(token, network.networkHandle.toString())
                            }
                            override fun onLost(network: Network) {
                                snapshots.lost(token, network.networkHandle.toString())
                            }
                            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                                val wifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                                val info = if (wifi) caps.transportInfo as? WifiInfo else null
                                val metric = metricFromWifiInfo(info).copy(
                                    isConnected = wifi, isSampleAvailable = info != null
                                )
                                snapshots.update(token, network.networkHandle.toString(), metric)
                            }
                        }
                        try {
                            cm.registerDefaultNetworkCallback(callback)
                            registered = callback
                        } catch (_: SecurityException) {
                            // The next permission transition retries registration; no stale identity.
                            snapshots.reset()
                        }
                    }
                }
                emit(getCurrentWifiMetric())
                delay(pollIntervalMs)
            }
        } finally {
            snapshots.reset()
            registered?.let { cm?.unregisterNetworkCallback(it) }
        }
    }
}
