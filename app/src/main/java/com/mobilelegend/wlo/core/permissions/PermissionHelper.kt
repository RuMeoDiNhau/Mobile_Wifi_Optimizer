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

/**
 * Tiện ích kiểm tra và quản lý quyền truy cập vị trí, trạng thái phần cứng GPS
 * và quyền Nearby Wi-Fi Devices trên Android 13+.
 */
class PermissionHelper(private val context: Context) {

    /**
     * Kiểm tra xem ứng dụng đã có quyền truy cập vị trí chính xác (FINE_LOCATION) hay chưa.
     */
    fun hasLocationPermission(): Boolean {
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocationGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineLocationGranted || coarseLocationGranted
    }

    /**
     * Kiểm tra quyền NEARBY_WIFI_DEVICES đối với Android 13 (API 33) trở lên.
     */
    fun hasNearbyWifiPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.NEARBY_WIFI_DEVICES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Các phiên bản cũ hơn không yêu cầu quyền này
        }
    }

    /**
     * Kiểm tra dịch vụ định vị phần cứng (GPS / Network Provider) có đang được bật hay không.
     * Lưu ý: Nếu GPS tắt, Android sẽ ẩn SSID và trả về "<unknown ssid>" và RSSI = -127.
     */
    fun isLocationServiceEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return false
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        return isGpsEnabled || isNetworkEnabled
    }

    /**
     * Kiểm tra tổng hợp: Đã đủ tất cả quyền và GPS đã bật để quét Wi-Fi chính xác.
     */
    fun isReadyForWifiScanning(): Boolean {
        return hasLocationPermission() && hasNearbyWifiPermission() && isLocationServiceEnabled()
    }

    /**
     * Tạo Intent mở trang Cài đặt chi tiết của ứng dụng để người dùng cấp lại quyền nếu bị từ chối vĩnh viễn.
     */
    fun createAppSettingsIntent(): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    /**
     * Tạo Intent dẫn người dùng trực tiếp tới trang Cài đặt Bật/Tắt Vị trí (GPS) của hệ thống.
     */
    fun createLocationSettingsIntent(): Intent {
        return Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }
}
