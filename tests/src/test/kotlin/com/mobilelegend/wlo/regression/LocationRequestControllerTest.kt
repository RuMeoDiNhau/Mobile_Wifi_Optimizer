package com.mobilelegend.wlo.regression

import com.mobilelegend.wlo.data.sensor.*
import com.mobilelegend.wlo.domain.model.LocationConfidence
import org.junit.Assert.*
import org.junit.Test

/** Fake source and manually fired deadline only; no Android services or wall-clock waits. */
class LocationRequestControllerTest {
    private var now = 300_000_000_000L
    private val source = Source()
    private val deadline = Deadline()
    private val controller = LocationRequestController(source, deadline) { now }
    private val delivered = mutableListOf<UserGeoCoordinate>()
    private val status get() = controller.state.value.status
    private fun fix(ageMs: Long = 0) = LocationSample(37.42, -122.08, 1234L, now - ageMs * 1_000_000, 10f)
    private fun start(): Long {
        controller.setForeground(true)
        return requireNotNull(controller.request { delivered.add(it) })
    }
    private fun assertClean() {
        assertEquals(0, source.active)
        assertTrue(deadline.tasks.all { it.cancelled })
    }

    @Test fun background_request_does_not_read_or_register_a_source() {
        assertNull(controller.request { delivered.add(it) })
        assertEquals(0, source.reads)
        assertTrue(source.requests.isEmpty())
        assertTrue(delivered.isEmpty())
        assertEquals(LocationRequestStatus.IDLE, status)
    }

    @Test fun no_fix_times_out_after_one_ten_second_deadline_and_cleans_both_handles() {
        val id = start()
        assertEquals(10_000L, deadline.tasks.single().delay)
        assertEquals(LocationRequestState(id, LocationRequestStatus.REQUESTING), controller.state.value)
        assertFalse(delivered.single().isRealFix)
        deadline.tasks.single().fire()
        assertEquals(LocationRequestState(id, LocationRequestStatus.TIMED_OUT), controller.state.value)
        assertEquals(2, delivered.size)
        assertClean()
    }

    @Test fun valid_cached_fix_does_not_finish_the_new_request() {
        source.cached = listOf(fix(20_000))
        start()
        assertEquals(20_000L, delivered.single().ageMillis)
        assertEquals(LocationRequestStatus.REQUESTING, status)
        assertEquals(1, source.active)
        deadline.tasks.single().fire()
        assertEquals(LocationRequestStatus.TIMED_OUT, status)
        assertTrue(delivered.last().isRealFix)
        assertClean()
    }

    @Test fun stale_cache_is_labeled_and_never_usable_for_a_measurement() {
        source.cached = listOf(fix(MAX_LOCATION_AGE_MS + 1))
        start()
        assertEquals(LocationConfidence.STALE, delivered.single().confidence)
        assertNull(delivered.single().toMeasurementLocation())
        deadline.tasks.single().fire()
        assertEquals(LocationConfidence.STALE, delivered.last().confidence)
        assertClean()
    }

    @Test fun valid_callback_finishes_once_and_cleanup_precedes_terminal_delivery() {
        controller.setForeground(true)
        controller.request {
            if (controller.state.value.status == LocationRequestStatus.UPDATED) assertClean()
            delivered.add(it)
        }
        val listener = source.requests.single()
        listener.sample(fix())
        listener.sample(fix().copy(latitude = 1.0))
        listener.unavailable()
        deadline.tasks.single().fire()
        assertEquals(2, delivered.size)
        assertEquals(37.42, delivered.last().latitude, 0.0)
        assertEquals(LocationRequestStatus.UPDATED, status)
        assertEquals(1, listener.cancellations)
        assertClean()
    }

    @Test fun repeated_refresh_keeps_one_listener_and_rejects_old_callbacks_and_timers() {
        val ids = (1..10).map { start() }
        assertEquals(10, ids.distinct().size)
        assertEquals(1, source.maxActive)
        source.requests.dropLast(1).forEach { it.sample(fix()); it.unavailable() }
        deadline.tasks.dropLast(1).forEach { it.fire() }
        assertEquals(10, delivered.size)
        assertEquals(LocationRequestState(ids.last(), LocationRequestStatus.REQUESTING), controller.state.value)
        source.requests.last().sample(fix())
        assertEquals(11, delivered.size)
        assertClean()
    }

    @Test fun explicit_cancel_is_idempotent_and_late_callbacks_cannot_deliver() {
        start()
        repeat(3) { controller.cancel() }
        source.requests.single().sample(fix())
        deadline.tasks.single().fire()
        assertEquals(1, delivered.size)
        assertEquals(1, source.requests.single().cancellations)
        assertEquals(LocationRequestStatus.CANCELLED, status)
        assertClean()
    }

    @Test fun stopping_owner_cancels_and_blocks_requests_until_foreground_returns() {
        start()
        controller.setForeground(false)
        assertNull(controller.request { delivered.add(it) })
        source.requests.single().sample(fix())
        assertEquals(1, delivered.size)
        assertClean()
        start()
        assertEquals(1, source.active)
        source.requests.last().sample(fix())
        assertEquals(LocationRequestStatus.UPDATED, status)
        assertClean()
    }

    @Test fun cancellation_during_initial_delivery_never_registers() {
        controller.setForeground(true)
        controller.request { controller.cancel() }
        assertTrue(source.requests.isEmpty())
        assertTrue(deadline.tasks.isEmpty())
        assertEquals(LocationRequestStatus.CANCELLED, status)
    }

    @Test fun replacement_during_initial_delivery_is_the_only_registered_request() {
        controller.setForeground(true)
        var newerId: Long? = null
        val olderId = controller.request { newerId = controller.request { delivered.add(it) } }
        assertNotEquals(olderId, newerId)
        assertEquals(newerId, controller.state.value.id)
        assertEquals(1, source.requests.size)
        source.requests.single().sample(fix())
        assertEquals(2, delivered.size)
        assertClean()
    }

    @Test fun synchronous_valid_callback_waits_for_registration_cleanup_handle() {
        source.onRegister = { it.sample(fix()) }
        controller.setForeground(true)
        controller.request {
            if (controller.state.value.status == LocationRequestStatus.UPDATED) assertClean()
            delivered.add(it)
        }
        assertEquals(LocationRequestStatus.UPDATED, status)
        assertEquals(2, delivered.size)
        assertClean()
    }

    @Test fun synchronous_unavailability_cleans_listener_and_does_not_accept_later_sample() {
        source.onRegister = { it.unavailable(); it.sample(fix()) }
        start()
        assertEquals(LocationRequestStatus.UNAVAILABLE, status)
        assertFalse(delivered.last().isRealFix)
        assertClean()
    }

    @Test fun stop_during_registration_cleans_the_handle_returned_after_stop() {
        source.onRegister = { controller.setForeground(false) }
        start()
        assertEquals(LocationRequestStatus.CANCELLED, status)
        assertEquals(1, delivered.size)
        assertClean()
    }

    @Test fun registration_failure_cancels_deadline_and_allows_retry() {
        source.registrationError = SecurityException("denied")
        start()
        assertEquals(LocationRequestStatus.ERROR, status)
        assertClean()
        source.registrationError = null
        start()
        source.requests.single().sample(fix())
        assertEquals(LocationRequestStatus.UPDATED, status)
        assertClean()
    }

    @Test fun deadline_registration_failure_never_opens_a_location_listener() {
        deadline.fail = true
        start()
        assertEquals(LocationRequestStatus.ERROR, status)
        assertTrue(source.requests.isEmpty())
        assertClean()
    }

    @Test fun missing_permission_or_disabled_service_does_not_register() {
        source.available = false
        start()
        assertEquals(LocationRequestStatus.UNAVAILABLE, status)
        assertTrue(source.requests.isEmpty())
        assertTrue(delivered.none { it.isRealFix })
        assertClean()
    }

    @Test fun permission_loss_before_sample_rejects_sample_and_cleans_request() {
        start()
        source.available = false
        source.requests.single().sample(fix())
        assertEquals(LocationRequestStatus.UNAVAILABLE, status)
        assertNull(delivered.last().toMeasurementLocation())
        assertClean()
    }

    @Test fun provider_disabled_event_ends_request_without_waiting_for_timeout() {
        start()
        source.available = false
        source.requests.single().unavailable()
        assertEquals(LocationRequestStatus.UNAVAILABLE, status)
        assertClean()
    }

    @Test fun permission_check_throwing_in_asynchronous_callback_is_contained_and_cleaned() {
        start()
        source.precisionError = SecurityException("permission revoked")
        source.requests.single().sample(fix())
        assertEquals(LocationRequestStatus.ERROR, status)
        assertNull(delivered.last().toMeasurementLocation())
        assertClean()
    }

    @Test fun invalid_mock_and_stale_callbacks_keep_waiting_until_a_usable_fix() {
        start()
        val listener = source.requests.single()
        listOf(fix().copy(latitude = Double.NaN), fix().copy(isMock = true),
            fix(MAX_LOCATION_AGE_MS + 1), fix().copy(accuracyMeters = null)).forEach {
            listener.sample(it)
            assertEquals(LocationRequestStatus.REQUESTING, status)
            assertEquals(1, source.active)
        }
        listener.sample(fix())
        assertEquals(LocationRequestStatus.UPDATED, status)
        assertEquals(2, delivered.size)
        assertClean()
    }

    @Test fun delivered_raw_sample_ages_even_if_android_cache_stays_empty() {
        start()
        source.requests.single().sample(fix())
        now += MAX_LOCATION_AGE_MS * 1_000_000
        assertTrue(controller.currentLocation().isRealFix)
        now += 1_000_000
        assertEquals(LocationConfidence.STALE, controller.currentLocation().confidence)
        assertNull(controller.currentLocation().toMeasurementLocation())
        assertClean()
    }

    @Test fun coarse_permission_is_reassessed_on_cached_delivered_sample() {
        start()
        source.requests.single().sample(fix())
        source.precise = false
        val location = controller.currentLocation()
        assertEquals(LocationConfidence.APPROXIMATE, location.confidence)
        assertEquals(10f, location.accuracyMeters)
        assertTrue(location.isRealFix)
    }

    @Test fun replacement_started_by_cleanup_is_not_overwritten_by_outer_request() {
        start()
        source.onCancel = { controller.request { delivered.add(it) } }
        assertNull(controller.request { fail("Superseded request delivered") })
        assertEquals(LocationRequestStatus.REQUESTING, status)
        assertEquals(1, source.active)
        assertEquals(1, source.maxActive)
        source.requests.last().sample(fix())
        assertEquals(LocationRequestStatus.UPDATED, status)
        assertClean()
    }

    @Test fun cancellation_does_not_overwrite_request_started_by_cleanup() {
        start()
        source.onCancel = { controller.request { delivered.add(it) } }
        controller.cancel()
        assertEquals(LocationRequestStatus.REQUESTING, status)
        assertEquals(1, source.active)
        controller.cancel()
        assertClean()
    }

    @Test fun terminal_cleanup_replacement_suppresses_old_terminal_delivery() {
        start()
        source.onCancel = { controller.request { delivered.add(it) } }
        source.requests.first().sample(fix())
        assertEquals(LocationRequestStatus.REQUESTING, status)
        assertEquals(2, delivered.size) // Initial deliveries only, one for each generation.
        assertEquals(1, source.active)
        source.requests.last().sample(fix())
        assertEquals(3, delivered.size)
        assertClean()
    }

    private class Source : LocationSource {
        var available = true
        var precise = true
        var reads = 0
        var cached = emptyList<LocationSample>()
        var registrationError: Exception? = null
        var precisionError: Exception? = null
        var onRegister: (Request) -> Unit = {}
        var onCancel: (() -> Unit)? = null
        var active = 0
        var maxActive = 0
        val requests = mutableListOf<Request>()
        override fun canLocate(): Boolean { reads++; return available }
        override fun hasPrecisePermission(): Boolean { precisionError?.let { throw it }; return precise }
        override fun lastKnownSamples() = cached
        override fun request(onSample: (LocationSample) -> Unit, onUnavailable: () -> Unit): LocationCleanup {
            registrationError?.let { throw it }
            val request = Request(onSample, onUnavailable)
            requests.add(request)
            active++
            maxActive = maxOf(maxActive, active)
            onRegister(request)
            return LocationCleanup {
                request.cancellations++
                if (request.cancellations == 1) active--
                val callback = onCancel.also { onCancel = null }
                callback?.invoke()
            }
        }
        class Request(val sample: (LocationSample) -> Unit, val unavailable: () -> Unit) {
            var cancellations = 0
        }
    }

    private class Deadline : LocationDeadline {
        var fail = false
        val tasks = mutableListOf<Task>()
        override fun schedule(delayMillis: Long, action: () -> Unit): LocationCleanup {
            if (fail) error("Cannot schedule")
            val task = Task(delayMillis, action)
            tasks.add(task)
            return LocationCleanup { task.cancelled = true }
        }
        // fire intentionally allows a callback already queued before cancellation.
        class Task(val delay: Long, val fire: () -> Unit) { var cancelled = false }
    }
}
