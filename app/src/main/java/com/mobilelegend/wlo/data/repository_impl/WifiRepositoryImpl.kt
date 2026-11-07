package com.mobilelegend.wlo.data.repository_impl

import android.content.Context

import com.mobilelegend.wlo.core.network.PingTester
import com.mobilelegend.wlo.core.permissions.PermissionHelper
import com.mobilelegend.wlo.data.sensor.AndroidWifiBenchmarkSession
import com.mobilelegend.wlo.domain.repository.WifiBenchmarkSession
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
    private val permissionHelper: PermissionHelper,
    context: Context
) : WifiRepository {
    private val appContext = context.applicationContext

    override fun getLiveWifiStream(): Flow<WifiMetric> {
        return wifiScanner.getLiveWifiStream(pollIntervalMs = 1000L)
    }

    override fun getCurrentWifiMetric(): WifiMetric {
        return wifiScanner.getCurrentWifiMetric()
    }

    override suspend fun <T> withBenchmarkSession(block: suspend (WifiBenchmarkSession) -> T): T {
        val session = AndroidWifiBenchmarkSession(appContext, pingTester)
        return try {
            session.awaitReady()
            block(session)
        } finally {
            session.close()
        }
    }

    override fun isWifiConnected(): Boolean {
        return wifiScanner.isConnectedToWifi()
    }

    override fun isReadyForScanning() = permissionHelper.isReadyForWifiScanning()

}
