package com.mobilelegend.wlo.presentation.components

import com.mobilelegend.wlo.domain.model.LocationScore

/** Do not invent a map location or use the SSID/GPS from the time the result is viewed. */
fun LocationScore.toWifiPlace(): WifiPlaceItem? {
    val context = measurement ?: return null
    val location = context.location ?: return null
    return WifiPlaceItem(
        id = "measured_${context.id}",
        rank = 1,
        name = networkName(context.ssid),
        district = "Vị trí tại lúc đo",
        distanceText = MISSING_METRIC,
        score = finalScore,
        qualityLabel = when {
            pingSuccessful == false -> "Không phản hồi"
            pingSuccessful == null || rawLinkSpeedMbps <= 0 -> "Thiếu dữ liệu"
            finalScore >= 85 -> "Rất tốt"
            finalScore >= 70 -> "Tốt"
            finalScore >= 50 -> "Trung bình"
            else -> "Kém"
        },
        linkSpeedMbps = rawLinkSpeedMbps.takeIf { it > 0 },
        pingMs = rawPingMs.takeIf { it >= 0 },
        pingSuccessful = pingSuccessful,
        measurementsToday = 1,
        isSaved = true,
        isFree = false,
        iconType = PlaceIconType.WORK,
        latitude = location.latitude,
        longitude = location.longitude,
        ssid = context.ssid,
        measurement = this,
        suitableNeeds = WifiNeedOption.values().filter { it.category == category }.toSet()
    )
}
