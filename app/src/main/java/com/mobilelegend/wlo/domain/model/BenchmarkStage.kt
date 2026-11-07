package com.mobilelegend.wlo.domain.model

/** Milestones of real work, not a prediction of elapsed time. */
enum class BenchmarkStage(val progress: Float, val message: String) {
    PREPARING(0.1f, "Đang xác nhận mạng Wi-Fi và chờ thông tin kết nối..."),
    PROBING(0.4f, "Đang thực hiện 3 lượt kết nối TCP..."),
    SCORING(0.9f, "Đang kiểm tra lại mạng và chấm điểm...")
}
