package com.mobilelegend.wlo.presentation.measure_and_result

import com.mobilelegend.wlo.presentation.components.linkSpeedText
import com.mobilelegend.wlo.presentation.components.pingText

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobilelegend.wlo.presentation.theme.CardBorder
import com.mobilelegend.wlo.presentation.theme.PrimaryBlue
import com.mobilelegend.wlo.presentation.theme.SignalGreen
import com.mobilelegend.wlo.presentation.theme.SignalRed
import com.mobilelegend.wlo.presentation.theme.SignalYellow
import com.mobilelegend.wlo.presentation.theme.SurfaceDark
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
    val highestScore = spots.maxOfOrNull { it.score.finalScore } ?: 0

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
                    text = "Bảng So Sánh Vị Trí",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Danh sách điểm đã đo trong lần mở app này",
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
                items(spots.sortedByDescending { it.score.finalScore }, key = { it.id }) { spot ->
                    val isBest = spot.score.finalScore == highestScore && spots.size > 1
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
                                    contentDescription = "Tốt nhất",
                                    tint = SignalGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = spot.spotName,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    if (isBest) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(SignalGreen.copy(alpha = 0.2f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(text = "TỐT NHẤT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SignalGreen)
                                        }
                                    }
                                }
                                Text(
                                    text = "TCP: ${pingText(spot.score.rawPingMs, spot.score.pingSuccessful)} | Sóng: ${spot.score.rawRssi}dBm | Liên kết Wi-Fi: ${linkSpeedText(spot.score.rawLinkSpeedMbps)}",
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
                                    contentDescription = "Xóa",
                                    tint = TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
