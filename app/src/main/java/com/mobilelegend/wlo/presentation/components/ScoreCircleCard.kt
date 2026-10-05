package com.mobilelegend.wlo.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
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
import com.mobilelegend.wlo.presentation.theme.PrimaryBlue
import com.mobilelegend.wlo.presentation.theme.SignalGreen
import com.mobilelegend.wlo.presentation.theme.SignalRed
import com.mobilelegend.wlo.presentation.theme.SignalYellow
import com.mobilelegend.wlo.presentation.theme.TextMuted

/**
 * Vòng tròn hiển thị điểm số tổng hợp (0 - 100) cho màn hình Kết quả Benchmark.
 */
@Composable
fun ScoreCircleCard(
    score: Int,
    isRecommended: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 180.dp
) {
    val progressColor = when {
        score >= 75 -> SignalGreen
        score >= 60 -> SignalYellow
        else -> SignalRed
    }

    val animatedProgress by animateFloatAsState(
        targetValue = (score / 100f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 800),
        label = "ScoreProgress"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 18.dp.toPx()
            val diameter = size.toPx() - strokeWidth
            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

            // Vòng tròn nền
            drawArc(
                color = Color(0xFF334155),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = Size(diameter, diameter),
                style = Stroke(width = strokeWidth)
            )

            // Vòng tròn điểm số
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                topLeft = topLeft,
                size = Size(diameter, diameter),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$score",
                fontSize = 48.sp,
                fontWeight = FontWeight.ExtraBold,
                color = progressColor
            )
            Text(
                text = "/ 100 ĐIỂM",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted
            )
        }
    }
}
