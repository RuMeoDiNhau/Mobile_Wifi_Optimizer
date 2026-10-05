package com.mobilelegend.wlo.presentation.measure_and_result

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilelegend.wlo.domain.model.LocationScore
import com.mobilelegend.wlo.domain.model.UseCaseCategory
import com.mobilelegend.wlo.domain.usecase.PerformBenchmarkUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ViewModel điều phối tiến trình Benchmark 1-chạm (3s) và quản lý lịch sử so sánh trong RAM.
 */
class MeasureViewModel(
    private val performBenchmarkUseCase: PerformBenchmarkUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MeasureUiState())
    val uiState: StateFlow<MeasureUiState> = _uiState.asStateFlow()

    /**
     * Bắt đầu quy trình kiểm định 1-chạm kéo dài 3 giây.
     */
    fun startBenchmark(category: UseCaseCategory) {
        _uiState.update { it.copy(selectedCategory = category) }

        viewModelScope.launch {
            try {
                // Giai đoạn 1: Đọc cảm biến phần cứng (1s)
                _uiState.update {
                    it.copy(status = MeasureStatus.Measuring(0.33f, "Đang đọc cường độ sóng RSSI và LinkSpeed..."))
                }
                delay(1000)

                // Giai đoạn 2: Đo độ trễ Socket Ping (1.5s)
                _uiState.update {
                    it.copy(status = MeasureStatus.Measuring(0.66f, "Đang gửi gói tin Socket kiểm tra Ping 1.1.1.1..."))
                }
                val score = performBenchmarkUseCase(category)
                delay(800)

                // Giai đoạn 3: Tính toán điểm số (0.5s)
                _uiState.update {
                    it.copy(status = MeasureStatus.Measuring(1.0f, "Đang tổng hợp điểm số và phân tích nhu cầu..."))
                }
                delay(400)

                // Hoàn tất
                _uiState.update {
                    it.copy(status = MeasureStatus.Completed(score))
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(status = MeasureStatus.Failed(e.localizedMessage ?: "Lỗi không xác định khi đo"))
                }
            }
        }
    }

    /**
     * Lưu vị trí vừa đo vào danh sách so sánh tạm thời trong phiên (In-memory).
     */
    fun saveSpotToHistory(name: String, score: LocationScore) {
        val spotName = name.ifBlank { "Vị trí #${_uiState.value.savedSpots.size + 1}" }
        val newItem = SavedSpotItem(
            id = UUID.randomUUID().toString(),
            spotName = spotName,
            score = score
        )

        _uiState.update { currentState ->
            currentState.copy(savedSpots = currentState.savedSpots + newItem)
        }
    }

    /**
     * Xóa một điểm đo khỏi danh sách so sánh.
     */
    fun removeSpotFromHistory(spotId: String) {
        _uiState.update { currentState ->
            currentState.copy(savedSpots = currentState.savedSpots.filterNot { it.id == spotId })
        }
    }

    /**
     * Đặt lại trạng thái để đo lại.
     */
    fun resetMeasureState() {
        _uiState.update { it.copy(status = MeasureStatus.Idle) }
    }
}
