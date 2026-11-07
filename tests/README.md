# Kiểm thử BUG-01 đến BUG-15

Ngày ghi nhận: **07/11/2026** (Asia/Saigon).

## Trạng thái thực tế

**Đã sửa code và viết test; chưa chạy build hoặc test.** Phiên làm việc chỉ cung cấp shell Windows tại workspace dùng chung trên máy người dùng. Không có runner JDK/Android độc lập của trợ lý để thực thi. Theo yêu cầu “đừng test ở máy tôi”, không chạy Gradle, ứng dụng, emulator, ADB, socket probe hoặc cài dependency trên máy này.

Không có kết quả `BUILD SUCCESSFUL`, JUnit XML hoặc ảnh chạy ứng dụng để khẳng định test đã đạt. Đọc source, tìm tham chiếu, đếm khai báo và kiểm tra diff là **rà soát tĩnh**, không thay thế biên dịch hoặc thực thi test. Các tuyên bố “100% Passed” trong tài liệu cũ không phải kết quả của đợt sửa này.

| Hạng mục | Kết quả |
| --- | --- |
| Rà luồng cảm biến → phiên mạng → probing → scoring → ViewModel → UI/navigation | Đã đọc và đối chiếu source |
| Kiểm tra whitespace bằng `git diff --check` | Exit code 0; không có lỗi whitespace |
| BUG-01–03 | Giữ lại thay đổi và 29 test từ lượt trước |
| BUG-04–06 | Thêm 36 test trong 3 file; cập nhật các repository giả theo API phiên đo |
| BUG-07–09 | Thêm 31 test trong 3 file; cập nhật fixture quyền/readiness và dữ liệu vị trí |
| BUG-10–11 | Thêm 30 test trong `LiveRadarLifecycleTest.kt`; cập nhật fixture mẫu Wi-Fi |
| BUG-12–13 | Thêm 26 test: 13 deadline/tiến trình, 12 TCP/scoring, 1 socket giả bị đóng khi timeout |
| BUG-14–15 | Thêm 45 test: 25 quản lý yêu cầu vị trí, 8 chọn/đánh giá mẫu và 12 danh sách Đã lưu |
| Toàn bộ test hồi quy trong `tests/` | **197 phương thức `@Test`, 0 đã thực thi** |
| Test có sẵn trong `app/src/test/` | **33 phương thức `@Test`, chưa chạy lại** |
| Tổng số khai báo test | **230**, đếm từ source; chưa xác nhận Gradle discover |
| Build / assemble / lint / unit test / UI test | **Chưa chạy** |
| Wi-Fi, quyền Android, GPS, Back/tab/background thật | **Chưa kiểm chứng trên runtime** |

## Thay đổi và kiểm tra BUG-14–15

### BUG-14: một yêu cầu vị trí, tuổi dữ liệu và vòng đời

- Tách `LocationRequestController` khỏi adapter Android để kiểm tra bằng nguồn vị trí/đồng hồ/deadline giả. Mỗi lượt có ID riêng, một listener và hạn chờ 10.000 ms. Lượt mới hủy lượt trước; callback hoặc timeout cũ không được hoàn tất lượt mới. Nhận fix hợp lệ, timeout, lỗi đăng ký, provider bị tắt hoặc hủy đều đi qua cleanup.
- Navigation cho phép yêu cầu khi lifecycle STARTED, dừng và hủy khi xuống nền/dispose; thay đổi quyền khởi động lại vòng theo dõi. Provider và PermissionHelper giữ application context. Callback đến ngay trong lúc đăng ký được hoãn hoàn tất đến khi có handle để cleanup; request/cancel gọi lại trong callback hoặc cleanup không ghi đè lượt mới.
- Đánh giá tất cả mẫu last-known và mẫu vừa nhận: ưu tiên mẫu dùng được, sau đó mới nhất, dùng accuracy khi cùng timestamp. Mẫu mock/sai/timestamp tương lai không che mất mẫu thật còn mới. Callback không hợp lệ tiếp tục chờ đến khi có fix hợp lệ hoặc hết hạn. Tuổi tính lại từ monotonic timestamp trên mỗi lần đọc; tối đa 120 giây, Coarse hoặc sai số trên 100 m được ghi gần đúng. Mountain View thật vẫn giữ nguyên.
- Bản đồ hiện tuổi/độ chính xác cùng trạng thái đang chờ, đã cập nhật, timeout, unavailable, lỗi hoặc hủy. Cache còn hiệu lực không được coi là một lần cập nhật thành công. Nút định vị chờ đúng ID hoàn tất với fix hợp lệ rồi mới đưa camera về vị trí; bỏ timer UI 10,5 giây riêng.
- Hạn chờ dùng Handler trên main looper, không phải cam kết callback đúng thời điểm khi main thread bị chặn. Việc gỡ listener trên Android thật và rendering Compose vẫn cần kiểm thử tích hợp.

### BUG-15: danh sách rỗng và thống kê từ lịch sử thật

- Giữ việc bỏ fallback đã thực hiện ở BUG-09; bổ sung `savedPlacesContent` lấy danh sách và số lượng trực tiếp từ cùng snapshot lịch sử ViewModel. Bỏ lưu toàn bộ cho danh sách rỗng; lưu lại một bản ghi cho đúng một dòng. Không có đầu vào demo hay danh sách thay thế.
- Bỏ số cố định “3 khu vực”. Ba thống kê là số địa điểm đã lưu có tọa độ dùng được, tổng lượt đo trong phiên và số kết quả đã lưu thiếu vị trí hợp lệ. Bỏ bookmark không xóa lịch sử, nên tổng lượt đo vẫn giữ; xóa bản ghi mới giảm tổng.
- Kết quả thiếu GPS vẫn xem được trong Lịch sử đo mạng và có thông báo riêng. Toggle dùng ID phép đo; đổi tên/bookmark không đổi snapshot, SSID, thời điểm, tọa độ hoặc số liệu.

### 45 test mới đã viết — chưa chạy

| File | Số ca | Tình huống được mô tả bằng assertion | Kết quả chạy |
| --- | ---: | --- | --- |
| `LocationRequestControllerTest.kt` | 25 | Không fix/timeout; cache mới/cũ; hủy/lặp refresh/background; callback/timer muộn; callback đồng bộ trong đăng ký; lỗi đăng ký/deadline/quyền; provider tắt; invalid/mock/stale; cache già đi; Coarse; request/cancel gọi lại trong callback/cleanup | Chưa chạy |
| `LocationSampleSelectionTest.kt` | 8 | Mới nhất trong mẫu dùng được; mock/invalid/future không che mẫu tốt; accuracy khi cùng tuổi; stale/rỗng; monotonic không phụ thuộc giờ hệ thống; timestamp lỗi; ranh giới 120 giây/100 m; Mountain View thật | Chưa chạy |
| `SavedPlacesContentTest.kt` | 12 | Rỗng → nhiều → bỏ lưu hết → lưu một; xóa hết/đo mới; thiếu GPS; số lượng đúng; ID khác nhau cùng SSID/time; di chuyển chỉ đổi khoảng cách; đổi tên/lưu lặp; tọa độ lỗi; ID demo/lạ; giữ snapshot khi lưu lại | Chưa chạy |

Các test mới chỉ dùng fake/manual deadline hoặc `StandardTestDispatcher` và `FakeWifiRepository`; không cần GPS, Wi-Fi, socket hay sleep thật. Test danh sách gọi các hành động đo/bookmark/lưu/xóa của ViewModel rồi kiểm tra projection mà màn hình dùng. Chưa chạy cả những test giả này vì người dùng đã từ chối chạy trên máy của họ; phiên này không có runner độc lập. **Không có Pass/Fail, log Gradle hay JUnit XML.**

Rà soát tĩnh đã thực hiện: `git -c core.safecrlf=false diff --check` exit **0**; đối chiếu chữ ký/call site provider, navigation, bản đồ và Đã lưu; đếm **25 + 8 + 12 = 45** khai báo mới, tổng **197 + 33 = 230**. Không có import trùng trong Kotlin thay đổi/thêm mới hoặc tên test trùng trong ba file mới. Đây là kiểm tra source/Git, **không chứng minh Kotlin biên dịch hay test chạy thành công**.

## Thay đổi và kiểm tra BUG-12–13

### BUG-12: thời hạn và tiến trình thực tế

- Nhãn Radar đổi từ “3s” sang “Đo nhanh mạng Wi-Fi hiện tại”. Đợt trước đã bỏ delay minh họa 2,2 giây; đợt này đặt **hạn tổng 10.000 ms** từ khi use case bắt đầu, bao gồm dispatch, kiểm tra/chờ phiên Wi-Fi, ba lượt TCP connect, chấm điểm và trả kết quả. Timeout mỗi connect vẫn là 2.000 ms; chờ callback Android vẫn tối đa 3.000 ms, cùng nằm trong hạn tổng.
- Đây là deadline hủy coroutine, **không cam kết màn hình luôn hoàn tất đúng 10 giây**: vẫn phải đợi đóng socket/gỡ callback và cleanup. Không mở lượt mới khi lượt cũ chưa dọn xong, không lưu kết quả đến sau deadline. Không thêm delay cho mạng nhanh.
- Hết hạn tổng chuyển thành `MeasureStatus.Failed`. Hủy từ người dùng/lifecycle/timeout của caller vẫn là cancellation. `withTimeoutOrNull` chỉ chuyển timeout do chính use case tạo ra.
- `BenchmarkStage` phát tại các mốc thật: chuẩn bị/chờ mạng, ba TCP connect, kiểm tra và chấm điểm. Các phần trăm là mốc công việc, không phải đồng hồ đếm thời gian.

### BUG-13: tỷ lệ TCP thất bại ảnh hưởng mọi mức điểm

- Đổi `PingResult.packetLossPercent` thành `tcpFailurePercent`; kiểm tra giá trị 0–100 và tính nhất quán với cờ có ít nhất một lượt thành công. Batch giữ cách làm tròn xuống: 1/3 là 33%, 2/3 là 66%. Độ trễ trung bình chỉ lấy các lượt thành công.
- Điểm mới = **làm tròn(điểm có trọng số × (100 − % TCP thất bại) / 100)**. Với các thông số đạt tối đa, 0%/33%/66%/100% tương ứng **100/67/34/0** điểm. Đây là policy chấm điểm của app, không phải chuẩn đo packet loss.
- Có bất kỳ TCP thất bại nào đều không khuyến nghị tốt, áp dụng cả ba nhu cầu. Bỏ nhánh cộng cố định 15 điểm khi thất bại; mạng yếu không được nâng điểm bởi nhánh fallback này.
- Lưu `tcpFailurePercent` trong `LocationScore`; màn đo, màn kết quả, lịch sử và chi tiết điểm đã đo đọc cùng snapshot. Dữ liệu chưa đo/không có tỷ lệ hiển thị `—`, không suy diễn thành 0%.
- Nội dung đánh giá giới hạn vào đích TCP được thử, bỏ lời hứa về Liên Quân/PUBG/Video HD. Kết nối tới một đích thất bại không đủ chứng minh toàn bộ Internet bị mất. Các test cũ được cập nhật tên trường và assertion theo policy mới; số lượng vẫn là 33.

### Test mới và tình trạng thực thi

| File/nhóm | Số test mới | Kỳ vọng | Trạng thái |
| --- | ---: | --- | --- |
| `BenchmarkTimingTest` | 13 | Mạng nhanh/chậm, ba TCP thất bại trong 6 giây vẫn lưu 0 điểm, timeout setup/probe, biên 9.999/10.000 ms, caller/manual cancellation, cleanup chậm, kết quả muộn, tiến trình | Chưa chạy |
| `TcpFailureScoringTest` | 12 | 0/33/66/100% cho ba nhu cầu, điểm giảm đơn điệu, Wi-Fi yếu, dữ liệu không hợp lệ, giới hạn nhận xét, formatter, offline, snapshot/history/place, batch socket giả | Chưa chạy |
| Thêm vào `PingCancellationTest` | 1 | Deadline đóng socket đang kẹt; coroutine không trả success hay biến hủy thành kết quả TCP | Chưa chạy |

Hai file mới dùng dữ liệu giả và thời gian ảo. Ca mới của `PingCancellationTest` dùng worker thread/latch với deadline 1 giây và chặn an toàn 3 giây; `Socket.connect` được override nên không có I/O mạng. Phân biệt timeout toàn phép đo (Failed, không lưu) với ba TCP connect thất bại hoàn tất trong hạn (Completed, 0 điểm, 100% thất bại).

**Kết quả thực tế:** mới rà source, chữ ký/API, assertion cũ và whitespace của diff; chưa biên dịch hoặc chạy Kotlin/Gradle. Người dùng đã xác nhận lại không chạy trên máy của họ, kể cả bộ test giả. Không chạy app, Gradle, emulator, ADB, socket probe hay cài dependency. Không có runner độc lập trong phiên này, nên không có kết quả Pass/Fail hoặc JUnit XML để báo cáo.

Rà soát tĩnh BUG-12–13: `git -c core.safecrlf=false diff --check` exit **0**; đếm **152 + 33 = 185** khai báo `@Test`; import trùng trong Kotlin thay đổi/thêm mới: **0**. Tìm `packetLossPercent`, `3s`, `3 giây`, `Rớt gói`, `ICMP/Socket` trong `app/src/main`: không còn kết quả (`rg` exit 1). Các kiểm tra này chỉ đọc source/Git, không thực thi test hay chứng minh biên dịch thành công.

## Thay đổi và kiểm tra BUG-10–11

### BUG-10: vòng đời Radar

- Giữ `ViewModelProvider`/factory và application context đã đưa vào từ BUG-04–06. Radar không còn nhận trực tiếp `PermissionHelper`; chỉ giữ repository/use case và đồng hồ monotonic.
- `MainActivity.onStart/onStop` bật/tắt quan sát. Chuyển tab giữ một observer chung vì Trang chủ và Đo mạng cũng dùng Radar; xuống nền dừng polling và nguồn Wi-Fi. `onResume`, callback cấp quyền và nút thử lại yêu cầu làm mới; refresh khi đang ở nền không truy cập phần cứng.
- Yêu cầu foreground/refresh có revision; `collectLatest` hủy và chờ cleanup trước khi tạo collector tiếp theo. Stop/start nhanh không bị gộp thành trạng thái cũ; dữ liệu/lỗi trả về muộn không được thay trạng thái của lượt mới.
- ViewModelStore giữ instance/nhu cầu khi Activity được tạo lại, clear hủy scope. Các test giả đã được viết; Activity recreation, process death và việc gỡ callback Android thật **chưa chạy**.

### BUG-11: lỗi, tuổi mẫu và phục hồi

- Bỏ `.catch {}` rỗng. Phân biệt nguồn lỗi, quyền bị từ chối, Flow kết thúc ngoài dự kiến và không có mẫu mới. Ngoại lệ đăng ký callback Wi-Fi được chuyển tới Radar.
- Lỗi/stop/thay điều kiện làm mất hiệu lực mẫu hiện tại. Sau **5 giây** không nhận mẫu dùng được, watchdog ẩn dữ liệu kể cả khi cleanup chưa xong; readiness khóa phép đo mới. Tuổi mẫu tính bằng đồng hồ monotonic và cập nhật mỗi giây khi foreground. Đây là tuổi mẫu nhận từ luồng Radar, không phải timestamp phần cứng của `WifiInfo`.
- Tối đa **3 lần tự thử lại**, chờ **1/2/4 giây**, tức tối đa 4 lần mở nguồn trong một chu kỳ. Một mẫu thành công không xóa ngân sách retry, để tránh vòng lặp phát-mẫu-rồi-lỗi vô hạn. Chu kỳ mới bắt đầu khi foreground trở lại, điều kiện quyền/Wi-Fi/vị trí thay đổi hoặc người dùng bấm thử lại.
- `SecurityException` dừng retry nguồn ngay; chỉ tiếp tục kiểm tra điều kiện mỗi giây. Thay đổi điều kiện hoặc refresh cho phép đăng ký lại. `CancellationException` đi theo luồng hủy, không trở thành thông báo lỗi.
- Trang chủ, Đo mạng và Radar hiển thị thông báo/tuổi mẫu/nút thử lại. Radar không hiện RSSI, LinkSpeed hoặc tần số giả khi chưa có mẫu. Kết quả đo đã hoàn tất vẫn dùng snapshot bất biến của chính nó.

### 30 test mới đã viết — chưa thực thi

| Nhóm tình huống | Số ca | Kỳ vọng trong assertion | Kết quả chạy |
| --- | ---: | --- | --- |
| Khởi tạo ở nền, start lặp, stop, stop/start nhanh, refresh khi cleanup, stop khi cleanup | 6 | Không đọc phần cứng ở nền; tối đa một collector; chờ cleanup | Chưa chạy |
| ViewModelProvider/Store: recreation, clear, mở lại | 3 | Giữ instance/nhu cầu khi store còn; hủy khi clear; instance mới khi mở lại | Chưa chạy |
| Nguồn lỗi rồi hồi phục, backoff giới hạn, emit-rồi-lỗi, retry thủ công | 4 | Ẩn mẫu lỗi; lịch 1/2/4 giây; tối đa 4 lần mở nguồn | Chưa chạy |
| SecurityException, thu hồi/cấp lại quyền, bật vị trí, mất/kết nối Wi-Fi | 5 | Không retry quyền vô hạn; điều kiện phục hồi khởi động lại | Chưa chạy |
| Flow kết thúc, im lặng, không có mẫu đầu, cleanup chậm, mẫu unavailable, luồng khỏe | 6 | Flow kết thúc là lỗi; watchdog 5 giây; không timeout giả với mẫu định kỳ | Chưa chạy |
| Xuống nền trong backoff, lỗi cleanup muộn, emission NonCancellable muộn | 3 | Không retry ngầm hoặc ghi đè trạng thái đã tạm dừng | Chưa chạy |
| Lỗi đọc điều kiện, lỗi refresh, kết quả đo cũ | 3 | Báo lỗi/hồi phục; không mở nguồn khi refresh bị từ chối; giữ snapshot đã đo | Chưa chạy |

Các test dùng `StandardTestDispatcher`, thời gian ảo và repository giả, không tạo socket hay gọi Wi-Fi thật. Dùng `runCurrent`/`advanceTimeBy` có giới hạn; clear ViewModelStore khi teardown. Không dùng `advanceUntilIdle` cho polling vô hạn. Test ViewModelStore không thay thế test Activity/Compose trên Android.

### Kết quả rà soát tĩnh của đợt này

- `git -c core.safecrlf=false diff --check`: exit **0**, không có lỗi whitespace trong diff được theo dõi.
- `rg -c '@Test' tests/src/test/kotlin`: **126** khai báo; `app/src/test`: **33**. File mới có **30** khai báo; không phải 30 test đã pass.
- Đối chiếu các lời gọi constructor Radar và chữ ký Home/Measure/Radar với navigation; fixture `onlineRadar()` đặt `isSampleAvailable = true`, còn trạng thái mặc định là false.
- Kiểm tra import trùng trong các file Kotlin thay đổi/thêm mới: **0**.
- Rà thứ tự cancel/join, kiểm tra revision/điều kiện trước khi nhận mẫu, dừng backoff khi xuống nền, invalidation trước cleanup chậm và giữ nguyên luồng kết quả đã hoàn tất.
- **Chưa có exit code của Gradle, JUnit XML hay log runtime.** Không khẳng định Kotlin đã biên dịch hoặc callback Android đã được dọn thành công trên thiết bị.

## Thay đổi BUG-04–06 cần kiểm chứng

### BUG-04: lịch sử thuộc ViewModel, giao diện chỉ đọc

- Kết quả lưu snapshot gồm ID, thời gian hoàn tất, SSID/BSSID, Android network ID, tọa độ đầu lượt đo nếu có, nhu cầu và thông số. ViewModel ghi một lần theo ID; giữ ID đã ghi cả khi người dùng xóa bản ghi.
- `saveSpotToHistory` đổi tên/đánh dấu lưu bản ghi hiện có. Không tạo UUID mới, không thay score/time/location bằng dữ liệu truyền lại, không khôi phục bản ghi đã xóa từ kết quả cũ.
- Bản đồ dựng danh sách trực tiếp từ lịch sử trong ViewModel. Đã bỏ effect thêm kết quả khi mở lại tab; không có tọa độ thật thì không tạo điểm bản đồ.
- Bỏ lưu chỉ đổi trạng thái bookmark; xóa lịch sử loại bản ghi khỏi nguồn dữ liệu bản đồ. Dữ liệu minh họa được giữ riêng.
- ViewModel được lấy qua `ViewModelProvider` với dependency dùng application context, để giữ lịch sử khi Activity được tạo lại và có cơ chế dọn scope. Lịch sử vẫn chỉ ở RAM, không được lưu qua process death.

### BUG-05: một mạng Wi-Fi cho toàn bộ phiên đo

- Repository mở `WifiBenchmarkSession` có phạm vi và luôn đóng trong `finally`, kể cả lỗi hoặc hủy trong lúc chờ thông tin mạng.
- Phiên giữ một Android `Network`; socket tạo bằng `network.socketFactory`. Không chuyển sang socket mặc định khi mất Wi-Fi.
- Callback theo dõi mạng mặc định và thông tin AP. Android 12+ dùng `FLAG_INCLUDE_LOCATION_INFO`; phiên từ chối khi không đọc được định danh.
- Guard so sánh network ID, SSID và BSSID; đã phát hiện thay đổi thì không phục hồi lượt đo dù mạng quay về A. Socket đang mở bị đóng khi mạng mất/đổi.
- Kiểm tra lại định danh hiện tại trước/sau từng lần TCP connect, sau probing và sau tạo kết quả. RSSI/LinkSpeed của kết quả thuộc snapshot ban đầu, không trộn dữ liệu lần đọc sau.
- Việc callback, quyền và đọc BSSID hoạt động trên từng thiết bị/API còn cần kiểm thử Android thật. Không coi các test guard giả là bằng chứng đã thử tắt Wi-Fi/chuyển 4G hoặc roaming phần cứng.

### BUG-06: một tác vụ, hủy có dọn tài nguyên

- ViewModel giữ một `benchmarkJob`; lần gọi mới bị từ chối cho đến khi job trước kết thúc cả phần cleanup.
- Job khởi tạo lazy để gán quyền sở hữu trước khi chạy. Completion handler dọn cờ bận kể cả hủy trước khi coroutine bắt đầu.
- Mỗi lượt có mã thế hệ; hủy/reset làm lượt cũ mất hiệu lực. `CancellationException` được xử lý riêng; kết quả hoặc lỗi về muộn không đổi UI/history.
- `PingTester` đóng socket khi coroutine bị hủy và trong `finally`; session đóng socket còn giữ và gỡ callback.
- Có nút “Hủy phép đo”. Rời màn đo bằng Back/tab, hủy graph hoặc Activity vào `onStop` đều yêu cầu hủy. Không tự bắt đầu lại lượt đo chỉ vì màn cũ được compose lại.
- Đợt BUG-04–06 đã bỏ các delay tiến trình giả, không chờ thêm 800/400 ms sau khi kiểm tra kết quả. Hạn tổng 10 giây được bổ sung trong đợt BUG-12 ở phía trên.
- UI báo đã hủy và khóa lần đo mới trong lúc dọn kết nối. Điều hướng của màn cũ không được đưa người dùng tới kết quả nếu đã rời route đó.

## Thay đổi BUG-01–03 được giữ lại

- **BUG-01:** phân biệt Idle/chưa đủ điều kiện/Measuring/Failed/Completed; thêm Cancelled trong đợt này. Giữ điểm 0, thiếu dữ liệu hiện `—`. Timeout hiện “Không phản hồi”; ping thành công chậm vẫn hiện đúng số ms. Khuyến nghị chỉ dựa vào thông số có đủ dữ liệu.
- **BUG-02:** dùng “Tốc độ liên kết Wi-Fi”, bỏ download/upload suy diễn và các giá trị sàn. Không đổi nhãn tốc độ download mẫu thành LinkSpeed. Các màn và lịch sử dùng chung formatter.
- **BUG-03:** màn hình chọn một nguồn: địa điểm đang xem hoặc kết quả benchmark. Bấm đo mạng đang kết nối thoát chế độ xem địa điểm; không ghép tên B với thông số A.

Tọa độ chỉ gắn vào lượt đo khi provider có fix mới/gần đúng còn đủ mới. Các ngưỡng và giới hạn được ghi trong phần BUG-07–09 dưới đây. Chưa xác nhận toàn bộ lỗi vòng đời/radar hoặc các BUG còn lại đã được giải quyết.

## Thay đổi BUG-07–09 cần kiểm chứng

### BUG-07: một bộ điều kiện bắt đầu đo

- `benchmarkReadiness` dùng chung cho Trang chủ, tab Đo, Radar cũ, màn thử lại, ViewModel và use case. Nút bị khóa khi bận, không có Wi-Fi, thiếu quyền/dịch vụ vị trí, chưa có mẫu callback, thiếu định danh hoặc RSSI/LinkSpeed không hợp lệ.
- ViewModel kiểm tra trước khi tạo job; use case kiểm tra lại khi thực thi, kiểm tra mẫu của phiên vừa mở trước probing và quyền trước khi chấp nhận kết quả. Quyền/mạng đổi sau khi nút được bật không được phép vượt qua kiểm tra thực thi.
- Có lý do và hành động tương ứng: mở Wi-Fi, quyền ứng dụng hoặc dịch vụ vị trí. Trạng thái chờ dữ liệu và trạng thái bận không giả làm lỗi GPS.
- Mọi lối bắt đầu đo lấy vị trí tại lần bấm nếu có. Vị trí chưa xác định không ngăn đo Wi-Fi; kết quả đó chỉ ở lịch sử, không có marker giả.

### BUG-08: quyền bản đồ và quyền đọc Wi-Fi

- `WifiAccessState` tách quyền bản đồ (Fine hoặc Coarse) với quyền đọc định danh Wi-Fi. Policy: API 26–28 chấp nhận Fine/Coarse; API 29+ cần Fine; API 28+ cần bật dịch vụ vị trí.
- Android 12+ lấy `WifiInfo` từ `NetworkCallback` có `FLAG_INCLUDE_LOCATION_INFO`. Không lấy SSID/BSSID từ kết quả đồng bộ `getNetworkCapabilities()`. API cũ dùng `WifiManager.connectionInfo`.
- Cache gắn với network ID và thế hệ đăng ký. Đổi mạng, mất mạng, thay quyền/dịch vụ vị trí hoặc quay lại foreground làm dữ liệu cũ mất hiệu lực; callback của đăng ký cũ không được khôi phục dữ liệu đó.
- SSID/BSSID bị ẩn có trạng thái riêng. Không đổi SSID chưa biết thành tên “Cần bật GPS”.
- Bỏ yêu cầu/khai báo `NEARBY_WIFI_DEVICES`: app hiện chỉ đọc Wi-Fi, không gọi API hotspot/P2P/Aware/RTT cần quyền này. Nearby không thay thế quyền vị trí cho SSID/BSSID. Cần thử Android 13+ cả bản cài mới và nâng cấp từ bản đã từng xin Nearby.

### BUG-09: tọa độ, độ tin cậy và dữ liệu minh họa

- Không di chuyển quán/thư viện mẫu quanh người dùng. `placesWithDistances` chỉ cập nhật nhãn khoảng cách; không sửa ID, tọa độ hoặc snapshot.
- Bản đồ mặc định ẩn demo; chỉ hiện khi bật “Hiện dữ liệu minh họa”, với ghim xám và nhãn rõ. Trang chủ/Đã lưu chỉ lấy điểm có measurement thật. Bỏ fallback đưa dữ liệu mẫu trở lại Đã lưu; cho phép danh sách rỗng.
- Tâm bản đồ mặc định không tạo marker người dùng. Fix mới/gần đúng được ghi kèm thời gian, accuracy và confidence; fix cũ, giả lập hoặc không hợp lệ không gắn vào lượt đo.
- Ngưỡng hiện tại: tuổi fix tối đa **120 giây** theo đồng hồ monotonic; accuracy trên **100 m** hoặc quyền Coarse được đánh dấu gần đúng. Đây là policy ứng dụng, không phải bảo đảm độ chính xác GPS. Không loại vị trí thật chỉ vì nằm trong khu vực Mountain View.
- Một yêu cầu location tại một thời điểm; timeout **10 giây**, hủy listener khi rời foreground/dispose. Camera chỉ recenter theo cập nhật vị trí hợp lệ sau yêu cầu. Cleanup/timeout Android thật chưa được thực thi kiểm chứng.
- Mỗi lần đo giữ ID riêng và tọa độ nguyên gốc; hai điểm A/B cùng tồn tại. Bỏ các thống kê mẫu 8 điểm/124 lượt và cộng thêm 12 lượt trong luồng đã chỉnh. Lịch sử vẫn ở RAM.

Thay đổi trạng thái rỗng, age/accuracy và cleanup có chạm một phần BUG-14/15/18; chưa xác nhận runtime cho toàn bộ các BUG này.


## Bộ test hồi quy

Thư mục `tests/src/test/kotlin/com/mobilelegend/wlo/regression/` đã được nối vào test source set của module app. Dùng JUnit 4 và kotlinx-coroutines-test đã có trong project.

| File | Số test | Phạm vi | Trạng thái |
| --- | ---: | --- | --- |
| [NetworkMeasureContentTest.kt](src/test/kotlin/com/mobilelegend/wlo/regression/NetworkMeasureContentTest.kt) | 16 | Offline, quyền/GPS, trạng thái đo, điểm 0, timeout, ping chậm, số liệu thiếu, LinkSpeed thấp, snapshot A/B, dữ liệu mẫu | Chưa chạy |
| [BenchmarkRegressionTest.kt](src/test/kotlin/com/mobilelegend/wlo/regression/BenchmarkRegressionTest.kt) | 7 | Snapshot/ID, offline, thiếu điều kiện, timeout xuyên suốt scoring/history/place, lỗi sau lượt thành công, lưu điểm 0 | Chưa chạy |
| [MeasuredWifiPlaceTest.kt](src/test/kotlin/com/mobilelegend/wlo/regression/MeasuredWifiPlaceTest.kt) | 6 | Formatter, tọa độ/định danh, không bịa điểm bản đồ/tốc độ, SSID/BSSID bị ẩn | Chưa chạy |
| [MeasurementOwnershipTest.kt](src/test/kotlin/com/mobilelegend/wlo/regression/MeasurementOwnershipTest.kt) | 17 | Xem/lưu/xóa/bookmark, snapshot bất biến, bấm liên tục, hủy trước dispatch/trong setup/probe, chờ cleanup, reset và kết quả/lỗi muộn, ViewModelStore, dispatcher chạy ngay | Chưa chạy |
| [NetworkSessionRegressionTest.kt](src/test/kotlin/com/mobilelegend/wlo/regression/NetworkSessionRegressionTest.kt) | 11 | Định danh ẩn, mất/đổi mạng/AP, A→B→A, callback báo mất, kiểm tra trước probing/sau scoring, giữ snapshot và đóng phiên giả | Chưa chạy |
| [PingCancellationTest.kt](src/test/kotlin/com/mobilelegend/wlo/regression/PingCancellationTest.kt) | 9 | Socket factory, đóng khi thành công/lỗi/hủy/deadline, chặn đổi mạng, lỗi binding, kết quả muộn, số lượt không hợp lệ | Chưa chạy |
| [WifiReadinessTest.kt](src/test/kotlin/com/mobilelegend/wlo/regression/WifiReadinessTest.kt) | 14 | Quyền API 26–34; busy/offline/chờ/ẩn định danh/sensor lỗi; UI và ViewModel cùng lý do; use case kiểm tra lại; thu hồi/cấp lại quyền | Chưa chạy |
| [WifiSnapshotCacheTest.kt](src/test/kotlin/com/mobilelegend/wlo/regression/WifiSnapshotCacheTest.kt) | 5 | Cache theo network ID, đổi mạng, callback mất mạng cũ, đăng ký cũ sau thay quyền và mẫu redacted | Chưa chạy |
| [LocationDataPolicyTest.kt](src/test/kotlin/com/mobilelegend/wlo/regression/LocationDataPolicyTest.kt) | 12 | Tâm mặc định, metadata, gần đúng/cũ/mock/invalid, Mountain View thật, không kéo demo, lọc demo, khoảng cách và hai lượt A/B | Chưa chạy |
| [LiveRadarLifecycleTest.kt](src/test/kotlin/com/mobilelegend/wlo/regression/LiveRadarLifecycleTest.kt) | 30 | Foreground, cleanup, ViewModelStore, lỗi/retry, quyền/mạng, dữ liệu hết hạn và kết quả muộn | Chưa chạy |
| [BenchmarkTimingTest.kt](src/test/kotlin/com/mobilelegend/wlo/regression/BenchmarkTimingTest.kt) | 13 | Deadline chung, mạng nhanh/chậm, cancellation/cleanup/kết quả muộn, tiến trình thực | Chưa chạy |
| [TcpFailureScoringTest.kt](src/test/kotlin/com/mobilelegend/wlo/regression/TcpFailureScoringTest.kt) | 12 | Tỷ lệ thất bại, scoring, phạm vi nhận xét, snapshot và batch TCP giả | Chưa chạy |
| [LocationRequestControllerTest.kt](src/test/kotlin/com/mobilelegend/wlo/regression/LocationRequestControllerTest.kt) | 25 | Timeout, cleanup, foreground, callback cũ/đồng bộ, lỗi và request gọi lại | Chưa chạy |
| [LocationSampleSelectionTest.kt](src/test/kotlin/com/mobilelegend/wlo/regression/LocationSampleSelectionTest.kt) | 8 | Chọn mẫu còn hiệu lực, tuổi monotonic, accuracy, mock/invalid, Mountain View | Chưa chạy |
| [SavedPlacesContentTest.kt](src/test/kotlin/com/mobilelegend/wlo/regression/SavedPlacesContentTest.kt) | 12 | Bỏ lưu hết/lưu một, thống kê, thiếu vị trí, xóa/lưu lại và snapshot | Chưa chạy |

197 test hồi quy dùng dữ liệu giả và không thực hiện kết nối mạng thật. `PingCancellationTest` override `Socket.connect`; hai ca dùng worker thread/latch với timeout để mô phỏng connect đang kẹt, những ca coroutine khác dùng scheduler kiểm thử. Các kiểm tra này chưa thay thế test tích hợp `AndroidWifiBenchmarkSession`, callback hệ điều hành, Compose navigation hoặc Activity lifecycle.

Các test cũ đã được cập nhật ở mức source: assertion nhãn LinkSpeed, repository giả dùng API phiên đo và fixture lịch sử có timestamp cố định. Hai test `PingTesterTest` cũ vẫn có kết nối mạng thật.

## Rà soát tĩnh của các đợt trước (BUG-01–09)

```text
git -c core.safecrlf=false diff --check: exit 0, không có lỗi whitespace
rg -c @Test tests/src/test/kotlin: tổng 96 khai báo
rg -c @Test app/src/test: tổng 33 khai báo
Không còn effect LaunchedEffect(completedScore...) thêm bản ghi
Không còn UUID/thời gian hiện tại trong luồng lưu lịch sử của ViewModel/UI
UUID của snapshot chỉ được tạo trong PerformBenchmarkUseCase
Đã đối chiếu cả 4 implementation WifiRepository với withBenchmarkSession
Không còn downloadMbps/uploadMbps/onScoreMeasured/displayDownload/displayUpload
Không còn giờ mẫu Hôm nay, 09:42 hoặc clamp ping coerceIn(1, 999)
```

Rà soát trên không chứng minh Kotlin biên dịch thành công, socket đã bind đúng trên thiết bị hoặc callback đã được unregister trên runtime.

### Rà soát tích hợp cuối BUG-07–09

- Đối chiếu chữ ký và lời gọi Home, Đo mạng, Radar, Thử lại, navigation và ba repository giả với API quyền/readiness mới.
- Sửa effect định vị bản đồ để phản hồi cả thao tác bấm khi fix hợp lệ vẫn giữ nguyên tọa độ; chỉ hoàn tất yêu cầu focus khi MapView đã có.
- Callback vị trí kiểm tra lại quyền bản đồ và dịch vụ vị trí trước khi nhận fix; dữ liệu về sau khi dịch vụ bị tắt không được coi là vị trí dùng được.
- Rà whitespace riêng cho 24 file văn bản chưa được Git theo dõi; sau khi dọn dòng trống thừa cuối file, số lỗi còn lại: **0**. Kiểm tra này không thực thi Kotlin hoặc ứng dụng.
- Import trùng hoàn toàn (phân biệt chữ hoa/thường): **0**. Tìm tham chiếu API quyền cũ và `NEARBY_WIFI_DEVICES` trong `app/src` và `tests/src`: không có kết quả (`rg` exit 1).
- Tổng số khai báo vẫn là 96 test hồi quy + 33 test cũ = 129; không có test nào được chạy trong lần rà soát này.

## Cách chạy trên runner riêng

Các lệnh dưới đây **chưa được thực thi**. Chỉ chạy trên bản sao project trong runner/máy kiểm thử độc lập, không phải máy người dùng. Cần JDK 17, Android SDK platform 34 và khả năng tải dependency theo cấu hình Gradle hiện tại.

```bash
# Chạy riêng 36 test BUG-04–06; không có socket probe thật.
bash ./gradlew --no-daemon :app:testDebugUnitTest \
  --tests 'com.mobilelegend.wlo.regression.MeasurementOwnershipTest' \
  --tests 'com.mobilelegend.wlo.regression.NetworkSessionRegressionTest' \
  --tests 'com.mobilelegend.wlo.regression.PingCancellationTest'

# Chạy riêng 31 test BUG-07–09; không truy cập Wi-Fi/GPS thật.
bash ./gradlew --no-daemon :app:testDebugUnitTest \
  --tests 'com.mobilelegend.wlo.regression.WifiReadinessTest' \
  --tests 'com.mobilelegend.wlo.regression.WifiSnapshotCacheTest' \
  --tests 'com.mobilelegend.wlo.regression.LocationDataPolicyTest'

# Chạy riêng 30 test BUG-10–11 bằng Flow giả và đồng hồ ảo.
bash ./gradlew --no-daemon :app:testDebugUnitTest \
  --tests 'com.mobilelegend.wlo.regression.LiveRadarLifecycleTest'

# BUG-12–13: hai file mới + toàn bộ 9 ca đóng socket (34 ca).
bash ./gradlew --no-daemon :app:testDebugUnitTest \
  --tests 'com.mobilelegend.wlo.regression.BenchmarkTimingTest' \
  --tests 'com.mobilelegend.wlo.regression.TcpFailureScoringTest' \
  --tests 'com.mobilelegend.wlo.regression.PingCancellationTest'

# BUG-14–15: 45 ca mới bằng nguồn vị trí/deadline/repository giả.
bash ./gradlew --no-daemon :app:testDebugUnitTest \
  --tests 'com.mobilelegend.wlo.regression.LocationRequestControllerTest' \
  --tests 'com.mobilelegend.wlo.regression.LocationSampleSelectionTest' \
  --tests 'com.mobilelegend.wlo.regression.SavedPlacesContentTest'

# Chạy toàn bộ 197 test hồi quy BUG-01–15.
bash ./gradlew --no-daemon :app:testDebugUnitTest \
  --tests 'com.mobilelegend.wlo.regression.*'

# Toàn bộ 230 khai báo test, bao gồm hai test socket thật cũ.
bash ./gradlew --no-daemon :app:testDebugUnitTest

# Biên dịch APK và kiểm tra Android lint.
bash ./gradlew --no-daemon :app:assembleDebug :app:lintDebug
```

Sau khi chạy, ghi stdout/stderr, exit code, môi trường, commit/diff được kiểm thử và lưu:

- JUnit XML: `app/build/test-results/testDebugUnitTest/TEST-*.xml`.
- Test HTML: `app/build/reports/tests/testDebugUnitTest/index.html`.
- Lint HTML: `app/build/reports/lint-results-debug.html`.

Nếu dừng ở dependency/SDK/biên dịch, ghi đúng giai đoạn; không ghi test chưa chạy là pass.

## Checklist UI/Android còn phải chạy trên môi trường riêng

| Tình huống | Kết quả cần đạt | Trạng thái |
| --- | --- | --- |
| Đo A, đổi sang B/GPS khác rồi mở lại tab nhiều lần | Giữ ID/time/SSID/BSSID/tọa độ/nhu cầu/thông số A; số bản ghi không tăng | Chưa chạy |
| Đổi tên/lưu cùng kết quả nhiều lần | Cùng một bản ghi; chỉ đổi tên/bookmark | Chưa chạy |
| Xóa bản ghi rồi xem lại kết quả/reset/chuyển tab | Không tự khôi phục bản ghi hoặc điểm bản đồ đã xóa | Chưa chạy |
| Đo A, xem B từ Trang chủ/Bản đồ/Đã lưu | Không trộn thông số A dưới tên B | Chưa chạy |
| Bấm đo liên tục từ Trang chủ/tab đo/luồng radar cũ | Chỉ một phiên đang chạy; không ghi đè hoặc lưu trùng | Chưa chạy |
| Hủy khi chờ callback / TCP đang connect | Đóng socket, gỡ callback, không lưu kết quả; đo tiếp được sau cleanup | Chưa chạy |
| Back/chuyển tab/đưa app xuống nền khi đang đo rồi quay lại | Hủy lượt cũ; không tự chạy lại; lượt mới không bị màn cũ hủy hoặc điều hướng | Chưa chạy |
| Tái tạo Activity sau khi đã đo; đóng hẳn Activity khi đang đo | Giữ lịch sử qua recreation; dọn job khi owner bị clear | Chưa chạy |
| Tắt Wi-Fi khi 4G vẫn bật giữa các lần TCP connect | Thất bại/hủy; không nhận độ trễ 4G làm kết quả Wi-Fi | Chưa chạy |
| Đổi SSID hoặc roam giữa hai AP cùng SSID; thử A→B→A | Lượt đã phát hiện thay đổi không được chấp nhận/lưu | Chưa chạy |
| Android 26–30, 31–32, 33–34; quyền chính xác/gần đúng, tắt vị trí, ẩn BSSID | Đọc đúng mạng hoặc từ chối rõ ràng; không chấp nhận định danh thiếu | Chưa chạy |
| Kết quả 0, timeout TCP, ping thành công chậm, LinkSpeed không biết | Giữ số liệu thật và nhãn đúng, không fallback thành dữ liệu tốt | Chưa chạy |
| Màn hình hẹp, font lớn, SSID dài và nút hủy | Nội dung/nút không cắt hoặc đè nhau | Chưa chạy |
| Bắt đầu qua Home/tab Đo/Radar/Thử lại với từng trạng thái quyền hoặc mạng | Cùng điều kiện và lý do; blocked không mở phiên/TCP | Chưa chạy |
| API 30/31/33/34: chỉ Coarse, Fine, Nearby bị từ chối, tắt vị trí, quay lại Settings | Coarse chỉ đủ cho bản đồ; Wi-Fi cần Fine; Nearby không làm khóa API đọc đang dùng; callback được làm mới | Chưa chạy |
| Di chuyển xa vị trí mặc định, bật/tắt chế độ minh họa | Quán/thư viện không đổi tọa độ; demo không vào khuyến nghị/Đã lưu | Chưa chạy |
| Chưa có fix, fix trên 120 giây, sai số trên 100 m hoặc location mock | Không gắn tâm mặc định/fix cũ/mock vào lượt đo; gần đúng được ghi nhãn và accuracy | Chưa chạy |
| Hai lượt đo có GPS tại A/B rồi di chuyển tới C | Hai ID/marker giữ tọa độ A/B, chỉ khoảng cách thay đổi | Chưa chạy |
| Kéo bản đồ ra xa rồi bấm định vị khi fix hợp lệ không đổi tọa độ | Camera về đúng fix; không chờ tọa độ phải thay đổi | Chưa chạy |
| Tắt dịch vụ vị trí khi request đang chờ; callback fix về muộn | Không nhận fix đó làm vị trí dùng được; listener/timeout được dọn | Chưa chạy |
| Tái tạo Activity, chuyển tab nhiều lần, đưa app xuống nền rồi mở lại | Giữ một Radar trong store; xuống nền gỡ callback/dừng polling; trở lại nhận mẫu mới | Chưa chạy |
| Nguồn Radar ném lỗi/hoàn tất/im lặng, bấm thử lại liên tục | Báo lỗi, ẩn mẫu hết hạn, retry hữu hạn, không có hai collector | Chưa chạy |
| Từ chối đăng ký callback rồi cấp lại quyền/quay về từ Settings | Không đăng ký lặp vô hạn khi bị từ chối; khôi phục sau thay đổi/refresh | Chưa chạy |
| Đóng hẳn Activity khi Radar hoặc retry đang chạy | ViewModelStore clear dọn nguồn và job; mở lại không còn callback của owner cũ | Chưa chạy |
| Wi-Fi phản hồi nhanh/chậm, chờ callback hoặc TCP connect kẹt | Không có delay minh họa; deadline 10 giây hủy và dọn tài nguyên; kết quả muộn không được lưu | Chưa chạy |
| 0/1/2/3 lượt TCP connect thất bại trên cùng mạng | Đúng 0/33/66/100%; chỉ độ trễ thành công được lấy trung bình; có lỗi thì không khuyến nghị tốt | Chưa chạy |
| Đo có TCP thất bại, lưu rồi xem trên lịch sử/bản đồ | Tỷ lệ và điểm cùng snapshot, không lấy tỷ lệ của mạng hiện tại | Chưa chạy |
| Trong nhà không có fix, bấm định vị nhiều lần rồi xuống nền/đóng màn | Một listener, timeout/hủy dọn tài nguyên, không nhận callback cũ | Chưa chạy |
| Cache mới/cũ/mock, timestamp tương lai, chỉ Coarse, Mountain View thật | Hiện đúng tuổi/accuracy/trạng thái; không dùng mẫu không hợp lệ, không loại tọa độ thật | Chưa chạy |
| Bỏ lưu hết → lưu lại một → xóa hết → đo mới | Số dòng lần lượt 0 → 1 → 0 → 1; tổng lượt đo theo lịch sử | Chưa chạy |
| Lưu phép đo thiếu GPS, rồi bỏ bookmark/xóa | Không tạo marker giả; thống kê thiếu vị trí và lịch sử cập nhật đúng | Chưa chạy |

Ghi thiết bị, Android API, thao tác, network/SSID/BSSID trước và sau, số bản ghi và log quan sát được khi thực hiện từng ca. Chỉ đánh dấu Pass/Fail khi đã chạy thật.

## Nguồn API đã đối chiếu

- [Android LocationManager](https://developer.android.com/reference/android/location/LocationManager): overload `requestLocationUpdates` với Looper và `removeUpdates(LocationListener)` dùng được ở min SDK 26; adapter chỉ đăng ký main looper.
- [Android Location.getElapsedRealtimeNanos](https://developer.android.com/reference/android/location/Location#getElapsedRealtimeNanos()): timestamp theo thời gian từ lúc boot, dùng đánh giá tuổi mẫu thay vì giờ hệ thống.
- [Kotlin withTimeoutOrNull](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/with-timeout-or-null.html) và [withContext](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/with-context.html): timeout hủy hợp tác, kết quả muộn và cleanup. Dùng overload millisecond đã có trong coroutines 1.8.0 của project.
- [Mã nguồn Timeout.kt tại coroutines 1.8.0](https://github.com/Kotlin/kotlinx.coroutines/blob/1.8.0/kotlinx-coroutines-core/common/src/Timeout.kt): đối chiếu cách phân biệt timeout của chính block với timeout từ caller.
- [Android ViewModel](https://developer.android.com/topic/libraries/architecture/viewmodel) và [lifecycle-aware coroutines](https://developer.android.com/topic/libraries/architecture/coroutines): owner/store và vòng đời coroutine.
- [Kotlin collectLatest](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/collect-latest.html): hủy xử lý giá trị trước khi yêu cầu mới đến.
- [Android NetworkCallback](https://developer.android.com/reference/android/net/ConnectivityManager.NetworkCallback): vòng đời callback, cờ thông tin vị trí từ API 31 và dữ liệu callback.
- [Android Network.getSocketFactory](https://developer.android.com/reference/android/net/Network#getSocketFactory()): tạo socket gắn với mạng.
- [Kotlin suspendCancellableCoroutine](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/suspend-cancellable-coroutine.html): hủy coroutine và đóng tài nguyên.
- [Android WifiInfo.getLinkSpeed](https://developer.android.com/reference/android/net/wifi/WifiInfo#getLinkSpeed()): LinkSpeed là tốc độ liên kết Wi-Fi.
- [Android Wi-Fi permissions](https://developer.android.com/develop/connectivity/wifi/wifi-permissions) và [WifiInfo](https://developer.android.com/reference/android/net/wifi/WifiInfo): phân biệt API cần Nearby và quyền cho các trường nhạy cảm vị trí.
