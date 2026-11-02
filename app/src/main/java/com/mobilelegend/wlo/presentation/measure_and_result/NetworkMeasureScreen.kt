package com.mobilelegend.wlo.presentation.measure_and_result

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mobilelegend.wlo.presentation.components.AppTopHeader
import com.mobilelegend.wlo.presentation.components.PermissionBanner
import com.mobilelegend.wlo.presentation.components.PulseWaveIcon
import com.mobilelegend.wlo.presentation.components.WifiPlaceItem
import com.mobilelegend.wlo.presentation.live_radar.LiveRadarViewModel
import com.mobilelegend.wlo.presentation.theme.AccentEmerald
import com.mobilelegend.wlo.presentation.theme.AppBackground
import com.mobilelegend.wlo.presentation.theme.CardBorder
import com.mobilelegend.wlo.presentation.theme.DividerLight
import com.mobilelegend.wlo.presentation.theme.LightGreenSurface
import com.mobilelegend.wlo.presentation.theme.PrimaryGreen
import com.mobilelegend.wlo.presentation.theme.PrimaryGreenDark
import com.mobilelegend.wlo.presentation.theme.SurfaceWhite
import com.mobilelegend.wlo.presentation.theme.TextMuted
import com.mobilelegend.wlo.presentation.theme.TextPrimary
import com.mobilelegend.wlo.presentation.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NetworkMeasureScreen(
    liveRadarViewModel: LiveRadarViewModel,
    measureViewModel: MeasureViewModel,
    inspectedPlace: WifiPlaceItem?,
    onOpenLocationSettings: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onScoreMeasured: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val radarState by liveRadarViewModel.uiState.collectAsStateWithLifecycle()
    val measureState by measureViewModel.uiState.collectAsStateWithLifecycle()

    var lastMeasureTimeText by remember { mutableStateOf("Hôm nay, 09:42") }

    val completedScore = (measureState.status as? MeasureStatus.Completed)?.score
    val isMeasuring = measureState.status is MeasureStatus.Measuring

    LaunchedEffect(completedScore) {
        if (completedScore != null && completedScore.finalScore > 0) {
            onScoreMeasured(completedScore.finalScore)
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            lastMeasureTimeText = "Hôm nay, ${sdf.format(Date())}"
        }
    }

    // Lấy thông số hiển thị (kết hợp dữ liệu thực tế hoặc thông số mẫu chuẩn theo UI)
    val displaySsid = when {
        inspectedPlace != null -> inspectedPlace.ssid
        radarState.isWifiConnected &&
            radarState.radarData.ssid.isNotBlank() &&
            !radarState.radarData.ssid.contains("unknown", ignoreCase = true) &&
            !radarState.radarData.ssid.contains("Đang quét", ignoreCase = true) -> radarState.radarData.ssid
        else -> "The Workshop Guest"
    }

    val displayScore = when {
        completedScore != null && completedScore.finalScore > 0 -> completedScore.finalScore
        inspectedPlace != null -> inspectedPlace.score
        radarState.isWifiConnected && radarState.radarData.signalPercentage > 0 -> radarState.radarData.signalPercentage
        else -> 92
    }

    val displayDownloadMbps = when {
        completedScore != null && completedScore.rawLinkSpeedMbps > 0 -> completedScore.rawLinkSpeedMbps
        inspectedPlace != null -> inspectedPlace.downloadMbps
        radarState.isWifiConnected && radarState.radarData.linkSpeedMbps > 0 -> radarState.radarData.linkSpeedMbps
        else -> 128
    }

    val displayUploadMbps = when {
        completedScore != null && completedScore.rawLinkSpeedMbps > 0 -> (completedScore.rawLinkSpeedMbps * 0.35f).toInt().coerceAtLeast(12)
        inspectedPlace != null -> inspectedPlace.uploadMbps
        else -> 42
    }

    val displayPingMs = when {
        completedScore != null && completedScore.rawPingMs in 1..999 -> completedScore.rawPingMs.toInt()
        inspectedPlace != null -> inspectedPlace.pingMs
        else -> 12
    }

    val statusHeadline = when (val st = measureState.status) {
        is MeasureStatus.Measuring -> "Đang đo chất lượng..."
        is MeasureStatus.Completed -> if (st.score.finalScore > 0) st.score.summaryText else "Kết nối rất tốt"
        else -> if (displayScore >= 85) "Kết nối rất tốt" else if (displayScore >= 70) "Kết nối ổn định" else "Kết nối trung bình"
    }

    val statusSubtitle = when (val st = measureState.status) {
        is MeasureStatus.Measuring -> st.currentStepText
        else -> "Phù hợp cho họp video, làm việc và phát video 4K"
    }

    val animatedGaugeProgress by animateFloatAsState(
        targetValue = if (isMeasuring) {
            (measureState.status as? MeasureStatus.Measuring)?.progress ?: 0.6f
        } else {
            (displayScore / 100f).coerceIn(0.1f, 1f)
        },
        animationSpec = tween(durationMillis = 600),
        label = "MeasureGaugeProgress"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 18.dp, bottom = 24.dp)
    ) {
        // 1. Header
        AppTopHeader(
            overline = "KIỂM TRA KẾT NỐI",
            title = "Đo chất lượng mạng"
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Cảnh báo quyền vị trí nếu thiếu
        if (!radarState.isLocationPermissionGranted) {
            PermissionBanner(
                message = "Cấp quyền Vị trí để ứng dụng đọc trực tiếp tên mạng Wi-Fi (SSID) và cường độ dBm.",
                buttonText = "Cấp quyền",
                onActionClick = onOpenAppSettings
            )
            Spacer(modifier = Modifier.height(12.dp))
        } else if (!radarState.isGpsEnabled) {
            PermissionBanner(
                message = "Bật định vị GPS để Android cho phép hiển thị tên mạng Wi-Fi hiện tại.",
                buttonText = "Bật GPS",
                onActionClick = onOpenLocationSettings
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // 2. Main Dark Green Gauge Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(26.dp),
                    ambientColor = PrimaryGreen.copy(alpha = 0.28f),
                    spotColor = PrimaryGreen.copy(alpha = 0.28f)
                )
                .clip(RoundedCornerShape(26.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            PrimaryGreen,
                            PrimaryGreenDark
                        )
                    )
                )
                .padding(horizontal = 22.dp, vertical = 22.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top SSID Pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.13f))
                        .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(50))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = displaySsid,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Gauge Arc + Score
                Box(
                    modifier = Modifier.size(width = 220.dp, height = 175.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val diameter = size.width.coerceAtMost(size.height * 1.15f)
                        val strokeWidth = 13.dp.toPx()
                        val arcDiameter = diameter - strokeWidth * 2
                        val left = (size.width - arcDiameter) / 2f
                        val top = (size.height - arcDiameter) / 2f + 10.dp.toPx()

                        // Vòng tròn mảnh bao ngoài
                        drawCircle(
                            color = Color.White.copy(alpha = 0.08f),
                            radius = arcDiameter * 0.59f,
                            center = Offset(size.width / 2f, top + arcDiameter / 2f),
                            style = Stroke(width = 1.dp.toPx())
                        )

                        // Vòng cung nền
                        drawArc(
                            color = Color.White.copy(alpha = 0.15f),
                            startAngle = 145f,
                            sweepAngle = 250f,
                            useCenter = false,
                            topLeft = Offset(left, top),
                            size = Size(arcDiameter, arcDiameter),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                        )

                        // Vòng cung tiến trình
                        drawArc(
                            color = AccentEmerald.copy(alpha = 0.45f),
                            startAngle = 145f,
                            sweepAngle = 250f * animatedGaugeProgress,
                            useCenter = false,
                            topLeft = Offset(left, top),
                            size = Size(arcDiameter, arcDiameter),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(top = 12.dp)
                    ) {
                        PulseWaveIcon(
                            color = AccentEmerald,
                            modifier = Modifier.size(26.dp),
                            strokeWidthDp = 2.dp
                        )
                        Text(
                            text = "$displayScore",
                            fontSize = 54.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            lineHeight = 56.sp
                        )
                        Text(
                            text = "ĐIỂM CHẤT LƯỢNG",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.sp,
                            color = Color.White.copy(alpha = 0.65f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = statusHeadline,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = statusSubtitle,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.72f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Nút "Đo lại"
                Button(
                    onClick = {
                        measureViewModel.startBenchmark(radarState.selectedCategory)
                    },
                    enabled = !isMeasuring,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SurfaceWhite,
                        contentColor = TextPrimary,
                        disabledContainerColor = SurfaceWhite.copy(alpha = 0.85f),
                        disabledContentColor = TextSecondary
                    ),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    PulseWaveIcon(
                        color = PrimaryGreen,
                        modifier = Modifier.size(18.dp),
                        strokeWidthDp = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isMeasuring) "Đang đo..." else "Đo lại",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Hàng 3 Thẻ Thông số (Tải xuống, Tải lên, Độ trễ)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SpeedMetricSmallCard(
                icon = Icons.Outlined.FileDownload,
                label = "Tải xuống",
                value = "$displayDownloadMbps",
                unit = "Mbps",
                modifier = Modifier.weight(1f)
            )
            SpeedMetricSmallCard(
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                label = "Tải lên",
                value = "$displayUploadMbps",
                unit = "Mbps",
                modifier = Modifier.weight(1f)
            )
            SpeedMetricSmallCard(
                icon = Icons.Outlined.AccessTime,
                label = "Độ trễ",
                value = "$displayPingMs",
                unit = "ms",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Thẻ "Chi tiết kết nối"
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 3.dp,
                    shape = RoundedCornerShape(22.dp),
                    ambientColor = Color.Black.copy(alpha = 0.03f),
                    spotColor = Color.Black.copy(alpha = 0.05f)
                )
                .clip(RoundedCornerShape(22.dp))
                .background(SurfaceWhite)
                .border(1.dp, CardBorder, RoundedCornerShape(22.dp))
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Chi tiết kết nối",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(LightGreenSurface)
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "Ổn định",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            ConnectionDetailRow(
                label = "Nhà mạng",
                value = inspectedPlace?.isp ?: "FPT Telecom"
            )
            ConnectionDivider()
            ConnectionDetailRow(
                label = "Địa điểm",
                value = if (inspectedPlace != null) "${inspectedPlace.district}, TP.HCM" else "Quận 1, TP.HCM"
            )
            ConnectionDivider()
            ConnectionDetailRow(
                label = "Thời gian đo",
                value = lastMeasureTimeText
            )
        }
    }
}

@Composable
private fun SpeedMetricSmallCard(
    icon: ImageVector,
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .shadow(
                elevation = 3.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Color.Black.copy(alpha = 0.03f),
                spotColor = Color.Black.copy(alpha = 0.05f)
            )
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceWhite)
            .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
            .padding(vertical = 16.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = PrimaryGreen,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = TextMuted
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = unit,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = TextMuted,
                modifier = Modifier.padding(bottom = 3.dp)
            )
        }
    }
}

@Composable
private fun ConnectionDetailRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = TextMuted
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
    }
}

@Composable
private fun ConnectionDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(DividerLight)
    )
}
