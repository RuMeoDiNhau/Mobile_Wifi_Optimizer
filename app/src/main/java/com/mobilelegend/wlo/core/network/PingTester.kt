package com.mobilelegend.wlo.core.network

import com.mobilelegend.wlo.data.model.PingResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.coroutines.resume

/**
 * Trình đo độ trễ mạng (Ping) sử dụng kỹ thuật TCP Socket Connect.
 * Benchmark truyền socketFactory đã gắn với mạng Wi-Fi; hủy coroutine sẽ đóng socket.
 */
class PingTester(
    private val defaultHost: String = "1.1.1.1",
    private val defaultPort: Int = 53,
    private val timeoutMs: Int = 2000,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val nanoTime: () -> Long = System::nanoTime
) {

    /**
     * Thực hiện một lần đo đơn lẻ tới đích (Host:Port).
     */
    suspend fun singlePing(
        host: String = defaultHost,
        port: Int = defaultPort,
        timeout: Int = timeoutMs,
        socketFactory: () -> Socket = { Socket() }
    ): PingResult = withContext(dispatcher) {
        require(timeout > 0)
        currentCoroutineContext().ensureActive()
        val socket = socketFactory()
        suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { runCatching { socket.close() } }
            try {
                if (continuation.isActive) {
                    val start = nanoTime()
                    socket.connect(InetSocketAddress(host, port), timeout)
                    val latency = ((nanoTime() - start) / 1_000_000L).coerceAtLeast(0)
                    continuation.resume(PingResult(latency, 0, true, "Kết nối thành công trong ${latency}ms"))
                }
            } catch (cancelled: CancellationException) {
                continuation.cancel(cancelled)
            } catch (error: Exception) {
                if (continuation.isActive) {
                    continuation.resume(PingResult(
                        timeout.toLong(), 100, false,
                        "Timeout hoặc lỗi kết nối: ${error.localizedMessage ?: "Unknown error"}"
                    ))
                }
            } finally {
                runCatching { socket.close() }
            }
        }
    }

    /**
     * Thực hiện chuỗi đo liên tiếp (mặc định 3 lần TCP connect)
     * và tính toán thời gian phản hồi trung bình cùng tỷ lệ rớt gói.
     */
    suspend fun batchPing(
        packetsCount: Int = 3,
        host: String = defaultHost,
        port: Int = defaultPort,
        timeout: Int = timeoutMs,
        socketFactory: () -> Socket = { Socket() },
        validateConnection: () -> Unit = {}
    ): PingResult = withContext(dispatcher) {
        require(packetsCount > 0)
        val latencies = mutableListOf<Long>()
        var failedCount = 0

        repeat(packetsCount) {
            currentCoroutineContext().ensureActive()
            validateConnection()
            val result = singlePing(host, port, timeout, socketFactory)
            currentCoroutineContext().ensureActive()
            validateConnection()
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
