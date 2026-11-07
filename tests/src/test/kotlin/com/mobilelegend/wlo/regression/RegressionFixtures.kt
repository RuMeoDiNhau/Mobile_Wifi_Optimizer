package com.mobilelegend.wlo.regression

import com.mobilelegend.wlo.data.model.PingResult
import com.mobilelegend.wlo.data.model.WifiMetric
import com.mobilelegend.wlo.domain.model.LocationScore
import com.mobilelegend.wlo.domain.model.MeasurementContext
import com.mobilelegend.wlo.domain.model.MeasurementLocation
import com.mobilelegend.wlo.domain.model.UseCaseCategory
import com.mobilelegend.wlo.domain.repository.WifiRepository
import com.mobilelegend.wlo.domain.repository.WifiBenchmarkSession
import com.mobilelegend.wlo.core.network.BenchmarkNetworkGuard
import com.mobilelegend.wlo.presentation.live_radar.LiveRadarUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

internal fun scoreA() = LocationScore(
    finalScore = 87,
    normalizedRssiScore = 90,
    normalizedPingScore = 95,
    normalizedSpeedScore = 70,
    category = UseCaseCategory.GAMING,
    summaryText = "Kết quả mạng A",
    recommendation = "Khuyến nghị mạng A",
    isRecommended = true,
    rawRssi = -55,
    rawPingMs = 25,
    rawLinkSpeedMbps = 150,
    pingSuccessful = true,
    measurement = MeasurementContext(
        id = "measurement-A",
        ssid = "Network A",
        bssid = "AA:BB:CC:DD:EE:01",
        measuredAtMillis = 1_800_000_000_000L,
        location = MeasurementLocation(10.77, 106.70)
    )
)

internal fun onlineRadar(): LiveRadarUiState {
    val state = LiveRadarUiState()
    return state.copy(
        radarData = state.radarData.copy(
            ssid = "Live network C",
            bssid = "AA:BB:CC:DD:EE:03",
            signalPercentage = 100,
            linkSpeedMbps = 866,
            isConnected = true
        ),
        isWifiConnected = true,
        isLocationPermissionGranted = true,
        isGpsEnabled = true
    )
}

internal class FakeWifiRepository : WifiRepository {
    var metric = WifiMetric("Network A", "AA:BB:CC:DD:EE:01", -55, 150, 5180)
    var ping = PingResult(25, 0, true)
    var ready = true
    var pingCalls = 0
    var requestedPackets = 0
    var error: Exception? = null
    var networkId = "wifi-network-A"
    var openedSessions = 0
    var closedSessions = 0
    var activeSessions = 0
    var maxActiveSessions = 0
    var validationCalls = 0
    var onOpen: suspend () -> Unit = {}
    var onProbe: suspend () -> Unit = {}
    var onClose: suspend () -> Unit = {}
    var onValidate: () -> Unit = {}
    var observeNetwork: () -> Unit = {}

    override fun getLiveWifiStream(): Flow<WifiMetric> = emptyFlow()
    override fun getCurrentWifiMetric(): WifiMetric = metric
    override fun isWifiConnected(): Boolean = metric.isConnected
    override fun isReadyForScanning(): Boolean = ready
    override suspend fun <T> withBenchmarkSession(block: suspend (WifiBenchmarkSession) -> T): T {
        openedSessions++
        activeSessions++
        maxActiveSessions = maxOf(maxActiveSessions, activeSessions)
        try {
            onOpen()
            val capturedNetwork = networkId
            val capturedMetric = metric
            val guard = BenchmarkNetworkGuard(capturedNetwork, capturedMetric)
            observeNetwork = { guard.observe(networkId, metric) }
            return block(object : WifiBenchmarkSession {
                override val networkId = capturedNetwork
                override val initialMetric = capturedMetric
                override fun ensureValid() {
                    validationCalls++
                    onValidate()
                    guard.observe(this@FakeWifiRepository.networkId, metric)
                }
                override suspend fun measurePing(packetsCount: Int): PingResult {
                    pingCalls++
                    requestedPackets = packetsCount
                    onProbe()
                    error?.let { throw it }
                    return ping
                }
            })
        } finally {
            try {
                onClose()
            } finally {
                activeSessions--
                closedSessions++
            }
        }
    }
}
