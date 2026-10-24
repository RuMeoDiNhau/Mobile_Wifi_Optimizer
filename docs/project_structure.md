# KHUNG DỰ ÁN (PROJECT STRUCTURE) - WI-FI LOCATION OPTIMIZER

**Tech Stack:** 
- Ngôn ngữ: Kotlin
- UI Framework: Jetpack Compose (Modern Android UI)
- Kiến trúc: MVVM (Model-View-ViewModel) + Clean Architecture cơ bản
- Bất đồng bộ: Kotlin Coroutines & Flow (Cực kỳ quan trọng để bắt tín hiệu Wi-Fi liên tục mà không đơ UI)

---

## 1. Cấu trúc thư mục (Directory Tree)

Dưới thư mục `app/src/main/java/com/mobilelegend/wlo/`:

```text
com.mobilelegend.wlo
│
├── MainActivity.kt                 # Entry point của ứng dụng, quản lý Navigation cơ bản
│
├── core/                           # Các tiện ích dùng chung (Utilities)
│   ├── network/                    # Logic đo Ping ICMP và kiểm tra kết nối mạng
│   ├── permissions/                # Trình xử lý xin quyền (Location, Wi-Fi State)
│   └── utils/                      # Constants, Extensions (chuyển đổi dBm sang % vạch sóng...)
│
├── data/                           # Lớp Dữ liệu (Data Layer - Xử lý phần cứng)
│   ├── model/                      # Data classes (WifiMetric, PingResult, ...)
│   ├── sensor/                     # Wrapper tương tác trực tiếp với API của thiết bị
│   │   └── WifiScanner.kt          # Đọc WifiManager.connectionInfo.rssi và linkSpeed
│   └── repository_impl/            # Triển khai thực tế của các interfaces
│       └── WifiRepositoryImpl.kt
│
├── domain/                         # Lớp Nghiệp vụ (Business Logic - Trái tim của app)
│   ├── model/                      # Các model đã xử lý để view dùng (LocationScore, UseCaseType)
│   ├── repository/                 # Interfaces (WifiRepository)
│   └── usecase/                    # Các thuật toán tính điểm và xếp hạng (Scoring Engine)
│       ├── CalculateScoreUseCase.kt# Chuyển Ping, RSSI -> Điểm Gaming, Study, Call
│       └── StreamLiveWifiUseCase.kt# Lấy dữ liệu Wi-Fi mỗi giây một lần (Polling)
│
└── presentation/                   # Lớp Giao diện (UI Layer - Jetpack Compose)
    ├── theme/                      # Định nghĩa Color, Typography, Shape cho app
    ├── components/                 # Các UI tĩnh dùng chung (Nút bấm, Biểu đồ Gauge, Radar...)
    │
    ├── live_radar/                 # [TÍNH NĂNG MỚI] Màn hình cầm máy đi dò liên tục
    │   ├── LiveRadarScreen.kt
    │   └── LiveRadarViewModel.kt   # Lắng nghe StreamLiveWifiUseCase, update UI 60fps
    │
    ├── session_measure/            # Màn hình lưu các điểm đo (A, B, C)
    │   ├── MeasureScreen.kt
    │   └── MeasureViewModel.kt
    │
    └── result_recommendation/      # Màn hình so sánh và Đề xuất vị trí
        ├── ResultScreen.kt
        └── ResultViewModel.kt
```

---

## 2. Giải thích các Module Quan Trọng (Áp dụng review của thầy cô)

### A. Thư mục `data/sensor/WifiScanner.kt`
Thay vì gọi lệnh `startScan()` bị Google giới hạn (throttle) vài lần mỗi phút, class này sẽ dùng `WifiManager.getConnectionInfo()` kết hợp với Kotlin `Flow` để phát (emit) thông số RSSI và Link Speed của mạng đang kết nối **mỗi giây một lần**. Điều này giải quyết bài toán "Live Radar" (đi tới đâu sóng nhảy tới đó).

### B. Thư mục `core/network/PingTester.kt`
Xử lý việc đo độ trễ (Latency). Cần chạy ngầm (Background thread `Dispatchers.IO`) gửi gói tin ICMP tới `1.1.1.1` (Cloudflare) hoặc Gateway Router (`192.168.1.1`). Trả về độ trễ (ms) và tỷ lệ rớt mạng (Packet loss).

### C. Thư mục `domain/usecase/CalculateScoreUseCase.kt` (Scoring Engine)
Đây là "Vũ khí bí mật" (Differentiation) của nhóm bạn. Chứa thuật toán dịch thông số kỹ thuật sang điểm nhu cầu.
*Ví dụ logic bên trong:*
- Nếu `useCase == GAMING`: Trọng số Ping chiếm 60%, RSSI chiếm 30%, Speed 10%. (Ping > 80ms => Cảnh báo đỏ).
- Nếu `useCase == STUDY`: Trọng số Speed chiếm 50%, RSSI 40%, Ping 10%.

### D. Thư mục `presentation/live_radar/` (Giải quyết "Ma sát hành vi")
Giảng viên nhận xét người dùng lười đi đo từng chỗ A, B, C rồi chờ đợi. Màn hình này sẽ hiển thị một la bàn/đồng hồ đo tốc độ (Gauge). Người dùng chỉ cần mở app, chọn "Gaming", cầm máy đi quanh phòng. Kim đồng hồ sẽ nhảy liên tục. Khi kim chỉ vùng xanh lá, bấm nút "Lưu vị trí này (Bàn gần cửa sổ)" là xong.

---

## 3. Các thư viện cấu hình trong `build.gradle.kts` (Dependencies)

Bạn chỉ cần các thư viện gọn nhẹ này, không cần Firebase hay Database phức tạp:
- **Jetpack Compose:** `ui`, `material3`, `ui-tooling-preview` (Xây dựng giao diện).
- **Coroutines & Flow:** `kotlinx-coroutines-android` (Xử lý đa luồng, ping mạng không làm đơ app).
- **ViewModel Compose:** `lifecycle-viewmodel-compose` (Quản lý state cho màn hình).
- **Navigation Compose:** `androidx.navigation:navigation-compose` (Chuyển qua lại giữa màn hình Đo đạc và màn hình Kết quả).