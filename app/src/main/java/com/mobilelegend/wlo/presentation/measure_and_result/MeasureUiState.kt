package com.mobilelegend.wlo.presentation.measure_and_result

import com.mobilelegend.wlo.domain.model.LocationScore
import com.mobilelegend.wlo.domain.model.UseCaseCategory

/**
 * Model đại diện cho một điểm đo trong phiên mở app hiện tại (Session History Item).
 */
data class SavedSpotItem(
    val id: String,
    val spotName: String,
    val score: LocationScore,
    val timestamp: Long = requireNotNull(score.measurement).measuredAtMillis,
    val isSaved: Boolean = true
)

sealed interface MeasureStatus {
    object Idle : MeasureStatus
    object Cancelled : MeasureStatus
    data class Measuring(val progress: Float, val currentStepText: String) : MeasureStatus
    data class Completed(val score: LocationScore) : MeasureStatus
    data class Failed(val errorMessage: String) : MeasureStatus
}

data class MeasureUiState(
    val status: MeasureStatus = MeasureStatus.Idle,
    val selectedCategory: UseCaseCategory = UseCaseCategory.GAMING,
    val savedSpots: List<SavedSpotItem> = emptyList(),
    // Remains true until cancellation has finished closing the session and its sockets.
    val isBenchmarkRunning: Boolean = false
)
