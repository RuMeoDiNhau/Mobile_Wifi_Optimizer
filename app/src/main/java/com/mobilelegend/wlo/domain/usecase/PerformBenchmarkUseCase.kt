package com.mobilelegend.wlo.domain.usecase

import com.mobilelegend.wlo.domain.model.LocationScore
import com.mobilelegend.wlo.domain.model.MeasurementContext
import com.mobilelegend.wlo.domain.model.MeasurementLocation
import com.mobilelegend.wlo.domain.model.UseCaseCategory
import com.mobilelegend.wlo.domain.model.BenchmarkReadiness
import com.mobilelegend.wlo.domain.model.BenchmarkStage
import com.mobilelegend.wlo.domain.model.benchmarkReadiness
import com.mobilelegend.wlo.domain.repository.WifiRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID

/**
 * UseCase thực hiện quy trình "Đo kiểm định 1-Chạm" (One-Tap Benchmark).
 * Chụp thông số Wi-Fi, thực hiện 3 lượt TCP connect và chấm điểm trong một thời hạn chung.
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
        location: MeasurementLocation? = null,
        onStage: (BenchmarkStage) -> Unit = {}
    ): LocationScore {
        // Starts before dispatch/session setup. withTimeoutOrNull distinguishes this deadline
        // from caller cancellation; repository finally + socket cancellation must still finish.
        val result = withTimeoutOrNull(TOTAL_TIMEOUT_MS) {
            val score = withContext(dispatcher) { measure(category, location, onStage) }
            currentCoroutineContext().ensureActive()
            score
        }
        currentCoroutineContext().ensureActive()
        return result ?: throw BenchmarkTimeoutException()
    }

    private suspend fun measure(
        category: UseCaseCategory,
        location: MeasurementLocation?,
        onStage: (BenchmarkStage) -> Unit
    ): LocationScore {
        onStage(BenchmarkStage.PREPARING)
        readiness().requireReady()
        return wifiRepository.withBenchmarkSession { session ->
            currentCoroutineContext().ensureActive()
            session.ensureValid()
            val currentMetric = session.initialMetric
            benchmarkReadiness(wifiRepository.getWifiAccessState(), true, currentMetric).requireReady()
            onStage(BenchmarkStage.PROBING)
            val pingResult = session.measurePing(packetsCount = 3)
            currentCoroutineContext().ensureActive()
            session.ensureValid()
            onStage(BenchmarkStage.SCORING)

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

    companion object {
        const val TOTAL_TIMEOUT_MS = 10_000L
    }
}

class BenchmarkTimeoutException : Exception("Phép đo đã hết thời hạn 10 giây. Hãy kiểm tra kết nối rồi thử lại.")
