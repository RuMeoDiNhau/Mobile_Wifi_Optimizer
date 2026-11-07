package com.mobilelegend.wlo.presentation.live_radar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilelegend.wlo.core.permissions.PermissionHelper
import com.mobilelegend.wlo.domain.model.UseCaseCategory
import com.mobilelegend.wlo.domain.repository.WifiRepository
import com.mobilelegend.wlo.domain.usecase.StreamLiveWifiUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel quản lý logic quét liên tục của màn hình Live Radar.
 */
class LiveRadarViewModel(
    private val streamLiveWifiUseCase: StreamLiveWifiUseCase,
    private val wifiRepository: WifiRepository,
    private val permissionHelper: PermissionHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(LiveRadarUiState())
    val uiState: StateFlow<LiveRadarUiState> = _uiState.asStateFlow()

    init {
        refreshHardwareStatus()
        startLiveRadarStream()
    }

    /**
     * Khởi động luồng Flow lắng nghe tín hiệu sóng từ WifiScanner mỗi 1 giây.
     */
    private fun startLiveRadarStream() {
        viewModelScope.launch {
            streamLiveWifiUseCase()
                .catch {
                    // Xử lý an toàn nếu xảy ra ngoại lệ
                }
                .collect { data ->
                    _uiState.update { currentState ->
                        currentState.copy(
                            radarData = data,
                            isWifiConnected = data.isConnected,
                            wifiAccess = permissionHelper.wifiAccessState()
                        )
                    }
                }
        }
    }

    /**
     * Cập nhật lại trạng thái quyền và GPS khi người dùng quay lại từ Settings.
     */
    fun refreshHardwareStatus() {
        wifiRepository.refreshWifiObservation()
        val access = permissionHelper.wifiAccessState()
        val isWifi = wifiRepository.isWifiConnected()

        _uiState.update {
            it.copy(
                wifiAccess = access,
                isWifiConnected = isWifi,
                radarData = streamLiveWifiUseCase.mapToLiveRadarData(wifiRepository.getCurrentWifiMetric())
            )
        }
    }

    /**
     * Thay đổi loại nhu cầu (Gaming / Study / Video Call).
     */
    fun selectCategory(category: UseCaseCategory) {
        _uiState.update { it.copy(selectedCategory = category) }
    }
}
