package com.mobilelegend.wlo.presentation.measure_and_result

import com.mobilelegend.wlo.presentation.components.linkSpeedText
import com.mobilelegend.wlo.presentation.components.pingText
import com.mobilelegend.wlo.presentation.components.tcpFailureText

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobilelegend.wlo.presentation.theme.CardBorder
import com.mobilelegend.wlo.presentation.theme.SignalGreen
import com.mobilelegend.wlo.presentation.theme.SignalRed
import com.mobilelegend.wlo.presentation.theme.SignalYellow
import com.mobilelegend.wlo.presentation.theme.SurfaceDarkElevated
import com.mobilelegend.wlo.presentation.theme.TextMuted
import com.mobilelegend.wlo.presentation.theme.TextPrimary
import com.mobilelegend.wlo.presentation.theme.TextSecondary

@Composable
fun SessionHistoryBottomSheetContent(
    spots: List<SavedSpotItem>,
    onRemoveSpot: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val groups = remember(spots) { historyComparison(spots) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Lịch sử theo nhu cầu",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Chỉ so sánh điểm đã lưu trong cùng nhu cầu; không xếp hạng giữa các nhóm.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (spots.isEmpty()) {
            Text(
                text = "Chưa có vị trí nào được lưu để so sánh.",
                fontSize = 13.sp,
                color = TextMuted,
                modifier = Modifier.padding(vertical = 24.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                groups.forEach { group ->
                    item(key = "category_${group.category.name}") {
                        Column(modifier = Modifier.padding(top = 8.dp)) {
                            Text(group.category.title, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(
                                "${group.rows.size} lượt đo · ${group.savedCount} đã lưu" +
                                    (if (group.savedCount < 2) " · Cần ít nhất 2 điểm đã lưu để so sánh" else ""),
                                color = TextSecondary, fontSize = 12.sp
                            )
                        }
                    }
                    items(group.rows, key = { "measurement_${it.spot.id}" }) { row ->
                        val spot = row.spot
                        val isBest = row.bestLabel != null
                        val details = historyMeasurementDetails(spot)
                        val scoreColor = when {
                            spot.score.finalScore >= 75 -> SignalGreen
                            spot.score.finalScore >= 60 -> SignalYellow
                            else -> SignalRed
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    if (isBest) SignalGreen else CardBorder,
                                    RoundedCornerShape(12.dp)
                                ),
                            colors = CardDefaults.cardColors(containerColor = SurfaceDarkElevated),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isBest) {
                                    Icon(
                                        imageVector = Icons.Default.EmojiEvents,
                                        contentDescription = null,
                                        tint = SignalGreen,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = spot.spotName,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    row.bestLabel?.let { label ->
                                        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SignalGreen)
                                    }
                                    Text(details.categoryTitle, fontSize = 12.sp, color = TextSecondary)
                                    Text("SSID: ${details.ssid}\nBSSID: ${details.bssid}", fontSize = 11.sp, color = TextSecondary)
                                    Text("Đo lúc: ${details.measuredAtText}", fontSize = 11.sp, color = TextSecondary)
                                    if (!spot.isSaved) Text("Chưa đánh dấu lưu", fontSize = 11.sp, color = TextMuted)
                                    Text(
                                        text = "TCP: ${pingText(spot.score.rawPingMs, spot.score.pingSuccessful)} | TCP thất bại: ${tcpFailureText(spot.score.tcpFailurePercent)} | Sóng: ${spot.score.rawRssi}dBm | Liên kết Wi-Fi: ${linkSpeedText(spot.score.rawLinkSpeedMbps)}",
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                }

                                Text(
                                    text = "${spot.score.finalScore}đ",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = scoreColor
                                )

                                IconButton(onClick = { onRemoveSpot(spot.id) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Xóa ${spot.spotName}",
                                        tint = TextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
