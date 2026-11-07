package com.mobilelegend.wlo.presentation.measure_and_result

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilelegend.wlo.domain.model.LocationScore
import com.mobilelegend.wlo.domain.model.MeasurementLocation
import com.mobilelegend.wlo.domain.model.UseCaseCategory
import com.mobilelegend.wlo.domain.usecase.PerformBenchmarkUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Owns one benchmark and the immutable measurements retained for this app session. */
class MeasureViewModel(
    private val performBenchmarkUseCase: PerformBenchmarkUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(MeasureUiState())
    val uiState: StateFlow<MeasureUiState> = _uiState.asStateFlow()
    private val lock = Any()
    private var benchmarkJob: Job? = null
    private var generation = 0L
    private val recordedIds = mutableSetOf<String>()

    /** Reject repeated starts, including while the previous job is cleaning up. */
    fun startBenchmark(category: UseCaseCategory, location: MeasurementLocation? = null): Boolean =
        synchronized(lock) {
            if (benchmarkJob != null) return@synchronized false
            val readiness = performBenchmarkUseCase.readiness()
            if (!readiness.canMeasure) {
                _uiState.update { it.copy(status = MeasureStatus.Failed(requireNotNull(readiness.reason).message)) }
                return@synchronized false
            }
            val runId = ++generation
            // Assign ownership before execution, including with an immediate Main dispatcher.
            val job = viewModelScope.launch(start = CoroutineStart.LAZY) {
                try {
                    val score = performBenchmarkUseCase(category, location)
                    currentCoroutineContext().ensureActive()
                    synchronized(lock) {
                        if (runId == generation && benchmarkJob?.isActive == true) {
                            val measurement = requireNotNull(score.measurement)
                            val firstResult = recordedIds.add(measurement.id)
                            _uiState.update { state ->
                                val spots = if (firstResult) state.savedSpots + SavedSpotItem(
                                    id = measurement.id,
                                    spotName = "Lượt đo #${recordedIds.size} (${category.title})",
                                    score = score,
                                    timestamp = measurement.measuredAtMillis
                                ) else state.savedSpots
                                state.copy(status = MeasureStatus.Completed(score), savedSpots = spots)
                            }
                        }
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    synchronized(lock) {
                        if (runId == generation && benchmarkJob?.isActive == true) {
                            _uiState.update {
                                it.copy(status = MeasureStatus.Failed(error.localizedMessage ?: "Lỗi không xác định khi đo"))
                            }
                        }
                    }
                }
            }
            benchmarkJob = job
            _uiState.update {
                it.copy(
                    selectedCategory = category,
                    isBenchmarkRunning = true,
                    status = MeasureStatus.Measuring(0.5f, "Đang kiểm tra Wi-Fi và đo độ trễ TCP...")
                )
            }
            // Also runs when cancelled before the lazy coroutine body ever starts.
            job.invokeOnCompletion {
                synchronized(lock) {
                    if (benchmarkJob === job) {
                        benchmarkJob = null
                        _uiState.update { state ->
                            state.copy(
                                isBenchmarkRunning = false,
                                status = if (state.status is MeasureStatus.Measuring) MeasureStatus.Cancelled else state.status
                            )
                        }
                    }
                }
            }
            job.start()
            true
        }

    /** Rename/bookmark the existing record. A stale result cannot recreate a deleted record. */
    fun saveSpotToHistory(name: String, score: LocationScore) {
        val id = score.measurement?.id ?: return
        synchronized(lock) {
            _uiState.update { state ->
                state.copy(savedSpots = state.savedSpots.map { spot ->
                    if (spot.id == id) spot.copy(
                        spotName = name.trim().ifBlank { spot.spotName },
                        isSaved = true
                    ) else spot
                })
            }
        }
    }

    fun toggleSpotSaved(spotId: String) {
        synchronized(lock) {
            _uiState.update { state ->
                state.copy(savedSpots = state.savedSpots.map {
                    if (it.id == spotId) it.copy(isSaved = !it.isSaved) else it
                })
            }
        }
    }

    fun removeSpotFromHistory(spotId: String) {
        synchronized(lock) {
            _uiState.update { state ->
                state.copy(savedSpots = state.savedSpots.filterNot { it.id == spotId })
            }
        }
    }

    fun cancelBenchmark() {
        synchronized(lock) {
            val job = benchmarkJob ?: return
            generation++
            _uiState.update {
                if (it.status is MeasureStatus.Measuring) it.copy(status = MeasureStatus.Cancelled) else it
            }
            job.cancel()
        }
    }

    fun resetMeasureState() {
        synchronized(lock) {
            generation++
            _uiState.update { it.copy(status = MeasureStatus.Idle) }
            benchmarkJob?.cancel()
        }
    }

    override fun onCleared() {
        cancelBenchmark()
        super.onCleared()
    }
}
