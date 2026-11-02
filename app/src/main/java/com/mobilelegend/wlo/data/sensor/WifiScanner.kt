package com.mobilelegend.wlo.data.sensor

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import com.mobilelegend.wlo.data.model.WifiMetric
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Trình đọc cảm biến Wi-Fi thời gian thực (Real-time Wi-Fi Scanner).
 * Sử dụng cơ chế Polling qua Kotlin Flow kết hợp API đa phiên bản (Android 11 và Android 12+)
 * để tránh bị Android bóp nghẹt tần suất quét (Wi-Fi Throttling).
 */
class WifiScanner(private val context: Context) {

    private val wifiManager: WifiManager? by lazy {
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    }

    private val connectivityManager: ConnectivityManager? by lazy {
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    }

    /**
     * Kiểm tra xem thiết bị hiện tại có đang kết nối vào một mạng Wi-Fi hay không.
     */
    fun isConnectedToWifi(): Boolean {
        val cm = connectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    /**
     * Thu thập thông số Wi-Fi hiện tại một cách tức thì.
     */
    fun getCurrentWifiMetric(): WifiMetric {
        if (!isConnectedToWifi()) {
            return WifiMetric(
                ssid = "Chưa kết nối Wi-Fi",
                bssid = "00:00:00:00:00:00",
                rssi = -100,
                linkSpeedMbps = 0,
                frequencyMhz = 0,
                isConnected = false
            )
        }

        val wifiInfo: WifiInfo? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12 (API 31) trở lên: Lấy qua NetworkCapabilities.transportInfo
            val activeNetwork = connectivityManager?.activeNetwork
            val capabilities = connectivityManager?.getNetworkCapabilities(activeNetwork)
            capabilities?.transportInfo as? WifiInfo
        } else {
            // Android 11 trở xuống: Lấy qua WifiManager.connectionInfo
            @Suppress("DEPRECATION")
            wifiManager?.connectionInfo
        }

        val rawSsid = wifiInfo?.ssid?.trim('\"') ?: "Mạng không xác định"
        val cleanSsid = if (rawSsid == "<unknown ssid>" || rawSsid.isBlank()) {
            "Mạng Wi-Fi (Cần bật GPS)"
        } else {
            rawSsid
        }

        val bssid = wifiInfo?.bssid ?: "00:00:00:00:00:00"
        val rssi = wifiInfo?.rssi ?: -100

        // Ưu tiên đọc rxLinkSpeedMbps (Android 10+) hoặc linkSpeed truyền thống
        var linkSpeed = wifiInfo?.linkSpeed ?: 0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val rxSpeed = wifiInfo?.rxLinkSpeedMbps ?: 0
            if (rxSpeed > linkSpeed) {
                linkSpeed = rxSpeed
            }
        }

        // Fallback thông minh: nếu linkSpeed <= 1 (thường gặp trên Máy ảo Emulator),
        // lấy băng thông downstream thực tế từ NetworkCapabilities
        if (linkSpeed <= 1) {
            val activeNetwork = connectivityManager?.activeNetwork
            val caps = connectivityManager?.getNetworkCapabilities(activeNetwork)
            val downstreamMbps = (caps?.linkDownstreamBandwidthKbps ?: 0) / 1000
            if (downstreamMbps > 1) {
                linkSpeed = downstreamMbps
            }
        }

        val frequency = wifiInfo?.frequency ?: 0

        return WifiMetric(
            ssid = cleanSsid,
            bssid = bssid,
            rssi = rssi,
            linkSpeedMbps = linkSpeed,
            frequencyMhz = frequency,
            isConnected = true
        )
    }

    /**
     * Tạo một luồng dữ liệu (Flow) phát thông số sóng Wi-Fi liên tục mỗi `pollIntervalMs` (mặc định 1000ms).
     * Phục vụ cho tính năng "Live Radar" đổi màu theo thời gian thực.
     */
    fun getLiveWifiStream(pollIntervalMs: Long = 1000L): Flow<WifiMetric> = flow {
        while (true) {
            val metric = getCurrentWifiMetric()
            emit(metric)
            delay(pollIntervalMs)
        }
    }
}
