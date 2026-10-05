package com.mobilelegend.wlo.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobilelegend.wlo.domain.model.SignalQualityLevel
import com.mobilelegend.wlo.presentation.theme.SignalGreen
import com.mobilelegend.wlo.presentation.theme.SignalRed
import com.mobilelegend.wlo.presentation.theme.SignalYellow
import com.mobilelegend.wlo.presentation.theme.TextMuted
import com.mobilelegend.wlo.presentation.theme.TextPrimary

/**
 * Đồng hồ Gauge dạng bán nguyệt đo cường độ sóng Wi-Fi thời gian thực (Live Radar Gauge).
 */
@Composable
fun SignalGaugeIndicator(
    rssi: Int,
    percentage: Int,
    qualityLevel: SignalQualityLevel,
    modifier: Modifier = Modifier,
    size: Dp = 220.dp
) {
    val targetColor = when (qualityLevel) {
        SignalQualityLevel.EXCELLENT -> SignalGreen
        SignalQualityLevel.GOOD -> SignalYellow
        SignalQualityLevel.POOR -> SignalRed
    }

    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 400),
        label = "GaugeColor"
    )

    val animatedProgress by animateFloatAsState(
        targetValue = (percentage / 100f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 400),
        label = "GaugeProgress"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 24.dp.toPx()
            val arcSize = size.toPx() - strokeWidth
            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

            // Vòng cung nền (Track) - 240 độ từ 150 độ đến 390 độ
            drawArc(
                color = Color(0xFF334155),
                startAngle = 150f,
                sweepAngle = 240f,
                useCenter = false,
                topLeft = topLeft,
                size = Size(arcSize, arcSize),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Vòng cung tiến trình (Progress)
            drawArc(
                color = animatedColor,
                startAngle = 150f,
                sweepAngle = 240f * animatedProgress,
                useCenter = false,
                topLeft = topLeft,
                size = Size(arcSize, arcSize),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${rssi} dBm",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = animatedColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$percentage% Tín hiệu",
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary
            )
            Text(
                text = when (qualityLevel) {
                    SignalQualityLevel.EXCELLENT -> "Sóng Mạnh"
                    SignalQualityLevel.GOOD -> "Sóng Ổn Định"
                    SignalQualityLevel.POOR -> "Sóng Yếu"
                },
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = animatedColor
            )
        }
    }
}
