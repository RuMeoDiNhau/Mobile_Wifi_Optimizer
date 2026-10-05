package com.mobilelegend.wlo.data.repository_impl

import com.mobilelegend.wlo.core.network.PingTester
import com.mobilelegend.wlo.core.permissions.PermissionHelper
import com.mobilelegend.wlo.data.model.PingResult
import com.mobilelegend.wlo.data.model.WifiMetric
import com.mobilelegend.wlo.data.sensor.WifiScanner
import com.mobilelegend.wlo.domain.repository.WifiRepository
import kotlinx.coroutines.flow.Flow

/**
 * Hiện thực hóa WifiRepository, kết nối WifiScanner, PingTester và PermissionHelper.
 */
class WifiRepositoryImpl(
    private val wifiScanner: WifiScanner,
    private val pingTester: PingTester,
    private val permissionHelper: PermissionHelper
) : WifiRepository {

    override fun getLiveWifiStream(): Flow<WifiMetric> {
        return wifiScanner.getLiveWifiStream(pollIntervalMs = 1000L)
    }

    override fun getCurrentWifiMetric(): WifiMetric {
        return wifiScanner.getCurrentWifiMetric()
    }

    override suspend fun measurePing(packetsCount: Int): PingResult {
        return pingTester.batchPing(packetsCount = packetsCount)
    }

    override fun isWifiConnected(): Boolean {
        return wifiScanner.isConnectedToWifi()
    }

    override fun isReadyForScanning(): Boolean {
        return permissionHelper.isReadyForWifiScanning()
    }
}
