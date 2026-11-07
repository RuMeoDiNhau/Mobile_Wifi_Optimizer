package com.mobilelegend.wlo.presentation.live_radar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mobilelegend.wlo.domain.model.UseCaseCategory
import com.mobilelegend.wlo.presentation.components.MeasurementReadinessNotice
import com.mobilelegend.wlo.presentation.components.RadarObservationNotice
import com.mobilelegend.wlo.presentation.components.MISSING_METRIC
import com.mobilelegend.wlo.domain.model.BenchmarkReadiness
import com.mobilelegend.wlo.presentation.components.SignalGaugeIndicator
import com.mobilelegend.wlo.presentation.theme.CardBorder
import com.mobilelegend.wlo.presentation.theme.DarkBackground
import com.mobilelegend.wlo.presentation.theme.PrimaryBlue
import com.mobilelegend.wlo.presentation.theme.PrimaryBlueVariant
import com.mobilelegend.wlo.presentation.theme.SecondaryTeal
import com.mobilelegend.wlo.presentation.theme.SignalGreen
import com.mobilelegend.wlo.presentation.theme.SignalRed
import com.mobilelegend.wlo.presentation.theme.SurfaceDark
import com.mobilelegend.wlo.presentation.theme.SurfaceDarkElevated
import com.mobilelegend.wlo.presentation.theme.TextMuted
import com.mobilelegend.wlo.presentation.theme.TextPrimary
import com.mobilelegend.wlo.presentation.theme.TextSecondary

@Composable
fun LiveRadarScreen(
    viewModel: LiveRadarViewModel,
    onNavigateToBenchmark: (UseCaseCategory) -> Unit,
    onOpenLocationSettings: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenWifiSettings: () -> Unit,
    readiness: BenchmarkReadiness,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val hasSample = uiState.radarData.isConnected && uiState.radarData.isSampleAvailable

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Live Wi-Fi Radar",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )
                Text(
                    text = "La bàn sóng thời gian thực",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }

            // Pulse dot
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(if (hasSample) SignalGreen else SignalRed)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Permission / GPS Warnings
        MeasurementReadinessNotice(readiness, onOpenAppSettings, onOpenLocationSettings, onOpenWifiSettings)
        RadarObservationNotice(uiState, viewModel::refreshHardwareStatus)
        Spacer(modifier = Modifier.height(12.dp))

        // 3. Wi-Fi Status Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardBorder, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(SurfaceDarkElevated),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (uiState.isWifiConnected) Icons.Default.Wifi else Icons.Default.WifiOff,
                        contentDescription = null,
                        tint = if (uiState.isWifiConnected) PrimaryBlue else SignalRed
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (hasSample) uiState.radarData.ssid else MISSING_METRIC,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = if (hasSample) "BSSID: ${uiState.radarData.bssid}" else "Chưa có mẫu Wi-Fi hiện tại",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 4. Đồng hồ Live Gauge
        if (hasSample && uiState.radarData.rssi in -126..-1) {
            SignalGaugeIndicator(
                rssi = uiState.radarData.rssi,
                percentage = uiState.radarData.signalPercentage,
                qualityLevel = uiState.radarData.qualityLevel,
                size = 230.dp
            )
        } else {
            Box(Modifier.size(230.dp), contentAlignment = Alignment.Center) {
                Text(MISSING_METRIC, fontSize = 48.sp, color = TextMuted)
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 5. Hardware Info Row (LinkSpeed, Frequency)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = "Tốc độ liên kết Wi-Fi", fontSize = 11.sp, color = TextMuted)
                    Text(
                        text = if (hasSample && uiState.radarData.linkSpeedMbps > 0) "${uiState.radarData.linkSpeedMbps} Mbps" else MISSING_METRIC,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = SecondaryTeal
                    )
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = "Tần số", fontSize = 11.sp, color = TextMuted)
                    Text(
                        text = if (hasSample && uiState.radarData.frequencyMhz > 0) "${uiState.radarData.frequencyMhz} MHz" else MISSING_METRIC,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 6. Category Selection
        Text(
            text = "CHỌN NHU CẦU ĐO KIỂM ĐỊNH",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextMuted,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            UseCaseCategory.values().forEach { category ->
                val isSelected = uiState.selectedCategory == category
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) SurfaceDarkElevated else SurfaceDark)
                        .border(
                            1.dp,
                            if (isSelected) PrimaryBlue else CardBorder,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { viewModel.selectCategory(category) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = category.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) PrimaryBlue else TextPrimary
                        )
                        Text(
                            text = category.description,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // 7. Benchmark Button (CTA)
        Button(
            onClick = { onNavigateToBenchmark(uiState.selectedCategory) },
            enabled = readiness.canMeasure,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryBlueVariant,
                contentColor = TextPrimary
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(imageVector = Icons.Default.Speed, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Đo nhanh mạng Wi-Fi hiện tại",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}
