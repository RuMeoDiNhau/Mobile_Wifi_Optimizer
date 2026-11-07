package com.mobilelegend.wlo.presentation.measure_and_result

import com.mobilelegend.wlo.domain.model.UseCaseCategory
import com.mobilelegend.wlo.presentation.components.MISSING_METRIC
import com.mobilelegend.wlo.presentation.components.networkName
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class HistoryComparisonRow(val spot: SavedSpotItem, val bestLabel: String?)
data class HistoryComparisonGroup(
    val category: UseCaseCategory,
    val rows: List<HistoryComparisonRow>,
    val savedCount: Int
)

/** All history stays visible, but only bookmarked snapshots compete within their own category. */
fun historyComparison(spots: List<SavedSpotItem>): List<HistoryComparisonGroup> =
    UseCaseCategory.values().mapNotNull { category ->
        val group = spots.filter { it.score.category == category }
        if (group.isEmpty()) return@mapNotNull null
        val saved = group.filter { it.isSaved && it.score.measurement != null }
        val highest = saved.maxOfOrNull { it.score.finalScore }
        val winners = saved.filter { it.score.finalScore == highest }.map { it.id }.toSet()
        val rows = group.sortedWith(compareByDescending<SavedSpotItem> { it.score.finalScore }
            .thenByDescending { it.score.measurement?.measuredAtMillis }
            .thenBy { it.id }).map { spot ->
            val label = if (saved.size < 2 || spot.id !in winners) null
                else if (winners.size > 1) "Đồng điểm cao nhất trong các điểm đã lưu cho nhu cầu này"
                else "Tốt nhất trong các điểm đã lưu cho nhu cầu này"
            HistoryComparisonRow(spot, label)
        }
        HistoryComparisonGroup(category, rows, saved.size)
    }

data class HistoryMeasurementDetails(
    val categoryTitle: String,
    val ssid: String,
    val bssid: String,
    val measuredAtText: String
)

fun historyMeasurementDetails(
    spot: SavedSpotItem,
    timeZone: TimeZone = TimeZone.getDefault()
): HistoryMeasurementDetails {
    val snapshot = spot.score.measurement
    val time = snapshot?.measuredAtMillis?.let {
        SimpleDateFormat("dd/MM/yyyy HH:mm:ss z", Locale.ROOT).apply { this.timeZone = timeZone }.format(Date(it))
    } ?: MISSING_METRIC
    return HistoryMeasurementDetails(spot.score.category.title, networkName(snapshot?.ssid),
        snapshot?.bssid?.takeIf { it.isNotBlank() } ?: MISSING_METRIC, time)
}
