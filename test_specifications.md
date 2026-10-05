# BẢNG ĐẶC TẢ TEST CASE TOÀN DIỆN CHO TỪNG BƯỚC (TEST SPECIFICATIONS)

Tài liệu này định nghĩa ma trận kiểm thử (Test Matrix) cho từng giai đoạn triển khai dự án **Wi-Fi Location Optimizer (WLO)**, bao gồm: **Unit Test (JVM)**, **Hardware Mock Test**, và **UI Instrumentation Test**.

---

## 1. Ma trận Test Case theo từng Bước Triển Khai

| Bước | Hạng mục kiểm thử | Loại Test | File thực thi / Kịch bản | Tiêu chuẩn Đạt (Acceptance Criteria) |
| :---: | :--- | :--- | :--- | :--- |
| **Bước 1** | Model Validation & Weights | Unit Test (JVM) | `UseCaseWeightTest.kt` | • Tổng 3 trọng số $\text{Ping} + \text{RSSI} + \text{Speed} = 1.0\ ( \pm 0.0001 )$<br>• Trọng số Gaming đúng $60\% - 30\% - 10\%$<br>• Trọng số Study đúng $10\% - 40\% - 50\%$ |
| **Bước 2** | Socket Ping Tester | Unit / Async Test | `PingTesterTest.kt` | • Ping tới IP hợp lệ (`1.1.1.1`) trả về latency $< 2000\text{ms}$ & `isSuccess = true`<br>• Ping tới IP rác/chết trả về `isSuccess = false` và latency $= 2000\text{ms}$ sau đúng timeout |
| **Bước 2** | Quản lý Quyền (Permissions) | Local Unit Test | `PermissionHelperTest.kt` | • Nhận diện đúng khi thiếu quyền Location hoặc GPS bị tắt |
| **Bước 3** | Chuẩn hóa Scoring Engine | Unit Test (JVM) | `CalculateScoreUseCaseTest.kt` | • RSSI $\ge -50\text{dBm} \rightarrow 100$ điểm; $\le -90\text{dBm} \rightarrow 0$ điểm<br>• Ping $\le 20\text{ms} \rightarrow 100$ điểm; $\ge 200\text{ms} \rightarrow 0$ điểm<br>• Không bao giờ ra điểm $<0$ hoặc $>100$ (Clamp an toàn) |
| **Bước 3** | Thuật toán Đánh giá Nhu cầu | Unit Test (JVM) | `CalculateScoreUseCaseTest.kt` | • Sóng yếu (-85dBm) nhưng Ping tốt (15ms) $\rightarrow$ Gaming vẫn điểm trung bình khá, Study điểm thấp<br>• Ping timeout (2000ms) $\rightarrow$ Điểm Gaming $= 0$ hoặc $< 30$ |
| **Bước 4** | Live Radar Flow | Coroutine Test | `LiveRadarViewModelTest.kt` | • State cập nhật liên tục mỗi 1000ms khi Scanner phát dữ liệu mới<br>• Chuyển màu đúng (Xanh / Vàng / Đỏ) theo ngưỡng RSSI |
| **Bước 4** | Tiến trình đo 1-chạm 3 giây | Coroutine Test | `MeasureViewModelTest.kt` | • Quá trình đo kết thúc đúng sau 3 giây và phát ra `LocationScore` hợp lệ |
| **Bước 5** | Edge Cases & Khả năng chịu lỗi | Manual / Device Test | Thiết bị thật | • Tắt GPS $\rightarrow$ hiện banner nhắc bật GPS<br>• Tắt Wi-Fi $\rightarrow$ hiện thông báo Offline, không crash |

---

## 2. Chi tiết các Test Case cụ thể

### 🧪 Test Suite 1: Data Models & Weights (Bước 1)
* **TC-01:** Tổng trọng số `UseCaseWeight` của tất cả các Category phải bằng `1.0f`.
* **TC-02:** `WifiMetric` khởi tạo với giá trị mặc định hợp lệ, `isConnected` đúng trạng thái.
* **TC-03:** `LocationScore` lưu giữ đầy đủ thông số thô (raw) và điểm chuẩn hóa (normalized).

### 🧪 Test Suite 2: Network & Hardware Layer (Bước 2)
* **TC-04:** `PingTester` trả về kết quả thành công khi có kết nối Internet.
* **TC-05:** `PingTester` xử lý `SocketTimeoutException` mượt mà, không throw Exception ra ngoài gây crash app.
* **TC-06:** `WifiScanner` emit dữ liệu đều đặn mỗi 1 giây qua Kotlin `Flow`.

### 🧪 Test Suite 3: Scoring Engine & Normalization (Bước 3)
* **TC-07 (Boundary RSSI):**
  * Input: `RSSI = -30 dBm` $\rightarrow$ Điểm sóng $= 100$ (không vượt quá 100).
  * Input: `RSSI = -95 dBm` $\rightarrow$ Điểm sóng $= 0$ (không âm).
  * Input: `RSSI = -70 dBm` $\rightarrow$ Điểm sóng $= 50$.
* **TC-08 (Boundary Ping):**
  * Input: `Ping = 10 ms` $\rightarrow$ Điểm Ping $= 100$.
  * Input: `Ping = 250 ms` $\rightarrow$ Điểm Ping $= 0$.
  * Input: `Ping = 110 ms` $\rightarrow$ Điểm Ping $= 50$.
* **TC-09 (Use Case Ranking Logic):**
  * Gaming với `Ping = 25ms, RSSI = -55dBm, Speed = 100Mbps` $\rightarrow$ `finalScore >= 85`, `isRecommended = true`.
  * Gaming với `Ping = 180ms, RSSI = -55dBm, Speed = 300Mbps` $\rightarrow$ `finalScore < 60`, `isRecommended = false`.

### 🧪 Test Suite 4: Presentation & ViewModel States (Bước 4)
* **TC-10:** `LiveRadarUiState` chuyển trạng thái `NoWifi` khi mất kết nối Wi-Fi.
* **TC-11:** `MeasureUiState` chuyển trạng thái `Measuring` $\rightarrow$ `Completed` sau 3 giây.
