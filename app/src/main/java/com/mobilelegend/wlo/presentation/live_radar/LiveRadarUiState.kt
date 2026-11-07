package com.mobilelegend.wlo.presentation.live_radar

import com.mobilelegend.wlo.domain.model.LiveRadarData
import com.mobilelegend.wlo.domain.model.SignalQualityLevel
import com.mobilelegend.wlo.domain.model.UseCaseCategory
import com.mobilelegend.wlo.domain.model.WifiAccessState
import com.mobilelegend.wlo.domain.model.benchmarkReadiness
import com.mobilelegend.wlo.data.model.WifiMetric

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
    val isWifiConnected: Boolean = false,
    val wifiAccess: WifiAccessState = WifiAccessState(),
    val isLoading: Boolean = false
) {
    val isLocationPermissionGranted: Boolean get() = wifiAccess.canUseMap
    val isGpsEnabled: Boolean get() = wifiAccess.locationServiceEnabled
    fun readiness(busy: Boolean = false) = benchmarkReadiness(wifiAccess, isWifiConnected,
        WifiMetric(radarData.ssid, radarData.bssid, radarData.rssi, radarData.linkSpeedMbps,
            radarData.frequencyMhz, radarData.isConnected, radarData.isSampleAvailable), busy)
}
