package com.mobilelegend.wlo.presentation.map

import com.mobilelegend.wlo.presentation.components.WifiNeedOption
import com.mobilelegend.wlo.presentation.components.WifiPlaceItem
import com.mobilelegend.wlo.presentation.components.visibleMapPlaces

enum class MapFilterChip(val label: String) {
    ALL("Tất cả"),
    EXCELLENT("Rất tốt"),
    WORK("Làm việc"),
    FREE("Miễn phí")
}

data class MapContent(
    val places: List<WifiPlaceItem>,
    val selectedPlace: WifiPlaceItem?,
    val emptyMessage: String?
)

/** Markers, details and the inspect action share exactly the same filtered snapshot. */
fun mapContent(
    places: List<WifiPlaceItem>,
    query: String,
    filter: MapFilterChip,
    showDemo: Boolean,
    selectedPlaceId: String?
): MapContent {
    val search = query.trim()
    val filtered = visibleMapPlaces(places, showDemo).filter { place ->
        val matchesQuery = search.isEmpty() || place.name.contains(search, ignoreCase = true) ||
            place.district.contains(search, ignoreCase = true)
        val matchesFilter = when (filter) {
            MapFilterChip.ALL -> true
            MapFilterChip.EXCELLENT -> place.score >= 90
            MapFilterChip.WORK -> WifiNeedOption.WORK in place.suitableNeeds
            MapFilterChip.FREE -> place.isFree
        }
        matchesQuery && matchesFilter
    }
    val selected = filtered.find { it.id == selectedPlaceId } ?: filtered.firstOrNull()
    val emptyMessage = when {
        filtered.isNotEmpty() -> null
        search.isNotEmpty() || filter != MapFilterChip.ALL ->
            "Không có địa điểm khớp tìm kiếm và bộ lọc. Hãy đổi từ khóa hoặc chọn Tất cả."
        !showDemo && visibleMapPlaces(places, true).any { it.isDemo } ->
            "Chưa có điểm đo thật trên bản đồ. Hãy đo Wi-Fi tại đây hoặc bật dữ liệu minh họa."
        else -> "Chưa có địa điểm có tọa độ hợp lệ. Hãy đo Wi-Fi tại vị trí của bạn."
    }
    return MapContent(filtered, selected, emptyMessage)
}
