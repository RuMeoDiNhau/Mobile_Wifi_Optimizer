package com.mobilelegend.wlo.presentation.live_radar

import com.mobilelegend.wlo.data.model.WifiMetric
import com.mobilelegend.wlo.domain.model.LiveRadarData
import com.mobilelegend.wlo.domain.model.SignalQualityLevel
import com.mobilelegend.wlo.domain.model.UseCaseCategory
import com.mobilelegend.wlo.domain.model.WifiAccessState
import com.mobilelegend.wlo.domain.model.benchmarkReadiness

enum class RadarObservation { PAUSED, WAITING_FOR_CONDITIONS, STARTING, LIVE, RETRYING, ERROR }
enum class RadarFailure { PERMISSION_DENIED, SOURCE_FAILED, STREAM_ENDED, STALE_DATA }

data class LiveRadarUiState(
    val radarData: LiveRadarData = EmptyRadarData,
    val selectedCategory: UseCaseCategory = UseCaseCategory.GAMING,
    val isWifiConnected: Boolean = false,
    val wifiAccess: WifiAccessState = WifiAccessState(),
    val isLoading: Boolean = false,
    val observation: RadarObservation = RadarObservation.PAUSED,
    val failure: RadarFailure? = null,
    val lastSampleElapsedMs: Long? = null,
    val sampleAgeMs: Long? = null,
    val automaticRetries: Int = 0
) {
    val isLocationPermissionGranted: Boolean get() = wifiAccess.canUseMap
    val isGpsEnabled: Boolean get() = wifiAccess.locationServiceEnabled
    fun readiness(busy: Boolean = false) = benchmarkReadiness(wifiAccess, isWifiConnected,
        WifiMetric(radarData.ssid, radarData.bssid, radarData.rssi, radarData.linkSpeedMbps,
            radarData.frequencyMhz, radarData.isConnected, radarData.isSampleAvailable), busy)

    companion object {
        val EmptyRadarData = LiveRadarData(
            ssid = "Chưa có dữ liệu Wi-Fi", bssid = "00:00:00:00:00:00",
            rssi = -100, signalPercentage = 0, qualityLevel = SignalQualityLevel.POOR,
            linkSpeedMbps = 0, frequencyMhz = 0, isConnected = false, isSampleAvailable = false
        )
    }
}
