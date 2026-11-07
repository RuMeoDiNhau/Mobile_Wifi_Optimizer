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

/** Android adapter; request ownership and location selection are testable without a device. */
class DeviceLocationProvider(context: Context, permissionHelper: PermissionHelper) {
    private val handler = Handler(Looper.getMainLooper())
    private val controller = LocationRequestController(
        AndroidLocationSource(context.applicationContext, permissionHelper),
        LocationDeadline { delay, action ->
            val task = Runnable { action() }
            check(handler.postDelayed(task, delay)) { "Location deadline could not be scheduled" }
            LocationCleanup { handler.removeCallbacks(task) }
        }, SystemClock::elapsedRealtimeNanos
    )
    val requestState = controller.state
    fun getCurrentLocation(): UserGeoCoordinate = controller.currentLocation()
    fun setForeground(active: Boolean) = controller.setForeground(active)
    fun requestSingleUpdate(onUpdated: (UserGeoCoordinate) -> Unit) = controller.request(onUpdated)
    fun cancelLocationUpdate() = controller.cancel()
}

@SuppressLint("MissingPermission")
private class AndroidLocationSource(context: Context, private val permissions: PermissionHelper) : LocationSource {
    private val manager = context.getSystemService(LocationManager::class.java)
    override fun canLocate(): Boolean = manager != null && permissions.hasMapLocationPermission() &&
        permissions.isLocationServiceEnabled()
    override fun hasPrecisePermission(): Boolean = permissions.wifiAccessState().fineLocation
    override fun lastKnownSamples(): List<LocationSample> = listOf(
        LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER
    ).mapNotNull { provider -> runCatching { manager?.getLastKnownLocation(provider)?.toSample() }.getOrNull() }

    override fun request(onSample: (LocationSample) -> Unit, onUnavailable: () -> Unit): LocationCleanup {
        val locationManager = checkNotNull(manager)
        val provider = when {
            hasPrecisePermission() && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> { onUnavailable(); return LocationCleanup {} }
        }
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) = onSample(location.toSample())
            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) = onUnavailable()
        }
        try {
            locationManager.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
        } catch (error: Exception) {
            // Also clean up if registration partially succeeded before throwing.
            runCatching { locationManager.removeUpdates(listener) }
            throw error
        }
        return LocationCleanup { locationManager.removeUpdates(listener) }
    }

    @Suppress("DEPRECATION")
    private fun Location.toSample() = LocationSample(latitude, longitude, time,
        elapsedRealtimeNanos, accuracy.takeIf { hasAccuracy() }, isFromMockProvider)
}
