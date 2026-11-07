package com.mobilelegend.wlo.domain.model

import com.mobilelegend.wlo.data.model.WifiMetric

enum class ReadinessAction { NONE, WIFI_SETTINGS, APP_SETTINGS, LOCATION_SETTINGS }

enum class BenchmarkBlockReason(val message: String, val action: ReadinessAction) {
    BUSY("Đang đo hoặc dọn kết nối của lượt trước", ReadinessAction.NONE),
    NO_WIFI("Chưa kết nối Wi-Fi", ReadinessAction.WIFI_SETTINGS),
    PRECISE_LOCATION("Cần cấp quyền vị trí chính xác để đọc Wi-Fi", ReadinessAction.APP_SETTINGS),
    LOCATION_PERMISSION("Cần cấp quyền vị trí để đọc Wi-Fi", ReadinessAction.APP_SETTINGS),
    LOCATION_DISABLED("Cần bật dịch vụ vị trí", ReadinessAction.LOCATION_SETTINGS),
    WAITING_FOR_WIFI("Đang chờ thông tin Wi-Fi", ReadinessAction.NONE),
    HIDDEN_IDENTITY("Android chưa cung cấp SSID/BSSID. Hãy kiểm tra quyền hoặc kết nối lại Wi-Fi.", ReadinessAction.APP_SETTINGS),
    INVALID_METRICS("Chưa có RSSI/LinkSpeed hợp lệ. Hãy chờ tín hiệu Wi-Fi rồi thử lại.", ReadinessAction.NONE)
}

data class BenchmarkReadiness(val reason: BenchmarkBlockReason? = null) {
    val canMeasure: Boolean get() = reason == null
    fun requireReady() { check(canMeasure) { requireNotNull(reason).message } }
}

fun hasReadableWifiIdentity(metric: WifiMetric): Boolean =
    metric.ssid.isNotBlank() && metric.ssid !in setOf("<unknown ssid>", "Mạng không xác định", "Mạng Wi-Fi (Cần bật GPS)") &&
        metric.bssid.matches(Regex("(?i)([0-9a-f]{2}:){5}[0-9a-f]{2}")) &&
        metric.bssid !in setOf("00:00:00:00:00:00", "02:00:00:00:00:00")

/** Shared by every entry point and re-evaluated at the execution boundary. */
fun benchmarkReadiness(
    access: WifiAccessState,
    wifiConnected: Boolean,
    metric: WifiMetric?,
    busy: Boolean = false
): BenchmarkReadiness = BenchmarkReadiness(when {
    busy -> BenchmarkBlockReason.BUSY
    !wifiConnected -> BenchmarkBlockReason.NO_WIFI
    !access.canReadWifiIdentity -> if (access.sdkInt >= 29) BenchmarkBlockReason.PRECISE_LOCATION else BenchmarkBlockReason.LOCATION_PERMISSION
    !access.locationServicesReady -> BenchmarkBlockReason.LOCATION_DISABLED
    metric == null || !metric.isConnected || !metric.isSampleAvailable -> BenchmarkBlockReason.WAITING_FOR_WIFI
    !hasReadableWifiIdentity(metric) -> BenchmarkBlockReason.HIDDEN_IDENTITY
    metric.rssi !in -126..-1 || metric.linkSpeedMbps <= 0 -> BenchmarkBlockReason.INVALID_METRICS
    else -> null
})
