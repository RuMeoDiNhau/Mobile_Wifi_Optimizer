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
        description = "Ưu tiên độ trễ Ping thấp, ổn định và không rớt gói tin."
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
