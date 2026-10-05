package com.mobilelegend.wlo.domain.model

/**
 * Cấu hình hệ số trọng số (Weights) phục vụ thuật toán Scoring Engine.
 * Tổng: pingWeight + rssiWeight + speedWeight = 1.0f
 */
data class UseCaseWeight(
    val pingWeight: Float,   // % ảnh hưởng của độ trễ Ping
    val rssiWeight: Float,   // % ảnh hưởng của cường độ sóng RSSI
    val speedWeight: Float   // % ảnh hưởng của tốc độ LinkSpeed
) {
    companion object {
        fun getWeightFor(category: UseCaseCategory): UseCaseWeight {
            return when (category) {
                UseCaseCategory.GAMING -> UseCaseWeight(
                    pingWeight = 0.60f,
                    rssiWeight = 0.30f,
                    speedWeight = 0.10f
                )
                UseCaseCategory.STUDY_DOWNLOAD -> UseCaseWeight(
                    pingWeight = 0.10f,
                    rssiWeight = 0.40f,
                    speedWeight = 0.50f
                )
                UseCaseCategory.VIDEO_CALL -> UseCaseWeight(
                    pingWeight = 0.40f,
                    rssiWeight = 0.40f,
                    speedWeight = 0.20f
                )
            }
        }
    }
}
