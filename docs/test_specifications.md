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

## 2. Chi tiết Toàn bộ Test Suites Đã Được Hiện Thực

Hiện tại dự án đã được trang bị **6 bộ Test Suite hoàn chỉnh với 31 Test Cases tự động (100% Passed)** trong thư mục `com.mobilelegend.wlo (test)`:

### 🧪 Test Suite 1: Trọng Số Nhu Cầu (`UseCaseWeightTest.kt`)
* **TC-01:** Tổng 3 trọng số của Gaming bằng $1.0\ (0.60 + 0.30 + 0.10)$.
* **TC-02:** Tổng 3 trọng số của Study/Download bằng $1.0\ (0.10 + 0.40 + 0.50)$.
* **TC-03:** Tổng 3 trọng số của Video Call bằng $1.0\ (0.40 + 0.40 + 0.20)$.
* **TC-04:** Mọi trọng số của tất cả các Use Case đều $> 0$.

### 🧪 Test Suite 2: Đo Độ Trễ Socket TCP (`PingTesterTest.kt`)
* **TC-05:** Ping tới host không tồn tại/chết (`192.0.2.1`) xử lý lỗi mượt mà, trả về `isSuccess = false` và `packetLoss = 100%` mà không gây crash app.
* **TC-06:** Batch ping nhiều gói tin tính toán chính xác tỷ lệ rớt gói khi gặp sự cố mạng.

### 🧪 Test Suite 3: Scoring Engine & Chuẩn Hóa Điểm (`CalculateScoreUseCaseTest.kt`)
* **TC-07 (Chuẩn hóa Sóng RSSI):**
  * `RSSI >= -50 dBm` $\rightarrow$ Điểm sóng $= 100$ (kẹp trần 100).
  * `RSSI <= -90 dBm` $\rightarrow$ Điểm sóng $= 0$ (kẹp sàn 0).
  * `RSSI = -70 dBm` $\rightarrow$ Điểm sóng $= 50$.
* **TC-08 (Chuẩn hóa Độ Trễ Ping):**
  * `Ping <= 20 ms` $\rightarrow$ Điểm Ping $= 100$.
  * `Ping >= 200 ms` $\rightarrow$ Điểm Ping $= 0$.
  * `Ping = 110 ms` $\rightarrow$ Điểm Ping $= 50$.
* **TC-09 (Chuẩn hóa Tốc Độ LinkSpeed):**
  * `Speed >= 300 Mbps` $\rightarrow$ Điểm Tốc độ $= 100$.
  * `Speed <= 10 Mbps` $\rightarrow$ Điểm Tốc độ $= 0$.
* **TC-10 (Đánh giá Gaming):** Ping thấp ($25\text{ms}$) $\rightarrow$ Điểm cao ($\ge 85$), Khuyến nghị Leo Rank.
* **TC-11 (Cảnh báo Gaming):** Ping cao ($180\text{ms}$) $\rightarrow$ Cảnh báo giật lag, không khuyến nghị chơi.
* **TC-12 (Đánh giá Gaming trung bình):** Ping $75\text{ms}$ $\rightarrow$ Điểm $\ge 60$, chỉ phù hợp game nhẹ.
* **TC-13 (Đánh giá Study/Download tốc độ cao):** Tốc độ $250\text{Mbps}$ $\rightarrow$ Khuyến nghị tải file.
* **TC-14 (Đánh giá Study/Download trung bình):** Tốc độ $35\text{Mbps}$ $\rightarrow$ Đáp ứng đọc sách/web.
* **TC-15 (Cảnh báo Study/Download tốc độ yếu):** Tốc độ $< 10\text{Mbps}$ $\rightarrow$ Cảnh báo tải yếu.
* **TC-16 (Đánh giá Video Call HD):** Sóng $\ge -75\text{dBm}$, Ping $\le 100\text{ms}$ $\rightarrow$ Khuyến nghị gọi HD.
* **TC-17 (Cảnh báo Video Call sóng yếu):** Sóng $-82\text{dBm}$ $\rightarrow$ Cảnh báo chập chờn.
* **TC-18 (Mất kết nối Wi-Fi):** `isConnected = false` $\rightarrow$ Điểm $= 0$, thông báo chưa kết nối.
* **TC-19 (Rớt gói mạng $\ge 50\%$):** Ping hỏng $\rightarrow$ Điểm $= 15$, cảnh báo rớt gói.
* **TC-20 (Biên cận kề Router):** Sóng $-15\text{dBm}$, Wi-Fi 6 $1200\text{Mbps}$, Ping $2\text{ms}$ $\rightarrow$ Điểm tuyệt đối $100/100$.

### 🧪 Test Suite 4: Radar Luồng Sóng (`StreamLiveWifiUseCaseTest.kt`)
* **TC-21:** Phân cấp `SignalQualityLevel.EXCELLENT` khi $\text{RSSI} \ge -60\text{dBm}$.
* **TC-22:** Phân cấp `SignalQualityLevel.GOOD` khi $-75\text{dBm} \le \text{RSSI} \le -61\text{dBm}$.
* **TC-23:** Phân cấp `SignalQualityLevel.POOR` khi $\text{RSSI} \le -76\text{dBm}$.
* **TC-24:** Kiểm tra công thức phần trăm vạch sóng ($0\%$ tại $-100\text{dBm}$, $50\%$ tại $-75\text{dBm}$, $100\%$ tại $-50\text{dBm}$).
* **TC-25:** Luồng Kotlin Flow phát dữ liệu chính xác và ánh xạ đúng sang `LiveRadarData`.

### 🧪 Test Suite 5: Tiến Trình Đo 1-Chạm (`PerformBenchmarkUseCaseTest.kt`)
* **TC-26:** Đo đạc điều phối chính xác 3 gói tin và tính điểm cho Use Case Gaming.
* **TC-27:** Điều phối tính điểm cho Use Case Study/Download theo đúng trọng số băng thông.
* **TC-28:** Xử lý ngoại lệ khi Ping hỏng trong quá trình benchmark.

### 🧪 Test Suite 6: Quản Lý Phiên Đo & So Sánh (`MeasureSessionTest.kt`)
* **TC-29:** Lưu vị trí vào lịch sử phiên đo (Session History).
* **TC-30:** Xác định chính xác vị trí có điểm cao nhất ("TỐT NHẤT").
* **TC-31:** Xử lý an toàn khi danh sách rỗng, sắp xếp giảm dần, xóa vị trí và giải quyết trường hợp đồng điểm (Tie-breaking).
