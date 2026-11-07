package com.mobilelegend.wlo.domain.usecase

import com.mobilelegend.wlo.domain.model.LocationScore
import com.mobilelegend.wlo.domain.model.MeasurementContext
import com.mobilelegend.wlo.domain.model.MeasurementLocation
import com.mobilelegend.wlo.domain.model.UseCaseCategory
import com.mobilelegend.wlo.domain.repository.WifiRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/** Captures the source of the displayed result; never borrows live UI identity. */
class PerformBenchmarkUseCase(
    private val wifiRepository: WifiRepository,
    private val calculateScoreUseCase: CalculateScoreUseCase,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis
) {
    suspend operator fun invoke(
        category: UseCaseCategory,
        location: MeasurementLocation? = null
    ): LocationScore = withContext(dispatcher) {
        check(wifiRepository.isWifiConnected()) { "Chưa kết nối Wi-Fi" }
        check(wifiRepository.isReadyForScanning()) { "Cần cấp quyền vị trí và bật dịch vụ vị trí" }
        val metric = wifiRepository.getCurrentWifiMetric()
        val ping = wifiRepository.measurePing(packetsCount = 3)
        calculateScoreUseCase(metric, ping, category).copy(
            measurement = MeasurementContext(
                id = UUID.randomUUID().toString(),
                ssid = metric.ssid,
                bssid = metric.bssid,
                measuredAtMillis = currentTimeMillis(),
                location = location
            )
        )
    }
}
