# MVP FEATURES DOC (Tinh chỉnh theo Feedback Giảng viên)

Dựa trên feedback từ LMS: *Hành vi bắt user cầm máy đi từng điểm A, B, C và bấm đo thủ công tạo ra ma sát lớn*. MVP đã được xoay trục (pivot) sang luồng **"Radar Thời Gian Thực"** và **"Đánh Giá 1-Chạm"**.

## Feature 1: Trợ lý Cấp quyền thông minh (Smart Permission Handler)
- **Mô tả:** Màn hình Onboarding tự động nhận diện thiết bị thiếu quyền gì.
- **Logic:**
  1. Kiểm tra `ACCESS_FINE_LOCATION` (Quyền vị trí).
  2. Kiểm tra phần cứng GPS có đang bật hay không.
  3. *(Android 13+)* Kiểm tra quyền `NEARBY_WIFI_DEVICES`.
- **UI:** Nút bấm 1-chạm đưa user thẳng vào màn hình Cài đặt của hệ thống nếu họ từ chối quyền.

## Feature 2: Live Radar (La bàn Sóng)
- **Mô tả:** Người dùng chỉ cần mở app và đi dạo quanh phòng. Màn hình hiển thị một đồng hồ (Gauge) giật liên tục theo thời gian thực (1 giây/lần).
- **Core Value:** Giải quyết ngay lập tức bài toán *đi tìm vùng phủ sóng tốt* mà không cần ấn nút lưu thủ công.
- **Logic Background:**
  - Dùng `Flow.sample(1000L)` thu thập `WifiManager.connectionInfo.rssi`.
  - Phân cực màu UI:
    - **Xanh lá:** `RSSI >= -60 dBm`
    - **Vàng:** `-61` đến `-75 dBm`
    - **Đỏ:** `<= -76 dBm`

## Feature 3: One-Tap Use Case Benchmark (Kiểm định theo nhu cầu)
- **Mô tả:** Khi user đã tìm được chỗ ngồi ưng ý qua Radar, họ chọn nhu cầu (Gaming / Study / Call) và bấm "Kiểm tra ngay".
- **Logic Background:**
  - Kích hoạt một tiến trình đo ẩn kéo dài đúng **3 giây**.
  - Bắn 3 gói tin ICMP tới `1.1.1.1` để lấy trung bình Ping.
  - Lấy thông số RSSI và LinkSpeed.
  - Chuyển số liệu thô qua `ScoringEngineUseCase` để áp trọng số (Weights).
- **UI:** Màn hình kết quả hiển thị một điểm số tổng (0-100) dạng vòng tròn lớn kèm kết luận: *"Vị trí này dư sức để chơi Liên Quân / Bạn nên tìm chỗ gần Router hơn để Call Video"*.

## Feature 4: Lịch sử Phiên đo (Optional cho MVP)
- **Mô tả:** Lưu tạm các điểm đã đo trong lần mở app hiện tại vào một danh sách (In-memory List trong ViewModel).
- **UI:** Một Bottom Sheet hiển thị danh sách để user có thể so sánh giữa 2 vị trí họ vừa đo (VD: Bàn học vs Giường ngủ) và app highlight vị trí nào điểm cao hơn.