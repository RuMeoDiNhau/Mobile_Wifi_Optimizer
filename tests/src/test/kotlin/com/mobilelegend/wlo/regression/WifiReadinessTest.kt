package com.mobilelegend.wlo.regression

import com.mobilelegend.wlo.domain.model.*
import com.mobilelegend.wlo.domain.usecase.CalculateScoreUseCase
import com.mobilelegend.wlo.domain.usecase.PerformBenchmarkUseCase
import com.mobilelegend.wlo.presentation.measure_and_result.MeasureStatus
import com.mobilelegend.wlo.presentation.measure_and_result.MeasureViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WifiReadinessTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeWifiRepository()
    private val benchmark = PerformBenchmarkUseCase(repository, CalculateScoreUseCase(), dispatcher)
    private val readyAccess = WifiAccessState(34, true, true, true)

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun coarse_map_permission_is_separate_from_wifi_identity_on_modern_android() {
        for (sdk in listOf(26, 28, 29, 30, 31, 32, 33, 34)) {
            val access = WifiAccessState(sdk, false, true, true)
            assertTrue(access.canUseMap)
            assertEquals(sdk < 29, access.canReadWifiIdentity)
            assertEquals(sdk < 29, benchmarkReadiness(access, true, repository.metric).canMeasure)
        }
    }

    @Test fun fine_location_allows_identity_on_supported_versions_without_nearby_management_permission() {
        for (sdk in listOf(26, 28, 30, 31, 33, 34)) {
            val access = readyAccess.copy(sdkInt = sdk)
            assertTrue(benchmarkReadiness(access, true, repository.metric).canMeasure)
        }
    }

    @Test fun nearby_does_not_replace_precise_location_on_android_13_and_above() {
        for (sdk in listOf(33, 34)) {
            val access = readyAccess.copy(sdkInt = sdk, fineLocation = false)
            assertEquals(BenchmarkBlockReason.PRECISE_LOCATION, benchmarkReadiness(access, true, repository.metric).reason)
        }
    }

    @Test fun missing_permission_and_disabled_service_have_different_actions() {
        val permission = benchmarkReadiness(readyAccess.copy(fineLocation = false), true, repository.metric)
        val location = benchmarkReadiness(readyAccess.copy(locationServiceEnabled = false), true, repository.metric)
        assertEquals(ReadinessAction.APP_SETTINGS, permission.reason!!.action)
        assertEquals(BenchmarkBlockReason.PRECISE_LOCATION, permission.reason)
        assertEquals(ReadinessAction.LOCATION_SETTINGS, location.reason!!.action)
        assertEquals(BenchmarkBlockReason.LOCATION_DISABLED, location.reason)
    }

    @Test fun no_wifi_and_busy_have_priority_and_never_offer_measurement() {
        val offline = benchmarkReadiness(readyAccess, false, repository.metric)
        assertEquals(ReadinessAction.WIFI_SETTINGS, offline.reason!!.action)
        assertEquals(BenchmarkBlockReason.BUSY, benchmarkReadiness(readyAccess, false, null, busy = true).reason)
    }

    @Test fun missing_callback_sample_is_waiting_not_ready_or_a_gps_error() {
        val sample = repository.metric.copy(isSampleAvailable = false)
        assertEquals(BenchmarkBlockReason.WAITING_FOR_WIFI, benchmarkReadiness(readyAccess, true, sample).reason)
    }

    @Test fun hidden_or_malformed_identity_is_not_reported_as_disabled_gps() {
        for (sample in listOf(
            repository.metric.copy(ssid = "<unknown ssid>"),
            repository.metric.copy(bssid = "02:00:00:00:00:00"),
            repository.metric.copy(bssid = "invalid")
        )) {
            assertEquals(BenchmarkBlockReason.HIDDEN_IDENTITY, benchmarkReadiness(readyAccess, true, sample).reason)
        }
    }

    @Test fun invalid_signal_or_linkspeed_blocks_scoring_while_low_valid_values_are_allowed() {
        for (sample in listOf(
            repository.metric.copy(rssi = -127), repository.metric.copy(rssi = 0),
            repository.metric.copy(linkSpeedMbps = 0), repository.metric.copy(linkSpeedMbps = -1)
        )) {
            assertEquals(BenchmarkBlockReason.INVALID_METRICS, benchmarkReadiness(readyAccess, true, sample).reason)
        }
        assertTrue(benchmarkReadiness(readyAccess, true, repository.metric.copy(rssi = -100, linkSpeedMbps = 1)).canMeasure)
    }

    @Test fun every_blocked_condition_matches_ui_and_viewmodel_and_opens_no_session() = runTest(dispatcher) {
        val original = repository.metric
        val scenarios: List<() -> Unit> = listOf(
            { repository.access = readyAccess.copy(fineLocation = false) },
            { repository.access = readyAccess.copy(locationServiceEnabled = false) },
            { repository.metric = original.copy(isConnected = false) },
            { repository.metric = original.copy(isSampleAvailable = false) },
            { repository.metric = original.copy(ssid = "<unknown ssid>") },
            { repository.metric = original.copy(rssi = -127) }
        )
        for (configure in scenarios) {
            repository.metric = original
            repository.access = readyAccess
            configure()
            val metric = repository.metric
            val radar = onlineRadar().let { it.copy(
                wifiAccess = repository.access, isWifiConnected = metric.isConnected,
                radarData = it.radarData.copy(ssid = metric.ssid, bssid = metric.bssid,
                    rssi = metric.rssi, linkSpeedMbps = metric.linkSpeedMbps,
                    isConnected = metric.isConnected, isSampleAvailable = metric.isSampleAvailable)
            ) }
            val model = MeasureViewModel(benchmark)
            assertFalse(model.startBenchmark(UseCaseCategory.GAMING))
            assertEquals(radar.readiness().reason?.message, (model.uiState.value.status as MeasureStatus.Failed).errorMessage)
            assertFalse(model.uiState.value.isBenchmarkRunning)
            assertTrue(model.uiState.value.savedSpots.isEmpty())
        }
        advanceUntilIdle()
        assertEquals(0, repository.openedSessions)
    }

    @Test fun direct_usecase_call_cannot_bypass_permission_gate() = runTest(dispatcher) {
        repository.access = readyAccess.copy(fineLocation = false)
        try { benchmark(UseCaseCategory.GAMING); fail("Permission must be checked in use case") }
        catch (expected: IllegalStateException) {
            assertEquals(BenchmarkBlockReason.PRECISE_LOCATION.message, expected.message)
        }
        assertEquals(0, repository.openedSessions)
    }

    @Test fun permission_revoked_between_button_click_and_dispatch_is_rechecked() = runTest(dispatcher) {
        val model = MeasureViewModel(benchmark)
        assertTrue(model.startBenchmark(UseCaseCategory.GAMING))
        repository.access = readyAccess.copy(fineLocation = false)
        advanceUntilIdle()
        assertTrue(model.uiState.value.status is MeasureStatus.Failed)
        assertEquals(0, repository.openedSessions)
        assertTrue(model.uiState.value.savedSpots.isEmpty())
    }

    @Test fun invalid_captured_sample_after_preflight_never_probes() = runTest(dispatcher) {
        repository.onOpen = { repository.metric = repository.metric.copy(linkSpeedMbps = -1) }
        val model = MeasureViewModel(benchmark)
        assertTrue(model.startBenchmark(UseCaseCategory.GAMING))
        advanceUntilIdle()
        assertTrue(model.uiState.value.status is MeasureStatus.Failed)
        assertEquals(0, repository.pingCalls)
        assertEquals(1, repository.closedSessions)
    }

    @Test fun revoked_permission_during_probe_prevents_history_acceptance() = runTest(dispatcher) {
        repository.onProbe = { repository.access = readyAccess.copy(fineLocation = false) }
        val model = MeasureViewModel(benchmark)
        model.startBenchmark(UseCaseCategory.GAMING)
        advanceUntilIdle()
        assertTrue(model.uiState.value.status is MeasureStatus.Failed)
        assertTrue(model.uiState.value.savedSpots.isEmpty())
        assertEquals(1, repository.closedSessions)
    }

    @Test fun granting_precise_permission_after_denial_allows_next_measurement() = runTest(dispatcher) {
        repository.access = readyAccess.copy(fineLocation = false)
        val model = MeasureViewModel(benchmark)
        assertFalse(model.startBenchmark(UseCaseCategory.GAMING))
        repository.access = readyAccess
        assertTrue(model.startBenchmark(UseCaseCategory.GAMING))
        advanceUntilIdle()
        assertTrue(model.uiState.value.status is MeasureStatus.Completed)
        assertEquals(1, model.uiState.value.savedSpots.size)
    }
}
