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
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.mobilelegend.wlo.presentation.components.MeasurementReadinessNotice
import com.mobilelegend.wlo.presentation.components.RadarObservationNotice
import com.mobilelegend.wlo.presentation.components.PulseWaveIcon
import com.mobilelegend.wlo.presentation.components.WifiPlaceItem
import com.mobilelegend.wlo.presentation.components.MISSING_METRIC
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
    onOpenWifiSettings: () -> Unit,
    onMeasureCurrentNetwork: () -> Unit,
    modifier: Modifier = Modifier
) {
    val radarState by liveRadarViewModel.uiState.collectAsStateWithLifecycle()
    val measureState by measureViewModel.uiState.collectAsStateWithLifecycle()

    val isMeasuring = measureState.isBenchmarkRunning
    val content = networkMeasureContent(measureState.status, radarState, inspectedPlace)
    val readiness = radarState.readiness(isMeasuring)
    val blockReason = readiness.reason?.message
    val lastMeasureTimeText = content.measuredAtMillis?.let {
        SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(it))
    } ?: MISSING_METRIC

    val animatedGaugeProgress by animateFloatAsState(
        targetValue = content.progress,
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
            overline = if (inspectedPlace != null) "THÔNG TIN ĐỊA ĐIỂM" else "KIỂM TRA KẾT NỐI",
            title = if (inspectedPlace != null) "Chi tiết Wi-Fi địa điểm" else "Đo chất lượng mạng"
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Cảnh báo quyền vị trí nếu thiếu
        MeasurementReadinessNotice(readiness, onOpenAppSettings, onOpenLocationSettings, onOpenWifiSettings)
        RadarObservationNotice(radarState, liveRadarViewModel::refreshHardwareStatus)
        Spacer(modifier = Modifier.height(12.dp))

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
                        text = content.ssid,
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
                            text = content.score?.toString() ?: MISSING_METRIC,
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
                    text = content.headline,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = content.description,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.72f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = if (inspectedPlace != null) {
                        "Nút bên dưới đo Wi-Fi thiết bị đang kết nối, không đo từ xa địa điểm này."
                    } else {
                        "Phép đo áp dụng cho Wi-Fi thiết bị đang kết nối."
                    },
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
                blockReason?.let {
                    Text(text = it, color = Color.White, fontSize = 12.sp, textAlign = TextAlign.Center)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onMeasureCurrentNetwork,
                    enabled = readiness.canMeasure,
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
                        text = if (isMeasuring) {
                            if (measureState.status is MeasureStatus.Measuring) "Đang đo..." else "Đang dọn kết nối..."
                        } else "Đo mạng đang kết nối",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                if (measureState.status is MeasureStatus.Measuring) {
                    Button(onClick = measureViewModel::cancelBenchmark) {
                        Text("Hủy phép đo")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Chỉ hiển thị thông số thực sự được thu thập.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SpeedMetricSmallCard(
                icon = Icons.Default.Wifi,
                label = "Tốc độ liên kết Wi-Fi",
                value = content.linkSpeed,
                modifier = Modifier.weight(1f)
            )
            SpeedMetricSmallCard(
                icon = Icons.Outlined.AccessTime,
                label = "Độ trễ TCP",
                value = content.ping,
                modifier = Modifier.weight(1f)
            )
        }
        Text(
            text = "Chưa đo tốc độ tải lên/tải xuống Internet.",
            fontSize = 12.sp,
            color = TextSecondary,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = "TCP connect thất bại: ${content.tcpFailure}. Độ trễ chỉ tính các lượt thành công tới đích kiểm tra, không phải ping máy chủ game.",
            fontSize = 12.sp,
            color = TextSecondary,
            modifier = Modifier.padding(top = 8.dp)
        )

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
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
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
                        text = content.statusLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            ConnectionDetailRow(
                label = "Nhà mạng",
                value = content.isp
            )
            ConnectionDivider()
            ConnectionDetailRow(
                label = "Địa điểm",
                value = content.placeName
            )
            ConnectionDivider()
            ConnectionDetailRow(label = "BSSID", value = content.bssid)
            ConnectionDivider()
            ConnectionDetailRow(label = "Nhu cầu lúc đo", value = content.category?.title ?: MISSING_METRIC)
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
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )
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
            modifier = Modifier.weight(1f).padding(start = 12.dp),
            textAlign = TextAlign.End,
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
