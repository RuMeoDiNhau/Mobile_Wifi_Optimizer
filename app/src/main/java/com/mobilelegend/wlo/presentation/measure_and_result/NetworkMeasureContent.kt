package com.mobilelegend.wlo.presentation.measure_and_result

import com.mobilelegend.wlo.domain.model.LocationScore
import com.mobilelegend.wlo.domain.model.UseCaseCategory
import com.mobilelegend.wlo.presentation.components.MISSING_METRIC
import com.mobilelegend.wlo.presentation.components.WifiPlaceItem
import com.mobilelegend.wlo.presentation.components.linkSpeedText
import com.mobilelegend.wlo.presentation.components.networkName
import com.mobilelegend.wlo.presentation.components.pingText
import com.mobilelegend.wlo.presentation.live_radar.LiveRadarUiState

/** One display source at a time: an inspected place OR the current benchmark. */
data class NetworkMeasureContent(
    val headline: String,
    val description: String,
    val statusLabel: String,
    val ssid: String = MISSING_METRIC,
    val score: Int? = null,
    val linkSpeed: String = MISSING_METRIC,
    val ping: String = MISSING_METRIC,
    val isp: String = MISSING_METRIC,
    val placeName: String = MISSING_METRIC,
    val bssid: String = MISSING_METRIC,
    val measuredAtMillis: Long? = null,
    val category: UseCaseCategory? = null,
    val progress: Float = 0f
)

fun measurementBlockReason(radar: LiveRadarUiState): String? = when {
    !radar.isWifiConnected -> "Chưa kết nối Wi-Fi"
    !radar.isLocationPermissionGranted -> "Cần cấp quyền vị trí để đọc Wi-Fi"
    !radar.isGpsEnabled -> "Cần bật dịch vụ vị trí"
    !radar.radarData.isConnected -> "Đang chờ thông tin Wi-Fi"
    else -> null
}

fun networkMeasureContent(
    status: MeasureStatus,
    radar: LiveRadarUiState,
    inspectedPlace: WifiPlaceItem?
): NetworkMeasureContent {
    if (inspectedPlace != null) {
        // Never consult the active benchmark or live radar for place metrics/identity.
        val measurement = inspectedPlace.measurement
        if (measurement != null) {
            return measurementContent(measurement).copy(
                placeName = inspectedPlace.name,
                isp = inspectedPlace.isp.ifBlank { MISSING_METRIC }
            )
        }
        return NetworkMeasureContent(
            headline = inspectedPlace.name,
            description = if (inspectedPlace.isDemo) {
                "Dữ liệu minh họa của địa điểm, chưa được đo trên thiết bị này."
            } else {
                "Thông tin đã lưu của địa điểm; không phải phép đo mạng đang kết nối."
            },
            statusLabel = if (inspectedPlace.isDemo) "Dữ liệu minh họa" else "Địa điểm đã lưu",
            ssid = networkName(inspectedPlace.ssid),
            score = inspectedPlace.score,
            linkSpeed = linkSpeedText(inspectedPlace.linkSpeedMbps),
            ping = pingText(inspectedPlace.pingMs, inspectedPlace.pingSuccessful),
            isp = inspectedPlace.isp.ifBlank { MISSING_METRIC },
            placeName = inspectedPlace.name,
            progress = (inspectedPlace.score / 100f).coerceIn(0f, 1f)
        )
    }

    val ssid = if (radar.isWifiConnected && radar.radarData.isConnected) {
        networkName(radar.radarData.ssid)
    } else MISSING_METRIC

    return when (status) {
        is MeasureStatus.Completed -> measurementContent(status.score)
        is MeasureStatus.Measuring -> NetworkMeasureContent(
            headline = "Đang đo chất lượng...",
            description = status.currentStepText,
            statusLabel = "Đang đo",
            // The live name can change before completion; do not attribute partial results to it.
            progress = status.progress.coerceIn(0f, 1f)
        )
        is MeasureStatus.Failed -> NetworkMeasureContent(
            headline = "Đo thất bại",
            description = status.errorMessage,
            statusLabel = "Thất bại"
        )
        MeasureStatus.Cancelled -> NetworkMeasureContent(
            headline = "Đã hủy phép đo",
            description = "Lượt đo bị hủy không được lưu. Bạn có thể bắt đầu lượt mới sau khi dọn xong kết nối.",
            statusLabel = "Đã hủy"
        )
        MeasureStatus.Idle -> {
            val reason = measurementBlockReason(radar)
            NetworkMeasureContent(
                headline = reason ?: "Chưa đo chất lượng mạng",
                description = if (reason != null) {
                    "Kết nối Wi-Fi, cấp quyền và bật dịch vụ vị trí trước khi đo."
                } else {
                    "Bấm đo để kiểm tra mạng Wi-Fi thiết bị đang kết nối."
                },
                statusLabel = if (reason == null) "Chưa đo" else "Chưa sẵn sàng",
                ssid = ssid
            )
        }
    }
}

private fun measurementContent(score: LocationScore): NetworkMeasureContent {
    val context = score.measurement
    val hasValidMetrics = score.pingSuccessful == true && score.rawPingMs >= 0 &&
        score.rawLinkSpeedMbps > 0 && score.rawRssi in -126..-1
    return NetworkMeasureContent(
        headline = when {
            score.pingSuccessful == false -> "Không phản hồi"
            score.finalScore == 0 -> "Chất lượng kết nối kém"
            !hasValidMetrics -> "Kết quả thiếu dữ liệu"
            else -> score.summaryText
        },
        description = when {
            score.pingSuccessful == false -> "Không nhận được phản hồi từ đích kiểm tra. Hãy kiểm tra kết nối rồi thử lại."
            !hasValidMetrics -> "Chưa đủ thông số hợp lệ để đưa ra khuyến nghị."
            else -> score.recommendation
        },
        statusLabel = when {
            score.pingSuccessful == false -> "Không phản hồi"
            !hasValidMetrics -> "Thiếu dữ liệu"
            else -> "Kết quả đã đo"
        },
        ssid = networkName(context?.ssid),
        score = score.finalScore,
        linkSpeed = linkSpeedText(score.rawLinkSpeedMbps),
        ping = pingText(score.rawPingMs, score.pingSuccessful),
        bssid = context?.bssid?.takeUnless {
            it.isBlank() || it == "00:00:00:00:00:00" || it == "02:00:00:00:00:00"
        } ?: MISSING_METRIC,
        measuredAtMillis = context?.measuredAtMillis,
        category = score.category,
        progress = (score.finalScore / 100f).coerceIn(0f, 1f)
    )
}
