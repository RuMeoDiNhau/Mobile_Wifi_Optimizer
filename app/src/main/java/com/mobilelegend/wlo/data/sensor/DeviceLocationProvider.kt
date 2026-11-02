package com.mobilelegend.wlo.data.sensor

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import com.mobilelegend.wlo.core.permissions.PermissionHelper
import com.mobilelegend.wlo.presentation.components.WifiPlaceItem
import kotlin.math.roundToInt

data class UserGeoCoordinate(
    val latitude: Double,
    val longitude: Double,
    val isRealFix: Boolean
)

/**
 * Cung cấp toạ độ GPS/Network thực tế của thiết bị Android.
 */
class DeviceLocationProvider(
    private val context: Context,
    private val permissionHelper: PermissionHelper
) {
    companion object {
        // Toạ độ trung tâm Quận 1 (Bến Nghé / Đa Kao, TP.HCM) làm mặc định khi máy ảo chưa có GPS tại VN
        const val DEFAULT_LAT = 10.7772
        const val DEFAULT_LON = 106.7018

        fun formatDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): String {
            val results = FloatArray(1)
            Location.distanceBetween(lat1, lon1, lat2, lon2, results)
            val meters = results[0].roundToInt().coerceAtLeast(10)
            return if (meters < 1000) {
                "$meters m"
            } else {
                val km = meters / 1000.0
                String.format(java.util.Locale.US, "%.1f km", km).replace('.', ',')
            }
        }

        fun distanceInMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
            val results = FloatArray(1)
            Location.distanceBetween(lat1, lon1, lat2, lon2, results)
            return results[0]
        }
    }

    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    /**
     * Lấy vị trí hiện tại của thiết bị.
     * Nếu máy ảo đang trả về toạ độ mặc định ở Mỹ (Mountain View 37.42, -122.08) hoặc chưa có fix,
     * ưu tiên dùng toạ độ trung tâm Quận 1 để hiển thị bản đồ Việt Nam chuẩn xác.
     */
    @SuppressLint("MissingPermission")
    fun getCurrentLocation(): UserGeoCoordinate {
        if (!permissionHelper.hasLocationPermission() || locationManager == null) {
            return UserGeoCoordinate(DEFAULT_LAT, DEFAULT_LON, isRealFix = false)
        }

        return try {
            val providers = listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER
            )

            var bestLocation: Location? = null
            for (provider in providers) {
                val loc = runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
                if (loc != null) {
                    if (bestLocation == null || loc.accuracy < bestLocation.accuracy) {
                        bestLocation = loc
                    }
                }
            }

            if (bestLocation != null) {
                val lat = bestLocation.latitude
                val lon = bestLocation.longitude
                // Kiểm tra xem có phải toạ độ mặc định của máy ảo Android (Googleplex Mountain View) không
                val isEmulatorDefault = lat in 37.35..37.48 && lon in -122.15..-122.00
                if (!isEmulatorDefault) {
                    UserGeoCoordinate(lat, lon, isRealFix = true)
                } else {
                    UserGeoCoordinate(DEFAULT_LAT, DEFAULT_LON, isRealFix = false)
                }
            } else {
                UserGeoCoordinate(DEFAULT_LAT, DEFAULT_LON, isRealFix = false)
            }
        } catch (e: Exception) {
            UserGeoCoordinate(DEFAULT_LAT, DEFAULT_LON, isRealFix = false)
        }
    }

    /**
     * Yêu cầu cập nhật 1 lần vị trí GPS mới nhất từ phần cứng.
     */
    @SuppressLint("MissingPermission")
    fun requestSingleUpdate(onUpdated: (UserGeoCoordinate) -> Unit) {
        if (!permissionHelper.hasLocationPermission() || locationManager == null) {
            onUpdated(getCurrentLocation())
            return
        }

        try {
            val provider = when {
                locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
                else -> null
            }

            if (provider == null) {
                onUpdated(getCurrentLocation())
                return
            }

            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    locationManager.removeUpdates(this)
                    val isEmulatorDefault =
                        location.latitude in 37.35..37.48 && location.longitude in -122.15..-122.00
                    if (!isEmulatorDefault) {
                        onUpdated(UserGeoCoordinate(location.latitude, location.longitude, isRealFix = true))
                    } else {
                        onUpdated(UserGeoCoordinate(DEFAULT_LAT, DEFAULT_LON, isRealFix = false))
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }

            locationManager.requestLocationUpdates(provider, 1000L, 1f, listener)
            // Trả về ngay vị trí lastKnown trong lúc chờ phần cứng GPS phản hồi
            onUpdated(getCurrentLocation())
        } catch (e: Exception) {
            onUpdated(getCurrentLocation())
        }
    }

    /**
     * Đồng bộ khoảng cách thực tế và nếu người dùng đang ở một vị trí thực tế cách xa Quận 1 (> 5km),
     * tự động neo các điểm mẫu xung quanh vị trí thực tế của người dùng để bản đồ luôn có các điểm lân cận.
     */
    fun syncPlacesWithUserLocation(
        places: List<WifiPlaceItem>,
        userCoord: UserGeoCoordinate
    ): List<WifiPlaceItem> {
        val distFromDefault = distanceInMeters(
            userCoord.latitude,
            userCoord.longitude,
            DEFAULT_LAT,
            DEFAULT_LON
        )

        val shouldAnchorAroundUser = userCoord.isRealFix && distFromDefault > 5000f

        val offsets = listOf(
            Pair(0.0022, -0.0012),
            Pair(-0.0005, -0.0030),
            Pair(-0.0024, 0.0024),
            Pair(-0.0038, -0.0014)
        )

        return places.mapIndexed { index, place ->
            val targetLat = if (shouldAnchorAroundUser && !place.id.startsWith("measured_")) {
                val offset = offsets.getOrElse(index % offsets.size) { Pair(0.0015, 0.0015) }
                userCoord.latitude + offset.first
            } else {
                place.latitude
            }

            val targetLon = if (shouldAnchorAroundUser && !place.id.startsWith("measured_")) {
                val offset = offsets.getOrElse(index % offsets.size) { Pair(0.0015, 0.0015) }
                userCoord.longitude + offset.second
            } else {
                place.longitude
            }

            val updatedDistanceText = formatDistance(
                userCoord.latitude,
                userCoord.longitude,
                targetLat,
                targetLon
            )

            place.copy(
                latitude = targetLat,
                longitude = targetLon,
                distanceText = updatedDistanceText
            )
        }
    }
}
