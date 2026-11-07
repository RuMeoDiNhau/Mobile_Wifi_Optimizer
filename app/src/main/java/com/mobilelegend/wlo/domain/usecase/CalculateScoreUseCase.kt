package com.mobilelegend.wlo.domain.usecase

import com.mobilelegend.wlo.data.model.PingResult
import com.mobilelegend.wlo.data.model.WifiMetric
import com.mobilelegend.wlo.domain.model.LocationScore
import com.mobilelegend.wlo.domain.model.UseCaseCategory
import com.mobilelegend.wlo.domain.model.UseCaseWeight
import kotlin.math.roundToInt

/**
 * Scoring Engine: Thuật toán chuẩn hóa dữ liệu phần cứng và chấm điểm chất lượng mạng (0 - 100)
 * theo từng loại nhu cầu cụ thể.
 */
class CalculateScoreUseCase {

    /**
     * Chuẩn hóa Cường độ sóng RSSI (-90 dBm -> 0 điểm, -50 dBm -> 100 điểm)
     */
    fun normalizeRssi(rssi: Int): Int {
        val minRssi = -90f
        val maxRssi = -50f
        val score = ((rssi - minRssi) / (maxRssi - minRssi)) * 100f
        return score.roundToInt().coerceIn(0, 100)
    }

    /**
     * Chuẩn hóa Độ trễ Ping (<= 20ms -> 100 điểm, >= 200ms -> 0 điểm)
     */
    fun normalizePing(pingMs: Long): Int {
        val minPing = 20f
        val maxPing = 200f
        val score = ((maxPing - pingMs) / (maxPing - minPing)) * 100f
        return score.roundToInt().coerceIn(0, 100)
    }

    /**
     * Chuẩn hóa Tốc độ kết nối LinkSpeed (<= 10Mbps -> 0 điểm, >= 300Mbps -> 100 điểm)
     */
    fun normalizeLinkSpeed(linkSpeedMbps: Int): Int {
        val minSpeed = 10f
        val maxSpeed = 300f
        val score = ((linkSpeedMbps - minSpeed) / (maxSpeed - minSpeed)) * 100f
        return score.roundToInt().coerceIn(0, 100)
    }

    /**
     * Tính toán điểm số tổng hợp và tạo đánh giá chi tiết.
     */
    operator fun invoke(
        wifiMetric: WifiMetric,
        pingResult: PingResult,
        category: UseCaseCategory
    ): LocationScore {
        if (!wifiMetric.isConnected) {
            return LocationScore(
                finalScore = 0,
                normalizedRssiScore = 0,
                normalizedPingScore = 0,
                normalizedSpeedScore = 0,
                category = category,
                summaryText = "Chưa kết nối Wi-Fi",
                recommendation = "Hãy bật Wi-Fi và kết nối vào một Access Point trước khi đo.",
                isRecommended = false,
                rawRssi = wifiMetric.rssi,
                rawPingMs = pingResult.latencyMs,
                rawLinkSpeedMbps = wifiMetric.linkSpeedMbps,
                pingSuccessful = null
            )
        }

        val sRssi = normalizeRssi(wifiMetric.rssi)
        val sPing = if (pingResult.isSuccess) normalizePing(pingResult.latencyMs) else 0
        val sSpeed = normalizeLinkSpeed(wifiMetric.linkSpeedMbps)

        val weight = UseCaseWeight.getWeightFor(category)
        val rawFinalScore = (sRssi * weight.rssiWeight) +
                (sPing * weight.pingWeight) +
                (sSpeed * weight.speedWeight)
        // Penalize every failed TCP attempt, including 1/3 failures. This is an app
        // scoring policy, not a measurement of IP packet loss or application throughput.
        val finalScore = (rawFinalScore * (100 - pingResult.tcpFailurePercent) / 100f)
            .roundToInt().coerceIn(0, 100)

        val (summary, recommendation, isRecommended) = when {
            !pingResult.isSuccess -> Triple(
                "Không kết nối được đích TCP",
                "100% lượt TCP connect thất bại. Chưa đủ dữ liệu để kết luận toàn bộ Internet bị mất. Hãy thử lại hoặc kiểm tra kết nối.",
                false
            )
            pingResult.tcpFailurePercent > 0 -> Triple(
                "Kết nối TCP chưa ổn định",
                "${pingResult.tcpFailurePercent}% lượt TCP connect thất bại; độ trễ ${pingResult.latencyMs} ms chỉ tính các lượt thành công. Chưa khuyến nghị cho nhu cầu này; hãy thử lại.",
                false
            )
            else -> generateEvaluation(
                category = category,
                finalScore = finalScore,
                rssi = wifiMetric.rssi,
                pingMs = pingResult.latencyMs,
                linkSpeedMbps = wifiMetric.linkSpeedMbps
            )
        }

        return LocationScore(
            finalScore = finalScore,
            normalizedRssiScore = sRssi,
            normalizedPingScore = sPing,
            normalizedSpeedScore = sSpeed,
            category = category,
            summaryText = summary,
            recommendation = "$recommendation $TCP_MEASUREMENT_SCOPE",
            isRecommended = isRecommended,
            rawRssi = wifiMetric.rssi,
            rawPingMs = pingResult.latencyMs,
            rawLinkSpeedMbps = wifiMetric.linkSpeedMbps,
            pingSuccessful = pingResult.isSuccess,
            tcpFailurePercent = pingResult.tcpFailurePercent
        )
    }

    private fun generateEvaluation(
        category: UseCaseCategory,
        finalScore: Int,
        rssi: Int,
        pingMs: Long,
        linkSpeedMbps: Int
    ): Triple<String, String, Boolean> {
        return when (category) {
            UseCaseCategory.GAMING -> {
                if (finalScore >= 75 && pingMs <= 60) {
                    Triple(
                        "Tín hiệu Wi-Fi và độ trễ TCP tốt",
                        "Độ trễ TCP tới đích kiểm tra là ${pingMs} ms. Các mẫu này đạt ngưỡng tham khảo cho Gaming.",
                        true
                    )
                } else if (finalScore >= 60 && pingMs <= 90) {
                    Triple(
                        "Khá ổn cho Gaming nhẹ",
                        "Độ trễ TCP tới đích kiểm tra là ${pingMs} ms. Cần thử trực tiếp với máy chủ game để đánh giá trải nghiệm.",
                        false
                    )
                } else {
                    Triple(
                        "Cảnh báo giật lag / High Ping",
                        "Độ trễ TCP cao (${pingMs} ms) hoặc sóng yếu (${rssi} dBm). Các mẫu chưa đạt ngưỡng tham khảo cho Gaming.",
                        false
                    )
                }
            }

            UseCaseCategory.STUDY_DOWNLOAD -> {
                if (finalScore >= 70 && linkSpeedMbps >= 50) {
                    Triple(
                        "Liên kết Wi-Fi tốt",
                        "Tốc độ liên kết Wi-Fi đạt ${linkSpeedMbps} Mbps. Chưa đo tốc độ tải lên/tải xuống Internet.",
                        true
                    )
                } else if (finalScore >= 50) {
                    Triple(
                        "Liên kết Wi-Fi trung bình",
                        "Tốc độ liên kết Wi-Fi: ${linkSpeedMbps} Mbps. Chưa đủ dữ liệu để kết luận tốc độ tải Internet.",
                        false
                    )
                } else {
                    Triple(
                        "Liên kết Wi-Fi yếu hoặc chưa xác định",
                        "Hãy kiểm tra kết nối hoặc di chuyển gần router hơn. Chưa đo tốc độ tải Internet.",
                        false
                    )
                }
            }

            UseCaseCategory.VIDEO_CALL -> {
                if (finalScore >= 70 && pingMs <= 100 && rssi >= -75) {
                    Triple(
                        "Tín hiệu Wi-Fi và TCP đạt ngưỡng gọi video",
                        "RSSI ${rssi} dBm, độ trễ TCP ${pingMs} ms. Chưa đo chất lượng âm thanh/hình ảnh của ứng dụng gọi video.",
                        true
                    )
                } else {
                    Triple(
                        "Chưa đạt ngưỡng tham khảo cho gọi video",
                        "RSSI ${rssi} dBm, độ trễ TCP ${pingMs} ms. Hãy kiểm tra kết nối và thử gọi trực tiếp để đánh giá chất lượng.",
                        false
                    )
                }
            }
        }
    }
}
