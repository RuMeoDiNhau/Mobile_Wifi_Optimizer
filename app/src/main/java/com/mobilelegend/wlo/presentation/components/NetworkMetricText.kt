package com.mobilelegend.wlo.presentation.components

const val MISSING_METRIC = "—"

fun linkSpeedText(mbps: Int?): String =
    mbps?.takeIf { it > 0 }?.let { "$it Mbps" } ?: MISSING_METRIC

fun pingText(latencyMs: Long?, successful: Boolean?): String = when {
    successful == false -> "Không phản hồi"
    successful == true && latencyMs != null && latencyMs >= 0 -> "$latencyMs ms"
    else -> MISSING_METRIC
}

fun networkName(ssid: String?): String = ssid?.trim()?.takeUnless {
    it.isBlank() || it.equals("<unknown ssid>", ignoreCase = true) ||
        it == "Đang quét tín hiệu..." || it == "Mạng không xác định" ||
        it == "Mạng Wi-Fi (Cần bật GPS)" || it == "Chưa kết nối Wi-Fi"
} ?: MISSING_METRIC
