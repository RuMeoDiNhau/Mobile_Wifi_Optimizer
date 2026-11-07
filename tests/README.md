# Kiểm thử BUG-01 đến BUG-03

**Đã sửa code và viết test; chưa chạy build/lint/unit test/UI test.**
Phiên này chỉ có môi trường dùng chung trên máy người dùng. Theo yêu cầu không test trên máy người dùng, không chạy Gradle, ứng dụng, emulator, ADB, socket probe hay cài dependency.

## Thay đổi cần kiểm chứng

- BUG-01: phân biệt chưa đo, thất bại và thành công; giữ điểm 0, timeout và trường thiếu.
- BUG-02: chỉ hiển thị LinkSpeed; bỏ download/upload suy diễn và giá trị sàn.
- BUG-03: snapshot mạng, thời gian, tọa độ và nhu cầu đi cùng kết quả; màn chi tiết không trộn mạng đang xem với kết quả khác.

## Bộ test hồi quy

Các test nằm trong tests/src/test/kotlin/com/mobilelegend/wlo/regression và đã nối vào test source set của app.

| File | Số test | Phạm vi | Trạng thái |
| --- | ---: | --- | --- |
| [NetworkMeasureContentTest.kt](src/test/kotlin/com/mobilelegend/wlo/regression/NetworkMeasureContentTest.kt) | 16 | Offline, quyền/GPS, trạng thái đo, điểm 0, timeout, ping chậm, số liệu thiếu, LinkSpeed thấp, snapshot A/B, dữ liệu mẫu | Chưa chạy |
| [BenchmarkRegressionTest.kt](src/test/kotlin/com/mobilelegend/wlo/regression/BenchmarkRegressionTest.kt) | 7 | Snapshot/ID, offline, thiếu điều kiện, timeout xuyên suốt scoring/history/place, lỗi sau lượt thành công, lưu điểm 0 | Chưa chạy |
| [MeasuredWifiPlaceTest.kt](src/test/kotlin/com/mobilelegend/wlo/regression/MeasuredWifiPlaceTest.kt) | 6 | Formatter, tọa độ/định danh, không bịa điểm bản đồ/tốc độ, SSID/BSSID bị ẩn | Chưa chạy |

Tổng: **29 test hồi quy + 33 test cũ = 62 khai báo**; chưa xác nhận biên dịch/discovery, không có test nào được thực thi trong đợt này.

## Cách chạy trên runner độc lập

Chỉ chạy các lệnh sau trên bản sao project trong runner riêng, không phải máy người dùng. Cần JDK 17 và Android SDK 34.

    bash ./gradlew --no-daemon :app:testDebugUnitTest --tests 'com.mobilelegend.wlo.regression.*'
    bash ./gradlew --no-daemon :app:assembleDebug :app:lintDebug

Các lệnh trên chưa được chạy. Hai test PingTesterTest cũ có socket thật; toàn bộ test hồi quy dùng dữ liệu giả.
Khi có runner, ghi stdout/stderr, exit code, phiên bản code, JUnit XML và báo cáo lint. Không đánh dấu test chưa chạy là pass.

## Kiểm chứng còn thiếu

- UI offline, kết quả 0, timeout, dữ liệu thiếu và xem mạng B sau khi đo A.
- Quay lại tab, lưu/xóa lịch sử, bấm đo liên tiếp, hủy và vòng đời Activity.
- Callback hệ điều hành, đổi AP/mạng, Wi-Fi mất khi 4G vẫn bật, cleanup socket.
- Layout trên màn hình hẹp và font lớn.
