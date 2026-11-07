package com.mobilelegend.wlo.core.network

import com.mobilelegend.wlo.data.model.WifiMetric

class WifiNetworkChangedException(message: String) : IllegalStateException(message)

/** A disconnect/roam is sticky: returning to the original AP cannot rescue a mixed sample. */
class BenchmarkNetworkGuard(
    private val networkId: String,
    private val initialMetric: WifiMetric,
    private val onInvalidated: () -> Unit = {}
) {
    @Volatile private var failure: WifiNetworkChangedException? = null

    init {
        check(initialMetric.isConnected) { "Chưa kết nối Wi-Fi. Hãy kết nối trước khi đo." }
        check(hasReadableWifiIdentity(initialMetric)) {
            "Android chưa cung cấp SSID/BSSID. Hãy kiểm tra quyền hoặc kết nối lại Wi-Fi."
        }
    }

    fun observe(currentNetworkId: String?, currentMetric: WifiMetric?) {
        if (currentNetworkId != networkId || currentMetric == null || !currentMetric.isConnected ||
            !hasReadableWifiIdentity(currentMetric) || currentMetric.ssid != initialMetric.ssid ||
            !currentMetric.bssid.equals(initialMetric.bssid, ignoreCase = true)
        ) invalidate()
        ensureValid()
    }

    fun invalidate(message: String = "Wi-Fi đã mất hoặc thay đổi trong lúc đo. Hãy đo lại.") {
        val firstFailure = synchronized(this) {
            if (failure != null) false else {
                failure = WifiNetworkChangedException(message)
                true
            }
        }
        if (firstFailure) onInvalidated()
    }

    fun ensureValid() { failure?.let { throw it } }

}

private fun hasReadableWifiIdentity(metric: WifiMetric): Boolean =
    metric.ssid.isNotBlank() && metric.ssid !in setOf("<unknown ssid>", "Mạng không xác định", "Mạng Wi-Fi (Cần bật GPS)") &&
        metric.bssid.matches(Regex("(?i)([0-9a-f]{2}:){5}[0-9a-f]{2}")) &&
        metric.bssid !in setOf("00:00:00:00:00:00", "02:00:00:00:00:00")
