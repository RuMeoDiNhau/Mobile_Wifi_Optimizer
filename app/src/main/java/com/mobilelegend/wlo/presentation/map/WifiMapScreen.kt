package com.mobilelegend.wlo.presentation.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.mobilelegend.wlo.data.sensor.UserGeoCoordinate
import com.mobilelegend.wlo.data.sensor.LocationRequestState
import com.mobilelegend.wlo.data.sensor.LocationRequestStatus
import com.mobilelegend.wlo.data.sensor.DEFAULT_MAP_LAT
import com.mobilelegend.wlo.data.sensor.DEFAULT_MAP_LON
import com.mobilelegend.wlo.presentation.components.visibleMapPlaces
import com.mobilelegend.wlo.presentation.components.AppTopHeader
import com.mobilelegend.wlo.presentation.components.WifiNeedOption
import com.mobilelegend.wlo.presentation.components.WifiPlaceItem
import com.mobilelegend.wlo.presentation.components.linkSpeedText
import com.mobilelegend.wlo.presentation.components.pingText
import com.mobilelegend.wlo.presentation.theme.AccentEmerald
import com.mobilelegend.wlo.presentation.theme.AppBackground
import com.mobilelegend.wlo.presentation.theme.CardBorder
import com.mobilelegend.wlo.presentation.theme.PrimaryGreen
import com.mobilelegend.wlo.presentation.theme.SignalYellow
import com.mobilelegend.wlo.presentation.theme.SurfaceWhite
import com.mobilelegend.wlo.presentation.theme.TextMuted
import com.mobilelegend.wlo.presentation.theme.TextPrimary
import com.mobilelegend.wlo.presentation.theme.TextSecondary
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

private enum class MapFilterChip(val label: String) {
    ALL("Tất cả"),
    EXCELLENT("Rất tốt"),
    WORK("Làm việc"),
    FREE("Miễn phí")
}

@Composable
fun WifiMapScreen(
    places: List<WifiPlaceItem>,
    userLocation: UserGeoCoordinate,
    locationRequest: LocationRequestState,
    onRefreshUserLocation: () -> Long?,
    onInspectPlace: (WifiPlaceItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(MapFilterChip.ALL) }
    var selectedPlaceId by remember { mutableStateOf(places.firstOrNull()?.id ?: "workshop_coffee") }
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var showDemo by remember { mutableStateOf(false) }
    var pendingLocationFocus by remember { mutableStateOf<Long?>(null) }

    // Khởi tạo cấu hình User-Agent cho OSMdroid
    LaunchedEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
        onRefreshUserLocation()
    }

    val filteredPlaces = remember(places, searchQuery, selectedFilter, showDemo) {
        visibleMapPlaces(places, showDemo).filter { place ->
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

    // Tự động bay camera khi đổi địa điểm được chọn
    LaunchedEffect(selectedPlace?.id) {
        selectedPlace?.let { mapViewRef?.controller?.animateTo(GeoPoint(it.latitude, it.longitude), 16.6, 550L) }
    }
    LaunchedEffect(pendingLocationFocus, locationRequest, userLocation, mapViewRef) {
        val mapView = mapViewRef
        val requestedId = pendingLocationFocus ?: return@LaunchedEffect
        if (locationRequest.id < requestedId) return@LaunchedEffect
        if (locationRequest.id == requestedId && locationRequest.status == LocationRequestStatus.REQUESTING) {
            return@LaunchedEffect
        }
        if (locationRequest.id == requestedId && locationRequest.status == LocationRequestStatus.UPDATED &&
            userLocation.isRealFix) {
            if (mapView == null) return@LaunchedEffect
            mapView.controller.animateTo(GeoPoint(userLocation.latitude, userLocation.longitude), 16.8, 650L)
        }
        pendingLocationFocus = null
    }

    DisposableEffect(Unit) {
        onDispose {
            mapViewRef?.onPause()
            mapViewRef?.onDetach()
        }
    }

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
        Text(userLocation.statusText, fontSize = 12.sp, color = TextSecondary)
        Text(locationRequest.status.message, fontSize = 12.sp, color = TextSecondary)
        Text(
            if (showDemo) "Đang xem dữ liệu minh họa · Ẩn" else "Hiện dữ liệu minh họa",
            modifier = Modifier.clickable { showDemo = !showDemo }.padding(vertical = 6.dp),
            color = PrimaryGreen, fontSize = 12.sp
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

        // 4. Khung Bản đồ thực tế OpenStreetMap (OSMdroid)
        Box(
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
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    Configuration.getInstance().userAgentValue = ctx.packageName
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                        minZoomLevel = 4.0
                        maxZoomLevel = 20.0

                        // Bộ lọc màu nhẹ giúp bản đồ đường phố thật hài hoà với giao diện Light Emerald
                        val softMintMatrix = ColorMatrix(
                            floatArrayOf(
                                0.90f, 0.04f, 0.02f, 0f, 10f,
                                0.03f, 0.94f, 0.03f, 0f, 14f,
                                0.02f, 0.04f, 0.90f, 0f, 11f,
                                0f, 0f, 0f, 1f, 0f
                            )
                        )
                        overlayManager.tilesOverlay.setColorFilter(ColorMatrixColorFilter(softMintMatrix))

                        controller.setZoom(16.5)
                        controller.setCenter(if (userLocation.isRealFix) GeoPoint(userLocation.latitude, userLocation.longitude)
                            else GeoPoint(DEFAULT_MAP_LAT, DEFAULT_MAP_LON))
                        onResume()
                        mapViewRef = this
                    }
                },
                update = { mapView ->
                    mapView.overlays.clear()

                    // 1. Marker chấm xanh định vị GPS thật của người dùng
                    if (userLocation.isRealFix) {
                        val userMarker = Marker(mapView).apply {
                            position = GeoPoint(userLocation.latitude, userLocation.longitude)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                            icon = createUserGpsDotDrawable(context)
                            title = userLocation.statusText
                            setOnMarkerClickListener { _, _ ->
                                mapView.controller.animateTo(position, 17.0, 500L)
                                true
                            }
                        }
                        mapView.overlays.add(userMarker)
                    }

                    // 2. Các ghim điểm số Wi-Fi (vẽ ghim đang chọn sau cùng để nổi lên trên cùng)
                    val sortedForRender = filteredPlaces.sortedBy { it.id == selectedPlace?.id }
                    sortedForRender.forEach { place ->
                        val isSelectedPin = place.id == selectedPlace?.id
                        val pinHexColor = when {
                            place.isDemo -> AndroidColor.GRAY
                            place.score >= 92 -> AndroidColor.parseColor("#0E6C50")
                            place.score >= 85 && place.score != 89 -> AndroidColor.parseColor("#499377")
                            else -> AndroidColor.parseColor("#DF9A3B")
                        }

                        val marker = Marker(mapView).apply {
                            position = GeoPoint(place.latitude, place.longitude)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            icon = createTeardropPinDrawable(
                                context = context,
                                score = place.score,
                                fillColor = pinHexColor,
                                isSelected = isSelectedPin
                            )
                            title = if (place.isDemo) "Minh họa: ${place.name}" else place.name
                            setOnMarkerClickListener { _, _ ->
                                selectedPlaceId = place.id
                                mapView.controller.animateTo(position, 16.8, 500L)
                                true
                            }
                        }
                        mapView.overlays.add(marker)
                    }

                    mapView.invalidate()
                }
            )

            // Legend góc trên bên trái ("• Tốt  • Trung bình")
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(14.dp),
                shape = RoundedCornerShape(50),
                color = SurfaceWhite,
                shadowElevation = 4.dp
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

            // Nút Định vị GPS thật góc trên bên phải
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(14.dp)
                    .size(42.dp)
                    .clickable {
                        pendingLocationFocus = onRefreshUserLocation()
                    },
                shape = RoundedCornerShape(14.dp),
                color = SurfaceWhite,
                shadowElevation = 4.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = "Định vị GPS của tôi",
                        tint = PrimaryGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Thẻ thông tin địa điểm nổi dưới đáy bản đồ
            if (selectedPlace == null) Text(
                "Chưa có điểm đo phù hợp. Đo Wi-Fi tại vị trí của bạn hoặc bật dữ liệu minh họa.",
                modifier = Modifier.align(Alignment.BottomCenter).background(SurfaceWhite).padding(16.dp),
                color = TextSecondary, fontSize = 13.sp
            )
            if (selectedPlace != null) Surface(
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
                                text = if (selectedPlace.isDemo) "DỮ LIỆU MINH HỌA" else "${selectedPlace.score} · ${selectedPlace.qualityLabel}",
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
                    Text(
                        if (selectedPlace.isDemo) "Không dùng để khuyến nghị kết nối thực tế" else selectedPlace.district,
                        fontSize = 12.sp, color = TextMuted
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Liên kết Wi-Fi: ${linkSpeedText(selectedPlace.linkSpeedMbps)} · ${pingText(selectedPlace.pingMs, selectedPlace.pingSuccessful)}",
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

/**
 * Tạo Drawable chấm tròn xanh định vị GPS của người dùng có vòng hào quang mờ.
 */
private fun createUserGpsDotDrawable(context: Context): BitmapDrawable {
    val density = context.resources.displayMetrics.density
    val sizePx = (44f * density).toInt().coerceAtLeast(44)
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    val cx = sizePx / 2f
    val cy = sizePx / 2f

    val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(46, 59, 130, 246)
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, sizePx * 0.48f, haloPaint)

    val whiteRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, sizePx * 0.23f, whiteRingPaint)

    val blueDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(37, 99, 235)
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, sizePx * 0.15f, blueDotPaint)

    return BitmapDrawable(context.resources, bitmap)
}

/**
 * Tạo Drawable ghim giọt nước (Teardrop Marker) hiển thị điểm số Wi-Fi sắc nét trên MapView.
 */
private fun createTeardropPinDrawable(
    context: Context,
    score: Int,
    fillColor: Int,
    isSelected: Boolean
): BitmapDrawable {
    val density = context.resources.displayMetrics.density
    val dpSize = if (isSelected) 60f else 48f
    val sizePx = (dpSize * density).toInt().coerceAtLeast(48)
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)

    val w = sizePx.toFloat()
    val h = sizePx.toFloat()
    val cx = w / 2f
    val cy = h * 0.43f
    val r = w * 0.40f

    val path = AndroidPath().apply {
        moveTo(cx, h * 0.93f)
        cubicTo(
            cx - r * 0.55f, h * 0.77f,
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
            cx + r * 0.55f, h * 0.77f,
            cx, h * 0.93f
        )
        close()
    }

    // Đổ bóng nhẹ dưới ghim
    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(36, 0, 0, 0)
        style = Paint.Style.FILL
    }
    canvas.save()
    canvas.translate(0f, 2f * density)
    canvas.drawPath(path, shadowPaint)
    canvas.restore()

    // Tô nền màu ghim
    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = fillColor
        style = Paint.Style.FILL
    }
    canvas.drawPath(path, fillPaint)

    // Viền trắng bao ngoài
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        style = Paint.Style.STROKE
        strokeWidth = (if (isSelected) 3.2f else 2.5f) * density
    }
    canvas.drawPath(path, borderPaint)

    // Vẽ biểu tượng sóng Wi-Fi nhỏ phía trên điểm số nếu ghim đang được chọn
    if (isSelected) {
        val wifiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 1.6f * density
            strokeCap = Paint.Cap.ROUND
        }
        val wifiCenterY = cy - r * 0.24f
        val arc1 = RectF(cx - 6f * density, wifiCenterY - 5f * density, cx + 6f * density, wifiCenterY + 7f * density)
        val arc2 = RectF(cx - 3.5f * density, wifiCenterY - 2.5f * density, cx + 3.5f * density, wifiCenterY + 4.5f * density)
        canvas.drawArc(arc1, 225f, 90f, false, wifiPaint)
        canvas.drawArc(arc2, 225f, 90f, false, wifiPaint)

        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, wifiCenterY + 2.2f * density, 1.2f * density, dotPaint)
    }

    // Vẽ điểm số ở giữa ghim
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        textSize = (if (isSelected) 13.5f else 12f) * density
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    val textOffsetY = if (isSelected) cy + r * 0.36f else cy + r * 0.22f
    canvas.drawText(score.toString(), cx, textOffsetY, textPaint)

    return BitmapDrawable(context.resources, bitmap)
}
