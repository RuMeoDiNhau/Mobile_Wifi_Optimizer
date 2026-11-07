package com.mobilelegend.wlo.data.sensor

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

fun interface LocationCleanup { fun cancel() }
fun interface LocationDeadline {
    fun schedule(delayMillis: Long, action: () -> Unit): LocationCleanup
}

interface LocationSource {
    fun canLocate(): Boolean
    fun hasPrecisePermission(): Boolean
    fun lastKnownSamples(): List<LocationSample>
    fun request(onSample: (LocationSample) -> Unit, onUnavailable: () -> Unit): LocationCleanup
}

enum class LocationRequestStatus(val message: String) {
    IDLE("Chưa yêu cầu cập nhật vị trí"),
    REQUESTING("Đang tìm vị trí mới..."),
    UPDATED("Đã nhận vị trí hợp lệ"),
    TIMED_OUT("Hết thời gian tìm vị trí; có thể thử lại"),
    UNAVAILABLE("Chưa có quyền hoặc nguồn định vị khả dụng"),
    ERROR("Không thể yêu cầu vị trí; hãy kiểm tra quyền và thử lại"),
    CANCELLED("Đã dừng yêu cầu vị trí")
}

data class LocationRequestState(
    val id: Long = 0,
    val status: LocationRequestStatus = LocationRequestStatus.IDLE
)

/** Confined to the main thread, like the Android listener and deadline adapter.
 * Owns exactly one request; cleanup completes before delivering a terminal callback.
 */
class LocationRequestController(
    private val source: LocationSource,
    private val deadline: LocationDeadline,
    private val nowNanos: () -> Long
) {
    private class Pending(val id: Long, val onUpdated: (UserGeoCoordinate) -> Unit) {
        var listener: LocationCleanup? = null
        var timeout: LocationCleanup? = null
        var registering = false
        var completion: LocationRequestStatus? = null
    }
    private var foreground = false
    private var revision = 0L
    private var pending: Pending? = null
    private var lastDelivered: LocationSample? = null
    private val _state = MutableStateFlow(LocationRequestState())
    val state: StateFlow<LocationRequestState> = _state.asStateFlow()

    fun currentLocation(): UserGeoCoordinate = try {
        if (!source.canLocate()) UserGeoCoordinate()
        else selectLocationSample(source.lastKnownSamples() + listOfNotNull(lastDelivered),
            nowNanos(), source.hasPrecisePermission())
    } catch (_: Exception) {
        UserGeoCoordinate()
    }

    fun setForeground(active: Boolean) {
        foreground = active
        if (!active) cancel()
    }

    fun request(onUpdated: (UserGeoCoordinate) -> Unit): Long? {
        val id = cancelPending()
        // Cleanup may synchronously start a newer request or stop the owner.
        if (!foreground || revision != id) return null
        val request = Pending(id, onUpdated)
        pending = request
        _state.value = LocationRequestState(id, LocationRequestStatus.REQUESTING)
        onUpdated(currentLocation())
        // Initial delivery may synchronously cancel, dispose or start another request.
        if (pending !== request) return id
        try {
            if (!source.canLocate()) {
                finish(request, LocationRequestStatus.UNAVAILABLE)
                return id
            }
            val timer = deadline.schedule(TIMEOUT_MS) { finish(request, LocationRequestStatus.TIMED_OUT) }
            if (pending !== request) { timer.cancel(); return id }
            request.timeout = timer
            request.registering = true
            try {
                val listener = source.request(onSample = sample@{ sample ->
                    if (pending !== request || request.completion != null) return@sample
                    try {
                        if (!source.canLocate()) {
                            finish(request, LocationRequestStatus.UNAVAILABLE)
                        } else if (sample.assess(nowNanos(), source.hasPrecisePermission()).isRealFix) {
                            lastDelivered = sample
                            finish(request, LocationRequestStatus.UPDATED)
                        }
                    } catch (_: Exception) {
                        finish(request, LocationRequestStatus.ERROR)
                    }
                    // Stale, mock and malformed callbacks do not end a live request.
                }, onUnavailable = { finish(request, LocationRequestStatus.UNAVAILABLE) })
                request.listener = listener
            } finally {
                request.registering = false
            }
            if (pending !== request) cleanup(request)
            else request.completion?.let { finish(request, it) }
        } catch (_: Exception) {
            finish(request, LocationRequestStatus.ERROR)
        }
        return id
    }

    private fun finish(request: Pending, result: LocationRequestStatus) {
        if (pending !== request) return
        // A platform/fake source can deliver during registration. Wait for its cleanup
        // handle before publishing, so a reentrant request cannot leak the old listener.
        if (request.registering) {
            if (request.completion == null) request.completion = result
            return
        }
        pending = null
        cleanup(request)
        if (revision != request.id || !foreground) return
        val location = currentLocation()
        if (revision != request.id || !foreground) return
        _state.value = LocationRequestState(request.id, result)
        request.onUpdated(location)
    }

    fun cancel() {
        cancelPending()
    }

    private fun cancelPending(): Long {
        val id = ++revision
        val request = pending
        pending = null
        if (request != null) {
            cleanup(request)
            if (revision == id) _state.value = LocationRequestState(request.id, LocationRequestStatus.CANCELLED)
        }
        return id
    }

    private fun cleanup(request: Pending) {
        val timer = request.timeout.also { request.timeout = null }
        val listener = request.listener.also { request.listener = null }
        runCatching { timer?.cancel() }
        runCatching { listener?.cancel() }
    }

    companion object { const val TIMEOUT_MS = 10_000L }
}
