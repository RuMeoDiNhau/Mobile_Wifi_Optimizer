package com.mobilelegend.wlo.presentation.measure_and_result

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mobilelegend.wlo.domain.model.UseCaseCategory
import com.mobilelegend.wlo.domain.model.BenchmarkReadiness
import com.mobilelegend.wlo.presentation.components.MeasurementReadinessNotice
import com.mobilelegend.wlo.presentation.theme.DarkBackground
import com.mobilelegend.wlo.presentation.theme.PrimaryBlue
import com.mobilelegend.wlo.presentation.theme.PrimaryBlueVariant
import com.mobilelegend.wlo.presentation.theme.SecondaryTeal
import com.mobilelegend.wlo.presentation.theme.SignalRed
import com.mobilelegend.wlo.presentation.theme.SurfaceDark
import com.mobilelegend.wlo.presentation.theme.TextMuted
import com.mobilelegend.wlo.presentation.theme.TextPrimary
import com.mobilelegend.wlo.presentation.theme.TextSecondary

@Composable
fun MeasureScreen(
    category: UseCaseCategory,
    viewModel: MeasureViewModel,
    onBenchmarkCompleted: () -> Unit,
    onBackToRadar: () -> Unit,
    onStartMeasurement: () -> Unit,
    readiness: BenchmarkReadiness,
    onOpenAppSettings: () -> Unit,
    onOpenLocationSettings: () -> Unit,
    onOpenWifiSettings: () -> Unit,
    radarNotice: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.status) {
        if (uiState.status is MeasureStatus.Completed) {
            onBenchmarkCompleted()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "RadarPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        MeasurementReadinessNotice(readiness, onOpenAppSettings, onOpenLocationSettings, onOpenWifiSettings)
        radarNotice()
        when (val status = uiState.status) {
            is MeasureStatus.Measuring -> {
                // Pulse Animation Box
                Box(
                    modifier = Modifier.size(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(PrimaryBlue.copy(alpha = 0.15f))
                    )

                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .background(SurfaceDark)
                            .border(2.dp, PrimaryBlue, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = SecondaryTeal,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "Đang Kiểm Định Vị Trí...",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = category.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryBlue
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = status.currentStepText,
                    fontSize = 13.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(24.dp))

                val animatedProgress by animateFloatAsState(
                    targetValue = status.progress,
                    animationSpec = tween(400),
                    label = "ProgressBar"
                )

                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = PrimaryBlue,
                    trackColor = SurfaceDark
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = viewModel::cancelBenchmark) { Text("Hủy phép đo") }
            }

            is MeasureStatus.Failed -> {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = null,
                    tint = SignalRed,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Kiểm định Thất Bại",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = SignalRed
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = status.errorMessage,
                    fontSize = 13.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onStartMeasurement,
                    enabled = readiness.canMeasure,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlueVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = "Thử lại", fontWeight = FontWeight.Bold)
                }
            }

            MeasureStatus.Idle, MeasureStatus.Cancelled -> {
                Text(if (uiState.status == MeasureStatus.Cancelled) "Đã hủy phép đo" else "Sẵn sàng đo", color = TextPrimary)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onStartMeasurement,
                    enabled = readiness.canMeasure
                ) {
                    Text(if (uiState.isBenchmarkRunning) "Đang dọn kết nối..." else "Đo lại")
                }
                Button(onClick = onBackToRadar) { Text("Quay lại") }
            }

            else -> {
                // Idle or Completed
            }
        }
    }
}
