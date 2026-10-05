# HƯỚNG DẪN KIỂM THỬ TOÀN DIỆN & CHẠY DỰ ÁN TRÊN THIẾT BỊ (BƯỚC 5)

Tài liệu này cung cấp quy trình kiểm thử chi tiết từ **Unit Test (JVM)** đến **Thử nghiệm trên Thiết bị Thật (Hardware Verification)** và xử lý toàn bộ các trường hợp biên (Edge Cases) cho dự án **Wi-Fi Location Optimizer (WLO)**.

---

## 1. Hướng dẫn Mở và Build Dự án trong Android Studio

1. **Mở Android Studio** (khuyến nghị phiên bản *Hedgehog 2023.1.1* hoặc mới hơn).
2. Chọn **Open** và duyệt tới thư mục:
   `d:\file_hoc_tap\Nam 4-ki 1\Phát triển Ứng dụng trên Thiết bị Di động\mobile_wifi_optimizer`
3. Chờ Android Studio đồng bộ Gradle (**Gradle Sync**).
4. Kiểm tra cấu hình JDK: Vào `File > Settings > Build, Execution, Deployment > Build Tools > Gradle` $\rightarrow$ Đảm bảo **Gradle JDK** là **Java 17**.

---

## 2. Hướng dẫn Chạy Toàn Bộ Unit Test (JVM)

Bộ Unit Test đã được tổ chức theo tiêu chuẩn để chạy trực tiếp trên máy tính mà không cần cắm điện thoại hay bật máy ảo.

### Cách 1: Chạy trực tiếp từ Android Studio
* Mở thư mục `app/src/test/java/com/mobilelegend/wlo/`.
* Chuột phải vào thư mục `wlo` và chọn **Run 'Tests in 'com.mobilelegend.wlo''**.
* Kết quả kỳ vọng: **Tất cả 100% test case đều Passed (Màu xanh lá)**.

### Danh sách các tệp Unit Test đã tạo:
1. `domain/model/UseCaseWeightTest.kt`:
   - [x] `gaming_weight_sum_must_equal_one`: Xác thực tổng trọng số Gaming $= 1.0f$.
   - [x] `study_download_weight_sum_must_equal_one`: Xác thực tổng trọng số Study $= 1.0f$.
   - [x] `video_call_weight_sum_must_equal_one`: Xác thực tổng trọng số Video Call $= 1.0f$.
   - [x] `all_weights_must_be_positive_and_non_zero`: Xác thực tất cả trọng số $> 0$.
2. `core/network/PingTesterTest.kt`:
   - [x] `single_ping_to_invalid_host_should_fail_gracefully`: Xác thực timeout không gây crash.
   - [x] `batch_ping_should_compute_correct_packet_loss_on_failure`: Xác thực tính tỷ lệ rớt gói.
3. `domain/usecase/CalculateScoreUseCaseTest.kt`:
   - [x] `normalize_rssi_boundary_tests`: Test clamp sóng $[-90dBm, -50dBm] \to [0, 100]$.
   - [x] `normalize_ping_boundary_tests`: Test clamp ping $[20ms, 200ms] \to [100, 0]$.
   - [x] `normalize_speed_boundary_tests`: Test clamp speed $[10Mbps, 300Mbps] \to [0, 100]$.
   - [x] `gaming_with_low_ping_should_give_high_score`: Test điểm cao khi Ping thấp.
   - [x] `gaming_with_high_ping_should_not_be_recommended`: Test cảnh báo lag khi Ping cao.
   - [x] `disconnected_wifi_should_return_zero_score`: Test trả về 0 điểm khi Offline.
4. `presentation/measure_and_result/MeasureSessionTest.kt`:
   - [x] `saving_spots_should_add_to_session_history`: Test lưu và xác định vị trí có điểm cao nhất (*"TỐT NHẤT"*).

---

## 3. Checklist Kiểm thử Thực tế trên Máy Thật (Real Device Testing)

> 💡 **Khuyến nghị:** Cắm điện thoại Android thật (bật USB Debugging) để có trải nghiệm cảm biến sóng và đo Ping chính xác nhất.

### Kịch bản 1: Mở ứng dụng lần đầu (Cấp quyền Vị trí & GPS)
1. Cài đặt app lên điện thoại.
2. Khi mở app, Dialog hệ thống sẽ hỏi quyền Vị trí (`ACCESS_FINE_LOCATION`).
3. **Thao tác:** Bấm *"Trong khi dùng ứng dụng"*.
4. **Kết quả kỳ vọng:** Màn hình Live Radar ngay lập tức đọc được tên SSID (VD: `"Phong_Tro_A"`) và chỉ số dBm bắt đầu nhảy.

### Kịch bản 2: Xử lý Edge Case "Tắt GPS"
1. Vuốt thanh thông báo của điện thoại xuống và **TẮT Vị trí (GPS)**.
2. Quay lại app WLO.
3. **Kết quả kỳ vọng:**
   - App hiển thị Banner màu vàng: *"Dịch vụ GPS đang tắt. Android sẽ ẩn tên sóng nếu không bật GPS."* kèm nút **"Bật GPS"**.
   - Bấm nút "Bật GPS" $\rightarrow$ App tự động mở trang Cài đặt GPS của điện thoại.
   - Bật GPS lên và quay lại app $\rightarrow$ Banner tự biến mất, hiển thị lại tên SSID.

### Kịch bản 3: Xử lý Edge Case "Tắt Wi-Fi / Đang dùng 4G"
1. Tắt kết nối Wi-Fi trên điện thoại (chỉ bật 4G hoặc Offline).
2. **Kết quả kỳ vọng:**
   - Chấm tròn trạng thái góc phải chuyển sang **Màu đỏ**.
   - Thẻ Wi-Fi hiển thị *"Chưa kết nối Wi-Fi. Vui lòng kết nối để bắt đầu"*.
   - Nút "Kiểm tra 1-Chạm" bị **Vô hiệu hóa (Disabled)**, ngăn ngừa đo sai.

### Kịch bản 4: Kiểm tra tính năng "Live Radar" (La bàn Sóng thời gian thực)
1. Bật Wi-Fi, đứng gần Router Wi-Fi ($< 2\text{m}$).
   - **Kết quả:** Kim đồng hồ chỉ vùng **Xanh lá** ($\ge -50\text{ dBm}$), $90 - 100\%$ tín hiệu.
2. Cầm điện thoại đi xa dần ra ngoài hành lang / cửa sổ / góc khuất tường.
   - **Kết quả:** Đồng hồ tự động hạ dần xuống vùng **Vàng** ($-65\text{ dBm}$) rồi xuống vùng **Đỏ** ($-80\text{ dBm}$) mỗi giây mà không cần ấn nút nào.

### Kịch bản 5: Đo Kiểm định 1-Chạm (3-Second Benchmark) & So sánh Vị trí
1. Chọn nhu cầu **"Chơi Game (Gaming)"**.
2. Bấm nút **"Kiểm tra 1-Chạm Vị trí Này (3s)"**.
   - **Kết quả:** Chuyển sang màn hình đếm ngược với hiệu ứng sóng tỏa tròn (Radar Pulse), thanh tiến trình chạy 3 giai đoạn: *Đọc phần cứng $\to$ Đo Socket Ping $1.1.1.1 \to$ Tính điểm*.
3. Sau 3 giây: Màn hình chuyển sang **Kết Quả Đánh Giá**:
   - Vòng tròn điểm số (VD: $85/100$ điểm).
   - Card đánh giá: *"Tuyệt vời để Leo Rank"*.
   - Chi tiết 3 thông số: Ping ms, Sóng dBm, LinkSpeed Mbps.
4. Bấm **"Lưu vị trí này"** $\rightarrow$ Nhập tên *"Bàn học gần cửa sổ"*.
5. Bấm **"Về Radar dò tiếp"** $\rightarrow$ Đi ra giường ngủ, bấm đo lại $\rightarrow$ Nhập tên *"Giường ngủ"*.
6. Bấm nút **"So sánh (2)"** ở góc trên $\rightarrow$ Bottom Sheet mở ra, hiển thị danh sách và tự động gắn huy hiệu **"TỐT NHẤT"** cho vị trí có điểm cao hơn!

---

## 4. Bảng Tổng kết Đạt Chuẩn Đồ Án (Submission Checklist)

- [x] Không còn code thừa hay class trung gian (chuẩn triết lý Antigravity).
- [x] Tránh được hoàn toàn lỗi Android Throttling (sử dụng polling `connectionInfo`/`NetworkCapabilities`).
- [x] Đo Socket Ping trên `Dispatchers.IO` có timeout nghiêm ngặt, không đơ UI.
- [x] Thuật toán Scoring Engine chuẩn hóa $0-100$ chính xác, có kiểm thử Unit Test.
- [x] Giao diện Jetpack Compose Material 3 mượt mà, phản hồi 60fps.
