package com.mobilelegend.wlo.domain.repository

import com.mobilelegend.wlo.data.model.WifiMetric
import com.mobilelegend.wlo.domain.model.WifiAccessState
import kotlinx.coroutines.flow.Flow

/**
 * Giao diện trừu tượng hóa toàn bộ dữ liệu cảm biến Wi-Fi và mạng từ Data Layer.
 */
interface WifiRepository {

    /**
     * Lấy luồng dữ liệu sóng Wi-Fi phát định kỳ mỗi giây một lần.
     */
    fun getLiveWifiStream(): Flow<WifiMetric>

    /**
     * Lấy thông số Wi-Fi tức thời ở thời điểm hiện tại.
     */
    fun getCurrentWifiMetric(): WifiMetric

    /**
     * Giữ cùng một mạng Wi-Fi cho toàn bộ phép đo; luôn dọn tài nguyên sau block.
     */
    suspend fun <T> withBenchmarkSession(block: suspend (WifiBenchmarkSession) -> T): T

    /**
     * Kiểm tra trạng thái kết nối Wi-Fi.
     */
    fun isWifiConnected(): Boolean

    /**
     * Kiểm tra quyền và dịch vụ GPS đã sẵn sàng hay chưa.
     */
    fun getWifiAccessState(): WifiAccessState

    /** Re-register location-bearing observations after returning to the foreground/settings. */
    fun refreshWifiObservation() {}
}
