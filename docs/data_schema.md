# DATA SCHEMA / MODEL REPRESENTATIONS

Vì ứng dụng không sử dụng Database lưu trữ vĩnh viễn (No CRUD) trong giai đoạn MVP, Schema ở đây định nghĩa các **Kotlin Data Classes** sống trong bộ nhớ (RAM) để luân chuyển giữa Hardware Sensor -> ViewModel -> UI.

## 1. Hardware Data Models (Lớp Dữ liệu thô)

```kotlin
// Chứa thông tin sóng thu thập từ WifiManager
data class WifiMetric(
    val ssid: String,           // Tên mạng (VD: "KTX_KhuA")
    val bssid: String,          // Địa chỉ MAC của Router (VD: "00:14:22:01:23:45")
    val rssi: Int,              // Cường độ tín hiệu dBm (VD: -55)
    val linkSpeed: Int,         // Tốc độ kết nối Mbps (VD: 144)
    val frequency: Int          // Tần số MHz (VD: 2412 cho 2.4GHz, 5180 cho 5GHz)
)

// Chứa kết quả đo độ trễ mạng
data class PingResult(
    val latencyMs: Long,        // Thời gian phản hồi (ms)
    val packetLossPercent: Int, // Tỷ lệ rớt gói (0-100%)
    val isSuccess: Boolean      // Ping có thành công hay timeout
)
```

## 2. Business Logic Models (Lớp Nghiệp vụ & Đánh giá)

```kotlin
// Định nghĩa các loại nhu cầu của sinh viên
enum class UseCaseCategory {
    GAMING,         // Cần Ping thấp, Packet Loss = 0
    VIDEO_CALL,     // Cần RSSI ổn định, Ping vừa phải
    STUDY_DOWNLOAD  // Cần LinkSpeed cao, Ping không quan trọng
}

// Cấu hình trọng số cho thuật toán tính điểm
data class UseCaseWeight(
    val pingWeight: Float,      // % ảnh hưởng của Ping (VD: 0.6 cho Gaming)
    val rssiWeight: Float,      // % ảnh hưởng của Sóng (VD: 0.3 cho Gaming)
    val speedWeight: Float      // % ảnh hưởng của Tốc độ (VD: 0.1 cho Gaming)
)

// Dữ liệu cuối cùng hiển thị lên màn hình Kết Quả
data class LocationScore(
    val finalScore: Int,        // Điểm 0 - 100
    val category: UseCaseCategory,
    val summaryText: String,    // Lời khuyên ngắn (VD: "Tuyệt vời để leo rank")
    val isRecommended: Boolean  // Có đạt chuẩn (Vùng xanh) hay không
)
```