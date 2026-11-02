package com.mobilelegend.wlo.presentation.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobilelegend.wlo.presentation.components.AppTopHeader
import com.mobilelegend.wlo.presentation.components.WifiNeedOption
import com.mobilelegend.wlo.presentation.components.WifiPlaceItem
import com.mobilelegend.wlo.presentation.theme.AccentEmerald
import com.mobilelegend.wlo.presentation.theme.AppBackground
import com.mobilelegend.wlo.presentation.theme.CardBorder
import com.mobilelegend.wlo.presentation.theme.PrimaryGreen
import com.mobilelegend.wlo.presentation.theme.SignalYellow
import com.mobilelegend.wlo.presentation.theme.SurfaceWhite
import com.mobilelegend.wlo.presentation.theme.TextMuted
import com.mobilelegend.wlo.presentation.theme.TextPrimary
import com.mobilelegend.wlo.presentation.theme.TextSecondary
import kotlin.math.roundToInt

private enum class MapFilterChip(val label: String) {
    ALL("Tất cả"),
    EXCELLENT("Rất tốt"),
    WORK("Làm việc"),
    FREE("Miễn phí")
}

@Composable
fun WifiMapScreen(
    places: List<WifiPlaceItem>,
    onInspectPlace: (WifiPlaceItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(MapFilterChip.ALL) }
    var selectedPlaceId by remember { mutableStateOf(places.firstOrNull()?.id ?: "workshop_coffee") }

    val filteredPlaces = remember(places, searchQuery, selectedFilter) {
        places.filter { place ->
            val matchesQuery = searchQuery.isBlank() ||
                place.name.contains(searchQuery, ignoreCase = true) ||
                place.district.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (selectedFilter) {
                MapFilterChip.ALL -> true
                MapFilterChip.EXCELLENT -> place.score >= 90
                MapFilterChip.WORK -> place.suitableNeeds.contains(WifiNeedOption.WORK)
                MapFilterChip.FREE -> place.isFree
            }
            matchesQuery && matchesFilter
        }
    }

    val selectedPlace = filteredPlaces.find { it.id == selectedPlaceId }
        ?: filteredPlaces.firstOrNull()
        ?: places.first()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(horizontal = 20.dp)
            .padding(top = 18.dp, bottom = 14.dp)
    ) {
        // 1. Header
        AppTopHeader(
            overline = "KHÁM PHÁ XUNG QUANH",
            title = "Bản đồ Wi-Fi"
        )

        Spacer(modifier = Modifier.height(16.dp))

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
                .padding(horizontal = 16.dp, vertical = 14.dp),
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
                        text = "Tìm trong khu vực này",
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
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3. Filter Chips Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MapFilterChip.values().forEach { chip ->
                val isSelected = chip == selectedFilter
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (isSelected) PrimaryGreen else SurfaceWhite)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) PrimaryGreen else CardBorder,
                            shape = RoundedCornerShape(50)
                        )
                        .clickable { selectedFilter = chip }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = chip.label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 4. Interactive Map Canvas Container
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(26.dp),
                    ambientColor = Color.Black.copy(alpha = 0.05f),
                    spotColor = Color.Black.copy(alpha = 0.08f)
                )
                .clip(RoundedCornerShape(26.dp))
                .background(Color(0xFFEAF1ED))
                .border(2.dp, SurfaceWhite, RoundedCornerShape(26.dp))
        ) {
            val mapWidthPx = constraints.maxWidth.toFloat()
            val mapHeightPx = constraints.maxHeight.toFloat()

            // Nền bản đồ đường phố & sông Sài Gòn
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Khối khu dân cư chéo mờ
                val blockPath = Path().apply {
                    moveTo(0f, h * 0.30f)
                    lineTo(w * 0.85f, h * 0.68f)
                    lineTo(w * 0.85f, h * 0.82f)
                    lineTo(0f, h * 0.44f)
                    close()
                }
                drawPath(blockPath, color = Color(0xFFE1EAE5))

                // Mảng sông Sài Gòn bên phải
                val riverPath = Path().apply {
                    moveTo(w * 0.98f, h * 0.06f)
                    lineTo(w, h * 0.06f)
                    lineTo(w, h)
                    lineTo(w * 0.72f, h)
                    close()
                }
                drawPath(riverPath, color = Color(0xFFCDE3DC))

                // Đường bờ sông màu trắng
                drawLine(
                    color = Color.White,
                    start = Offset(w * 0.98f, h * 0.04f),
                    end = Offset(w * 0.72f, h),
                    strokeWidth = 6.dp.toPx()
                )

                // Các trục đường lớn màu trắng
                drawLine(
                    color = Color.White,
                    start = Offset(w * 0.41f, 0f),
                    end = Offset(w * 0.60f, h),
                    strokeWidth = 12.dp.toPx()
                )
                drawLine(
                    color = Color.White,
                    start = Offset(0f, h * 0.28f),
                    end = Offset(w * 0.93f, h * 0.14f),
                    strokeWidth = 10.dp.toPx()
                )
                drawLine(
                    color = Color.White,
                    start = Offset(0f, h * 0.44f),
                    end = Offset(w * 0.85f, h * 0.69f),
                    strokeWidth = 10.dp.toPx()
                )
                drawLine(
                    color = Color.White,
                    start = Offset(w * 0.16f, h * 0.76f),
                    end = Offset(w * 0.78f, h * 0.76f),
                    strokeWidth = 8.dp.toPx()
                )
            }

            // Nhãn khu vực BẾN NGHÉ & ĐA KAO
            Text(
                text = "BẾN NGHÉ",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                color = Color(0xFF8D9C95),
                modifier = Modifier
                    .offset(
                        x = (maxWidth * 0.16f),
                        y = (maxHeight * 0.25f)
                    )
            )

            Text(
                text = "ĐA KAO",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                color = Color(0xFF8D9C95),
                modifier = Modifier
                    .offset(
                        x = (maxWidth * 0.71f),
                        y = (maxHeight * 0.46f)
                    )
            )

            // Chấm xanh vị trí hiện tại của người dùng
            Box(
                modifier = Modifier
                    .offset(
                        x = (maxWidth * 0.56f) - 22.dp,
                        y = (maxHeight * 0.49f) - 22.dp
                    )
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF3B82F6).copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2563EB))
                    )
                }
            }

            // Các ghim điểm số Wi-Fi trên bản đồ
            filteredPlaces.forEach { place ->
                val isSelectedPin = place.id == selectedPlace.id
                val pinSizeDp = if (isSelectedPin) 58.dp else 46.dp
                val pinColor = when {
                    place.score >= 92 -> PrimaryGreen
                    place.score == 86 -> Color(0xFF499377)
                    else -> SignalYellow
                }

                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = (mapWidthPx * place.mapX - (pinSizeDp.toPx() / 2f)).roundToInt(),
                                y = (mapHeightPx * place.mapY - (pinSizeDp.toPx() / 2f)).roundToInt()
                            )
                        }
                        .size(pinSizeDp)
                        .clickable { selectedPlaceId = place.id },
                    contentAlignment = Alignment.Center
                ) {
                    MapTeardropPin(
                        score = place.score,
                        color = pinColor,
                        isSelected = isSelectedPin,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Legend góc trên bên trái ("• Tốt  • Trung bình")
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(14.dp),
                shape = RoundedCornerShape(50),
                color = SurfaceWhite,
                shadowElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(PrimaryGreen)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Tốt",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(SignalYellow)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Trung bình",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Nút định vị góc trên bên phải
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(14.dp)
                    .size(42.dp)
                    .clickable {
                        selectedPlaceId = places.first().id
                    },
                shape = RoundedCornerShape(14.dp),
                color = SurfaceWhite,
                shadowElevation = 4.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = "Định vị",
                        tint = PrimaryGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Thẻ thông tin địa điểm nổi dưới đáy bản đồ
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(14.dp),
                shape = RoundedCornerShape(22.dp),
                color = SurfaceWhite,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(AccentEmerald)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${selectedPlace.score} · ${selectedPlace.qualityLabel}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryGreen
                            )
                        }

                        Text(
                            text = selectedPlace.distanceText,
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = selectedPlace.name,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${selectedPlace.downloadMbps} Mbps · ${selectedPlace.pingMs} ms · ${selectedPlace.measurementsToday} lượt đo hôm nay",
                        fontSize = 12.sp,
                        color = TextMuted
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { onInspectPlace(selectedPlace) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryGreen,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = "Xem chi tiết",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MapTeardropPin(
    score: Int,
    color: Color,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h * 0.44f
            val r = w * 0.42f

            val pinPath = Path().apply {
                moveTo(cx, h * 0.95f)
                cubicTo(
                    cx - r * 0.55f, h * 0.78f,
                    cx - r, cy + r * 0.35f,
                    cx - r, cy
                )
                cubicTo(
                    cx - r, cy - r * 0.56f,
                    cx - r * 0.56f, cy - r,
                    cx, cy - r
                )
                cubicTo(
                    cx + r * 0.56f, cy - r,
                    cx + r, cy - r * 0.56f,
                    cx + r, cy
                )
                cubicTo(
                    cx + r, cy + r * 0.35f,
                    cx + r * 0.55f, h * 0.78f,
                    cx, h * 0.95f
                )
                close()
            }

            // Viền trắng bên ngoài
            drawPath(
                path = pinPath,
                color = Color.White,
                style = Stroke(width = if (isSelected) 4.dp.toPx() else 3.dp.toPx())
            )
            // Phần tô màu bên trong
            drawPath(
                path = pinPath,
                color = color,
                style = Fill
            )
        }

        Column(
            modifier = Modifier.padding(bottom = if (isSelected) 6.dp else 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Wifi,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(11.dp)
                )
            }
            Text(
                text = "$score",
                fontSize = if (isSelected) 13.sp else 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
        }
    }
}
