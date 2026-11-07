package com.mobilelegend.wlo.domain.usecase

import com.mobilelegend.wlo.data.model.WifiMetric
import com.mobilelegend.wlo.domain.model.LiveRadarData
import com.mobilelegend.wlo.domain.model.SignalQualityLevel
import com.mobilelegend.wlo.domain.repository.WifiRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * UseCase chuyển đổi luồng dữ liệu thô từ Wi-Fi thành đối tượng LiveRadarData hiển thị lên UI.
 */
class StreamLiveWifiUseCase(
    private val wifiRepository: WifiRepository
) {

    operator fun invoke(): Flow<LiveRadarData> {
        return wifiRepository.getLiveWifiStream().map { metric ->
            mapToLiveRadarData(metric)
        }
    }

    fun mapToLiveRadarData(metric: WifiMetric): LiveRadarData {
        val rssi = metric.rssi
        val qualityLevel = when {
            rssi >= -60 -> SignalQualityLevel.EXCELLENT
            rssi in -75..-61 -> SignalQualityLevel.GOOD
            else -> SignalQualityLevel.POOR
        }

        // Chuyển đổi dBm sang % vạch sóng từ -100dBm (0%) đến -50dBm (100%)
        val percentage = when {
            rssi <= -100 -> 0
            rssi >= -50 -> 100
            else -> (2 * (rssi + 100)).coerceIn(0, 100)
        }

        return LiveRadarData(
            ssid = metric.ssid,
            bssid = metric.bssid,
            rssi = rssi,
            signalPercentage = percentage,
            qualityLevel = qualityLevel,
            linkSpeedMbps = metric.linkSpeedMbps,
            frequencyMhz = metric.frequencyMhz,
            isConnected = metric.isConnected,
            isSampleAvailable = metric.isSampleAvailable
        )
    }
}
