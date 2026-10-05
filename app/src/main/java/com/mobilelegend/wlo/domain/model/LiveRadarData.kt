package com.mobilelegend.wlo.domain.model

/**
 * Phân cấp chất lượng tín hiệu Wi-Fi dựa trên chỉ số dBm.
 */
enum class SignalQualityLevel {
    EXCELLENT, // Xanh lá: >= -60 dBm
    GOOD,      // Vàng: -61 dBm đến -75 dBm
    POOR       // Đỏ: <= -76 dBm
}

/**
 * Model biểu diễn thông tin hiển thị trực tiếp trên màn hình Live Radar.
 */
data class LiveRadarData(
    val ssid: String,
    val bssid: String,
    val rssi: Int,
    val signalPercentage: Int,          // 0 - 100%
    val qualityLevel: SignalQualityLevel,
    val linkSpeedMbps: Int,
    val frequencyMhz: Int,
    val isConnected: Boolean
)
