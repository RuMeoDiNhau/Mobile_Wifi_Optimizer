package com.mobilelegend.wlo.regression

import com.mobilelegend.wlo.core.network.BenchmarkNetworkGuard
import com.mobilelegend.wlo.core.network.WifiNetworkChangedException
import com.mobilelegend.wlo.data.model.WifiMetric
import com.mobilelegend.wlo.domain.model.UseCaseCategory
import com.mobilelegend.wlo.domain.usecase.CalculateScoreUseCase
import com.mobilelegend.wlo.domain.usecase.PerformBenchmarkUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NetworkSessionRegressionTest {
    private val metric = WifiMetric("A", "AA:BB:CC:DD:EE:01", -55, 150, 5180)

    @Test fun unchanged_network_accepts_signal_variation_and_bssid_case() {
        val guard = BenchmarkNetworkGuard("wifi-A", metric)
        guard.observe("wifi-A", metric.copy(rssi = -70, linkSpeedMbps = 72, bssid = metric.bssid.lowercase()))
        guard.ensureValid()
    }

    @Test fun missing_or_redacted_initial_identity_is_rejected() {
        val cases = listOf(
            metric.copy(ssid = ""), metric.copy(ssid = "<unknown ssid>"),
            metric.copy(bssid = "02:00:00:00:00:00"), metric.copy(bssid = "00:00:00:00:00:00")
        )
        for (sample in cases) {
            assertThrows(IllegalStateException::class.java) { BenchmarkNetworkGuard("wifi-A", sample) }
        }
    }

    @Test fun offline_initial_metric_is_rejected() {
        assertThrows(IllegalStateException::class.java) {
            BenchmarkNetworkGuard("wifi-A", metric.copy(isConnected = false))
        }
    }

    @Test fun loss_network_switch_ssid_change_and_ap_roam_each_invalidate() {
        val cases = listOf(
            null to null, "mobile" to metric, "wifi-B" to metric,
            "wifi-A" to metric.copy(ssid = "B"),
            "wifi-A" to metric.copy(bssid = "AA:BB:CC:DD:EE:02"),
            "wifi-A" to metric.copy(isConnected = false),
            "wifi-A" to metric.copy(bssid = "02:00:00:00:00:00")
        )
        for ((network, sample) in cases) {
            val guard = BenchmarkNetworkGuard("wifi-A", metric)
            assertThrows(WifiNetworkChangedException::class.java) { guard.observe(network, sample) }
        }
    }

    @Test fun returning_to_original_ap_does_not_clear_failure_and_cleanup_runs_once() {
        var invalidations = 0
        val guard = BenchmarkNetworkGuard("wifi-A", metric) { invalidations++ }
        assertThrows(WifiNetworkChangedException::class.java) {
            guard.observe("wifi-A", metric.copy(bssid = "AA:BB:CC:DD:EE:02"))
        }
        assertThrows(WifiNetworkChangedException::class.java) { guard.observe("wifi-A", metric) }
        guard.invalidate("Repeated callback")
        assertEquals(1, invalidations)
    }

    @Test fun loss_callback_is_sticky_even_if_current_snapshot_looks_unchanged() {
        val guard = BenchmarkNetworkGuard("wifi-A", metric)
        guard.invalidate("Lost")
        assertThrows(WifiNetworkChangedException::class.java) { guard.observe("wifi-A", metric) }
    }

    @Test fun usecase_rejects_network_change_after_ping_and_closes_session() = runTest {
        val repository = FakeWifiRepository()
        repository.onProbe = { repository.networkId = "mobile-data" }
        val benchmark = PerformBenchmarkUseCase(repository, CalculateScoreUseCase(), StandardTestDispatcher(testScheduler))
        try {
            benchmark(UseCaseCategory.GAMING)
            fail("Mixed measurement must not be accepted")
        } catch (expected: WifiNetworkChangedException) {
            assertEquals(1, repository.closedSessions)
            assertEquals(0, repository.activeSessions)
        }
    }

    @Test fun usecase_rechecks_identity_after_scoring_before_accepting_result() = runTest {
        val repository = FakeWifiRepository()
        val benchmark = PerformBenchmarkUseCase(repository, CalculateScoreUseCase(), StandardTestDispatcher(testScheduler)) {
            repository.metric = repository.metric.copy(bssid = "AA:BB:CC:DD:EE:02")
            1234L
        }
        try {
            benchmark(UseCaseCategory.GAMING)
            fail("Change during result construction must be rejected")
        } catch (expected: WifiNetworkChangedException) {
            assertEquals(1, repository.closedSessions)
            assertTrue(repository.validationCalls >= 3)
        }
    }

    @Test fun transient_ap_change_during_probe_remains_invalid_after_return_to_a() = runTest {
        val repository = FakeWifiRepository()
        val original = repository.metric
        repository.onProbe = {
            repository.metric = original.copy(bssid = "AA:BB:CC:DD:EE:02")
            assertThrows(WifiNetworkChangedException::class.java) { repository.observeNetwork() }
            repository.metric = original
        }
        val benchmark = PerformBenchmarkUseCase(repository, CalculateScoreUseCase(), StandardTestDispatcher(testScheduler))
        try {
            benchmark(UseCaseCategory.GAMING)
            fail("Returning to A must not rescue mixed samples")
        } catch (expected: WifiNetworkChangedException) {
            assertEquals(1, repository.closedSessions)
        }
    }

    @Test fun accepted_result_keeps_captured_metric_and_network_id_and_closes_session() = runTest {
        val repository = FakeWifiRepository()
        repository.onProbe = { repository.metric = repository.metric.copy(rssi = -80, linkSpeedMbps = 6) }
        val benchmark = PerformBenchmarkUseCase(repository, CalculateScoreUseCase(), StandardTestDispatcher(testScheduler))
        val result = benchmark(UseCaseCategory.GAMING)
        assertEquals(-55, result.rawRssi)
        assertEquals(150, result.rawLinkSpeedMbps)
        assertEquals("wifi-network-A", result.measurement!!.networkId)
        assertEquals(1, repository.closedSessions)
    }

    @Test fun invalid_before_first_probe_never_probes_and_still_closes_session() = runTest {
        val repository = FakeWifiRepository()
        repository.onValidate = { repository.metric = repository.metric.copy(isConnected = false) }
        val benchmark = PerformBenchmarkUseCase(repository, CalculateScoreUseCase(), StandardTestDispatcher(testScheduler))
        try {
            benchmark(UseCaseCategory.GAMING)
            fail("Lost Wi-Fi must not be probed")
        } catch (expected: WifiNetworkChangedException) {
            assertEquals(0, repository.pingCalls)
            assertEquals(1, repository.closedSessions)
        }
    }
}
