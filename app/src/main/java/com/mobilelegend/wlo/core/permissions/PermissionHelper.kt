package com.mobilelegend.wlo.core.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.mobilelegend.wlo.domain.model.WifiAccessState

class PermissionHelper(private val context: Context) {
    private fun granted(permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    fun wifiAccessState() = WifiAccessState(
        sdkInt = Build.VERSION.SDK_INT,
        fineLocation = granted(Manifest.permission.ACCESS_FINE_LOCATION),
        coarseLocation = granted(Manifest.permission.ACCESS_COARSE_LOCATION),
        locationServiceEnabled = isLocationServiceEnabled()
    )

    /** Approximate location suffices for the map, but not Wi-Fi identity on Android 10+. */
    fun hasMapLocationPermission(): Boolean = wifiAccessState().canUseMap

    fun isLocationServiceEnabled(): Boolean {
        val manager = context.getSystemService(LocationManager::class.java) ?: return false
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) manager.isLocationEnabled
            else manager.isProviderEnabled(LocationManager.GPS_PROVIDER) || manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }.getOrDefault(false)
    }

    fun createAppSettingsIntent() = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    fun createLocationSettingsIntent() = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    fun createWifiSettingsIntent() = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
}
