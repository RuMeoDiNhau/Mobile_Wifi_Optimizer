package com.mobilelegend.wlo.presentation.measure_and_result

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SignalWifi4Bar
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mobilelegend.wlo.domain.model.LocationScore
import com.mobilelegend.wlo.presentation.components.ScoreCircleCard
import com.mobilelegend.wlo.presentation.theme.CardBorder
import com.mobilelegend.wlo.presentation.theme.DarkBackground
import com.mobilelegend.wlo.presentation.theme.PrimaryBlue
import com.mobilelegend.wlo.presentation.theme.PrimaryBlueVariant
import com.mobilelegend.wlo.presentation.theme.SecondaryTeal
import com.mobilelegend.wlo.presentation.theme.SignalGreen
import com.mobilelegend.wlo.presentation.theme.SignalRed
import com.mobilelegend.wlo.presentation.theme.SignalYellow
import com.mobilelegend.wlo.presentation.theme.SurfaceDark
import com.mobilelegend.wlo.presentation.theme.SurfaceDarkElevated
import com.mobilelegend.wlo.presentation.theme.TextMuted
import com.mobilelegend.wlo.presentation.theme.TextPrimary
import com.mobilelegend.wlo.presentation.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    score: LocationScore,
    viewModel: MeasureViewModel,
    onBackToRadar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showSaveDialog by remember { mutableStateOf(false) }
    var spotNameInput by remember { mutableStateOf("") }
    var showHistorySheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Kết Quả Đánh Giá",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )
                Text(
                    text = score.category.title,
                    fontSize = 13.sp,
                    color = PrimaryBlue
                )
            }

            if (uiState.savedSpots.isNotEmpty()) {
                OutlinedButton(
                    onClick = { showHistorySheet = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SecondaryTeal)
                ) {
                    Icon(imageVector = Icons.Default.CompareArrows, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "So sánh (${uiState.savedSpots.size})", fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 2. Điểm tổng hợp vòng tròn
        ScoreCircleCard(
            score = score.finalScore,
            isRecommended = score.isRecommended,
            size = 190.dp
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 3. Recommendation Card
        val badgeColor = if (score.isRecommended) SignalGreen else if (score.finalScore >= 60) SignalYellow else SignalRed
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, badgeColor.copy(alpha = 0.6f), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (score.isRecommended) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = score.summaryText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = score.recommendation,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = Color(0xFFE2E8F0)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 4. Breakdown Metrics Grid
        Text(
            text = "CHI TIẾT THÔNG SỐ ĐÃ CHUẨN HÓA",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextMuted,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(10.dp))

        MetricDetailRow(
            icon = Icons.Default.NetworkCheck,
            title = "Độ trễ Ping ICMP/Socket",
            rawValue = "${score.rawPingMs} ms",
            normalizedScore = score.normalizedPingScore,
            color = PrimaryBlue
        )

        Spacer(modifier = Modifier.height(8.dp))

        MetricDetailRow(
            icon = Icons.Default.SignalWifi4Bar,
            title = "Cường độ tín hiệu sóng",
            rawValue = "${score.rawRssi} dBm",
            normalizedScore = score.normalizedRssiScore,
            color = SecondaryTeal
        )

        Spacer(modifier = Modifier.height(8.dp))

        MetricDetailRow(
            icon = Icons.Default.Speed,
            title = "Tốc độ đường truyền LinkSpeed",
            rawValue = "${score.rawLinkSpeedMbps} Mbps",
            normalizedScore = score.normalizedSpeedScore,
            color = SignalYellow
        )

        Spacer(modifier = Modifier.height(28.dp))

        // 5. Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { showSaveDialog = true },
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
            ) {
                Icon(imageVector = Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Lưu vị trí này", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    viewModel.resetMeasureState()
                    onBackToRadar()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlueVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Về Radar dò tiếp", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }

    // Dialog lưu tên vị trí
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text(text = "Lưu điểm đo để so sánh", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(text = "Đặt tên cho vị trí bạn vừa đứng (VD: Bàn học, Ban công, Giường ngủ...):", fontSize = 13.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = spotNameInput,
                        onValueChange = { spotNameInput = it },
                        placeholder = { Text("VD: Bàn học gần cửa sổ") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveSpotToHistory(spotNameInput, score)
                        showSaveDialog = false
                        spotNameInput = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text(text = "Lưu lại", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text(text = "Hủy", color = TextMuted)
                }
            },
            containerColor = SurfaceDark,
            titleContentColor = TextPrimary
        )
    }

    // BottomSheet so sánh các điểm trong phiên
    if (showHistorySheet) {
        ModalBottomSheet(
            onDismissRequest = { showHistorySheet = false },
            sheetState = sheetState,
            containerColor = SurfaceDark
        ) {
            SessionHistoryBottomSheetContent(
                spots = uiState.savedSpots,
                onRemoveSpot = { viewModel.removeSpotFromHistory(it) },
                onClose = { showHistorySheet = false }
            )
        }
    }
}

@Composable
private fun MetricDetailRow(
    icon: ImageVector,
    title: String,
    rawValue: String,
    normalizedScore: Int,
    color: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 12.sp, color = TextMuted)
                Text(text = rawValue, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(SurfaceDarkElevated)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "$normalizedScore/100đ",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        }
    }
}
