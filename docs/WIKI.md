# 📡 Wi-Fi Location Optimizer (WLO) — Trợ lý Tối ưu Hóa Vị trí Sóng Wi-Fi

> **WLO** là ứng dụng Android giúp sinh viên tìm ra vị trí tối ưu trong phòng để kết nối Wi-Fi ổn định nhất, dựa trên nhu cầu cụ thể: Chơi game, Học tập/Tải file, hoặc Họp/Học online — tất cả chỉ trong 3 giây, không cần đo thủ công.

---

## 👥 Thành viên nhóm

| Họ và Tên | MSSV | Vai trò |
|---|---|---|
| Nguyễn Văn A | 21XXXXXX | Team Lead / Android Dev |
| Trần Thị B | 21XXXXXX | Android Dev / Scoring Engine |
| Lê Văn C | 21XXXXXX | UI/UX Design |
| Phạm Thị D | 21XXXXXX | Testing & Documentation |
| Hoàng Văn E | 21XXXXXX | Research & Presentation |

---

## 📑 Mục lục Wiki

1. [Project Direction & Problem Definition](#1-project-direction--problem-definition)
   - [1.1. Vấn đề cần giải quyết](#11-vấn-đề-cần-giải-quyết)
   - [1.2. Người dùng mục tiêu](#12-người-dùng-mục-tiêu)
   - [1.3. Bối cảnh xuất hiện vấn đề](#13-bối-cảnh-xuất-hiện-vấn-đề)
   - [1.4. Cách người dùng hiện giải quyết vấn đề](#14-cách-người-dùng-hiện-giải-quyết-vấn-đề)
   - [1.5. Hướng giải pháp đề xuất](#15-hướng-giải-pháp-đề-xuất)
2. [Mobile Product Rationale](#2-mobile-product-rationale)
   - [2.1. Tác vụ người dùng và bối cảnh thực hiện](#21-tác-vụ-người-dùng-và-bối-cảnh-thực-hiện)
   - [2.2. Năng lực di động và giá trị cho người dùng](#22-năng-lực-di-động-và-giá-trị-cho-người-dùng)
   - [2.3. Ràng buộc và rủi ro](#23-ràng-buộc-và-rủi-ro)
   - [2.4. Đánh giá các nền tảng thay thế](#24-đánh-giá-các-nền-tảng-thay-thế)
   - [2.5. Lý do lựa chọn ứng dụng di động](#25-lý-do-lựa-chọn-ứng-dụng-di-động)
3. [Critical Assumptions & Evidence Plan](#3-critical-assumptions--evidence-plan)
4. [User & Market Research Findings](#4-user--market-research-findings)
5. [Existing Solutions & Alternatives Analysis](#5-existing-solutions--alternatives-analysis)
6. [Evidence Evaluation & Product Decisions](#6-evidence-evaluation--product-decisions)
7. [Business Canvas](#7-business-canvas)
8. [MVP Scope & Prioritization](#8-mvp-scope--prioritization)
9. [User Stories](#9-user-stories)
10. [Critical User Flows](#10-critical-user-flows)
11. [Project Foundation & Tech Stack](#11-project-foundation--tech-stack)
12. [References & Project Entry Links](#12-references--project-entry-links)

---

## 1. Project Direction & Problem Definition

### 1.1. Vấn đề cần giải quyết

Sinh viên sống tại ký túc xá, nhà trọ, hoặc căn hộ chung thường gặp tình trạng **chất lượng kết nối Wi-Fi không ổn định** và dao động mạnh theo vị trí trong phòng. Vấn đề xảy ra đặc biệt thường xuyên khi:

- **Chơi game online (MOBA, FPS):** Ping đột ngột tăng cao, gây giật lag, mất điểm đột ngột.
- **Học và họp online (Zoom, Google Meet):** Video bị mờ, rớt kết nối, mất tiếng giữa chừng.
- **Tải tài liệu / nộp bài:** Tốc độ tải chậm, timeout khi upload file lớn.

Người dùng không có công cụ nào giúp họ **hiểu tại sao** và **biết phải ngồi ở đâu** để Wi-Fi tốt hơn — họ chỉ di chuyển theo bản năng hoặc chấp nhận sống chung với chất lượng mạng kém.

### 1.2. Người dùng mục tiêu

| Nhóm người dùng | Đặc điểm | Nhu cầu chính |
|---|---|---|
| **Sinh viên đại học (18–24 tuổi)** | Sống trong KTX, nhà trọ; sử dụng Wi-Fi chung; dùng điện thoại Android là thiết bị chính | Tìm vị trí ngồi tốt nhất để học online, chơi game, họp nhóm |
| **Người đi làm / Work-from-home** | Làm việc tại nhà; tham gia video call liên tục; cần kết nối ổn định cho hiệu suất | Tối ưu không gian làm việc theo chất lượng mạng thực tế |
| **Người thuê phòng trọ** | Router được đặt cố định; không thể di chuyển router; Wi-Fi phủ sóng không đều | Tìm điểm phủ sóng tốt nhất trong không gian bị giới hạn |

### 1.3. Bối cảnh xuất hiện vấn đề

```
📍 Bối cảnh điển hình:

Sinh viên A đang họp nhóm cuối học kỳ qua Google Meet.
Camera bỗng mờ, mic phát ra tiếng rè, rồi toàn bộ kết nối ngắt.
Anh ấy di chuyển từ giường ra bàn học — kết nối phục hồi.
Ngày hôm sau, ngồi đúng bàn học — lại mất mạng.

→ Vị trí trong phòng ảnh hưởng đến chất lượng Wi-Fi, nhưng
  người dùng không có cách nào đo lường điều này một cách khoa học.
```

Nguyên nhân kỹ thuật bao gồm: khoảng cách tới router, vật cản vật lý (tường, tủ, cửa sắt), nhiễu tần số từ thiết bị lân cận, và sự chênh lệch giữa băng tần 2.4GHz và 5GHz.

### 1.4. Cách người dùng hiện giải quyết vấn đề

| Giải pháp hiện tại | Vấn đề tồn tại |
|---|---|
| Di chuyển theo bản năng, thử sai | Không có dữ liệu; mất thời gian; vẫn có thể sai |
| Dùng ứng dụng đo Wi-Fi chuyên nghiệp (WiFi Analyzer, NetSpot) | Giao diện phức tạp, dành cho kỹ thuật viên; bắt đo thủ công từng điểm; không đưa ra lời khuyên theo nhu cầu |
| Hỏi người khác trong phòng | Chủ quan; không có cơ sở dữ liệu |
| Đặt lại Router | Không phải quyết định của người dùng (Router thuộc chủ nhà/KTX) |

**Khoảng trống thị trường:** Không có ứng dụng nào kết hợp được **đo tự động thời gian thực** + **tính điểm theo nhu cầu cụ thể** + **giao diện thân thiện cho người không chuyên**.

### 1.5. Hướng giải pháp đề xuất

WLO giải quyết vấn đề theo triết lý **"Zero Friction"** — người dùng không cần hiểu kỹ thuật, không cần bấm nhiều nút:

1. **Mở app → tự động đo sóng** liên tục mỗi 1 giây (Live Wi-Fi Radar).
2. **Cầm điện thoại đi quanh phòng** → đồng hồ Gauge đổi màu theo thời gian thực (Xanh/Vàng/Đỏ).
3. **Chọn nhu cầu → bấm 1 nút** → app đo kiểm định đầy đủ trong 3 giây và trả ra điểm số + lời khuyên.
4. **Lưu và so sánh** nhiều vị trí trong phòng để biết nơi nào tốt nhất cho Gaming, Học tập, hoặc Video Call.

---

## 2. Mobile Product Rationale

### 2.1. Tác vụ người dùng và bối cảnh thực hiện

| Tác vụ người dùng | Bối cảnh thực hiện | Vì sao cần Mobile? |
|---|---|---|
| Dò sóng Wi-Fi theo không gian | Người dùng cầm điện thoại đi lại quanh phòng | Cần cảm biến di động, không thể thực hiện từ máy tính để bàn |
| Đo chất lượng mạng tại một vị trí | Ngồi tại bàn/giường và bấm 1 nút | Cần thực hiện ngay tại nơi người dùng thực sự sử dụng |
| So sánh 2–3 vị trí trong phòng | Liên tục di chuyển và lưu kết quả | Cần thiết bị cầm tay đi cùng người dùng |
| Nhận lời khuyên về vị trí tốt nhất | Bất cứ khi nào cần, không cần bật máy tính | Phản hồi tức thì trong tình huống thực tế |

### 2.2. Năng lực di động và giá trị cho người dùng

Điện thoại Android sở hữu các **Mobile Capabilities** đặc thù mà ứng dụng WLO khai thác triệt để:

| Mobile Capability | Cách WLO khai thác |
|---|---|
| **Wi-Fi Sensor (WifiManager API)** | Đọc RSSI (dBm), SSID, BSSID, LinkSpeed, Frequency mỗi giây — không cần phần cứng ngoài |
| **Location Services (GPS)** | Android yêu cầu GPS để đọc Wi-Fi Info đầy đủ (API 10+); WLO xử lý gracefully khi GPS tắt |
| **Always-on & Portable** | Người dùng mang điện thoại theo khi di chuyển trong phòng — không thể thay thế bằng laptop |
| **Native Push Notification** | (Tương lai) Cảnh báo khi sóng yếu đột ngột dưới ngưỡng an toàn |
| **Screen Real-time Rendering** | Jetpack Compose Canvas vẽ lại Gauge mỗi giây theo cảm biến |

### 2.3. Ràng buộc và rủi ro

| Ràng buộc | Mô tả | Giải pháp đã áp dụng |
|---|---|---|
| **Android Wi-Fi Throttling (API 28+)** | `WifiManager.startScan()` bị Android giới hạn vài lần/phút | Dùng `connectionInfo` polling thay vì `startScan()` |
| **GPS bắt buộc để đọc SSID** | Android 10+ ẩn SSID nếu GPS tắt | Hiển thị `PermissionBanner` cảnh báo và dẫn vào Settings |
| **ICMP bị chặn bởi SELinux** | Một số dòng máy (Samsung, Xiaomi) chặn `exec("ping")` | Dùng **TCP Socket Connect** tới `1.1.1.1:53` thay thế |
| **API Deprecation Android 12+** | `WifiManager.connectionInfo` bị deprecated từ API 31 | Code tương thích đa phiên bản qua `ConnectivityManager.getNetworkCapabilities()` |
| **Không có Internet** | Wi-Fi connected nhưng không có Internet → Ping timeout | Bắt lỗi, trả `PingResult(isSuccess=false)`, hiển thị cảnh báo cụ thể |

### 2.4. Đánh giá các nền tảng thay thế

| Nền tảng | Có thể dùng không? | Lý do không phù hợp |
|---|---|---|
| **Web App (PWA)** | ❌ Không | Trình duyệt không cho phép truy cập Wi-Fi sensor trực tiếp |
| **Desktop App (Windows/macOS)** | ⚠️ Một phần | Người dùng không cầm laptop đi quanh phòng — mất ý nghĩa đo theo không gian |
| **Wearable / Smartwatch** | ❌ Không | Màn hình quá nhỏ, không đủ API Wi-Fi đầy đủ |
| **Android Mobile App** | ✅ Tối ưu | Di động, có đầy đủ Wi-Fi API, người dùng mang theo liên tục |

### 2.5. Lý do lựa chọn ứng dụng di động

> Android Mobile là nền tảng **duy nhất và tối ưu** để giải quyết bài toán "tìm vị trí tốt nhất trong không gian" vì:
> 1. Người dùng **mang thiết bị theo khi di chuyển** — điều không thể thực hiện với laptop hay desktop.
> 2. **WifiManager API** cung cấp dữ liệu sóng chi tiết (RSSI, LinkSpeed, Frequency, BSSID) mà không cần phần cứng bổ sung.
> 3. **Jetpack Compose** cho phép render giao diện cảm biến thời gian thực ở 60fps mà không chiếm tài nguyên.
> 4. **Coroutines + Flow** quản lý bất đồng bộ cảm biến và đo mạng mà không block UI thread.

---

## 3. Critical Assumptions & Evidence Plan

### 3.1. Các giả định rủi ro

| # | Giả định | Mức rủi ro | Kế hoạch kiểm chứng |
|---|---|---|---|
| 1 | **Người dùng sẵn sàng cầm điện thoại đi quanh phòng để dò sóng** | 🔴 Cao | Usability test: quan sát hành vi người dùng thực tế |
| 2 | **Điểm số 0–100 dễ hiểu và đáng tin cậy hơn chỉ số dBm thô** | 🟡 Trung bình | Thu thập phản hồi sau khi xem màn hình kết quả |
| 3 | **Người dùng có ít nhất 2 vị trí có thể ngồi trong phòng để so sánh** | 🟢 Thấp | Khảo sát điều kiện sống của sinh viên |
| 4 | **Thuật toán Scoring phân biệt được "Gaming" vs "Study" dựa trên trọng số** | 🟡 Trung bình | Unit test + so sánh kịch bản thực tế |
| 5 | **TCP Socket Ping phản ánh đúng trải nghiệm người dùng thực tế** | 🟡 Trung bình | So sánh kết quả TCP Socket với ping thực trong game |

### 3.2. Kế hoạch kiểm chứng

| Giả định | Phương pháp kiểm chứng | Ngưỡng thành công |
|---|---|---|
| Giả định 1 | Pilot Walkthrough với 3–5 người dùng thực | ≥ 4/5 người hoàn thành luồng không cần hướng dẫn |
| Giả định 2 | Hỏi người dùng sau khi xem kết quả: "Bạn có hiểu điểm số này không?" | ≥ 80% trả lời "Hiểu rõ" |
| Giả định 4 | Unit Test: Kiểm tra điểm Gaming với Ping 25ms vs 180ms | Chênh lệch ≥ 25 điểm |
| Giả định 5 | Đo trong game thực, so sánh với TCP Socket Ping | Sai số < 20ms |

---

## 4. User & Market Research Findings

### 4.1. Kết quả nghiên cứu người dùng

**Phương pháp:** Khảo sát nhanh (15–20 câu hỏi) + phỏng vấn sâu định tính.

**Kết quả khảo sát (N ≈ 30 sinh viên, môi trường KTX/nhà trọ):**

| Câu hỏi | Kết quả |
|---|---|
| Bạn có thường xuyên gặp vấn đề mạng Wi-Fi khi học online? | **87%** Có / Thỉnh thoảng |
| Bạn thường làm gì khi mạng yếu? | **73%** Di chuyển vị trí ngồi theo bản năng |
| Bạn có biết vị trí nào trong phòng có sóng tốt nhất không? | **61%** Không chắc / Không biết |
| Nhu cầu chính khi cần mạng tốt? | Gaming: 48% / Học online: 35% / Video Call: 17% |
| Bạn có muốn dùng app tự động tìm vị trí Wi-Fi tốt? | **83%** Muốn / Rất muốn |

**Insight quan trọng từ phỏng vấn định tính:**
> *"Tôi biết ngồi gần cửa sổ thường mạng hơn, nhưng không biết bao nhiêu là đủ cho game hay không."* — Sinh viên năm 3
>
> *"Khi họp online, nếu mạng yếu tôi chỉ biết tắt camera chứ không biết làm gì khác."* — Sinh viên năm 2

### 4.2. Kết quả Usability Testing (Pilot Walkthrough)

**Phiên thử nghiệm:** Quan sát 3 người dùng thực sử dụng bản prototype không có hướng dẫn.

| Tác vụ | Tỷ lệ hoàn thành | Điểm cần cải thiện phát hiện |
|---|---|---|
| Mở app và bật GPS | 3/3 (100%) | — |
| Đọc được đồng hồ Live Radar | 3/3 (100%) | 1 người hỏi "Xanh nghĩa là tốt hay xấu?" → Cần thêm label |
| Bấm đo 1-chạm theo nhu cầu | 3/3 (100%) | — |
| Hiểu điểm số kết quả | 2/3 (67%) | 1 người không hiểu "85/100 nghĩa là gì" → Thêm lời khuyên rõ hơn |
| Lưu và so sánh vị trí | 2/3 (67%) | Nút "Lưu vị trí" chưa đủ nổi bật |

**Cải tiến đã áp dụng sau Pilot:**
- Thêm nhãn "Sóng Mạnh / Sóng Ổn Định / Sóng Yếu" bên dưới đồng hồ Gauge.
- Thêm `summaryText` và `recommendation` trong màn hình kết quả.
- Nút "Lưu vị trí này" được đặt nổi bật hơn với icon bookmark.

---

## 5. Existing Solutions & Alternatives Analysis

### 5.1. Bảng so sánh tổng quan các giải pháp hiện có

| Tiêu chí | WiFi Analyzer | NetSpot | Network Cell Info | **WLO (Sản phẩm này)** |
|---|---|---|---|---|
| Đo sóng thời gian thực | ✅ | ✅ | ✅ | ✅ |
| Giao diện cho người không chuyên | ❌ | ❌ | ❌ | ✅ |
| Tính điểm theo nhu cầu (Gaming/Study/Call) | ❌ | ❌ | ❌ | ✅ |
| Đo Ping cùng lúc với RSSI | ❌ | ✅ (Trả phí) | ❌ | ✅ |
| So sánh vị trí trong cùng phiên | ❌ | ✅ (Trả phí) | ❌ | ✅ |
| Đưa ra lời khuyên tối ưu hóa | ❌ | ❌ | ❌ | ✅ |
| Miễn phí | ✅ | ❌ (Freemium) | ✅ | ✅ |
| Cần kỹ thuật để đọc kết quả | ✅ | ✅ | ✅ | ❌ |

### 5.2. Khoảng trống sản phẩm & Hướng khác biệt khả thi của WLO

**3 khoảng trống chính:**
1. **"Use-case Aware Scoring"**: Không ứng dụng nào tính điểm theo nhu cầu người dùng (Gaming ≠ Streaming ≠ Download).
2. **"Zero Friction UX"**: Tất cả ứng dụng hiện tại yêu cầu người dùng biết đọc dBm, channel, SSID để tự phán đoán.
3. **"Session Comparison"**: Không có ứng dụng miễn phí nào cho so sánh vị trí trong cùng một phiên sử dụng.

**Định vị của WLO:**
> WLO là **"Thám tử Wi-Fi thông minh"** — không chỉ đo mà còn *phán xét*, *tư vấn*, và *so sánh* thay mặt người dùng.

---

## 6. Evidence Evaluation & Product Decisions

### 6.1. Đánh giá Keep / Revise / Kill

| Feature ban đầu | Quyết định | Lý do |
|---|---|---|
| Đo điểm từng vị trí A, B, C thủ công | 🔄 **REVISE → Live Radar** | Feedback giảng viên: "Ma sát hành vi quá lớn" — bắt người dùng bấm quá nhiều |
| Lưu dữ liệu vào Database | ❌ **KILL** | Không thuộc phạm vi MVP; tăng độ phức tạp; dữ liệu chỉ hữu ích trong phiên đo |
| Heatmap tự động theo GPS indoor | ❌ **KILL** | GPS indoor không đủ chính xác trong phòng kín; ngoài phạm vi MVP |
| Scoring Engine theo Use Case | ✅ **KEEP** | Đây là differentiation chính; được xác nhận qua khảo sát (83% muốn) |
| So sánh vị trí In-memory | ✅ **KEEP** | Đơn giản, hiệu quả; đủ giá trị cho MVP |
| Cảnh báo thông minh khi GPS tắt | ✅ **KEEP** | Bắt buộc để app hoạt động trên Android 10+ |

---

## 7. Business Canvas

### 7.1. Business Model Canvas – 3×3

| **Key Partners** | **Key Activities** | **Value Propositions** |
|---|---|---|
| Giảng viên hướng dẫn (phản hồi sản phẩm) | Phát triển ứng dụng Android Native | Tìm vị trí Wi-Fi tốt nhất theo nhu cầu trong 3 giây |
| Cộng đồng sinh viên (tester) | Nghiên cứu người dùng & Usability Testing | Không cần kiến thức kỹ thuật để sử dụng |
| | Tối ưu Scoring Algorithm | So sánh nhiều vị trí trong cùng một phiên |

| **Key Resources** | **Channels** | **Customer Segments** |
|---|---|---|
| Android SDK (Kotlin, Jetpack Compose) | Google Play Store (tương lai) | Sinh viên đại học (KTX, nhà trọ) |
| WifiManager & ConnectivityManager API | GitHub Repository | Người làm việc tại nhà (WFH) |
| Thuật toán Scoring Engine (IP nội bộ) | LMS / Báo cáo môn học | Người thuê phòng trọ chung |

| **Cost Structure** | **Revenue Streams** | |
|---|---|---|
| Chi phí phát triển (thời gian học tập) | Ứng dụng miễn phí (Giai đoạn học thuật) | |
| Không có chi phí cơ sở hạ tầng (No Backend) | Tương lai: Freemium với History & Heatmap nâng cao | |

### 7.2. Giả định cần kiểm chứng trên Canvas

1. **Value Prop**: Người dùng thực sự thấy "điểm số 0–100" dễ hiểu hơn "dBm" (→ Kiểm chứng qua Usability Test ✅).
2. **Customer Segment**: Sinh viên KTX là nhóm đủ lớn và đủ đau để cần giải pháp (→ Kiểm chứng qua Khảo sát 83% ✅).
3. **Key Activity**: Scoring Engine có thể phân biệt ý nghĩa giữa Gaming và Study rõ ràng (→ Kiểm chứng qua Unit Test ✅).

---

## 8. MVP Scope & Prioritization

### 8.1. Phạm vi MVP (MoSCoW Prioritization)

| Tính năng | Ưu tiên | Trạng thái |
|---|---|---|
| **Live Wi-Fi Radar** (Gauge đổi màu thời gian thực mỗi 1s) | 🔴 Must Have | ✅ Hoàn thành |
| **Smart Permission Handler** (Xin quyền GPS + Location thông minh) | 🔴 Must Have | ✅ Hoàn thành |
| **One-Tap Benchmark** (Đo 3s + Scoring theo nhu cầu) | 🔴 Must Have | ✅ Hoàn thành |
| **Scoring Engine** (Chuẩn hóa 0–100 + Trọng số Use Case) | 🔴 Must Have | ✅ Hoàn thành |
| **Session History + So sánh vị trí** (Bottom Sheet) | 🟡 Should Have | ✅ Hoàn thành |
| **Heatmap theo GPS Indoor** | 🟢 Could Have | ❌ Không có trong MVP |
| **Lưu lịch sử lâu dài (Database/Cloud)** | 🔴 Won't Have | ❌ Không có trong MVP |
| **Machine Learning dự đoán sóng** | 🔴 Won't Have | ❌ Không có trong MVP |

### 8.2. Ma trận Impact–Effort

```
High Impact │  Live Radar ⭐     One-Tap Benchmark ⭐
            │  Scoring Engine ⭐
            │
            │  Session History     Smart Permission
Low Impact  │                         Handler
            └─────────────────────────────────────
              Low Effort              High Effort
```

**Quyết định:** Tập trung 100% vào góc trên-trái (High Impact, Low Effort) cho MVP. Không triển khai Heatmap hay Database.

---

## 9. User Stories

### 9.1. Danh sách User Stories cốt lõi

**Nhóm US-1: Thiết lập & Quyền hạn**

> **US-1.1:** *Là người dùng mới, tôi muốn được hướng dẫn cấp quyền Vị trí và bật GPS một cách rõ ràng ngay khi mở app, để ứng dụng có thể đo được sóng Wi-Fi chính xác.*
>
> **Acceptance Criteria:**
> - App hiển thị Dialog xin quyền `ACCESS_FINE_LOCATION` khi mở lần đầu.
> - Nếu GPS tắt, Banner màu vàng xuất hiện với nút "Bật GPS" dẫn thẳng vào Settings.
> - Nếu người dùng từ chối quyền, Banner cung cấp nút mở Cài đặt ứng dụng.

---

**Nhóm US-2: Live Radar**

> **US-2.1:** *Là người dùng muốn tìm vị trí ngồi tốt, tôi muốn thấy cường độ sóng Wi-Fi thay đổi liên tục khi tôi di chuyển trong phòng, để tôi có thể tìm ra vùng phủ sóng tốt nhất mà không cần bấm nút.*
>
> **Acceptance Criteria:**
> - Đồng hồ Gauge cập nhật số dBm và màu sắc mỗi 1 giây.
> - Màu **Xanh lá** khi RSSI ≥ -60 dBm; **Vàng** khi -61 đến -75 dBm; **Đỏ** khi ≤ -76 dBm.
> - Tên mạng (SSID), tốc độ (Mbps) và băng tần (2.4/5GHz) luôn hiển thị.

---

**Nhóm US-3: Benchmark theo nhu cầu**

> **US-3.1:** *Là game thủ, tôi muốn kiểm tra vị trí tôi đang ngồi có đủ chuẩn chơi game online không, để tôi biết mình có nên ở lại hay đổi chỗ.*
>
> **Acceptance Criteria:**
> - Chọn "Chơi Game" → Bấm đo → Nhận điểm số sau ≤ 4 giây.
> - Điểm ≥ 75 + Ping ≤ 60ms → Hiển thị "Tuyệt vời để leo rank".
> - Điểm < 60 → Hiển thị lý do cụ thể và đề xuất đổi vị trí.

> **US-3.2:** *Là sinh viên học online, tôi muốn biết vị trí nào trong phòng phù hợp nhất cho Zoom/Meet, để buổi học của tôi không bị gián đoạn.*
>
> **Acceptance Criteria:**
> - Chọn "Video Call" → Scoring tập trung vào RSSI ổn định (40%) + Ping (40%).
> - Kết quả hiển thị: Ping (ms), Sóng (dBm), Tốc độ (Mbps) chi tiết.

---

**Nhóm US-4: So sánh vị trí**

> **US-4.1:** *Là người dùng đã đo nhiều vị trí trong phòng, tôi muốn xem danh sách xếp hạng để biết nơi nào tốt nhất, để tôi quyết định đặt bàn làm việc hay góc chơi game.*
>
> **Acceptance Criteria:**
> - Bottom Sheet hiển thị danh sách các vị trí đã đo, sắp xếp theo điểm giảm dần.
> - Vị trí có điểm cao nhất được trao huy hiệu 🏆 "TỐT NHẤT".
> - Mỗi vị trí hiển thị: Tên, điểm số, Ping, RSSI, LinkSpeed.

### 9.2. Nguyên tắc viết Tiêu chí chấp nhận (Acceptance Criteria)

Tất cả Acceptance Criteria trong dự án WLO tuân theo chuẩn **Given–When–Then**:

```
Given: [Trạng thái ban đầu]
When:  [Hành động người dùng thực hiện]
Then:  [Kết quả hệ thống phải đáp ứng]
```

**Ví dụ:**
```
Given: Điện thoại đang kết nối Wi-Fi, GPS đang bật
When:  Người dùng bấm nút "Kiểm tra 1-Chạm" với nhu cầu Gaming
Then:  App đo trong ≤ 4 giây và hiển thị điểm số từ 0–100
       kèm nhãn phân loại ("Tuyệt vời", "Khá ổn", "Cảnh báo lag")
```

---

## 10. Critical User Flows

### 10.1. Luồng người dùng cốt lõi (Core User Flow)

```
┌─────────────────────────────────────────────────────────────────┐
│                     LUỒNG SỬ DỤNG CHÍNH                        │
└─────────────────────────────────────────────────────────────────┘

[Mở App]
    │
    ▼
[Xin quyền Location + GPS?]
    ├── Chưa cấp → [Hiện Banner cảnh báo → Bấm → Settings → Quay lại]
    └── Đã cấp ──────────────────────────────────────────────┐
                                                              │
                                                              ▼
                                            [MÀN HÌNH LIVE RADAR]
                                          Đồng hồ Gauge cập nhật 1s/lần
                                          Hiển thị: SSID, RSSI, Speed, Band
                                                              │
                                            Cầm máy đi quanh phòng
                                                              │
                                            Chọn nhu cầu (Gaming/Study/Call)
                                                              │
                                            Bấm "Kiểm tra 1-Chạm"
                                                              │
                                                              ▼
                                          [MÀN HÌNH ĐO KIỂM ĐỊNH - 3s]
                                          Giai đoạn 1: Đọc RSSI + LinkSpeed
                                          Giai đoạn 2: Đo Socket Ping × 3
                                          Giai đoạn 3: Tính điểm Scoring Engine
                                                              │
                                                              ▼
                                          [MÀN HÌNH KẾT QUẢ]
                                          Vòng tròn điểm số 0–100
                                          Lời nhận xét + Lời khuyên
                                          Chi tiết: Ping / RSSI / Speed
                                                              │
                             ┌────────────┴────────────┐
                             │                         │
                             ▼                         ▼
                    [Lưu vị trí này]         [Về Radar → Đo vị trí khác]
                             │
                             ▼
                    [So sánh (Bottom Sheet)]
                    Xếp hạng các vị trí
                    Highlight "TỐT NHẤT"
```

### 10.2. Nhánh thay thế và điểm rủi ro (Alternative Branches & Drop-off Risks)

| Tình huống biên | Hành vi hệ thống | Điểm rủi ro |
|---|---|---|
| **GPS tắt** | Hiển thị Banner vàng cảnh báo; SSID hiện `"Cần bật GPS"` | Nếu không xử lý → app crash hoặc đo sai |
| **Không kết nối Wi-Fi (4G/Offline)** | Chấm tròn trạng thái đỏ; nút đo bị vô hiệu hóa | Nếu không xử lý → NullPointerException |
| **Mạng mất kết nối giữa đo 3s** | Bắt Exception → `PingResult(isSuccess=false)` → Hiển thị lỗi thân thiện | Nếu không xử lý → app đóng đột ngột |
| **Từ chối quyền vĩnh viễn** | Banner dẫn vào App Settings (không phải System Permission Dialog) | Nếu không xử lý → Permission Dialog không hiện được |
| **Router có sóng Wi-Fi nhưng không có Internet** | Ping 100% timeout → Điểm = 0 → Cảnh báo "Không có Internet" | Dễ nhầm với sóng yếu |

---

## 11. Project Foundation & Tech Stack

### 11.1. Nền tảng dự án

| Thành phần | Lựa chọn | Lý do |
|---|---|---|
| **Ngôn ngữ** | Kotlin 1.9+ | Ngôn ngữ chính thức của Android; null-safety; Coroutines tích hợp |
| **UI Framework** | Jetpack Compose (Material 3) | Declarative UI; reactive với StateFlow; Canvas API để vẽ Gauge |
| **Kiến trúc** | Clean Architecture + MVVM | Tách biệt rõ ràng Domain / Data / Presentation; dễ Unit Test |
| **Bất đồng bộ** | Kotlin Coroutines + Flow | `Dispatchers.IO` cho sensor/network; `StateFlow` cho UI state |
| **DI** | Manual DI (không dùng Hilt) | Đủ đơn giản cho MVP; tránh overhead setup Hilt |
| **Navigation** | Navigation Compose | Type-safe route; back-stack quản lý màn hình |

### 11.2. Cấu trúc thư mục dự án

```text
mobile_wifi_optimizer/
├── app/src/
│   ├── main/java/com/mobilelegend/wlo/
│   │   ├── MainActivity.kt                      # Entry point + Navigation host
│   │   ├── core/
│   │   │   ├── network/PingTester.kt           # TCP Socket Latency đo ngầm Dispatchers.IO
│   │   │   └── permissions/PermissionHelper.kt # Quản lý quyền Fine Location & GPS status
│   │   ├── data/
│   │   │   ├── model/ (WifiMetric, PingResult) # Raw hardware data classes
│   │   │   ├── sensor/WifiScanner.kt           # Flow polling RSSI/LinkSpeed mỗi 1s
│   │   │   └── repository_impl/WifiRepositoryImpl.kt
│   │   ├── domain/
│   │   │   ├── model/ (LocationScore, UseCaseCategory, UseCaseWeight, LiveRadarData)
│   │   │   ├── repository/WifiRepository.kt    # Interface Clean Architecture
│   │   │   └── usecase/
│   │   │       ├── CalculateScoreUseCase.kt    # 🧠 Scoring Engine chuẩn hóa thang 0-100
│   │   │       ├── StreamLiveWifiUseCase.kt    # Phân cấp sóng & % tín hiệu thời gian thực
│   │   │       └── PerformBenchmarkUseCase.kt  # Điều phối tiến trình đo 1-chạm 3s
│   │   └── presentation/
│   │       ├── theme/ (Color, Theme, Type)      # Jetpack Compose Material 3 Dark Theme
│   │       ├── components/ (SignalGaugeIndicator, ScoreCircleCard, PermissionBanner)
│   │       ├── live_radar/ (LiveRadarScreen + LiveRadarViewModel)
│   │       ├── measure_and_result/ (MeasureScreen, ResultScreen, MeasureViewModel)
│   │       └── navigation/ (Screen, AppNavigation)
│   └── test/java/com/mobilelegend/wlo/          # 🧪 6 bộ Test Suite (33 Unit Tests tự động)
│       ├── core/network/PingTesterTest.kt
│       ├── domain/model/UseCaseWeightTest.kt
│       ├── domain/usecase/CalculateScoreUseCaseTest.kt
│       ├── domain/usecase/StreamLiveWifiUseCaseTest.kt
│       ├── domain/usecase/PerformBenchmarkUseCaseTest.kt
│       └── presentation/measure_and_result/MeasureSessionTest.kt
└── docs/                                        # 📁 Toàn bộ tài liệu kỹ thuật & báo cáo
    ├── USER_GUIDE.md                            # Hướng dẫn cài đặt & sử dụng app
    ├── WIKI.md                                  # Tài liệu GitHub Wiki đồ án (12 chương)
    ├── architecture.md                          # Sơ đồ kiến trúc Mermaid tương tác
    ├── project_structure.md                     # Đặc tả cấu trúc module chi tiết
    ├── features_docs.md                         # Đặc tả tính năng cốt lõi
    ├── data_schema.md                           # Lược đồ dữ liệu
    ├── test_specifications.md                   # Bảng đặc tả Test Matrix
    ├── testing_and_verification_guide.md        # Hướng dẫn kiểm thử Studio & máy thật
    ├── assets/wifi_benchmark_architecture.png   # Sơ đồ kiến trúc trực quan
    └── reports/report.pdf                       # Báo cáo đồ án / Đề tài môn học
```

### 11.3. Thuật toán Scoring Engine

**Công thức chuẩn hóa:**

$$S_{final} = S_{RSSI} \times W_{RSSI} + S_{Ping} \times W_{Ping} + S_{Speed} \times W_{Speed}$$

**Hàm chuẩn hóa từng chỉ số (thang điểm 0–100):**

| Chỉ số | Công thức | Ngưỡng Tốt | Ngưỡng Kém |
|---|---|---|---|
| RSSI (dBm) | `clamp((RSSI + 90) / 40 × 100, 0, 100)` | ≥ -50 dBm → 100 | ≤ -90 dBm → 0 |
| Ping (ms) | `clamp((200 - Ping) / 180 × 100, 0, 100)` | ≤ 20ms → 100 | ≥ 200ms → 0 |
| Speed (Mbps) | `clamp((Speed - 10) / 290 × 100, 0, 100)` | ≥ 300Mbps → 100 | ≤ 10Mbps → 0 |

**Bảng trọng số Use Case:**

| Use Case | W_Ping | W_RSSI | W_Speed | Ngưỡng Khuyến nghị |
|---|---|---|---|---|
| 🎮 **Gaming** | **0.60** | **0.30** | 0.10 | Score ≥ 75 & Ping ≤ 60ms |
| 📚 **Study/Download** | 0.10 | **0.40** | **0.50** | Score ≥ 70 & Speed ≥ 50Mbps |
| 📞 **Video Call** | **0.40** | **0.40** | 0.20 | Score ≥ 70 & Ping ≤ 100ms |

### 11.4. Liên kết bằng chứng với quyết định (Traceability Matrix)

| Quyết định sản phẩm | Bằng chứng liên kết |
|---|---|
| Dùng Live Radar thay vì đo thủ công | Feedback giảng viên: "Ma sát hành vi lớn" |
| Scoring Engine theo Use Case | Khảo sát: 48% Gaming, 35% Study, 17% Call |
| TCP Socket thay vì ICMP exec | Kỹ thuật: SELinux chặn `exec("ping")` trên nhiều dòng máy |
| No Database trong MVP | YAGNI; dữ liệu phiên đo đủ cho bài toán người dùng |
| Hỗ trợ đa API (Android 11 & 12+) | `WifiManager.connectionInfo` deprecated từ API 31 |

### 11.5. Đảm bảo chất lượng & Kiểm thử tự động (Quality Assurance)

Dự án áp dụng quy trình kiểm thử tự động nghiêm ngặt với **6 bộ Test Suite (33 Test Cases)**, đạt tỷ lệ vượt qua **33/33 Tests Passed (100% Xanh lá)**:

| Test Suite | File Kiểm Thử | Số lượng Cases | Nội dung Kiểm Thử Trọng Tâm | Kết Quả |
|---|---|:---:|---|:---:|
| **Scoring Engine** | `CalculateScoreUseCaseTest.kt` | 13 | Chuẩn hóa biên RSSI, Ping, Speed; đánh giá 3 Use Case; kẹp trần 100; rớt gói $\ge 50\%$; mất mạng | **13/13 PASSED** ✅ |
| **Live Radar Stream** | `StreamLiveWifiUseCaseTest.kt` | 5 | Phân cấp sóng EXCELLENT/GOOD/POOR; tính % vạch sóng; kiểm tra luồng phát Flow | **5/5 PASSED** ✅ |
| **Benchmark 1-Tap** | `PerformBenchmarkUseCaseTest.kt` | 3 | Điều phối 3 gói tin; chấm điểm Gaming & Study; xử lý timeout an toàn | **3/3 PASSED** ✅ |
| **Session History** | `MeasureSessionTest.kt` | 6 | Lưu vị trí, xác định vị trí TỐT NHẤT, sắp xếp giảm dần, tie-breaking, xóa vị trí | **6/6 PASSED** ✅ |
| **Weights Integrity** | `UseCaseWeightTest.kt` | 4 | Tổng trọng số $\sum W = 1.0$; xác thực mọi trọng số $> 0$ | **4/4 PASSED** ✅ |
| **Network Socket** | `PingTesterTest.kt` | 2 | Bắt lỗi host chết, tính % packet loss khi rớt mạng | **2/2 PASSED** ✅ |
| **Tổng kết** | **6 Test Suites** | **33 Cases** | **Độ bao phủ toàn diện toàn bộ nghiệp vụ cốt lõi** | **100% PASSED** ✅ |

---

## 12. References & Project Entry Links

### 12.1. Tài liệu tham khảo

- Android Developers — [WifiManager API](https://developer.android.com/reference/android/net/wifi/WifiManager)
- Android Developers — [ConnectivityManager.getNetworkCapabilities](https://developer.android.com/reference/android/net/ConnectivityManager#getNetworkCapabilities(android.net.Network))
- Android Developers — [Jetpack Compose](https://developer.android.com/jetpack/compose)
- Kotlin Documentation — [Coroutines & Flow](https://kotlinlang.org/docs/coroutines-overview.html)
- Android Developers — [Wi-Fi scan throttling (API 28+)](https://developer.android.com/guide/topics/connectivity/wifi-scan#wifi-scan-throttling)
- RFC 5737 — [IPv4 Address Blocks Reserved for Documentation (TEST-NET)](https://datatracker.ietf.org/doc/html/rfc5737)

### 12.2. Tài liệu kỹ thuật đi kèm (docs/)

* 📖 [USER_GUIDE.md](USER_GUIDE.md): Hướng dẫn cài đặt và sử dụng ứng dụng chi tiết.
* 🏛️ [architecture.md](architecture.md): Sơ đồ kiến trúc Mermaid tương tác và luồng Clean Architecture.
* 📂 [project_structure.md](project_structure.md): Chi tiết cấu trúc module và phân lớp trách nhiệm.
* 📋 [test_specifications.md](test_specifications.md): Bảng đặc tả ma trận kiểm thử Test Matrix.
* ✅ [testing_and_verification_guide.md](testing_and_verification_guide.md): Hướng dẫn chạy test trên Android Studio và máy thật.
* 📑 [Báo Cáo Đề Tài (PDF)](reports/report.pdf): Báo cáo đề cương đồ án gốc môn học.

### 12.3. Liên kết nộp bài công khai

| Hạng mục | Liên kết |
|---|---|
| 🐙 **GitHub Repository** | https://github.com/RuMeoDiNhau/Mobile_Wifi_Optimizer |
| 📖 **GitHub Wiki (trang này)** | https://github.com/RuMeoDiNhau/Mobile_Wifi_Optimizer/wiki |
| 📱 **APK Release** | *(Thêm link sau khi build)* |
| 🎨 **Figma Prototype** | *(Thêm link nếu có)* |

---

*📡 Wi-Fi Location Optimizer — Môn: Phát triển Ứng dụng trên Thiết bị Di động — Năm học 2024–2025*

