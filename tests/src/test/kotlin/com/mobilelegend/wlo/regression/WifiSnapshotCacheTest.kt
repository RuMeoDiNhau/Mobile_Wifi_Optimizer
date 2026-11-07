package com.mobilelegend.wlo.regression

import com.mobilelegend.wlo.data.sensor.WifiSnapshotCache
import org.junit.Assert.*
import org.junit.Test

class WifiSnapshotCacheTest {
    @Test fun callback_identity_is_read_only_for_matching_default_network() {
        val cache = WifiSnapshotCache()
        val token = cache.reset()
        val metric = FakeWifiRepository().metric
        cache.available(token, "A")
        assertNull(cache.read("A"))
        cache.update(token, "A", metric)
        assertEquals(metric, cache.read("A"))
        assertNull(cache.read("B"))
        assertNull(cache.read(null))
    }

    @Test fun switching_network_clears_old_sample_until_new_callback_arrives() {
        val cache = WifiSnapshotCache()
        val token = cache.reset()
        val metric = FakeWifiRepository().metric
        cache.available(token, "A")
        cache.update(token, "A", metric)
        cache.available(token, "B")
        cache.update(token, "A", metric)
        assertNull(cache.read("A"))
        assertNull(cache.read("B"))
        val next = metric.copy(ssid = "B")
        cache.update(token, "B", next)
        assertEquals(next, cache.read("B"))
    }

    @Test fun late_lost_callback_for_old_network_does_not_clear_new_network() {
        val cache = WifiSnapshotCache()
        val token = cache.reset()
        val metric = FakeWifiRepository().metric
        cache.available(token, "B")
        cache.update(token, "B", metric)
        cache.lost(token, "A")
        assertEquals(metric, cache.read("B"))
        cache.lost(token, "B")
        assertNull(cache.read("B"))
    }

    @Test fun permission_transition_rejects_callbacks_from_old_registration() {
        val cache = WifiSnapshotCache()
        val old = cache.reset()
        val metric = FakeWifiRepository().metric
        cache.available(old, "A")
        cache.update(old, "A", metric)
        val current = cache.reset()
        cache.available(old, "A")
        cache.update(old, "A", metric)
        assertNull(cache.read("A"))
        cache.available(current, "A")
        assertNull(cache.read("A"))
        cache.update(current, "A", metric)
        cache.lost(old, "A")
        assertEquals(metric, cache.read("A"))
    }

    @Test fun redacted_callback_replaces_old_identity_instead_of_reusing_it() {
        val cache = WifiSnapshotCache()
        val token = cache.reset()
        val metric = FakeWifiRepository().metric
        cache.available(token, "A")
        cache.update(token, "A", metric)
        val redacted = metric.copy(ssid = "<unknown ssid>", bssid = "02:00:00:00:00:00")
        cache.update(token, "A", redacted)
        assertEquals(redacted, cache.read("A"))
    }
}
