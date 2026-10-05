package com.mobilelegend.wlo.presentation.live_radar

import com.mobilelegend.wlo.domain.model.LiveRadarData
import com.mobilelegend.wlo.domain.model.SignalQualityLevel
import com.mobilelegend.wlo.domain.model.UseCaseCategory

/**
 * Trạng thái UI của màn hình Live Radar.
 */
data class LiveRadarUiState(
    val radarData: LiveRadarData = LiveRadarData(
        ssid = "Đang quét tín hiệu...",
        bssid = "00:00:00:00:00:00",
        rssi = -100,
        signalPercentage = 0,
        qualityLevel = SignalQualityLevel.POOR,
        linkSpeedMbps = 0,
        frequencyMhz = 0,
        isConnected = false
    ),
    val selectedCategory: UseCaseCategory = UseCaseCategory.GAMING,
    val isWifiConnected: Boolean = true,
    val isLocationPermissionGranted: Boolean = true,
    val isGpsEnabled: Boolean = true,
    val isLoading: Boolean = false
)
