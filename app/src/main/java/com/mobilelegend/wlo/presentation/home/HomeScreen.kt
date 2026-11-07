package com.mobilelegend.wlo.presentation.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobilelegend.wlo.presentation.components.AppTopHeader
import com.mobilelegend.wlo.presentation.components.PlaceIconType
import com.mobilelegend.wlo.presentation.components.PulseWaveIcon
import com.mobilelegend.wlo.presentation.components.WifiNeedOption
import com.mobilelegend.wlo.presentation.components.WifiPlaceItem
import com.mobilelegend.wlo.presentation.components.linkSpeedText
import com.mobilelegend.wlo.presentation.components.pingText
import com.mobilelegend.wlo.presentation.theme.AccentEmerald
import com.mobilelegend.wlo.presentation.theme.AppBackground
import com.mobilelegend.wlo.presentation.theme.CardBorder
import com.mobilelegend.wlo.presentation.theme.DarkFabBackground
import com.mobilelegend.wlo.presentation.theme.LightGreenSurface
import com.mobilelegend.wlo.presentation.theme.PrimaryGreen
import com.mobilelegend.wlo.presentation.theme.PrimaryGreenDark
import com.mobilelegend.wlo.presentation.theme.SelectedNeedBg
import com.mobilelegend.wlo.presentation.theme.SelectedNeedBorder
import com.mobilelegend.wlo.presentation.theme.SurfaceWhite
import com.mobilelegend.wlo.presentation.theme.TextMuted
import com.mobilelegend.wlo.presentation.theme.TextPrimary
import com.mobilelegend.wlo.presentation.theme.TextSecondary
import com.mobilelegend.wlo.presentation.theme.ThumbnailBlue
import com.mobilelegend.wlo.presentation.theme.ThumbnailLavender
import com.mobilelegend.wlo.presentation.theme.ThumbnailMint

@Composable
fun HomeScreen(
    places: List<WifiPlaceItem>,
    selectedNeed: WifiNeedOption,
    areaScore: Int,
    onSelectNeed: (WifiNeedOption) -> Unit,
    onToggleSavePlace: (String) -> Unit,
    onOpenMap: () -> Unit,
    onStartMeasureHere: () -> Unit,
    onPlaceClick: (WifiPlaceItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var showAllPlaces by remember { mutableStateOf(false) }

    val filteredPlaces = remember(places, searchQuery, showAllPlaces) {
        val list = if (searchQuery.isBlank()) {
            places
        } else {
            places.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                    it.district.contains(searchQuery, ignoreCase = true)
            }
        }
        if (showAllPlaces) list else list.take(3)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 18.dp, bottom = 96.dp)
        ) {
            // 1. Header
            AppTopHeader(
                overline = "CHÀO BUỔI SÁNG",
                title = "Tìm Wi-Fi tốt gần bạn"
            )

            Spacer(modifier = Modifier.height(18.dp))

            // 2. Search Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 4.dp,
                        shape = RoundedCornerShape(20.dp),
                        ambientColor = Color.Black.copy(alpha = 0.04f),
                        spotColor = Color.Black.copy(alpha = 0.06f)
                    )
                    .clip(RoundedCornerShape(20.dp))
                    .background(SurfaceWhite)
                    .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
                    .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "Tìm kiếm",
                    tint = TextMuted,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "Tìm quán, thư viện, khu vực...",
                            fontSize = 14.sp,
                            color = TextMuted
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = 14.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(LightGreenSurface)
                        .clickable { onOpenMap() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = "Vị trí",
                        tint = PrimaryGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 3. Hero Card
            HeroQualityCard(
                score = areaScore,
                onOpenMap = onOpenMap
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 4. Nhu cầu sử dụng Wi-Fi
            Text(
                text = "Bạn cần Wi-Fi để làm gì?",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Gợi ý sẽ phù hợp hơn với nhu cầu của bạn",
                fontSize = 13.sp,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                WifiNeedOption.values().forEach { option ->
                    val isSelected = option == selectedNeed
                    NeedCategoryCard(
                        option = option,
                        isSelected = isSelected,
                        onClick = { onSelectNeed(option) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            // 5. Phù hợp nhất gần bạn
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Phù hợp nhất gần bạn",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Xếp hạng theo dữ liệu thực tế",
                        fontSize = 13.sp,
                        color = TextMuted
                    )
                }

                Text(
                    text = if (showAllPlaces) "Thu gọn" else "Xem tất cả",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryGreen,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showAllPlaces = !showAllPlaces }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                filteredPlaces.forEachIndexed { index, place ->
                    RankedPlaceCard(
                        place = place,
                        colorIndex = index,
                        onToggleSave = { onToggleSavePlace(place.id) },
                        onClick = { onPlaceClick(place) }
                    )
                }
            }
        }

        // 6. Nút nổi "Đo Wi-Fi tại đây" (Floating Action Pill)
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
                .clickable { onStartMeasureHere() },
            shape = RoundedCornerShape(50),
            color = DarkFabBackground,
            shadowElevation = 10.dp
        ) {
            Row(
                modifier = Modifier.padding(start = 8.dp, end = 18.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(PrimaryGreen),
                    contentAlignment = Alignment.Center
                ) {
                    PulseWaveIcon(
                        color = Color.White,
                        modifier = Modifier.size(18.dp),
                        strokeWidthDp = 2.dp
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Đo Wi-Fi tại đây",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun HeroQualityCard(
    score: Int,
    onOpenMap: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(26.dp),
                ambientColor = PrimaryGreen.copy(alpha = 0.25f),
                spotColor = PrimaryGreen.copy(alpha = 0.25f)
            )
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        PrimaryGreen,
                        PrimaryGreenDark
                    )
                )
            )
    ) {
        // Hoạ tiết đường bản đồ / radar chìm phía sau
        Canvas(modifier = Modifier.matchParentSize()) {
            val strokeColor = Color.White.copy(alpha = 0.08f)
            drawCircle(
                color = strokeColor,
                radius = size.height * 0.62f,
                center = Offset(size.width * 0.80f, size.height * 0.45f),
                style = Stroke(width = 1.2.dp.toPx())
            )
            drawCircle(
                color = strokeColor,
                radius = size.height * 0.40f,
                center = Offset(size.width * 0.80f, size.height * 0.45f),
                style = Stroke(width = 1.dp.toPx())
            )
            drawLine(
                color = strokeColor,
                start = Offset(size.width * 0.52f, 0f),
                end = Offset(size.width * 0.76f, size.height),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = strokeColor,
                start = Offset(0f, size.height * 0.72f),
                end = Offset(size.width, size.height * 0.58f),
                strokeWidth = 1.dp.toPx()
            )
            drawRoundRect(
                color = strokeColor,
                topLeft = Offset(size.width * 0.30f, size.height * 0.08f),
                size = Size(size.width * 0.22f, size.height * 0.16f),
                cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx()),
                style = Stroke(width = 1.dp.toPx())
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Pill "8 điểm tốt quanh bạn"
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.14f))
                    .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(AccentEmerald)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "8 điểm tốt quanh bạn",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Khu vực này đang có\nkết nối rất tốt",
                        fontSize = 21.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        lineHeight = 27.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Dữ liệu từ 124 lượt đo trong 24 giờ qua",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.72f)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Vòng tròn điểm 92/100
                Box(
                    modifier = Modifier
                        .size(86.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.13f))
                        .border(1.5.dp, Color.White.copy(alpha = 0.28f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(16.dp)
                        )
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "$score",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = "/100",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.75f),
                                modifier = Modifier.padding(bottom = 4.dp, start = 1.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Nút "Xem trên bản đồ ->"
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = onOpenMap,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SurfaceWhite,
                        contentColor = PrimaryGreen
                    ),
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "Xem trên bản đồ",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryGreen
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = PrimaryGreen,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun NeedCategoryCard(
    option: WifiNeedOption,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon: ImageVector = when (option) {
        WifiNeedOption.WORK -> Icons.Outlined.WorkOutline
        WifiNeedOption.VIDEO_MEETING -> Icons.Outlined.Videocam
        WifiNeedOption.GAMING -> Icons.Outlined.SportsEsports
        WifiNeedOption.RELAX -> Icons.Outlined.Coffee
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (isSelected) SelectedNeedBg else SurfaceWhite)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) SelectedNeedBorder else CardBorder,
                shape = RoundedCornerShape(18.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 14.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(if (isSelected) PrimaryGreen else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = option.label,
                tint = if (isSelected) Color.White else TextSecondary,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = option.label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) PrimaryGreen else TextSecondary
        )
    }
}

@Composable
private fun RankedPlaceCard(
    place: WifiPlaceItem,
    colorIndex: Int,
    onToggleSave: () -> Unit,
    onClick: () -> Unit
) {
    val thumbBg = when (colorIndex % 3) {
        0 -> ThumbnailMint
        1 -> ThumbnailBlue
        else -> ThumbnailLavender
    }

    val placeIcon = when (place.iconType) {
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
        // Thumbnail bên trái có badge #1, #2, #3
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(thumbBg)
        ) {
            Box(
                modifier = Modifier
                    .padding(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background( Color(0xFF285F4E))
                    .padding(horizontal = 7.dp, vertical = 2.dp)
                    .align(Alignment.TopStart)
            ) {
                Text(
                    text = "#${place.rank}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Icon(
                imageVector = placeIcon,
                contentDescription = null,
                tint = Color(0xFF5A6D66),
                modifier = Modifier
                    .size(26.dp)
                    .align(Alignment.Center)
                    .padding(top = 6.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Thông tin ở giữa
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
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(AccentEmerald)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${place.score} ${place.qualityLabel}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryGreen
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Wifi,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "Liên kết Wi-Fi: ${linkSpeedText(place.linkSpeedMbps)}",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.AccessTime,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = pingText(place.pingMs, place.pingSuccessful),
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        // Nút Bookmark
        IconButton(
            onClick = onToggleSave,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = if (place.isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                contentDescription = "Lưu địa điểm",
                tint = if (place.isSaved) PrimaryGreen else TextMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
