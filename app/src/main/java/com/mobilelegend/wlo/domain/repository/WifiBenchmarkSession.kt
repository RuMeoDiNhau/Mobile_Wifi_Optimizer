package com.mobilelegend.wlo.domain.repository

import com.mobilelegend.wlo.data.model.PingResult
import com.mobilelegend.wlo.data.model.WifiMetric

/** A single captured Wi-Fi connection. The repository owns its lifetime. */
interface WifiBenchmarkSession {
    val networkId: String
    val initialMetric: WifiMetric
    suspend fun measurePing(packetsCount: Int): PingResult
    fun ensureValid()
}
