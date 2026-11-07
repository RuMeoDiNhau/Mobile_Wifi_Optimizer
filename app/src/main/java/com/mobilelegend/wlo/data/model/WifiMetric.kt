package com.mobilelegend.wlo.data.model

/**
 * Đại diện cho các thông số thô thu thập từ cảm biến Wi-Fi (WifiManager / ConnectivityManager).
 */
data class WifiMetric(
    val ssid: String,           // Tên mạng (VD: "KTX_KhuA")
    val bssid: String,          // Địa chỉ MAC của Access Point (VD: "00:14:22:01:23:45")
    val rssi: Int,              // Cường độ tín hiệu dBm (VD: -55)
    val linkSpeedMbps: Int,     // Tốc độ liên kết đường truyền Mbps (VD: 144)
    val frequencyMhz: Int,      // Tần số sóng MHz (VD: 2412 cho 2.4GHz, 5180 cho 5GHz)
    val isConnected: Boolean = true, // Trạng thái đang kết nối Wi-Fi hay không
    val isSampleAvailable: Boolean = true
)
