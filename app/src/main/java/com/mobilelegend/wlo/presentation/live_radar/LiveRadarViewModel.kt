package com.mobilelegend.wlo.presentation.live_radar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilelegend.wlo.domain.model.UseCaseCategory
import com.mobilelegend.wlo.domain.model.WifiAccessState
import com.mobilelegend.wlo.domain.repository.WifiRepository
import com.mobilelegend.wlo.domain.usecase.StreamLiveWifiUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** One foreground observer shared by Home, measurement and Radar, owned by the Activity store. */
class LiveRadarViewModel(
    private val streamLiveWifiUseCase: StreamLiveWifiUseCase,
    private val wifiRepository: WifiRepository,
    private val elapsedRealtimeMs: () -> Long = { System.nanoTime() / 1_000_000L }
) : ViewModel() {
    private data class ObservationRequest(val foreground: Boolean = false, val revision: Long = 0)
    private data class Conditions(
        val access: WifiAccessState = WifiAccessState(),
        val connected: Boolean = false,
        val failure: RadarFailure? = null
    ) {
        val ready: Boolean get() = failure == null && connected &&
            access.canReadWifiIdentity && access.locationServicesReady
    }

    private val requests = MutableStateFlow(ObservationRequest())
    private var latestConditions: Conditions? = null
    private val _uiState = MutableStateFlow(LiveRadarUiState())
    val uiState: StateFlow<LiveRadarUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // collectLatest cancels AND joins cleanup before the next observer starts.
            requests.collectLatest { request ->
                if (request.foreground) {
                    conditions(request).distinctUntilChanged().collectLatest conditionChanged@{ conditions ->
                        if (requests.value != request || latestConditions != conditions) return@conditionChanged
                        invalidate(
                            if (conditions.ready) RadarObservation.STARTING
                            else if (conditions.failure != null) RadarObservation.ERROR
                            else RadarObservation.WAITING_FOR_CONDITIONS,
                            conditions.failure
                        )
                        if (conditions.ready) observeWithRetry(request, conditions)
                    }
                }
            }
        }
    }

    /** Called by the Activity, not individual destinations; repeated start/stop is harmless. */
    fun setForeground(foreground: Boolean) {
        if (requests.value.foreground == foreground) return
        invalidate(if (foreground) RadarObservation.STARTING else RadarObservation.PAUSED)
        requests.value = requests.value.copy(foreground = foreground, revision = requests.value.revision + 1)
    }

    /** Explicit retry or return from Settings. Never starts hardware while backgrounded. */
    fun refreshHardwareStatus() {
        if (!requests.value.foreground) return
        invalidate(RadarObservation.STARTING)
        requests.value = requests.value.copy(revision = requests.value.revision + 1)
    }

    private fun conditions(request: ObservationRequest) = flow {
        var previous: Conditions? = null
        while (currentCoroutineContext().isActive && requests.value == request) {
            val conditions = try {
                Conditions(wifiRepository.getWifiAccessState(), wifiRepository.isWifiConnected())
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: SecurityException) {
                Conditions(failure = RadarFailure.PERMISSION_DENIED)
            } catch (_: Exception) {
                Conditions(failure = RadarFailure.SOURCE_FAILED)
            }
            latestConditions = conditions
            _uiState.update {
                it.copy(wifiAccess = conditions.access, isWifiConnected = conditions.connected,
                    sampleAgeMs = it.lastSampleElapsedMs?.let(::age))
            }
            if (conditions != previous) {
                // Invalidate before waiting for the previous collector's cleanup.
                invalidate(if (conditions.ready) RadarObservation.STARTING
                    else if (conditions.failure != null) RadarObservation.ERROR
                    else RadarObservation.WAITING_FOR_CONDITIONS, conditions.failure)
                previous = conditions
            }
            emit(conditions)
            delay(1_000L)
        }
    }

    private suspend fun observeWithRetry(request: ObservationRequest, conditions: Conditions) {
        // A finite budget per foreground/conditions/manual-refresh cycle. A single sample
        // does not reset it: emit-then-fail sources must not cause an endless retry loop.
        var retries = 0
        while (currentCoroutineContext().isActive && requests.value == request && latestConditions == conditions) {
            val failure = try {
                wifiRepository.refreshWifiObservation()
                collectAttempt(request, conditions)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: SecurityException) {
                RadarFailure.PERMISSION_DENIED
            } catch (_: StaleSampleException) {
                RadarFailure.STALE_DATA
            } catch (_: StreamEndedException) {
                RadarFailure.STREAM_ENDED
            } catch (_: Exception) {
                RadarFailure.SOURCE_FAILED
            }
            currentCoroutineContext().ensureActive()
            if (requests.value != request || latestConditions != conditions) return
            val retry = failure != RadarFailure.PERMISSION_DENIED && retries < MAX_RETRIES
            invalidate(if (retry) RadarObservation.RETRYING else RadarObservation.ERROR,
                failure, retries)
            if (!retry) {
                // Conditions polling remains active; only a real transition or explicit
                // refresh restarts registration after permission denial/exhausted retries.
                awaitCancellation()
            }
            delay(1_000L shl retries)
            retries++
        }
    }

    private suspend fun collectAttempt(request: ObservationRequest, conditions: Conditions): Nothing = coroutineScope {
        val attemptContext = currentCoroutineContext()
        var lastUsableSample = elapsedRealtimeMs()
        launch {
            while (isActive) {
                val remaining = STALE_AFTER_MS - age(lastUsableSample)
                if (remaining <= 0L) {
                    // Do not keep old metrics visible while cancellation awaits slow cleanup.
                    if (requests.value == request && latestConditions == conditions) {
                        invalidate(RadarObservation.ERROR, RadarFailure.STALE_DATA)
                    }
                    throw StaleSampleException()
                }
                delay(remaining)
            }
        }
        streamLiveWifiUseCase().collect { data ->
            // Also rejects an ill-behaved source emitting from NonCancellable cleanup.
            attemptContext.ensureActive()
            if (requests.value != request || latestConditions != conditions) {
                throw CancellationException("Obsolete Radar observer")
            }
            if (data.isConnected && data.isSampleAvailable) {
                val now = elapsedRealtimeMs()
                lastUsableSample = now
                _uiState.update {
                    it.copy(radarData = data, observation = RadarObservation.LIVE,
                        failure = null, lastSampleElapsedMs = now, sampleAgeMs = 0L,
                        automaticRetries = 0)
                }
            } else {
                invalidate(RadarObservation.STARTING)
            }
        }
        throw StreamEndedException()
    }

    private fun age(timestamp: Long): Long = (elapsedRealtimeMs() - timestamp).coerceAtLeast(0L)

    private fun invalidate(status: RadarObservation, failure: RadarFailure? = null, retries: Int = 0) {
        _uiState.update {
            it.copy(radarData = LiveRadarUiState.EmptyRadarData, observation = status,
                failure = failure, sampleAgeMs = it.lastSampleElapsedMs?.let(::age),
                automaticRetries = retries)
        }
    }

    fun selectCategory(category: UseCaseCategory) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    private class StaleSampleException : Exception()
    private class StreamEndedException : Exception()

    companion object {
        const val MAX_RETRIES = 3
        const val STALE_AFTER_MS = 5_000L
    }
}
