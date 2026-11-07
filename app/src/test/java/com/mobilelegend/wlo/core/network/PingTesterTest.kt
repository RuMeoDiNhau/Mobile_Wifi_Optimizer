package com.mobilelegend.wlo.core.network

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit Test kiểm thử cơ chế đo độ trễ mạng qua Socket của PingTester.
 */
class PingTesterTest {

    private lateinit var pingTester: PingTester

    @Before
    fun setUp() {
        pingTester = PingTester(
            defaultHost = "1.1.1.1",
            defaultPort = 53,
            timeoutMs = 1000
        )
    }

    @Test
    fun single_ping_to_invalid_host_should_fail_gracefully() = runBlocking {
        // Ping tới IP không thể định tuyến / không tồn tại
        val invalidHost = "192.0.2.1" // TEST-NET-1 (RFC 5737 - unroutable)
        val result = pingTester.singlePing(host = invalidHost, port = 80, timeout = 500)

        assertFalse("Kết nối tới host chết phải trả về isSuccess = false", result.isSuccess)
        assertEquals(100, result.tcpFailurePercent)
        assertTrue("Latency phải xấp xỉ timeout", result.latencyMs >= 400)
    }

    @Test
    fun batch_ping_should_compute_correct_packet_loss_on_failure() = runBlocking {
        val invalidHost = "192.0.2.1"
        val result = pingTester.batchPing(packetsCount = 2, host = invalidHost, port = 80, timeout = 300)

        assertFalse(result.isSuccess)
        assertEquals(100, result.tcpFailurePercent)
    }
}
