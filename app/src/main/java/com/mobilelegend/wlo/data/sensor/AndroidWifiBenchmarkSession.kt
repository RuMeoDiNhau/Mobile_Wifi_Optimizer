package com.mobilelegend.wlo.data.sensor

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import com.mobilelegend.wlo.core.network.BenchmarkNetworkGuard
import com.mobilelegend.wlo.core.network.PingTester
import com.mobilelegend.wlo.core.network.WifiNetworkChangedException
import com.mobilelegend.wlo.data.model.PingResult
import com.mobilelegend.wlo.data.model.WifiMetric
import com.mobilelegend.wlo.domain.repository.WifiBenchmarkSession
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import java.io.Closeable
import java.net.Socket

/** Captures the default Wi-Fi once and fails closed on loss, handover or hidden identity. */
@SuppressLint("MissingPermission")
internal class AndroidWifiBenchmarkSession(
    context: Context,
    private val pingTester: PingTester
) : WifiBenchmarkSession, Closeable {
    private val cm = checkNotNull(context.applicationContext.getSystemService(ConnectivityManager::class.java))
    private val wifiManager = checkNotNull(context.applicationContext.getSystemService(WifiManager::class.java))
    private val network = checkNotNull(cm.activeNetwork) { "Chưa kết nối Wi-Fi. Hãy kết nối trước khi đo." }
    override val networkId: String = network.networkHandle.toString()
    override val initialMetric: WifiMetric get() = synchronized(lock) { checkNotNull(capturedMetric) }

    private val lock = Any()
    private val ready = CompletableDeferred<Unit>()
    private val sockets = mutableSetOf<Socket>()
    private var registered = false
    private var closed = false
    private var capturedMetric: WifiMetric? = null
    private var guard: BenchmarkNetworkGuard? = null
    private var failure: Exception? = null

    private val callback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        object : ConnectivityManager.NetworkCallback(ConnectivityManager.NetworkCallback.FLAG_INCLUDE_LOCATION_INFO) {
            override fun onAvailable(network: Network) = available(network)
            override fun onLost(network: Network) = lost(network)
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) = capabilities(network, caps)
        }
    } else {
        object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = available(network)
            override fun onLost(network: Network) = lost(network)
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) = capabilities(network, caps)
        }
    }

    suspend fun awaitReady() {
        check(cm.getNetworkCapabilities(network)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
            "Chưa kết nối Wi-Fi. Hãy kết nối trước khi đo."
        }
        cm.registerDefaultNetworkCallback(callback)
        registered = true
        check(withTimeoutOrNull(3000L) { ready.await(); true } == true) {
            "Không nhận được thông tin Wi-Fi. Hãy kiểm tra quyền vị trí chính xác rồi thử lại."
        }
        ensureValid()
    }

    private fun available(current: Network) {
        if (current != network) invalidate(WifiNetworkChangedException("Mạng đang kết nối đã thay đổi. Hãy đo lại."))
    }

    private fun lost(current: Network) {
        if (current == network) invalidate(WifiNetworkChangedException("Wi-Fi đã ngắt trong lúc đo. Hãy đo lại."))
    }

    private fun capabilities(current: Network, caps: NetworkCapabilities) {
        if (current != network) {
            available(current)
            return
        }
        try {
            check(caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) { "Kết nối hiện tại không còn là Wi-Fi." }
            // Use location-bearing callback data on Android 12+, rather than redacted synchronous capabilities.
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                caps.transportInfo as? WifiInfo
            } else legacyWifiInfo()
            val metric = metricFromWifiInfo(info)
            synchronized(lock) {
                if (closed || failure != null) return
                if (guard == null) {
                    guard = BenchmarkNetworkGuard(networkId, metric) { closeSockets() }
                    capturedMetric = metric
                } else {
                    guard!!.observe(current.networkHandle.toString(), metric)
                }
                ready.complete(Unit)
            }
        } catch (error: Exception) {
            invalidate(error)
        }
    }

    @Suppress("DEPRECATION")
    private fun legacyWifiInfo(): WifiInfo? = wifiManager.connectionInfo

    override fun ensureValid() {
        synchronized(lock) {
            check(!closed) { "Phiên đo đã đóng." }
            failure?.let { throw it }
            val current = cm.activeNetwork
            val wifi = cm.getNetworkCapabilities(current)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
            // Re-read current AP identity as a final cross-check; callbacks also latch transient A -> B -> A changes.
            val metric = if (wifi) metricFromWifiInfo(legacyWifiInfo()) else null
            try {
                checkNotNull(guard).observe(current?.networkHandle?.toString(), metric)
            } catch (error: Exception) {
                invalidate(error)
                throw error
            }
        }
    }

    private fun createBoundSocket(): Socket {
        ensureValid()
        synchronized(lock) {
            failure?.let { throw it }
            check(!closed) { "Phiên đo đã đóng." }
            // Never use the process default socket factory: it could fall back to mobile data.
            return network.socketFactory.createSocket().also { sockets.add(it) }
        }
    }

    override suspend fun measurePing(packetsCount: Int): PingResult = pingTester.batchPing(
        packetsCount = packetsCount,
        socketFactory = ::createBoundSocket,
        validateConnection = ::ensureValid
    )

    private fun invalidate(error: Exception) {
        synchronized(lock) {
            if (closed || failure != null) return
            failure = error
            guard?.invalidate(error.message ?: "Wi-Fi đã thay đổi.")
            ready.completeExceptionally(error)
        }
        closeSockets()
    }

    private fun closeSockets() {
        val toClose = synchronized(lock) { sockets.toList().also { sockets.clear() } }
        toClose.forEach { runCatching { it.close() } }
    }

    override fun close() {
        synchronized(lock) {
            if (closed) return
            closed = true
        }
        closeSockets()
        if (registered) {
            cm.unregisterNetworkCallback(callback)
            registered = false
        }
    }
}

/** Shared raw conversion; missing identities are rejected by the benchmark guard. */
internal fun metricFromWifiInfo(info: WifiInfo?): WifiMetric {
    val linkSpeed = info?.linkSpeed ?: -1
    val rxSpeed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) info?.rxLinkSpeedMbps ?: -1 else -1
    return WifiMetric(
        ssid = info?.ssid?.trim('"') ?: "<unknown ssid>",
        bssid = info?.bssid ?: "00:00:00:00:00:00",
        rssi = info?.rssi ?: -127,
        linkSpeedMbps = maxOf(linkSpeed, rxSpeed),
        frequencyMhz = info?.frequency ?: 0,
        isConnected = info != null
    )
}
