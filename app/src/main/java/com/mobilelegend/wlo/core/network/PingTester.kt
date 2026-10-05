package com.mobilelegend.wlo.core.network

import com.mobilelegend.wlo.data.model.PingResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Trình đo độ trễ mạng (Ping) sử dụng kỹ thuật TCP Socket Connect.
 * Ưu điểm: Ổn định 100% trên mọi dòng máy Android, không bị chặn bởi SELinux hay lỗi locale regex.
 */
class PingTester(
    private val defaultHost: String = "1.1.1.1",
    private val defaultPort: Int = 53,
    private val timeoutMs: Int = 2000
) {

    /**
     * Thực hiện một lần đo đơn lẻ tới đích (Host:Port).
     */
    suspend fun singlePing(
        host: String = defaultHost,
        port: Int = defaultPort,
        timeout: Int = timeoutMs
    ): PingResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), timeout)
            }
            val latency = System.currentTimeMillis() - startTime
            PingResult(
                latencyMs = latency,
                packetLossPercent = 0,
                isSuccess = true,
                message = "Kết nối thành công trong ${latency}ms"
            )
        } catch (e: Exception) {
            PingResult(
                latencyMs = timeout.toLong(),
                packetLossPercent = 100,
                isSuccess = false,
                message = "Timeout hoặc lỗi kết nối: ${e.localizedMessage ?: "Unknown error"}"
            )
        }
    }

    /**
     * Thực hiện chuỗi đo liên tiếp (ví dụ 3 gói tin cho phiên Benchmark 3 giây)
     * và tính toán thời gian phản hồi trung bình cùng tỷ lệ rớt gói.
     */
    suspend fun batchPing(
        packetsCount: Int = 3,
        host: String = defaultHost,
        port: Int = defaultPort,
        timeout: Int = timeoutMs
    ): PingResult = withContext(Dispatchers.IO) {
        val latencies = mutableListOf<Long>()
        var failedCount = 0

        repeat(packetsCount) {
            val result = singlePing(host, port, timeout)
            if (result.isSuccess) {
                latencies.add(result.latencyMs)
            } else {
                failedCount++
            }
        }

        val packetLossPercent = (failedCount * 100) / packetsCount
        if (latencies.isNotEmpty()) {
            val averageLatency = latencies.average().toLong()
            PingResult(
                latencyMs = averageLatency,
                packetLossPercent = packetLossPercent,
                isSuccess = true,
                message = "Ping TB: ${averageLatency}ms | Rớt gói: $packetLossPercent%"
            )
        } else {
            PingResult(
                latencyMs = timeout.toLong(),
                packetLossPercent = 100,
                isSuccess = false,
                message = "Không có gói tin nào phản hồi (100% rớt gói)"
            )
        }
    }
}
