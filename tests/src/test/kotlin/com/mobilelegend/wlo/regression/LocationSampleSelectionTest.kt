package com.mobilelegend.wlo.regression

import com.mobilelegend.wlo.data.sensor.*
import com.mobilelegend.wlo.domain.model.LocationConfidence
import org.junit.Assert.*
import org.junit.Test

class LocationSampleSelectionTest {
    private val now = 300_000_000_000L
    private fun sample(age: Long, accuracy: Float = 10f) =
        LocationSample(37.42, -122.08, 1234L, now - age * 1_000_000, accuracy)
    private fun select(vararg samples: LocationSample) = selectLocationSample(samples.toList(), now, true)

    @Test fun newest_usable_sample_wins_over_older_more_accurate_sample() {
        val chosen = select(sample(10_000, 1f), sample(1_000, 80f))
        assertEquals(1_000L, chosen.ageMillis)
        assertEquals(80f, chosen.accuracyMeters)
    }

    @Test fun newer_mock_invalid_or_future_sample_cannot_hide_valid_recent_fix() {
        val valid = sample(20_000)
        for (bad in listOf(sample(0).copy(isMock = true), sample(0).copy(latitude = Double.NaN),
            sample(0).copy(accuracyMeters = null), sample(-1))) {
            val chosen = select(valid, bad)
            assertTrue(chosen.isRealFix)
            assertEquals(20_000L, chosen.ageMillis)
        }
    }

    @Test fun equal_timestamps_prefer_better_accuracy_regardless_of_input_order() {
        val accurate = sample(1000, 15f)
        val broad = sample(1000, 500f)
        assertEquals(15f, select(accurate, broad).accuracyMeters)
        assertEquals(15f, select(broad, accurate).accuracyMeters)
    }

    @Test fun stale_only_input_stays_stale_and_empty_input_has_no_real_location() {
        assertEquals(LocationConfidence.STALE, select(sample(MAX_LOCATION_AGE_MS + 1)).confidence)
        assertNull(select().toMeasurementLocation())
        assertEquals(LocationConfidence.UNAVAILABLE, select().confidence)
    }

    @Test fun monotonic_age_ignores_wall_clock_capture_time() {
        val a = sample(30_000)
        val b = a.copy(capturedAtMillis = Long.MAX_VALUE)
        assertEquals(a.assess(now, true).ageMillis, b.assess(now, true).ageMillis)
        assertEquals(LocationConfidence.RECENT, b.assess(now, true).confidence)
        assertEquals("ghi nhận 30 giây trước", b.assess(now, true).ageText)
    }

    @Test fun missing_or_future_monotonic_time_cannot_be_used_even_with_good_accuracy() {
        for (time in listOf(0L, -1L, now + 1)) {
            val location = sample(0).copy(elapsedRealtimeNanos = time).assess(now, true)
            assertFalse(location.isRealFix)
            assertNull(location.toMeasurementLocation())
        }
    }

    @Test fun age_and_accuracy_boundaries_keep_precise_approximate_and_stale_distinct() {
        assertEquals(LocationConfidence.RECENT, select(sample(MAX_LOCATION_AGE_MS, 100f)).confidence)
        assertEquals(LocationConfidence.APPROXIMATE, select(sample(0, 100.1f)).confidence)
        assertEquals(LocationConfidence.STALE, select(sample(MAX_LOCATION_AGE_MS + 1)).confidence)
        assertEquals(LocationConfidence.APPROXIMATE, sample(0).assess(now, false).confidence)
    }

    @Test fun genuine_mountain_view_fix_retains_exact_coordinates_and_original_timestamp() {
        val selected = select(sample(0))
        assertEquals(37.42, selected.latitude, 0.0)
        assertEquals(-122.08, selected.longitude, 0.0)
        assertEquals(1234L, selected.capturedAtMillis)
        assertEquals("vừa ghi nhận", selected.ageText)
        assertNotNull(selected.toMeasurementLocation())
    }
}
