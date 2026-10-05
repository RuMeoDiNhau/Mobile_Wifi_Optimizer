package com.mobilelegend.wlo.domain.usecase

import com.mobilelegend.wlo.domain.model.LocationScore
import com.mobilelegend.wlo.domain.model.UseCaseCategory
import com.mobilelegend.wlo.domain.repository.WifiRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * UseCase thực hiện quy trình "Đo kiểm định 1-Chạm" (One-Tap Benchmark).
 * Kết hợp lấy thông số phần cứng hiện tại, thực hiện đo Ping 3 gói tin và chấm điểm.
 */
class PerformBenchmarkUseCase(
    private val wifiRepository: WifiRepository,
    private val calculateScoreUseCase: CalculateScoreUseCase
) {

    suspend operator fun invoke(category: UseCaseCategory): LocationScore = withContext(Dispatchers.IO) {
        // 1. Lấy thông số sóng Wi-Fi tức thời
        val currentMetric = wifiRepository.getCurrentWifiMetric()

        // 2. Thực hiện đo Ping 3 gói tin (khoảng 1.5s - 3s)
        val pingResult = wifiRepository.measurePing(packetsCount = 3)

        // 3. Chấm điểm thông qua Scoring Engine
        calculateScoreUseCase(
            wifiMetric = currentMetric,
            pingResult = pingResult,
            category = category
        )
    }
}
