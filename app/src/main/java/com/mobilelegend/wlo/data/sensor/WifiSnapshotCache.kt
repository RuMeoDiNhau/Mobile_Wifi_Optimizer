package com.mobilelegend.wlo.data.sensor

import com.mobilelegend.wlo.data.model.WifiMetric

/** Callback snapshots are usable only for the same default network and registration. */
internal class WifiSnapshotCache {
    private var generation = 0L
    private var networkId: String? = null
    private var metric: WifiMetric? = null

    @Synchronized fun reset(): Long {
        generation++
        networkId = null
        metric = null
        return generation
    }
    @Synchronized fun available(token: Long, id: String) {
        if (token != generation) return
        if (networkId != id) metric = null
        networkId = id
    }
    @Synchronized fun update(token: Long, id: String, value: WifiMetric) {
        if (token == generation && id == networkId) metric = value
    }
    @Synchronized fun lost(token: Long, id: String) {
        if (token == generation && id == networkId) { networkId = null; metric = null }
    }
    @Synchronized fun read(id: String?): WifiMetric? = if (id != null && id == networkId) metric else null
}
