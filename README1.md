# Ghi chú review và hướng sửa lỗi Mobile_Wifi_Optimizer

> **Cập nhật tiếp 07/11/2026 — BUG-07, BUG-08, BUG-09:** Đã bổ sung bộ điều kiện đo chung ở UI/ViewModel/use case, tách quyền bản đồ với quyền đọc SSID/BSSID, dùng callback có thông tin vị trí trên Android 12+, giữ nguyên tọa độ và ẩn dữ liệu demo theo mặc định. Snapshot vị trí có thời gian, accuracy và confidence. Thêm 31 test, tổng 96 test hồi quy + 33 test cũ = 129 khai báo. **Chưa chạy build/lint/test/runtime** vì không có runner độc lập và yêu cầu không chạy trên máy người dùng. Xem [chi tiết thay đổi, rà soát tĩnh và các ca chưa kiểm chứng](tests/README.md). Các mốc dưới đây là lịch sử review, không phải kết quả chạy test mới.

> **Cập nhật 07/11/2026:** Đã thay đổi code cho BUG-01 đến BUG-06. Đợt BUG-04–06 chuyển lịch sử về ViewModel theo ID, gắn TCP với một phiên Wi-Fi có kiểm tra mất/đổi mạng, quản lý một job và hủy khi rời màn đo. Thêm 36 test, nâng bộ hồi quy trong `tests/` lên 65 test (cộng 33 test cũ là 98 khai báo). **Chưa chạy build/lint/unit test/UI test** vì phiên làm việc chỉ có môi trường trên máy người dùng, trong khi yêu cầu không chạy test ở máy này. Xem [báo cáo, giới hạn và cách chạy kiểm thử](tests/README.md). Phần bên dưới giữ nguyên nội dung review ngày 03/11/2026; chưa coi các bản sửa là đã kiểm chứng trên runtime, các BUG khác chưa được xác nhận hoàn tất.

Tài liệu tổng hợp các vấn đề phát hiện khi đọc source để nhóm xem xét và sửa sau. Các mục bên dưới là đề xuất; chưa có thay đổi nào đối với code ứng dụng trong quá trình lập tài liệu này.

- **Ngày tổng hợp:** 03/11/2026.
- **Phiên bản được review:** commit `47c3cdd`.
- **Phạm vi:** code Android, cấu hình, 6 file unit test và tài liệu liên quan; đối chiếu một số API với tài liệu Android chính thức.
- **Phương pháp:** review tĩnh từ source. Chưa chạy build, lint, unit test hoặc thử trên điện thoại trong lượt review này.
- **Giới hạn:** các lỗi logic nêu rõ nhánh code có thể xác định từ source; mức ảnh hưởng về thiết bị, vòng đời, mạng và định vị cần kiểm chứng thực tế. Tài liệu này không khẳng định đã tái hiện crash.

## 1. Cách đọc mức ưu tiên

| Mức | Ý nghĩa |
| --- | --- |
| **P1** | Nên sửa trước khi demo hoặc dùng kết quả đo: sai thông số, sai đối tượng được đo, thiếu điều kiện đo hoặc có thể chạy tác vụ chồng nhau. |
| **P2** | Sửa tiếp để hoàn thiện độ ổn định, vòng đời, định vị, lưu dữ liệu và tính nhất quán của giao diện. |

Các số dòng được ghi theo phiên bản review, có thể thay đổi sau khi chỉnh sửa. Tất cả mục trong checklist hiện mang trạng thái **chưa xử lý trong lượt review này**.

## 2. Danh sách tổng hợp

| ID | Ưu tiên | Vấn đề |
| --- | --- | --- |
| BUG-01 | P1 | Mất mạng, chưa đo hoặc timeout vẫn có thể hiển thị kết nối tốt và số liệu mẫu. |
| BUG-02 | P1 | Download/upload hiển thị chưa phải tốc độ Internet được đo thực tế. |
| BUG-03 | P1 | Tên địa điểm và thông số có thể thuộc hai mạng khác nhau. |
| BUG-04 | P1 | Xem lại tab đo có thể gán thời gian, SSID và vị trí mới cho kết quả cũ. |
| BUG-05 | P1 | Phép đo chưa bảo đảm toàn bộ dữ liệu thuộc cùng mạng Wi-Fi. |
| BUG-06 | P1 | Nhiều lần đo có thể chạy chồng nhau; thiếu hủy đo. |
| BUG-07 | P1 | Các lối bắt đầu đo chưa kiểm tra thống nhất Wi-Fi, quyền và trạng thái tác vụ. |
| BUG-08 | P1 | Lấy SSID/BSSID trên Android 12+ và kiểm tra quyền còn chưa phù hợp. |
| BUG-09 | P1 | Địa điểm mẫu bị di chuyển quanh người dùng; điểm đo thật bị lệch tọa độ và ghi đè. |
| BUG-10 | P2 | ViewModel được tạo trực tiếp, chưa được hệ thống quản lý vòng đời đúng cách. |
| BUG-11 | P2 | Radar có thể dừng âm thầm sau ngoại lệ. |
| BUG-12 | P2 | Thời gian đo không bảo đảm đúng 3 giây. |
| BUG-13 | P2 | Một trong ba lần TCP connect thất bại vẫn có thể đạt điểm Gaming tuyệt đối. |
| BUG-14 | P2 | Yêu cầu GPS thiếu timeout/cleanup; vị trí cũ bị coi là vị trí hiện tại. |
| BUG-15 | P2 | Bỏ lưu hết nhưng danh sách Đã lưu vẫn hiện địa điểm. |
| BUG-16 | P2 | Chọn nhu cầu chưa làm thay đổi gợi ý địa điểm trên Trang chủ. |
| BUG-17 | P2 | So sánh điểm chung giữa các nhu cầu có bộ trọng số khác nhau. |
| BUG-18 | P2 | Tìm kiếm bản đồ không có kết quả vẫn hiển thị một địa điểm khác. |
| BUG-19 | P2 | Hai file quy tắc backup XML thiếu thuộc tính bắt buộc. |

## 3. Chi tiết các lỗi P1

### BUG-01 — Hiển thị kết quả tốt khi chưa có dữ liệu hợp lệ

**Vị trí:** [NetworkMeasureScreen.kt](app/src/main/java/com/mobilelegend/wlo/presentation/measure_and_result/NetworkMeasureScreen.kt), khoảng dòng 110–144 và 425–426.

**Biểu hiện từ code:**

- Khi thiếu dữ liệu, màn hình có thể dùng mặc định 92 điểm, download 128 Mbps, upload 42 Mbps và ping 12 ms.
- `Completed` với điểm 0 lại được gán tiêu đề “Kết nối rất tốt”.
- Ping chỉ được lấy từ kết quả nếu nằm trong khoảng `1..999`; timeout 2000 ms có thể rơi về giá trị mẫu 12 ms.
- `MeasureStatus.Failed` chưa có giao diện lỗi riêng trong màn hình đo mới.
- Nhãn “Ổn định” và lời giới thiệu phù hợp video 4K chưa phụ thuộc đầy đủ vào kết quả thực tế.

**Hướng sửa:** phân biệt rõ chưa đo, thiếu điều kiện, đang đo, thất bại và thành công; giữ nguyên điểm 0; dữ liệu thiếu hiển thị `—`; timeout hiển thị “Không phản hồi”. Chỉ hiện khuyến nghị khi có kết quả hợp lệ.

**Kiểm chứng sau sửa:** mở tab đo khi offline; thử kết quả 0 điểm, ping 2000 ms và trạng thái Failed. Màn hình phải phản ánh đúng trạng thái, không tự thay bằng số liệu tốt.

### BUG-02 — Download/upload chưa phải phép đo tốc độ Internet

**Vị trí:** [NetworkMeasureScreen.kt](app/src/main/java/com/mobilelegend/wlo/presentation/measure_and_result/NetworkMeasureScreen.kt), khoảng dòng 117–127; [AppNavigation.kt](app/src/main/java/com/mobilelegend/wlo/presentation/navigation/AppNavigation.kt), khoảng dòng 184–185.

**Biểu hiện từ code:** download lấy từ LinkSpeed. Upload được tính bằng 35% LinkSpeed và còn bị ép giá trị tối thiểu; màn hình đo dùng tối thiểu 12 Mbps, còn bản ghi địa điểm dùng tối thiểu 5 Mbps. Hai nơi có thể hiển thị khác nhau cho cùng một lần đo.

**Ảnh hưởng:** người dùng có thể hiểu nhầm rằng app đã kiểm tra tốc độ tải lên/tải xuống Internet, trong khi chưa có phép đo truyền dữ liệu tương ứng.

**Hướng sửa:** trước mắt chỉ hiển thị “Tốc độ liên kết Wi-Fi”, bỏ upload suy diễn và giá trị tối thiểu không có căn cứ. Nếu phạm vi yêu cầu speed test thật, triển khai tải lên/tải xuống có giới hạn dung lượng, thời gian và khả năng hủy.

**Kiểm chứng sau sửa:** với LinkSpeed thấp hoặc không xác định, giao diện và bản ghi phải nhất quán; không tự tạo ra tốc độ upload.

### BUG-03 — Trộn thông tin địa điểm với kết quả của mạng khác

**Vị trí:** [NetworkMeasureScreen.kt](app/src/main/java/com/mobilelegend/wlo/presentation/measure_and_result/NetworkMeasureScreen.kt), khoảng dòng 101–138 và 330–334.

**Tình huống:** đo mạng ở nhà, sau đó mở thông tin một quán trên bản đồ. SSID ưu tiên `inspectedPlace`, trong khi điểm/tốc độ có thể ưu tiên `completedScore` của mạng ở nhà. Bấm “Đo lại” vẫn đo mạng thiết bị đang kết nối.

**Hướng sửa:** phân biệt xem thông tin địa điểm với đo mạng hiện tại. Kết quả phải gắn với định danh mạng, địa điểm nếu có, nhu cầu và thời gian đo. Không ghép các trường từ nhiều nguồn vào một kết quả.

**Kiểm chứng sau sửa:** đo mạng A rồi xem địa điểm B; thông số của A không xuất hiện dưới tên B. Màn hình phải giải thích đúng mạng được đo khi bấm nút.

### BUG-04 — Xem lại kết quả có thể làm thay đổi dữ liệu lịch sử

**Vị trí:** [NetworkMeasureScreen.kt](app/src/main/java/com/mobilelegend/wlo/presentation/measure_and_result/NetworkMeasureScreen.kt), khoảng dòng 87–97; [AppNavigation.kt](app/src/main/java/com/mobilelegend/wlo/presentation/navigation/AppNavigation.kt), khoảng dòng 168–200.

**Nguyên nhân:** `LaunchedEffect(completedScore)` cập nhật thời gian theo lúc xem và gọi `onScoreMeasured`. Khi màn hình được tạo lại, effect có thể chạy với kết quả cũ; callback lại lấy SSID/GPS hiện tại để dựng địa điểm.

**Tình huống cần kiểm chứng:** đo tại A, đổi mạng hoặc vị trí rồi quay lại tab Đo mạng mà chưa đo mới. Kết quả cũ có thể được gán thời gian/SSID/tọa độ mới.

**Hướng sửa:** tạo một bản ghi đầy đủ ngay khi tác vụ đo hoàn tất: ID, thời gian, mạng, tọa độ, nhu cầu và thông số. Việc lưu do ViewModel/repository quản lý, thực hiện một lần theo ID. Giao diện chỉ đọc dữ liệu; xem lại không làm thay đổi bản ghi.

**Kiểm chứng sau sửa:** chuyển tab và mở lại nhiều lần phải giữ nguyên thời gian, mạng, vị trí và số bản ghi của lần đo cũ.

### BUG-05 — Phép đo chưa được ràng buộc với cùng một mạng Wi-Fi

**Vị trí:** [PerformBenchmarkUseCase.kt](app/src/main/java/com/mobilelegend/wlo/domain/usecase/PerformBenchmarkUseCase.kt), khoảng dòng 18–29; [PingTester.kt](app/src/main/java/com/mobilelegend/wlo/core/network/PingTester.kt), khoảng dòng 29–30.

**Nguyên nhân:** app đọc RSSI/LinkSpeed một lần trước khi đo ping; Socket không được gắn với một `Network` Wi-Fi cụ thể. Chưa kiểm tra mạng đã mất hoặc thay đổi trong suốt tác vụ.

**Ảnh hưởng:** khi tắt Wi-Fi hoặc đổi mạng giữa lúc đo, có nguy cơ ghép thông số Wi-Fi ban đầu với độ trễ của đường truyền khác, chẳng hạn dữ liệu di động.

**Hướng sửa:** giữ mạng cần đo và định danh ban đầu; dùng `network.socketFactory` hoặc `bindSocket`; theo dõi mất/đổi mạng và trả về trạng thái phù hợp. Đánh giá lại tính hợp lệ trước khi chấm điểm/lưu kết quả.

**Kiểm chứng sau sửa:** đo rồi tắt Wi-Fi khi 4G vẫn bật; đổi AP/mạng giữa lúc đo. Không chấp nhận kết quả ghép từ hai mạng.

### BUG-06 — Đo chồng nhau và thiếu hủy tác vụ

**Vị trí:** [MeasureViewModel.kt](app/src/main/java/com/mobilelegend/wlo/presentation/measure_and_result/MeasureViewModel.kt), khoảng dòng 29–70 và 101–102; lối bắt đầu đo ở [AppNavigation.kt](app/src/main/java/com/mobilelegend/wlo/presentation/navigation/AppNavigation.kt), khoảng dòng 134–138.

**Tình huống:** bắt đầu đo, chuyển về Trang chủ rồi bấm đo tiếp. Mỗi lần gọi tạo coroutine mới và các coroutine cùng cập nhật một state. `resetMeasureState()` chỉ đổi trạng thái, không hủy phép đo đang chạy.

**Hướng sửa:** quản lý một `benchmarkJob`; quy định rõ chặn lần mới hay hủy lần cũ. Khi hủy phải dừng tài nguyên mạng tương ứng. Xử lý riêng `CancellationException` và chỉ nhận kết quả từ lần đo còn hiệu lực.

**Kiểm chứng sau sửa:** bấm đo nhiều lần qua các lối vào; Back/chuyển tab khi đo rồi đo tiếp. Không có kết quả cũ ghi đè, lưu trùng hoặc tác vụ chạy ngầm ngoài chủ đích.

### BUG-07 — Chưa thống nhất điều kiện được phép đo

**Vị trí:** [NetworkMeasureScreen.kt](app/src/main/java/com/mobilelegend/wlo/presentation/measure_and_result/NetworkMeasureScreen.kt), khoảng dòng 330–334; [AppNavigation.kt](app/src/main/java/com/mobilelegend/wlo/presentation/navigation/AppNavigation.kt), khoảng dòng 134–138.

**Biểu hiện:** nút “Đo lại” chỉ kiểm tra đang đo hay không. Nút Trang chủ gọi tác vụ trực tiếp. Các lối vào chưa kiểm tra thống nhất Wi-Fi, quyền và dữ liệu cảm biến hợp lệ.

**Hướng sửa:** có điều kiện `canMeasure` thống nhất và kiểm tra lại tại ViewModel/use case, không chỉ ở nút UI. Nêu rõ lý do chưa đo được và hành động khắc phục.

**Kiểm chứng sau sửa:** thử offline, chỉ dùng 4G, từ chối quyền, quyền gần đúng và đang có phép đo. Mọi lối bắt đầu đo phải xử lý nhất quán.

### BUG-08 — Lấy thông tin Wi-Fi và kiểm tra quyền chưa phù hợp

**Vị trí:** [WifiScanner.kt](app/src/main/java/com/mobilelegend/wlo/data/sensor/WifiScanner.kt), khoảng dòng 54–67; [PermissionHelper.kt](app/src/main/java/com/mobilelegend/wlo/core/permissions/PermissionHelper.kt), khoảng dòng 22–33.

**Nguyên nhân:** nhánh Android 12+ lấy `WifiInfo` từ `getNetworkCapabilities()`, API loại bỏ thông tin nhạy cảm về vị trí. Hàm quyền lại coi Fine hoặc Coarse Location là đủ. Vì vậy, app có thể báo đủ quyền nhưng SSID/BSSID vẫn bị ẩn.

**Hướng sửa:** phân biệt quyền phục vụ bản đồ và quyền đọc Wi-Fi; xử lý theo phiên bản Android. Dùng `NetworkCallback` với `FLAG_INCLUDE_LOCATION_INFO` khi cần thông tin nhạy cảm và đã có quyền phù hợp. Không quy mọi SSID bị ẩn thành “cần bật GPS”.

**Kiểm chứng sau sửa:** thử Android 11, 12 và 13+; lần lượt cấp quyền chính xác, chỉ quyền gần đúng, từ chối Nearby, bật/tắt dịch vụ vị trí. Giao diện phải chỉ đúng điều kiện còn thiếu.

### BUG-09 — Tọa độ và nguồn dữ liệu bản đồ không đáng tin

**Vị trí:** [DeviceLocationProvider.kt](app/src/main/java/com/mobilelegend/wlo/data/sensor/DeviceLocationProvider.kt), khoảng dòng 162–196; [AppNavigation.kt](app/src/main/java/com/mobilelegend/wlo/presentation/navigation/AppNavigation.kt), khoảng dòng 177–198; dữ liệu mẫu tại [WifiPlacesData.kt](app/src/main/java/com/mobilelegend/wlo/presentation/components/WifiPlacesData.kt).

**Biểu hiện:**

- Khi cách tọa độ mặc định hơn 5 km, các quán/thư viện mẫu được chuyển tới quanh người dùng nhưng giữ tên cũ.
- Điểm vừa đo bị cộng thêm `0.0004` vào cả vĩ độ và kinh độ.
- Mọi điểm mới dùng ID `measured_user_spot`, nên thay thế điểm đo trước trên bản đồ.
- Khi chưa có vị trí, app có tọa độ mặc định nhưng marker vẫn mang ý nghĩa “Vị trí của bạn”.

**Hướng sửa:** giữ tọa độ thật của địa điểm; đánh dấu dữ liệu demo rõ ràng và không dùng làm khuyến nghị thực tế. Mỗi lần đo có ID riêng, tọa độ tại lúc đo và trạng thái độ tin cậy của vị trí. Khi chưa định vị được, thể hiện đúng trạng thái đó.

**Kiểm chứng sau sửa:** đổi vị trí thiết bị không được kéo quán/thư viện đi theo. Hai lần đo tại A/B phải tồn tại riêng, không tự dịch tọa độ và không ghi đè ngoài chủ đích.

## 4. Chi tiết các lỗi P2

### BUG-10 — ViewModel chưa được quản lý vòng đời đúng cách

**Vị trí:** [MainActivity.kt](app/src/main/java/com/mobilelegend/wlo/MainActivity.kt), dòng 53–54.

ViewModel được tạo bằng constructor trực tiếp, chưa đăng ký với `ViewModelStore`. Hệ thống không tự giữ/dọn các instance này như khi lấy qua `ViewModelProvider`; Radar đang chạy bằng `viewModelScope` có nguy cơ tiếp tục giữ đối tượng cũ khi Activity bị hủy.

**Hướng sửa:** dùng `ViewModelProvider`/`viewModel()` với factory và phạm vi phù hợp; tránh giữ Activity trong đối tượng sống lâu; quản lý thời điểm bật/tắt Radar. Kiểm chứng khi tái tạo Activity, đóng/mở màn hình và chuyển nền.

### BUG-11 — Radar dừng âm thầm sau lỗi

**Vị trí:** [LiveRadarViewModel.kt](app/src/main/java/com/mobilelegend/wlo/presentation/live_radar/LiveRadarViewModel.kt), khoảng dòng 36–50.

`.catch {}` rỗng có thể khiến luồng kết thúc sau ngoại lệ trong khi UI vẫn giữ thông số cũ. `refreshHardwareStatus()` chỉ cập nhật cờ trạng thái, không khởi động lại luồng đã kết thúc.

**Hướng sửa:** biểu diễn lỗi và tuổi dữ liệu; phục hồi khi điều kiện trở lại, tránh retry vô hạn với lỗi quyền; ngăn nhiều collector. Kiểm chứng bằng nguồn dữ liệu phát lỗi rồi phục hồi và bằng thao tác quyền/mạng trên thiết bị.

### BUG-12 — Thời gian đo không bảo đảm 3 giây

**Vị trí:** [MeasureViewModel.kt](app/src/main/java/com/mobilelegend/wlo/presentation/measure_and_result/MeasureViewModel.kt), dòng 38, 45 và 51; [PingTester.kt](app/src/main/java/com/mobilelegend/wlo/core/network/PingTester.kt), dòng 16 và vòng lặp batch.

Có 2,2 giây delay cố định cộng thời gian ba lần TCP connect. Khi cả ba lần timeout 2 giây, tổng có thể khoảng 8,2 giây, chưa tính overhead. Các giai đoạn hiển thị chưa phản ánh chính xác thời điểm công việc thực hiện.

**Hướng sửa:** đặt thời hạn tổng, điều phối số mẫu phù hợp và bảo đảm đóng Socket khi hủy. Nếu không cần đúng 3 giây, đổi mô tả thành “đo nhanh trong vài giây”. Kiểm chứng cả mạng nhanh, chậm và timeout.

### BUG-13 — Đánh giá thấp ảnh hưởng của thất bại kết nối

**Vị trí:** [CalculateScoreUseCase.kt](app/src/main/java/com/mobilelegend/wlo/domain/usecase/CalculateScoreUseCase.kt), khoảng dòng 70–101; [PingTester.kt](app/src/main/java/com/mobilelegend/wlo/core/network/PingTester.kt), khoảng dòng 71–78.

Một trong ba lần TCP connect thất bại tạo tỷ lệ 33%, nhưng scoring chỉ xử lý tỷ lệ từ 50%. Với RSSI/LinkSpeed tốt và hai lần thành công có độ trễ thấp, Gaming vẫn có thể đạt 100 điểm và được khuyến nghị.

**Hướng sửa:** đưa tỷ lệ thất bại vào điểm hoặc điều kiện khuyến nghị. Gọi đúng chỉ số là tỷ lệ TCP connect thất bại, chưa phải packet loss thực tế; không suy luận chất lượng mọi game từ một đích duy nhất. Thêm kiểm thử các mức 0%, 33%, 66% và 100%.

### BUG-14 — Yêu cầu vị trí thiếu timeout/cleanup; dữ liệu cũ bị coi là hiện tại

**Vị trí:** [DeviceLocationProvider.kt](app/src/main/java/com/mobilelegend/wlo/data/sensor/DeviceLocationProvider.kt), khoảng dòng 71–87 và 103–141.

Listener chỉ được gỡ khi nhận vị trí mới. Nếu không có GPS fix, yêu cầu có thể tồn tại lâu; bấm làm mới nhiều lần có thể tạo thêm listener. Vị trí last-known được chọn theo accuracy nhưng chưa xét tuổi dữ liệu. Việc loại bỏ cả một vùng Mountain View cũng áp dụng cho thiết bị thật, không chỉ máy ảo.

**Hướng sửa:** timeout, hủy và cleanup theo vòng đời; quản lý một yêu cầu tại một thời điểm; xét tuổi/độ chính xác dữ liệu; phân biệt vị trí cũ, mới và chưa xác định. Chỉ áp dụng mô phỏng tọa độ trong chế độ demo rõ ràng.

**Kiểm chứng:** thử trong nhà không có fix, tắt GPS khi đang chờ, bấm làm mới nhiều lần và cung cấp last-known location cũ.

### BUG-15 — Danh sách Đã lưu không thể rỗng đúng nghĩa

**Vị trí:** [SavedPlacesScreen.kt](app/src/main/java/com/mobilelegend/wlo/presentation/saved/SavedPlacesScreen.kt), khoảng dòng 76–78.

`bookmarked.ifEmpty { places.take(2) }` khiến bỏ lưu hết vẫn hiện hai địa điểm đầu.

**Hướng sửa:** bỏ fallback và thêm trạng thái danh sách rỗng. Kiểm chứng bỏ lưu toàn bộ, lưu lại một địa điểm và đối chiếu số lượng hiển thị.

### BUG-16 — Nhu cầu đã chọn chưa tác động đến gợi ý địa điểm

**Vị trí:** [HomeScreen.kt](app/src/main/java/com/mobilelegend/wlo/presentation/home/HomeScreen.kt), khoảng dòng 100–109; [AppNavigation.kt](app/src/main/java/com/mobilelegend/wlo/presentation/navigation/AppNavigation.kt), dòng 66; [LiveRadarUiState.kt](app/src/main/java/com/mobilelegend/wlo/presentation/live_radar/LiveRadarUiState.kt), dòng 21.

Trang chủ chỉ lọc theo tên/quận, chưa dùng nhu cầu đã chọn để lọc/xếp hạng. Giá trị mặc định của Trang chủ là WORK, trong khi Radar mặc định GAMING; vào thẳng tab đo có thể dùng nhu cầu khác với lựa chọn đang thể hiện trên Trang chủ.

**Hướng sửa:** có một nguồn trạng thái nhu cầu thống nhất; lọc/xếp hạng theo nhu cầu và thông tin thực có. Kiểm chứng đổi nhu cầu, mở tab đo trực tiếp và mở qua Trang chủ.

### BUG-17 — So sánh chung các điểm có ý nghĩa khác nhau

**Vị trí:** [SessionHistoryBottomSheetContent.kt](app/src/main/java/com/mobilelegend/wlo/presentation/measure_and_result/SessionHistoryBottomSheetContent.kt), khoảng dòng 51 và 92–93.

Bảng chọn điểm cao nhất toàn bộ danh sách, trong khi Gaming, Download và Video Call dùng bộ trọng số khác nhau. Nhãn “TỐT NHẤT” có thể gây hiểu nhầm khi các lượt đo không cùng nhu cầu.

**Hướng sửa:** so sánh trong cùng nhu cầu; hiển thị nhu cầu, mạng và thời gian đo; gọi rõ “tốt nhất trong các điểm đã lưu cho nhu cầu này”. Kiểm chứng danh sách nhiều nhu cầu và trường hợp đồng điểm.

### BUG-18 — Tìm kiếm bản đồ rỗng vẫn hiển thị địa điểm khác

**Vị trí:** [WifiMapScreen.kt](app/src/main/java/com/mobilelegend/wlo/presentation/map/WifiMapScreen.kt), khoảng dòng 122–124.

Khi không có kết quả phù hợp, `selectedPlace` fallback về `places.first()`. Marker theo bộ lọc có thể không còn, nhưng thẻ thông tin vẫn chỉ tới một địa điểm ngoài kết quả tìm kiếm. Nếu nguồn danh sách sau này rỗng hoàn toàn, `first()` còn có thể ném ngoại lệ.

**Hướng sửa:** cho phép không có địa điểm được chọn; hiển thị trạng thái không có kết quả và ẩn thẻ/nút xem chi tiết. Kiểm chứng từ khóa không tồn tại, bộ lọc không khớp và danh sách đầu vào rỗng.

### BUG-19 — Quy tắc backup XML thiếu thuộc tính bắt buộc

**Vị trí:** [backup_rules.xml](app/src/main/res/xml/backup_rules.xml), dòng 3; [data_extraction_rules.xml](app/src/main/res/xml/data_extraction_rules.xml), dòng 4.

File cũ có `<exclude domain="root" />` thiếu `path`; file mới có `<exclude path="." />` thiếu `domain`. Chưa xác nhận mức ảnh hưởng bằng lint hoặc chạy backup.

**Hướng sửa:** bổ sung cả `domain` và `path` theo chính sách dữ liệu cần backup; kiểm tra lại bằng lint và nếu cần bằng luồng backup/restore. Nếu chỉ lưu phiên trong RAM, làm rõ chủ đích cấu hình thay vì giữ quy tắc không hợp lệ.

## 5. Các chi tiết nên hoàn thiện sau

| Vấn đề | Hướng xử lý |
| --- | --- |
| [LiveRadarScreen.kt](app/src/main/java/com/mobilelegend/wlo/presentation/live_radar/LiveRadarScreen.kt) coi mọi tần số trên 4900 MHz là 5 GHz; không có tần số vẫn hiện 2.4 GHz. | Phân loại đúng 2.4/5/6 GHz; giá trị không xác định hiển thị `—`. |
| [HomeScreen.kt](app/src/main/java/com/mobilelegend/wlo/presentation/home/HomeScreen.kt) có “8 điểm tốt”, “124 lượt đo” và nội dung xếp hạng theo dữ liệu thực tế nhưng nguồn là dữ liệu mẫu. | Dùng số liệu tính từ nguồn thực hoặc ghi rõ chế độ minh họa. |
| [ProfileScreen.kt](app/src/main/java/com/mobilelegend/wlo/presentation/profile/ProfileScreen.kt) chứa tài khoản mẫu, thống kê mẫu và nhãn “Luôn cho phép” không đọc từ quyền thực tế. | Dùng dữ liệu/trạng thái thực; chỉ giữ tính năng thuộc phạm vi nhóm đã chốt. |
| Công tắc thông báo chỉ đổi state trong RAM; nút Quyền riêng tư và chuông thông báo mặc định chưa có hành động thực. | Hoàn thiện hành động hoặc ẩn/ghi rõ tính năng chưa hỗ trợ; không thể hiện là đã thay đổi cài đặt hệ thống. |
| Nút định vị bản đồ gọi cập nhật bất đồng bộ nhưng di chuyển camera ngay bằng tọa độ cũ. | Di chuyển camera khi nhận được kết quả vị trí mới; có trạng thái đang định vị và thất bại. |
| Mỗi lần hoàn tất được tự thêm lịch sử, trong khi màn hình kết quả cũ vẫn có thao tác lưu thêm. | Chốt ý nghĩa “lượt đo” và “địa điểm đã lưu”; đặt tên/cập nhật cùng bản ghi khi phù hợp để tránh bản sao ngoài ý muốn. |

## 6. Khoảng trống kiểm thử

Source hiện có **33 test trong 6 file**. Con số này chưa chứng minh các luồng phần cứng, UI và vòng đời đã đạt yêu cầu; chưa có lần chạy mới trong lượt review này.

- Chưa thấy test cho `PermissionHelper`, `DeviceLocationProvider`, `LiveRadarViewModel` và `MeasureViewModel`.
- Chưa có thư mục `app/src/androidTest` trong phiên bản được review để kiểm thử giao diện/thiết bị.
- [MeasureSessionTest.kt](app/src/test/java/com/mobilelegend/wlo/presentation/measure_and_result/MeasureSessionTest.kt) chủ yếu thao tác danh sách tự tạo; chưa gọi hàm lưu/xóa thực tế của ViewModel.
- [StreamLiveWifiUseCaseTest.kt](app/src/test/java/com/mobilelegend/wlo/domain/usecase/StreamLiveWifiUseCaseTest.kt) dùng `flowOf()` một giá trị; chưa kiểm tra nhịp cập nhật một giây hay phục hồi sau lỗi.
- [PerformBenchmarkUseCaseTest.kt](app/src/test/java/com/mobilelegend/wlo/domain/usecase/PerformBenchmarkUseCaseTest.kt) chưa kiểm tra thời hạn tổng, hủy đo hoặc mất/đổi mạng giữa chừng.
- [PingTesterTest.kt](app/src/test/java/com/mobilelegend/wlo/core/network/PingTesterTest.kt) dùng kết nối mạng thật; giá trị latency khi lỗi hiện lấy timeout cấu hình nên assertion đó chưa chứng minh thời gian chờ thực tế.

**Các ca nên ưu tiên bổ sung khi bắt đầu sửa:**

1. Offline/0 điểm/timeout/Failed không bị hiển thị thành kết quả tốt.
2. Xem địa điểm B sau khi đo mạng A không trộn thông số.
3. Chuyển tab xem lại không đổi timestamp, mạng, GPS hoặc số bản ghi cũ.
4. Chỉ một phép đo được chạy; hủy đo không tạo kết quả hoàn tất hoặc lưu lịch sử ngoài ý muốn.
5. Mất Wi-Fi khi 4G đang bật không tạo kết quả đánh giá Wi-Fi từ 4G.
6. Thiếu quyền, chỉ quyền gần đúng, tắt vị trí và cấp lại quyền được xử lý nhất quán.
7. Radar phục hồi sau lỗi; GPS không có fix được timeout và cleanup.
8. Bỏ lưu hết, tìm kiếm không có kết quả, so sánh nhiều nhu cầu và nhiều điểm trên bản đồ.

## 7. Thứ tự sửa đề xuất

- [ ] **Đợt 1 — Tính đúng đắn trên màn hình:** BUG-01, BUG-02, BUG-03, BUG-04.
- [ ] **Đợt 2 — Điều phối phép đo và vòng đời:** BUG-05, BUG-06, BUG-07, BUG-10, BUG-11, BUG-12.
- [ ] **Đợt 3 — Quyền và định vị:** BUG-08, BUG-09, BUG-14.
- [ ] **Đợt 4 — Nghiệp vụ và danh sách:** BUG-13, BUG-15, BUG-16, BUG-17, BUG-18.
- [ ] **Đợt 5 — Cấu hình và bàn giao:** BUG-19, các chi tiết UI, chạy các kiểm tra phù hợp và cập nhật tài liệu.

Với mỗi lỗi: xác nhận tình huống hiện tại, sửa đúng phạm vi, kiểm tra lại ca lỗi và ca sử dụng bình thường. Chỉ ghi Pass khi đã có kết quả kiểm chứng.

### Checklist thử trên điện thoại cho Bước 5

| Ca thử | Kết quả cần đạt | Trạng thái |
| --- | --- | --- |
| Tắt/bật dịch vụ vị trí khi dùng app. | Cảnh báo đúng, mở được Settings, dữ liệu phục hồi sau khi bật lại. | Chưa kiểm chứng trong lượt review. |
| Từ chối quyền, chỉ cấp gần đúng, cấp lại chính xác. | Xác định đúng điều kiện còn thiếu; không báo đủ quyền sai. | Chưa kiểm chứng trong lượt review. |
| Tắt Wi-Fi, chỉ dùng 4G, offline rồi kết nối lại. | Không đo sai mạng; khóa/hướng dẫn đúng; Radar phục hồi. | Chưa kiểm chứng trong lượt review. |
| Mất/đổi mạng khi đo; Back/chuyển tab rồi đo tiếp. | Thất bại/hủy an toàn; không có kết quả ghép hoặc tác vụ chồng. | Chưa kiểm chứng trong lượt review. |
| Đi gần/xa router và quay lại. | RSSI cập nhật, màu đúng ngưỡng, xu hướng hợp lý qua nhiều mẫu. | Chưa kiểm chứng trong lượt review. |

RSSI có dao động tự nhiên; không yêu cầu giá trị tăng/giảm đều từng giây. Ghi rõ thiết bị, phiên bản Android, thao tác và kết quả thực tế khi đánh dấu Pass/Fail.

## 8. Tham khảo API chính thức

- [ConnectivityManager.getNetworkCapabilities — dữ liệu nhạy cảm và callback](https://developer.android.com/reference/android/net/ConnectivityManager#getNetworkCapabilities(android.net.Network)).
- [WifiInfo — quyền truy cập và ý nghĩa LinkSpeed](https://developer.android.com/reference/android/net/wifi/WifiInfo).
- [Network.getSocketFactory — gắn Socket với một mạng](https://developer.android.com/reference/android/net/Network#getSocketFactory()).
- [ViewModel Scoping APIs — phạm vi và cách lấy ViewModel](https://developer.android.com/topic/libraries/architecture/viewmodel/viewmodel-apis).
- [Auto Backup — cú pháp domain/path](https://developer.android.com/identity/data/autobackup).

