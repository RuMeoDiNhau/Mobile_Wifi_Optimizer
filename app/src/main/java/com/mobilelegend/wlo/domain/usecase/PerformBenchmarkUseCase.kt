package com.mobilelegend.wlo.domain.usecase

import com.mobilelegend.wlo.domain.model.LocationScore
import com.mobilelegend.wlo.domain.model.MeasurementContext
import com.mobilelegend.wlo.domain.model.MeasurementLocation
import com.mobilelegend.wlo.domain.model.UseCaseCategory
import com.mobilelegend.wlo.domain.model.BenchmarkReadiness
import com.mobilelegend.wlo.domain.model.benchmarkReadiness
import com.mobilelegend.wlo.domain.repository.WifiRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.util.UUID

/**
 * UseCase thực hiện quy trình "Đo kiểm định 1-Chạm" (One-Tap Benchmark).
 * Kết hợp lấy thông số phần cứng hiện tại, thực hiện đo Ping 3 gói tin và chấm điểm.
 */
class PerformBenchmarkUseCase(
    private val wifiRepository: WifiRepository,
    private val calculateScoreUseCase: CalculateScoreUseCase,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis
) {

    fun readiness(busy: Boolean = false): BenchmarkReadiness = benchmarkReadiness(
        wifiRepository.getWifiAccessState(), wifiRepository.isWifiConnected(),
        wifiRepository.getCurrentWifiMetric(), busy
    )

    suspend operator fun invoke(
        category: UseCaseCategory,
        location: MeasurementLocation? = null
    ): LocationScore = withContext(dispatcher) {
        readiness().requireReady()
        wifiRepository.withBenchmarkSession { session ->
            currentCoroutineContext().ensureActive()
            session.ensureValid()
            val currentMetric = session.initialMetric
            benchmarkReadiness(wifiRepository.getWifiAccessState(), true, currentMetric).requireReady()
            val pingResult = session.measurePing(packetsCount = 3)
            currentCoroutineContext().ensureActive()
            session.ensureValid()

            val result = calculateScoreUseCase(currentMetric, pingResult, category).copy(
                measurement = MeasurementContext(
                    id = UUID.randomUUID().toString(),
                    ssid = currentMetric.ssid,
                    bssid = currentMetric.bssid,
                    measuredAtMillis = currentTimeMillis(),
                    location = location,
                    networkId = session.networkId
                )
            )
            currentCoroutineContext().ensureActive()
            session.ensureValid()
            benchmarkReadiness(wifiRepository.getWifiAccessState(), true, currentMetric).requireReady()
            result
        }
    }
}
