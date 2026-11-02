package com.mobilelegend.wlo.presentation.saved

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobilelegend.wlo.presentation.components.AppTopHeader
import com.mobilelegend.wlo.presentation.components.PlaceIconType
import com.mobilelegend.wlo.presentation.components.PulseWaveIcon
import com.mobilelegend.wlo.presentation.components.WifiPlaceItem
import com.mobilelegend.wlo.presentation.measure_and_result.SavedSpotItem
import com.mobilelegend.wlo.presentation.measure_and_result.SessionHistoryBottomSheetContent
import com.mobilelegend.wlo.presentation.theme.AppBackground
import com.mobilelegend.wlo.presentation.theme.CardBorder
import com.mobilelegend.wlo.presentation.theme.MintBannerSurface
import com.mobilelegend.wlo.presentation.theme.PrimaryGreen
import com.mobilelegend.wlo.presentation.theme.SurfaceWhite
import com.mobilelegend.wlo.presentation.theme.TextMuted
import com.mobilelegend.wlo.presentation.theme.TextPrimary
import com.mobilelegend.wlo.presentation.theme.TextSecondary
import com.mobilelegend.wlo.presentation.theme.ThumbnailBlue
import com.mobilelegend.wlo.presentation.theme.ThumbnailMint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedPlacesScreen(
    places: List<WifiPlaceItem>,
    sessionSpots: List<SavedSpotItem>,
    onToggleSavePlace: (String) -> Unit,
    onRemoveSessionSpot: (String) -> Unit,
    onOpenMap: () -> Unit,
    onPlaceClick: (WifiPlaceItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val savedPlaces = remember(places) {
        val bookmarked = places.filter { it.isSaved }
        bookmarked.ifEmpty { places.take(2) }
    }

    val totalMeasurementsCount = 12 + sessionSpots.size
    var showHistorySheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

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
            overline = "BỘ SƯU TẬP CỦA BẠN",
            title = "Địa điểm đã lưu"
        )

        Spacer(modifier = Modifier.height(18.dp))

        // 2. Hàng 3 Thẻ Thống Kê (2 địa điểm, 12 lượt đo, 3 khu vực)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CollectionStatCard(
                value = "${savedPlaces.size}",
                label = "địa điểm",
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Filled.Bookmark,
                    contentDescription = null,
                    tint = PrimaryGreen,
                    modifier = Modifier.size(22.dp)
                )
            }

            CollectionStatCard(
                value = "$totalMeasurementsCount",
                label = "lượt đo",
                modifier = Modifier.weight(1f)
            ) {
                PulseWaveIcon(
                    color = PrimaryGreen,
                    modifier = Modifier.size(22.dp),
                    strokeWidthDp = 2.dp
                )
            }

            CollectionStatCard(
                value = "3",
                label = "khu vực",
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Map,
                    contentDescription = null,
                    tint = PrimaryGreen,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(26.dp))

        // 3. Phần "Gần đây" + "Xem bản đồ"
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Gần đây",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onOpenMap() }
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Map,
                    contentDescription = null,
                    tint = PrimaryGreen,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "Xem bản đồ",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryGreen
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            savedPlaces.forEachIndexed { index, place ->
                SavedPlaceRowCard(
                    place = place,
                    colorIndex = index,
                    onToggleSave = { onToggleSavePlace(place.id) },
                    onClick = { onPlaceClick(place) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Banner "Lịch sử đo mạng"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(MintBannerSurface)
                .clickable { showHistorySheet = true }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceWhite),
                contentAlignment = Alignment.Center
            ) {
                PulseWaveIcon(
                    color = PrimaryGreen,
                    modifier = Modifier.size(20.dp),
                    strokeWidthDp = 2.dp
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Lịch sử đo mạng",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Xem lại $totalMeasurementsCount kết quả đo gần đây",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = PrimaryGreen,
                modifier = Modifier.size(18.dp)
            )
        }
    }

    if (showHistorySheet) {
        ModalBottomSheet(
            onDismissRequest = { showHistorySheet = false },
            sheetState = sheetState,
            containerColor = SurfaceWhite
        ) {
            SessionHistoryBottomSheetContent(
                spots = sessionSpots,
                onRemoveSpot = onRemoveSessionSpot,
                onClose = { showHistorySheet = false }
            )
        }
    }
}

@Composable
private fun CollectionStatCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    iconContent: @Composable () -> Unit
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
        iconContent()
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = TextMuted
        )
    }
}

@Composable
private fun SavedPlaceRowCard(
    place: WifiPlaceItem,
    colorIndex: Int,
    onToggleSave: () -> Unit,
    onClick: () -> Unit
) {
    val thumbBg = if (colorIndex % 2 == 0) ThumbnailMint else ThumbnailBlue
    val icon = when (place.iconType) {
        PlaceIconType.COFFEE -> Icons.Outlined.Coffee
        PlaceIconType.WORK -> Icons.Outlined.WorkOutline
    }

    Row(
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
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail có huy hiệu điểm góc dưới phải
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(thumbBg)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF3E6B5C),
                modifier = Modifier
                    .size(26.dp)
                    .align(Alignment.Center)
            )

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(5.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(PrimaryGreen)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${place.score}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = place.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "${place.district} · ${place.distanceText}",
                    fontSize = 12.sp,
                    color = TextMuted
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.FileDownload,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${place.downloadMbps} Mbps",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.AccessTime,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${place.pingMs} ms",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        IconButton(
            onClick = onToggleSave,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Bookmark,
                contentDescription = "Đã lưu",
                tint = PrimaryGreen,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
