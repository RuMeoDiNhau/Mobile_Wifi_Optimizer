package com.mobilelegend.wlo.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mobilelegend.wlo.presentation.live_radar.LiveRadarUiState
import com.mobilelegend.wlo.presentation.live_radar.RadarFailure
import com.mobilelegend.wlo.presentation.live_radar.RadarObservation

/** Describes only current-network observation; saved benchmark results keep their own time. */
@Composable
fun RadarObservationNotice(state: LiveRadarUiState, onRetry: () -> Unit) {
    val message = when (state.observation) {
        RadarObservation.PAUSED -> "Radar đã tạm dừng."
        RadarObservation.WAITING_FOR_CONDITIONS -> "Radar đang chờ Wi-Fi, quyền và dịch vụ vị trí."
        RadarObservation.STARTING -> "Radar đang chờ mẫu Wi-Fi mới."
        RadarObservation.LIVE -> "Radar đang cập nhật."
        RadarObservation.RETRYING, RadarObservation.ERROR -> when (state.failure) {
            RadarFailure.PERMISSION_DENIED -> "Android từ chối đọc Wi-Fi. Kiểm tra quyền rồi thử lại."
            RadarFailure.STALE_DATA -> "Radar không nhận được mẫu mới; đã ẩn thông số cũ."
            RadarFailure.STREAM_ENDED -> "Luồng Radar đã dừng trước dự kiến."
            else -> "Không thể cập nhật Radar."
        }
    }
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Mạng hiện tại: $message")
            Text(state.sampleAgeMs?.let { "Mẫu gần nhất: ${it / 1_000L} giây trước." }
                ?: "Chưa nhận được mẫu Wi-Fi.")
            if (state.observation == RadarObservation.RETRYING) {
                Text("Đang tự thử lại (${state.automaticRetries + 1}/3).")
            }
            if (state.observation == RadarObservation.ERROR || state.observation == RadarObservation.RETRYING) {
                TextButton(onClick = onRetry) { Text("Thử lại Radar") }
            }
        }
    }
}
