package com.mobilelegend.wlo.domain.model

/**
 * Kết quả tính điểm tổng hợp và đánh giá chất lượng vị trí theo nhu cầu.
 */
data class LocationScore(
    val finalScore: Int,                 // Điểm tổng hợp từ 0 đến 100
    val normalizedRssiScore: Int,        // Điểm sóng đã chuẩn hóa (0-100)
    val normalizedPingScore: Int,        // Điểm ping đã chuẩn hóa (0-100)
    val normalizedSpeedScore: Int,       // Điểm tốc độ đã chuẩn hóa (0-100)
    val category: UseCaseCategory,       // Nhu cầu được đánh giá
    val summaryText: String,             // Lời nhận xét ngắn (VD: "Tuyệt vời để leo rank")
    val recommendation: String,          // Lời khuyên chi tiết cho người dùng
    val isRecommended: Boolean,          // Có đạt chuẩn (Vùng xanh) hay không
    val rawRssi: Int,                    // Thông số dBm gốc
    val rawPingMs: Long,                 // Thông số Ping gốc
    val rawLinkSpeedMbps: Int            // Thông số Mbps gốc
)
