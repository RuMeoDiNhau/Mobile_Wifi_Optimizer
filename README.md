# Wi-Fi Location Optimizer (WLO) - Android App

Ứng dụng di động hỗ trợ đo lường, tối ưu hóa vị trí bắt sóng Wi-Fi thời gian thực theo nhu cầu cá nhân (Gaming, Download học tập, Video Call) dành cho sinh viên.

**Môn học:** Phát triển Ứng dụng trên Thiết bị Di động

---

## 🌟 Tính Năng Cốt Lõi (MVP Features)

1. **Live Wi-Fi Radar (La bàn Sóng Thời Gian Thực):**
   * Người dùng chỉ cần mở app và di chuyển trong phòng. Đồng hồ Gauge đo cường độ dBm và phân cấp màu sắc (Xanh lá $\ge -60\text{dBm}$, Vàng $-61 \to -75\text{dBm}$, Đỏ $\le -76\text{dBm}$) liên tục mỗi 1 giây.
   * Giải quyết triệt để vấn đề "ma sát hành vi" (không bắt người dùng phải bấm đo thủ công liên tục).
2. **One-Tap Benchmark (Kiểm định Nhu cầu 1-Chạm):**
   * Khi tìm được vị trí ưng ý, người dùng chọn mục đích sử dụng (**Gaming**, **Study/Download**, **Video Call**) và bấm kiểm tra.
   * Hệ thống tự động kích hoạt tiến trình đo 3 giây (đo 3 gói tin Socket tới `1.1.1.1:53`, đọc RSSI và LinkSpeed).
3. **Scoring Engine (Thuật toán Chuẩn hóa & Chấm điểm 0 - 100):**
   * Áp dụng công thức chuẩn hóa các thông số thô và nhân bộ trọng số riêng cho từng Use Case.
   * Tự động đưa ra lời nhận xét và khuyến nghị thông minh.
4. **Session History & Comparison (So Sánh Vị Trí Trong Phiên):**
   * Cho phép đặt tên và lưu tạm các vị trí đã đo (VD: Bàn học, Giường ngủ, Cửa sổ...).
   * Bảng so sánh Bottom Sheet tự động highlight và trao huy hiệu **"TỐT NHẤT"** cho vị trí có điểm cao nhất.
5. **Smart Permission & GPS Handler:**
   * Tự động phát hiện thiếu quyền vị trí hoặc dịch vụ GPS đang tắt, cung cấp nút 1-chạm đưa thẳng vào Cài đặt hệ thống.

---

## 🛠️ Công Nghệ & Kiến Trúc (Tech Stack)

* **Ngôn ngữ:** Kotlin 1.9+
* **UI Framework:** Jetpack Compose (Material 3)
* **Kiến trúc:** Clean Architecture + MVVM
  * `core/`: Xử lý quyền vị trí (`PermissionHelper`), đo độ trễ TCP Socket (`PingTester`).
  * `data/`: Cảm biến đọc sóng đa phiên bản Android 11 & 12+ (`WifiScanner`), `WifiRepositoryImpl`.
  * `domain/`: Scoring Engine (`CalculateScoreUseCase`), Live Stream (`StreamLiveWifiUseCase`), Benchmark (`PerformBenchmarkUseCase`).
  * `presentation/`: Các màn hình Compose (`LiveRadarScreen`, `MeasureScreen`, `ResultScreen`, `SessionHistoryBottomSheetContent`).
* **Bất đồng bộ:** Kotlin Coroutines & Flow (`Dispatchers.IO` cho phần cứng/mạng, `Dispatchers.Main` cho UI).

---

## 📂 Cấu Trúc Mã Nguồn

```text
app/src/
├── main/
│   ├── AndroidManifest.xml
│   └── java/com/mobilelegend/wlo/
│       ├── MainActivity.kt
│       ├── core/
│       │   ├── network/PingTester.kt
│       │   └── permissions/PermissionHelper.kt
│       ├── data/
│       │   ├── model/ (WifiMetric.kt, PingResult.kt)
│       │   ├── repository_impl/WifiRepositoryImpl.kt
│       │   └── sensor/WifiScanner.kt
│       ├── domain/
│       │   ├── model/ (LocationScore.kt, UseCaseCategory.kt, UseCaseWeight.kt, LiveRadarData.kt)
│       │   ├── repository/WifiRepository.kt
│       │   └── usecase/ (CalculateScoreUseCase.kt, StreamLiveWifiUseCase.kt, PerformBenchmarkUseCase.kt)
│       └── presentation/
│           ├── components/ (SignalGaugeIndicator.kt, ScoreCircleCard.kt, PermissionBanner.kt)
│           ├── live_radar/ (LiveRadarScreen.kt, LiveRadarViewModel.kt, LiveRadarUiState.kt)
│           ├── measure_and_result/ (MeasureScreen.kt, ResultScreen.kt, MeasureViewModel.kt, ...)
│           ├── navigation/ (Screen.kt, AppNavigation.kt)
│           └── theme/ (Color.kt, Theme.kt, Type.kt)
└── test/
    └── java/com/mobilelegend/wlo/
        ├── core/network/PingTesterTest.kt
        ├── domain/model/UseCaseWeightTest.kt
        ├── domain/usecase/CalculateScoreUseCaseTest.kt
        └── presentation/measure_and_result/MeasureSessionTest.kt
```

---

## 📚 Tài Liệu Dự Án (Documentation)

Toàn bộ tài liệu chi tiết của dự án được lưu trữ quy củ trong thư mục [`docs/`](docs/):

### 🚀 Hướng Dẫn & Báo Cáo
* 📖 [USER_GUIDE.md](docs/USER_GUIDE.md): Hướng dẫn cài đặt, thiết lập quyền và sử dụng ứng dụng.
* 🌐 [WIKI.md](docs/WIKI.md): Toàn văn tài liệu GitHub Wiki đồ án môn học (12 chương hoàn chỉnh).
* 📑 [Báo Cáo Đồ Án (PDF)](docs/reports/report.pdf): Báo cáo đề cương và đề tài gốc.

### 🏛️ Kiến Trúc & Thiết Kế
* 🏛️ [architecture.md](docs/architecture.md): Sơ đồ luồng hệ thống tương tác bằng Mermaid và hình ảnh trực quan.
* 📜 [swagger.yaml](docs/swagger.yaml) / [swagger.json](docs/swagger.json): Đặc tả Swagger / OpenAPI 3.0 RESTful API của hệ thống.
* 📂 [project_structure.md](docs/project_structure.md): Chi tiết cấu trúc thư mục, trách nhiệm module theo Clean Architecture.
* 🌟 [features_docs.md](docs/features_docs.md): Đặc tả chi tiết các tính năng cốt lõi (Live Radar, 1-Tap Benchmark, Scoring Engine).
* 🗄️ [data_schema.md](docs/data_schema.md): Đặc tả lược đồ dữ liệu và mô hình Domain/Data.

### 🛠️ Kỹ Thuật & Triển Khai
* 📝 [implementation_plan.md](docs/implementation_plan.md): Lộ trình 5 bước triển khai ứng dụng.
* 📐 [implementation_notes.md](docs/implementation_notes.md): 5 lưu ý kỹ thuật trọng yếu và công thức chuẩn hóa điểm số.

### 🧪 Kiểm Thử & Đảm Bảo Chất Lượng
* 📋 [test_specifications.md](docs/test_specifications.md): Bảng đặc tả Test Matrix (Unit Tests & Edge Cases).
* ✅ [testing_and_verification_guide.md](docs/testing_and_verification_guide.md): Hướng dẫn chạy kiểm thử trên Android Studio và máy thật.

