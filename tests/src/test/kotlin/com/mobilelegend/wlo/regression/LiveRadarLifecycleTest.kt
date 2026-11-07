package com.mobilelegend.wlo.regression

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.mobilelegend.wlo.data.model.WifiMetric
import com.mobilelegend.wlo.domain.model.UseCaseCategory
import com.mobilelegend.wlo.domain.model.WifiAccessState
import com.mobilelegend.wlo.domain.repository.WifiBenchmarkSession
import com.mobilelegend.wlo.domain.repository.WifiRepository
import com.mobilelegend.wlo.domain.usecase.StreamLiveWifiUseCase
import com.mobilelegend.wlo.presentation.live_radar.LiveRadarUiState
import com.mobilelegend.wlo.presentation.live_radar.LiveRadarViewModel
import com.mobilelegend.wlo.presentation.live_radar.RadarFailure
import com.mobilelegend.wlo.presentation.live_radar.RadarObservation
import com.mobilelegend.wlo.presentation.measure_and_result.MeasureStatus
import com.mobilelegend.wlo.presentation.measure_and_result.networkMeasureContent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.IOException

/** Virtual time + fake source only; no Android hardware, real sockets or wall-clock sleeps. */
@OptIn(ExperimentalCoroutinesApi::class)
class LiveRadarLifecycleTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = RadarRepository()
    private val store = ViewModelStore()
    private var creations = 0
    private val factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            creations++
            return LiveRadarViewModel(StreamLiveWifiUseCase(repository), repository) {
                dispatcher.scheduler.currentTime
            } as T
        }
    }
    private lateinit var viewModel: LiveRadarViewModel
    private val state: LiveRadarUiState get() = viewModel.uiState.value

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = ViewModelProvider(store, factory)[LiveRadarViewModel::class.java]
    }

    @After fun tearDown() {
        store.clear()
        dispatcher.scheduler.runCurrent()
        Dispatchers.resetMain()
    }

    @Test fun construction_and_background_refresh_do_not_access_hardware() = runTest(dispatcher) {
        repeat(5) { viewModel.refreshHardwareStatus() }
        runCurrent()
        advanceTimeBy(10_000)
        runCurrent()
        assertEquals(0, repository.starts)
        assertEquals(0, repository.conditionReads)
        assertEquals(0, repository.refreshes)
        assertEquals(RadarObservation.PAUSED, state.observation)
        assertFalse(state.radarData.isSampleAvailable)
        assertFalse(state.readiness().canMeasure)
    }

    @Test fun repeated_foreground_calls_create_one_collector() = runTest(dispatcher) {
        repeat(10) { viewModel.setForeground(true) }
        runCurrent()
        repeat(10) { viewModel.setForeground(true) }
        advanceTimeBy(3_000)
        runCurrent()
        assertEquals(1, repository.starts)
        assertEquals(1, repository.maxActive)
        assertEquals(RadarObservation.LIVE, state.observation)
        assertTrue(state.readiness().canMeasure)
    }

    @Test fun stop_invalidates_immediately_then_cleans_up_and_stops_polling() = runTest(dispatcher) {
        viewModel.setForeground(true)
        runCurrent()
        viewModel.setForeground(false)
        assertFalse(state.radarData.isSampleAvailable)
        assertFalse(state.readiness().canMeasure)
        assertEquals(RadarObservation.PAUSED, state.observation)
        runCurrent()
        val readsAtStop = repository.conditionReads
        advanceTimeBy(10_000)
        runCurrent()
        assertEquals(0, repository.active)
        assertEquals(1, repository.closed)
        assertEquals(readsAtStop, repository.conditionReads)
        assertNull(state.failure)
    }

    @Test fun rapid_stop_start_before_dispatch_still_replaces_old_collector() = runTest(dispatcher) {
        viewModel.setForeground(true)
        runCurrent()
        viewModel.setForeground(false)
        viewModel.setForeground(true)
        runCurrent()
        assertEquals(2, repository.starts)
        assertEquals(1, repository.closed)
        assertEquals(1, repository.maxActive)
    }

    @Test fun refresh_waits_for_cleanup_and_coalesces_repeated_requests() = runTest(dispatcher) {
        val releaseCleanup = CompletableDeferred<Unit>()
        repository.cleanup = { withContext(NonCancellable) { releaseCleanup.await() } }
        viewModel.setForeground(true)
        runCurrent()
        try {
            viewModel.refreshHardwareStatus()
            runCurrent()
            repeat(10) { viewModel.refreshHardwareStatus() }
            runCurrent()
            assertEquals(1, repository.starts)
            assertFalse(state.radarData.isSampleAvailable)
        } finally {
            releaseCleanup.complete(Unit)
        }
        runCurrent()
        assertEquals(2, repository.starts)
        assertEquals(1, repository.maxActive)
        assertEquals(RadarObservation.LIVE, state.observation)
    }

    @Test fun stop_during_refresh_cleanup_does_not_start_a_replacement() = runTest(dispatcher) {
        val releaseCleanup = CompletableDeferred<Unit>()
        repository.cleanup = { withContext(NonCancellable) { releaseCleanup.await() } }
        viewModel.setForeground(true)
        runCurrent()
        try {
            viewModel.refreshHardwareStatus()
            runCurrent()
            viewModel.setForeground(false)
            runCurrent()
        } finally {
            releaseCleanup.complete(Unit)
        }
        runCurrent()
        assertEquals(1, repository.starts)
        assertEquals(0, repository.active)
        assertEquals(RadarObservation.PAUSED, state.observation)
    }

    @Test fun provider_reuses_radar_and_category_across_owner_recreation() = runTest(dispatcher) {
        viewModel.selectCategory(UseCaseCategory.VIDEO_CALL)
        viewModel.setForeground(true)
        runCurrent()
        viewModel.setForeground(false)
        val recreated = ViewModelProvider(store, factory)[LiveRadarViewModel::class.java]
        recreated.setForeground(true)
        runCurrent()
        assertSame(viewModel, recreated)
        assertEquals(1, creations)
        assertEquals(UseCaseCategory.VIDEO_CALL, recreated.uiState.value.selectedCategory)
        assertEquals(1, repository.maxActive)
    }

    @Test fun store_clear_cancels_observation_and_pending_retries() = runTest(dispatcher) {
        repository.body = { throw IOException("fake source failure") }
        viewModel.setForeground(true)
        runCurrent()
        store.clear()
        runCurrent()
        advanceTimeBy(60_000)
        runCurrent()
        assertEquals(1, repository.starts)
        assertEquals(0, repository.active)
    }

    @Test fun reopening_after_store_clear_uses_a_new_instance() = runTest(dispatcher) {
        viewModel.setForeground(true)
        runCurrent()
        store.clear()
        runCurrent()
        val reopened = ViewModelProvider(store, factory)[LiveRadarViewModel::class.java]
        reopened.setForeground(true)
        runCurrent()
        assertNotSame(viewModel, reopened)
        assertEquals(2, creations)
        assertEquals(1, repository.maxActive)
        assertEquals(RadarObservation.LIVE, reopened.uiState.value.observation)
    }

    @Test fun error_hides_previous_sample_and_retry_recovers() = runTest(dispatcher) {
        repository.body = { attempt ->
            emit(repository.metric)
            if (attempt == 1) { delay(100); throw IOException("fake failure") }
            awaitCancellation()
        }
        viewModel.setForeground(true)
        runCurrent()
        advanceTimeBy(100)
        runCurrent()
        assertEquals(RadarObservation.RETRYING, state.observation)
        assertEquals(RadarFailure.SOURCE_FAILED, state.failure)
        assertFalse(state.radarData.isSampleAvailable)
        assertFalse(state.readiness().canMeasure)
        assertEquals(100L, state.sampleAgeMs)
        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(2, repository.starts)
        assertEquals(RadarObservation.LIVE, state.observation)
        assertNull(state.failure)
        assertEquals(0L, state.sampleAgeMs)
    }

    @Test fun repeated_errors_stop_after_three_backoff_retries() = runTest(dispatcher) {
        repository.body = { throw IOException("persistent") }
        viewModel.setForeground(true)
        runCurrent()
        advanceTimeBy(999)
        runCurrent()
        assertEquals(1, repository.starts)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(2, repository.starts)
        advanceTimeBy(2_000)
        runCurrent()
        assertEquals(3, repository.starts)
        advanceTimeBy(4_000)
        runCurrent()
        assertEquals(4, repository.starts)
        assertEquals(RadarObservation.ERROR, state.observation)
        advanceTimeBy(60_000)
        runCurrent()
        assertEquals(4, repository.starts)
        assertEquals(1, repository.maxActive)
    }

    @Test fun emitting_once_before_each_error_does_not_reset_retry_budget() = runTest(dispatcher) {
        repository.body = { emit(repository.metric); throw IOException("emit then fail") }
        viewModel.setForeground(true)
        runCurrent()
        advanceTimeBy(60_000)
        runCurrent()
        assertEquals(4, repository.starts)
        assertEquals(RadarObservation.ERROR, state.observation)
        assertFalse(state.radarData.isSampleAvailable)
    }

    @Test fun explicit_retry_recovers_after_automatic_budget_is_exhausted() = runTest(dispatcher) {
        repository.body = { throw IOException("persistent") }
        viewModel.setForeground(true)
        runCurrent()
        advanceTimeBy(7_000)
        runCurrent()
        repository.body = { emit(repository.metric); awaitCancellation() }
        viewModel.refreshHardwareStatus()
        runCurrent()
        assertEquals(5, repository.starts)
        assertEquals(RadarObservation.LIVE, state.observation)
        assertTrue(state.readiness().canMeasure)
    }

    @Test fun security_exception_waits_without_automatic_registration_retries() = runTest(dispatcher) {
        repository.body = { throw SecurityException("revoked during registration") }
        viewModel.setForeground(true)
        runCurrent()
        advanceTimeBy(60_000)
        runCurrent()
        assertEquals(1, repository.starts)
        assertEquals(1, repository.refreshes)
        assertEquals(RadarFailure.PERMISSION_DENIED, state.failure)
        assertEquals(RadarObservation.ERROR, state.observation)
        repository.body = { emit(repository.metric); awaitCancellation() }
        viewModel.refreshHardwareStatus()
        runCurrent()
        assertEquals(2, repository.starts)
        assertEquals(RadarObservation.LIVE, state.observation)
    }

    @Test fun permission_revoke_and_regrant_cancels_then_recovers() = runTest(dispatcher) {
        viewModel.setForeground(true)
        runCurrent()
        repository.access = repository.access.copy(fineLocation = false)
        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(0, repository.active)
        assertFalse(state.radarData.isSampleAvailable)
        assertFalse(state.readiness().canMeasure)
        assertEquals(RadarObservation.WAITING_FOR_CONDITIONS, state.observation)
        repository.access = repository.access.copy(fineLocation = true)
        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(2, repository.starts)
        assertTrue(state.readiness().canMeasure)
    }

    @Test fun permission_transition_recovers_even_after_security_error() = runTest(dispatcher) {
        repository.body = { attempt ->
            if (attempt == 1) throw SecurityException("denied")
            emit(repository.metric)
            awaitCancellation()
        }
        viewModel.setForeground(true)
        runCurrent()
        repository.access = repository.access.copy(fineLocation = false)
        advanceTimeBy(1_000)
        runCurrent()
        repository.access = repository.access.copy(fineLocation = true)
        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(2, repository.starts)
        assertEquals(RadarObservation.LIVE, state.observation)
    }

    @Test fun disabled_location_does_not_collect_until_enabled() = runTest(dispatcher) {
        repository.access = repository.access.copy(locationServiceEnabled = false)
        viewModel.setForeground(true)
        runCurrent()
        advanceTimeBy(5_000)
        runCurrent()
        assertEquals(0, repository.starts)
        repository.access = repository.access.copy(locationServiceEnabled = true)
        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(1, repository.starts)
        assertTrue(state.readiness().canMeasure)
    }

    @Test fun wifi_disconnect_stops_stream_and_reconnects_without_manual_refresh() = runTest(dispatcher) {
        viewModel.setForeground(true)
        runCurrent()
        repository.connected = false
        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(0, repository.active)
        assertFalse(state.isWifiConnected)
        assertFalse(state.radarData.isSampleAvailable)
        repository.connected = true
        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(2, repository.starts)
        assertEquals(RadarObservation.LIVE, state.observation)
    }

    @Test fun normally_completed_stream_is_visible_as_failure_and_retried() = runTest(dispatcher) {
        repository.body = { emit(repository.metric) }
        viewModel.setForeground(true)
        runCurrent()
        assertEquals(RadarFailure.STREAM_ENDED, state.failure)
        assertFalse(state.radarData.isSampleAvailable)
        advanceTimeBy(7_000)
        runCurrent()
        assertEquals(4, repository.starts)
        assertEquals(RadarObservation.ERROR, state.observation)
    }

    @Test fun silent_stream_expires_sample_at_five_seconds() = runTest(dispatcher) {
        repository.body = { emit(repository.metric); awaitCancellation() }
        viewModel.setForeground(true)
        runCurrent()
        advanceTimeBy(4_999)
        runCurrent()
        assertTrue(state.radarData.isSampleAvailable)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(RadarFailure.STALE_DATA, state.failure)
        assertFalse(state.radarData.isSampleAvailable)
        assertEquals(5_000L, state.sampleAgeMs)
        assertEquals(1, repository.closed)
    }

    @Test fun no_first_sample_also_times_out_without_inventing_age() = runTest(dispatcher) {
        repository.body = { awaitCancellation() }
        viewModel.setForeground(true)
        runCurrent()
        advanceTimeBy(5_000)
        runCurrent()
        assertEquals(RadarFailure.STALE_DATA, state.failure)
        assertNull(state.sampleAgeMs)
        assertNull(state.lastSampleElapsedMs)
    }

    @Test fun stale_data_is_hidden_before_slow_cleanup_finishes() = runTest(dispatcher) {
        val releaseCleanup = CompletableDeferred<Unit>()
        repository.body = { emit(repository.metric); awaitCancellation() }
        repository.cleanup = { withContext(NonCancellable) { releaseCleanup.await() } }
        viewModel.setForeground(true)
        runCurrent()
        try {
            advanceTimeBy(5_000)
            runCurrent()
            assertFalse(state.radarData.isSampleAvailable)
            assertFalse(state.readiness().canMeasure)
            assertEquals(RadarFailure.STALE_DATA, state.failure)
            assertEquals(1, repository.active)
            advanceTimeBy(10_000)
            runCurrent()
            assertEquals(1, repository.starts)
        } finally {
            releaseCleanup.complete(Unit)
        }
        runCurrent()
        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(2, repository.starts)
        assertEquals(1, repository.maxActive)
    }

    @Test fun backgrounding_during_backoff_cancels_the_scheduled_retry() = runTest(dispatcher) {
        repository.body = { throw IOException("retry pending") }
        viewModel.setForeground(true)
        runCurrent()
        viewModel.setForeground(false)
        runCurrent()
        advanceTimeBy(10_000)
        runCurrent()
        assertEquals(1, repository.starts)
        assertEquals(RadarObservation.PAUSED, state.observation)
        assertNull(state.failure)
    }

    @Test fun source_error_during_cancelled_cleanup_cannot_overwrite_paused_state() = runTest(dispatcher) {
        repository.body = {
            emit(repository.metric)
            try { awaitCancellation() } finally { throw IOException("late cleanup error") }
        }
        viewModel.setForeground(true)
        runCurrent()
        viewModel.setForeground(false)
        runCurrent()
        assertEquals(RadarObservation.PAUSED, state.observation)
        assertNull(state.failure)
        assertFalse(state.radarData.isSampleAvailable)
        assertEquals(0, repository.active)
    }

    @Test fun repeated_unavailable_samples_do_not_keep_watchdog_alive() = runTest(dispatcher) {
        repository.metric = repository.metric.copy(isSampleAvailable = false)
        viewModel.setForeground(true)
        runCurrent()
        advanceTimeBy(5_000)
        runCurrent()
        assertEquals(RadarFailure.STALE_DATA, state.failure)
        assertNull(state.lastSampleElapsedMs)
        assertFalse(state.readiness().canMeasure)
    }

    @Test fun healthy_samples_continue_without_timeout_or_extra_collectors() = runTest(dispatcher) {
        viewModel.setForeground(true)
        runCurrent()
        advanceTimeBy(60_000)
        runCurrent()
        assertEquals(1, repository.starts)
        assertEquals(RadarObservation.LIVE, state.observation)
        assertEquals(60_000L, state.lastSampleElapsedMs)
        assertNull(state.failure)
    }

    @Test fun late_noncancellable_emission_cannot_replace_paused_state() = runTest(dispatcher) {
        var attemptedLateEmission = false
        repository.body = {
            emit(repository.metric)
            try { awaitCancellation() } finally {
                withContext(NonCancellable) {
                    attemptedLateEmission = true
                    emit(repository.metric.copy(ssid = "obsolete"))
                }
            }
        }
        viewModel.setForeground(true)
        runCurrent()
        viewModel.setForeground(false)
        runCurrent()
        assertTrue(attemptedLateEmission)
        assertEquals(RadarObservation.PAUSED, state.observation)
        assertFalse(state.radarData.isSampleAvailable)
        assertNotEquals("obsolete", state.radarData.ssid)
        assertEquals(0, repository.active)
    }

    @Test fun failed_condition_read_is_reported_and_recovers_when_reading_works() = runTest(dispatcher) {
        repository.conditionError = SecurityException("denied")
        viewModel.setForeground(true)
        runCurrent()
        assertEquals(RadarFailure.PERMISSION_DENIED, state.failure)
        assertEquals(0, repository.starts)
        repository.conditionError = null
        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(RadarObservation.LIVE, state.observation)
        assertEquals(1, repository.starts)
    }

    @Test fun refresh_failure_is_handled_without_opening_source() = runTest(dispatcher) {
        repository.refreshError = SecurityException("refresh denied")
        viewModel.setForeground(true)
        runCurrent()
        advanceTimeBy(10_000)
        runCurrent()
        assertEquals(RadarFailure.PERMISSION_DENIED, state.failure)
        assertEquals(1, repository.refreshes)
        assertEquals(0, repository.starts)
    }

    @Test fun radar_error_does_not_change_completed_measurement_identity_or_metrics() = runTest(dispatcher) {
        val score = scoreA()
        val expected = networkMeasureContent(MeasureStatus.Completed(score), onlineRadar(), null)
        repository.body = { throw IOException("failed") }
        viewModel.setForeground(true)
        runCurrent()
        assertEquals(expected, networkMeasureContent(MeasureStatus.Completed(score), state, null))
        assertFalse(state.readiness().canMeasure)
    }
}

/** Custom cold Flow permits a deliberately late emission from cleanup for cancellation tests. */
@OptIn(InternalCoroutinesApi::class)
private class RadarRepository : WifiRepository {
    var metric = WifiMetric("Network A", "AA:BB:CC:DD:EE:01", -55, 150, 5180)
    var access = WifiAccessState(fineLocation = true, coarseLocation = true, locationServiceEnabled = true)
    var connected = true
    var starts = 0
    var active = 0
    var maxActive = 0
    var closed = 0
    var refreshes = 0
    var conditionReads = 0
    var conditionError: Exception? = null
    var refreshError: Exception? = null
    var cleanup: suspend () -> Unit = {}
    var body: suspend FlowCollector<WifiMetric>.(Int) -> Unit = {
        while (true) {
            emit(metric)
            delay(1_000L)
        }
    }

    override fun getLiveWifiStream(): Flow<WifiMetric> = object : Flow<WifiMetric> {
        override suspend fun collect(collector: FlowCollector<WifiMetric>) {
            starts++
            active++
            maxActive = maxOf(maxActive, active)
            try {
                body.invoke(collector, starts)
            } finally {
                try { cleanup() } finally { active--; closed++ }
            }
        }
    }

    override fun getCurrentWifiMetric() = metric
    override fun isWifiConnected() = connected
    override fun getWifiAccessState(): WifiAccessState {
        conditionReads++
        conditionError?.let { throw it }
        return access
    }
    override fun refreshWifiObservation() {
        refreshes++
        refreshError?.let { throw it }
    }
    override suspend fun <T> withBenchmarkSession(block: suspend (WifiBenchmarkSession) -> T): T =
        error("Radar must not open a benchmark session")
}
