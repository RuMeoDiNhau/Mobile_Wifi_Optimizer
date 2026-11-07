package com.mobilelegend.wlo.presentation.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.mobilelegend.wlo.domain.model.BenchmarkReadiness
import com.mobilelegend.wlo.domain.model.ReadinessAction

@Composable
fun MeasurementReadinessNotice(
    readiness: BenchmarkReadiness,
    onOpenAppSettings: () -> Unit,
    onOpenLocationSettings: () -> Unit,
    onOpenWifiSettings: () -> Unit
) {
    val reason = readiness.reason ?: return
    val action = when (reason.action) {
        ReadinessAction.APP_SETTINGS -> "Mở quyền ứng dụng" to onOpenAppSettings
        ReadinessAction.LOCATION_SETTINGS -> "Bật vị trí" to onOpenLocationSettings
        ReadinessAction.WIFI_SETTINGS -> "Kết nối Wi-Fi" to onOpenWifiSettings
        ReadinessAction.NONE -> null
    }
    if (action == null) Text(reason.message)
    else PermissionBanner(reason.message, action.first, action.second)
}
