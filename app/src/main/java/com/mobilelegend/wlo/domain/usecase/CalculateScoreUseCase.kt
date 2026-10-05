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
                rawLinkSpeedMbps = wifiMetric.linkSpeedMbps
            )
        }

        if (!pingResult.isSuccess || pingResult.packetLossPercent >= 50) {
            return LocationScore(
                finalScore = 15,
                normalizedRssiScore = normalizeRssi(wifiMetric.rssi),
                normalizedPingScore = 0,
                normalizedSpeedScore = normalizeLinkSpeed(wifiMetric.linkSpeedMbps),
                category = category,
                summaryText = "Mạng mất tín hiệu Internet / Rớt gói",
                recommendation = "Đường truyền bị gián đoạn (Rớt gói ${pingResult.packetLossPercent}%). Hãy kiểm tra router hoặc di chuyển lại gần hơn.",
                isRecommended = false,
                rawRssi = wifiMetric.rssi,
                rawPingMs = pingResult.latencyMs,
                rawLinkSpeedMbps = wifiMetric.linkSpeedMbps
            )
        }

        val sRssi = normalizeRssi(wifiMetric.rssi)
        val sPing = normalizePing(pingResult.latencyMs)
        val sSpeed = normalizeLinkSpeed(wifiMetric.linkSpeedMbps)

        val weight = UseCaseWeight.getWeightFor(category)
        val rawFinalScore = (sRssi * weight.rssiWeight) +
                (sPing * weight.pingWeight) +
                (sSpeed * weight.speedWeight)
        val finalScore = rawFinalScore.roundToInt().coerceIn(0, 100)

        val (summary, recommendation, isRecommended) = generateEvaluation(
            category = category,
            finalScore = finalScore,
            rssi = wifiMetric.rssi,
            pingMs = pingResult.latencyMs,
            linkSpeedMbps = wifiMetric.linkSpeedMbps
        )

        return LocationScore(
            finalScore = finalScore,
            normalizedRssiScore = sRssi,
            normalizedPingScore = sPing,
            normalizedSpeedScore = sSpeed,
            category = category,
            summaryText = summary,
            recommendation = recommendation,
            isRecommended = isRecommended,
            rawRssi = wifiMetric.rssi,
            rawPingMs = pingResult.latencyMs,
            rawLinkSpeedMbps = wifiMetric.linkSpeedMbps
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
                        "Tuyệt vời để Leo Rank",
                        "Độ trễ cực thấp (${pingMs}ms), tín hiệu sóng ổn định. Phù hợp hoàn hảo cho Liên Quân, PUBG Mobile.",
                        true
                    )
                } else if (finalScore >= 60 && pingMs <= 90) {
                    Triple(
                        "Khá ổn cho Gaming nhẹ",
                        "Độ trễ trung bình (${pingMs}ms). Có thể chơi tốt các game turn-based hoặc game giải trí nhẹ.",
                        false
                    )
                } else {
                    Triple(
                        "Cảnh báo giật lag / High Ping",
                        "Ping cao (${pingMs}ms) hoặc sóng yếu (${rssi}dBm). Không nên chơi game hành động tại vị trí này.",
                        false
                    )
                }
            }

            UseCaseCategory.STUDY_DOWNLOAD -> {
                if (finalScore >= 70 && linkSpeedMbps >= 50) {
                    Triple(
                        "Băng thông tải File tốc độ cao",
                        "Tốc độ kết nối đạt ${linkSpeedMbps}Mbps. Tải tài liệu học tập, slide bài giảng và video 4K siêu nhanh.",
                        true
                    )
                } else if (finalScore >= 50) {
                    Triple(
                        "Đáp ứng duyệt Web & Đọc sách",
                        "Tốc độ ${linkSpeedMbps}Mbps vừa đủ để đọc tài liệu online và xem video 1080p.",
                        false
                    )
                } else {
                    Triple(
                        "Tốc độ tải yếu",
                        "Băng thông bị giới hạn (${linkSpeedMbps}Mbps). Hãy di chuyển tới vị trí gần Router hơn để tải tệp lớn.",
                        false
                    )
                }
            }

            UseCaseCategory.VIDEO_CALL -> {
                if (finalScore >= 70 && pingMs <= 100 && rssi >= -75) {
                    Triple(
                        "Đường truyền Video Call HD sắc nét",
                        "Sóng ổn định (${rssi}dBm), độ trễ tốt (${pingMs}ms). Hoàn hảo cho các buổi học Zoom, Google Meet.",
                        true
                    )
                } else {
                    Triple(
                        "Chất lượng Call có thể bị mờ / gián đoạn",
                        "Tín hiệu chập chờn hoặc độ trễ dao động. Nên đổi chỗ ngồi để tránh giật khung hình khi bật camera.",
                        false
                    )
                }
            }
        }
    }
}
