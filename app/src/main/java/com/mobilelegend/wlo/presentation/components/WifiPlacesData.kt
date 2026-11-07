package com.mobilelegend.wlo.presentation.components

import com.mobilelegend.wlo.domain.model.UseCaseCategory
import com.mobilelegend.wlo.domain.model.LocationScore

enum class PlaceIconType {
    COFFEE,
    WORK
}

enum class WifiNeedOption(
    val label: String,
    val category: UseCaseCategory
) {
    WORK("Làm việc", UseCaseCategory.STUDY_DOWNLOAD),
    VIDEO_MEETING("Họp video", UseCaseCategory.VIDEO_CALL),
    GAMING("Chơi game", UseCaseCategory.GAMING),
    RELAX("Thư giãn", UseCaseCategory.STUDY_DOWNLOAD)
}

data class WifiPlaceItem(
    val id: String,
    val rank: Int,
    val name: String,
    val district: String,
    val distanceText: String,
    val score: Int,
    val qualityLabel: String,
    val linkSpeedMbps: Int?,
    val pingMs: Long?,
    val measurementsToday: Int,
    val isSaved: Boolean,
    val isFree: Boolean,
    val iconType: PlaceIconType,
    val latitude: Double,
    val longitude: Double,
    val mapX: Float = 0.5f,
    val mapY: Float = 0.5f,
    val ssid: String = "",
    val isp: String = "",
    val pingSuccessful: Boolean? = null,
    val isDemo: Boolean = false,
    val measurement: LocationScore? = null,
    val suitableNeeds: Set<WifiNeedOption> = setOf(
        WifiNeedOption.WORK,
        WifiNeedOption.VIDEO_MEETING,
        WifiNeedOption.GAMING,
        WifiNeedOption.RELAX
    )
)

val DefaultWifiPlaces = listOf(
    WifiPlaceItem(
        id = "workshop_coffee",
        rank = 1,
        name = "The Workshop Coffee",
        district = "Quận 1",
        distanceText = "450 m",
        score = 94,
        qualityLabel = "Rất tốt",
        linkSpeedMbps = null,
        isDemo = true,
        pingSuccessful = true,
        pingMs = 12,
        measurementsToday = 36,
        isSaved = true,
        isFree = false,
        iconType = PlaceIconType.COFFEE,
        latitude = 10.7794,
        longitude = 106.7010,
        mapX = 0.49f,
        mapY = 0.27f,
        ssid = "The Workshop Guest",
        isp = "FPT Telecom",
        suitableNeeds = setOf(
            WifiNeedOption.WORK,
            WifiNeedOption.VIDEO_MEETING,
            WifiNeedOption.GAMING,
            WifiNeedOption.RELAX
        )
    ),
    WifiPlaceItem(
        id = "general_library",
        rank = 2,
        name = "Thư viện Khoa học Tổng hợp",
        district = "Quận 1",
        distanceText = "1,2 km",
        score = 89,
        qualityLabel = "Rất tốt",
        linkSpeedMbps = null,
        isDemo = true,
        pingSuccessful = true,
        pingMs = 18,
        measurementsToday = 28,
        isSaved = true,
        isFree = true,
        iconType = PlaceIconType.WORK,
        latitude = 10.7768,
        longitude = 106.6985,
        mapX = 0.22f,
        mapY = 0.47f,
        ssid = "ThuVienKHTH_Free",
        isp = "VNPT",
        suitableNeeds = setOf(
            WifiNeedOption.WORK,
            WifiNeedOption.VIDEO_MEETING,
            WifiNeedOption.RELAX
        )
    ),
    WifiPlaceItem(
        id = "workflow_nguyen_hue",
        rank = 3,
        name = "WorkFlow Nguyễn Huệ",
        district = "Quận 1",
        distanceText = "1,8 km",
        score = 86,
        qualityLabel = "Rất tốt",
        linkSpeedMbps = null,
        isDemo = true,
        pingSuccessful = true,
        pingMs = 21,
        measurementsToday = 19,
        isSaved = false,
        isFree = false,
        iconType = PlaceIconType.COFFEE,
        latitude = 10.7752,
        longitude = 106.7042,
        mapX = 0.79f,
        mapY = 0.58f,
        ssid = "WorkFlow_5GHz",
        isp = "Viettel",
        suitableNeeds = setOf(
            WifiNeedOption.WORK,
            WifiNeedOption.VIDEO_MEETING,
            WifiNeedOption.GAMING
        )
    ),
    WifiPlaceItem(
        id = "cong_caphe_dakao",
        rank = 4,
        name = "Cộng Cà Phê Đa Kao",
        district = "Quận 1",
        distanceText = "2,1 km",
        score = 78,
        qualityLabel = "Trung bình",
        linkSpeedMbps = null,
        isDemo = true,
        pingSuccessful = true,
        pingMs = 29,
        measurementsToday = 14,
        isSaved = false,
        isFree = true,
        iconType = PlaceIconType.COFFEE,
        latitude = 10.7736,
        longitude = 106.7002,
        mapX = 0.37f,
        mapY = 0.67f,
        ssid = "CongCaPhe_Free",
        isp = "FPT Telecom",
        suitableNeeds = setOf(
            WifiNeedOption.RELAX,
            WifiNeedOption.WORK
        )
    )
)
