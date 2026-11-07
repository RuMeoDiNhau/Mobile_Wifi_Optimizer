package com.mobilelegend.wlo.regression

import com.mobilelegend.wlo.core.network.PingTester
import com.mobilelegend.wlo.core.network.WifiNetworkChangedException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import java.net.Socket
import java.net.SocketAddress
import java.net.SocketException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** Socket.connect is overridden everywhere: these tests never perform network I/O. */
@OptIn(ExperimentalCoroutinesApi::class)
class PingCancellationTest {
    private class FakeSocket(private val connectAction: () -> Unit = {}) : Socket() {
        val connectCalls = AtomicInteger()
        val closeCalls = AtomicInteger()
        override fun connect(endpoint: SocketAddress, timeout: Int) {
            connectCalls.incrementAndGet()
            connectAction()
        }
        override fun close() { closeCalls.incrementAndGet() }
    }

    @Test fun batch_uses_supplied_sockets_and_closes_every_success() = runTest {
        val sockets = mutableListOf<FakeSocket>()
        var clock = 0L
        val tester = PingTester(dispatcher = StandardTestDispatcher(testScheduler), nanoTime = { clock.also { clock += 2_000_000 } })
        val result = tester.batchPing(socketFactory = { FakeSocket().also { sockets.add(it) } })
        assertEquals(3, sockets.size)
        assertTrue(sockets.all { it.connectCalls.get() == 1 && it.closeCalls.get() >= 1 })
        assertEquals(2L, result.latencyMs)
        assertEquals(0, result.packetLossPercent)
        assertTrue(result.isSuccess)
    }

    @Test fun connect_errors_close_all_sockets_and_report_failed_probes() = runTest {
        val sockets = mutableListOf<FakeSocket>()
        val tester = PingTester(dispatcher = StandardTestDispatcher(testScheduler))
        val result = tester.batchPing(socketFactory = {
            FakeSocket { throw SocketException("Test failure") }.also { sockets.add(it) }
        })
        assertEquals(3, sockets.size)
        assertTrue(sockets.all { it.closeCalls.get() >= 1 })
        assertFalse(result.isSuccess)
        assertEquals(100, result.packetLossPercent)
    }

    @Test fun invalidation_after_connect_rejects_batch_and_stops_remaining_probes() = runTest {
        var valid = true
        val sockets = mutableListOf<FakeSocket>()
        val tester = PingTester(dispatcher = StandardTestDispatcher(testScheduler))
        try {
            tester.batchPing(
                socketFactory = { FakeSocket { valid = false }.also { sockets.add(it) } },
                validateConnection = { if (!valid) throw WifiNetworkChangedException("Changed AP") }
            )
            fail("Mixed batch must be rejected")
        } catch (expected: WifiNetworkChangedException) {
            assertEquals(1, sockets.size)
            assertTrue(sockets.single().closeCalls.get() >= 1)
        }
    }

    @Test fun invalid_connection_is_rejected_before_socket_creation() = runTest {
        var creations = 0
        val tester = PingTester(dispatcher = StandardTestDispatcher(testScheduler))
        try {
            tester.batchPing(
                socketFactory = { creations++; FakeSocket() },
                validateConnection = { throw WifiNetworkChangedException("Lost") }
            )
            fail("Must not create a socket on an invalid connection")
        } catch (expected: WifiNetworkChangedException) {
            assertEquals(0, creations)
        }
    }

    @Test fun bound_socket_factory_failure_is_not_converted_to_timeout() = runTest {
        val tester = PingTester(dispatcher = StandardTestDispatcher(testScheduler))
        try {
            tester.singlePing(socketFactory = { throw WifiNetworkChangedException("Lost before bind") })
            fail("Binding failure must propagate")
        } catch (expected: WifiNetworkChangedException) {
            assertEquals("Lost before bind", expected.message)
        }
    }

    @Test fun cancellation_unblocks_fake_connect_by_closing_socket() = runBlocking {
        val entered = CountDownLatch(1)
        val closed = CountDownLatch(1)
        val closeCalls = AtomicInteger()
        val socket = object : Socket() {
            override fun connect(endpoint: SocketAddress, timeout: Int) {
                entered.countDown()
                check(closed.await(3, TimeUnit.SECONDS)) { "Cancellation did not close socket" }
                throw SocketException("Closed during fake connect")
            }
            override fun close() {
                closeCalls.incrementAndGet()
                closed.countDown()
            }
        }
        val pending = async(Dispatchers.Default) { PingTester().singlePing(socketFactory = { socket }) }
        try {
            assertTrue(withContext(Dispatchers.IO) { entered.await(3, TimeUnit.SECONDS) })
            pending.cancel()
            withTimeout(3000) { pending.join() }
            assertTrue(pending.isCancelled)
            assertTrue(closeCalls.get() >= 1)
            assertEquals(0L, closed.count)
        } finally {
            socket.close()
            pending.cancel()
        }
    }

    @Test fun cancellation_just_before_connect_returns_cannot_publish_success() = runTest {
        val tester = PingTester(dispatcher = StandardTestDispatcher(testScheduler))
        lateinit var job: Job
        var published = false
        val socket = FakeSocket { job.cancel() }
        job = launch(start = CoroutineStart.LAZY) {
            tester.singlePing(socketFactory = { socket })
            published = true
        }
        job.start()
        job.join()
        assertTrue(job.isCancelled)
        assertFalse(published)
        assertTrue(socket.closeCalls.get() >= 1)
    }

    @Test fun nonpositive_packet_count_is_rejected_without_creating_socket() = runTest {
        var creations = 0
        val tester = PingTester(dispatcher = StandardTestDispatcher(testScheduler))
        for (count in listOf(0, -1)) {
            try {
                tester.batchPing(packetsCount = count, socketFactory = { creations++; FakeSocket() })
                fail("Invalid packet count must fail")
            } catch (expected: IllegalArgumentException) {
                assertEquals(0, creations)
            }
        }
    }
}
