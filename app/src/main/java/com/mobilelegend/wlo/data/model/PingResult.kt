package com.mobilelegend.wlo.data.model

/**
 * Đại diện cho kết quả đo độ trễ mạng thông qua TCP Socket / ICMP.
 */
data class PingResult(
    val latencyMs: Long,        // Thời gian phản hồi trung bình (ms)
    val packetLossPercent: Int, // Tỷ lệ rớt gói tin (0 - 100%)
    val isSuccess: Boolean,     // Có thực hiện kết nối thành công hay bị timeout
    val message: String = ""    // Thông báo trạng thái chi tiết (nếu có)
)
