package com.mobilelegend.wlo.domain.model

/**
 * Định nghĩa các loại nhu cầu sử dụng mạng của sinh viên.
 */
enum class UseCaseCategory(
    val title: String,
    val description: String
) {
    GAMING(
        title = "Chơi Game (Gaming)",
        description = "Tham khảo RSSI, độ trễ và tỷ lệ kết nối TCP thành công; chưa đo máy chủ game."
    ),
    STUDY_DOWNLOAD(
        title = "Học tập & Tải File (Download)",
        description = "Ưu tiên băng thông và tốc độ kết nối LinkSpeed cao."
    ),
    VIDEO_CALL(
        title = "Họp & Học Online (Video Call)",
        description = "Cần cường độ sóng ổn định và độ trễ ở mức trung bình."
    )
}
