package com.mobilelegend.wlo

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.mobilelegend.wlo.core.network.PingTester
import com.mobilelegend.wlo.core.permissions.PermissionHelper
import com.mobilelegend.wlo.data.repository_impl.WifiRepositoryImpl
import com.mobilelegend.wlo.data.sensor.WifiScanner
import com.mobilelegend.wlo.domain.usecase.CalculateScoreUseCase
import com.mobilelegend.wlo.domain.usecase.PerformBenchmarkUseCase
import com.mobilelegend.wlo.domain.usecase.StreamLiveWifiUseCase
import com.mobilelegend.wlo.presentation.live_radar.LiveRadarViewModel
import com.mobilelegend.wlo.presentation.measure_and_result.MeasureViewModel
import com.mobilelegend.wlo.presentation.navigation.AppNavigation
import com.mobilelegend.wlo.presentation.theme.AppBackground
import com.mobilelegend.wlo.presentation.theme.WloTheme

class MainActivity : ComponentActivity() {

    private lateinit var permissionHelper: PermissionHelper
    private lateinit var liveRadarViewModel: LiveRadarViewModel
    private lateinit var measureViewModel: MeasureViewModel

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        liveRadarViewModel.refreshHardwareStatus()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Khởi tạo Core & Hardware
        permissionHelper = PermissionHelper(this)
        val wifiScanner = WifiScanner(this)
        val pingTester = PingTester()
        val wifiRepository = WifiRepositoryImpl(wifiScanner, pingTester, permissionHelper)

        // 2. Khởi tạo UseCases
        val streamLiveWifiUseCase = StreamLiveWifiUseCase(wifiRepository)
        val calculateScoreUseCase = CalculateScoreUseCase()
        val performBenchmarkUseCase = PerformBenchmarkUseCase(wifiRepository, calculateScoreUseCase)

        // 3. Khởi tạo ViewModels
        liveRadarViewModel = LiveRadarViewModel(streamLiveWifiUseCase, wifiRepository, permissionHelper)
        measureViewModel = MeasureViewModel(performBenchmarkUseCase)

        // 4. Yêu cầu cấp quyền khi mở ứng dụng lần đầu
        requestRequiredPermissions()

        setContent {
            WloTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = AppBackground
                ) {
                    val navController = rememberNavController()
                    AppNavigation(
                        navController = navController,
                        liveRadarViewModel = liveRadarViewModel,
                        measureViewModel = measureViewModel,
                        onOpenLocationSettings = {
                            startActivity(permissionHelper.createLocationSettingsIntent())
                        },
                        onOpenAppSettings = {
                            startActivity(permissionHelper.createAppSettingsIntent())
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        liveRadarViewModel.refreshHardwareStatus()
    }

    private fun requestRequiredPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }

        permissionLauncher.launch(permissions.toTypedArray())
    }
}
