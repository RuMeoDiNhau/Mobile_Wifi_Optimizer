package com.mobilelegend.wlo.data.model

/**
 * TCP connect results for one destination, not ICMP packet loss.
 */
data class PingResult(
    val latencyMs: Long,        // Thời gian phản hồi trung bình (ms)
    val tcpFailurePercent: Int, // Phần trăm lượt TCP connect thất bại (0 - 100%)
    val isSuccess: Boolean,     // Có ít nhất một lượt TCP connect thành công
    val message: String = ""    // Thông báo trạng thái chi tiết (nếu có)
) {
    init {
        require(tcpFailurePercent in 0..100)
        require(isSuccess == (tcpFailurePercent < 100))
    }
}
