package com.mobilelegend.wlo.data.sensor

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.mobilelegend.wlo.core.permissions.PermissionHelper

/** Default coordinates are only a map camera center, never a location fix. */
class DeviceLocationProvider(context: Context, private val permissionHelper: PermissionHelper) {
    private val locationManager = context.applicationContext.getSystemService(LocationManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private var pendingListener: LocationListener? = null
    private var timeout: Runnable? = null

    @Suppress("DEPRECATION")
    private fun coordinate(location: Location): UserGeoCoordinate = assessLocation(
        location.latitude, location.longitude, location.time,
        (SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos) / 1_000_000,
        location.accuracy.takeIf { location.hasAccuracy() },
        permissionHelper.wifiAccessState().fineLocation, location.isFromMockProvider
    )

    @SuppressLint("MissingPermission")
    fun getCurrentLocation(): UserGeoCoordinate {
        if (!permissionHelper.hasMapLocationPermission() || !permissionHelper.isLocationServiceEnabled()) {
            return UserGeoCoordinate()
        }
        val manager = locationManager ?: return UserGeoCoordinate()
        val latest = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
            .mapNotNull { provider -> runCatching { manager.getLastKnownLocation(provider) }.getOrNull() }
            .filter { it.elapsedRealtimeNanos > 0 && it.elapsedRealtimeNanos <= SystemClock.elapsedRealtimeNanos() }
            .maxByOrNull { it.elapsedRealtimeNanos }
        return latest?.let(::coordinate) ?: UserGeoCoordinate()
    }

    /** One request at a time; timeout and owner disposal release its listener. */
    @SuppressLint("MissingPermission")
    fun requestSingleUpdate(onUpdated: (UserGeoCoordinate) -> Unit) {
        cancelLocationUpdate()
        onUpdated(getCurrentLocation())
        val manager = locationManager ?: return
        if (!permissionHelper.hasMapLocationPermission() || !permissionHelper.isLocationServiceEnabled()) return
        try {
            val provider = when {
                permissionHelper.wifiAccessState().fineLocation && manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
                manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
                else -> return
            }
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    if (pendingListener !== this) return
                    cancelLocationUpdate()
                    val canUseLocation = permissionHelper.hasMapLocationPermission() &&
                        permissionHelper.isLocationServiceEnabled()
                    onUpdated(if (canUseLocation) coordinate(location) else UserGeoCoordinate())
                }
                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {
                    if (pendingListener !== this) return
                    cancelLocationUpdate()
                    onUpdated(getCurrentLocation())
                }
            }
            pendingListener = listener
            manager.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
            val deadline = Runnable {
                if (pendingListener === listener) {
                    cancelLocationUpdate()
                    onUpdated(getCurrentLocation())
                }
            }
            timeout = deadline
            handler.postDelayed(deadline, 10_000L)
        } catch (_: SecurityException) {
            cancelLocationUpdate()
            onUpdated(UserGeoCoordinate())
        } catch (_: IllegalArgumentException) {
            cancelLocationUpdate()
            onUpdated(getCurrentLocation())
        }
    }

    fun cancelLocationUpdate() {
        timeout?.let(handler::removeCallbacks)
        timeout = null
        pendingListener?.let { listener -> runCatching { locationManager?.removeUpdates(listener) } }
        pendingListener = null
    }
}
