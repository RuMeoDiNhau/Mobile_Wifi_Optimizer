package com.mobilelegend.wlo.domain.repository

import com.mobilelegend.wlo.data.model.PingResult
import com.mobilelegend.wlo.data.model.WifiMetric
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
     * Thực hiện chuỗi đo độ trễ Ping (3 gói tin) bất đồng bộ.
     */
    suspend fun measurePing(packetsCount: Int = 3): PingResult

    /**
     * Kiểm tra trạng thái kết nối Wi-Fi.
     */
    fun isWifiConnected(): Boolean

    /**
     * Kiểm tra quyền và dịch vụ GPS đã sẵn sàng hay chưa.
     */
    fun isReadyForScanning(): Boolean
}
