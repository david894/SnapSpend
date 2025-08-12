package com.kx.snapspend.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kx.snapspend.model.MonthlyTotal
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryOf
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ChartCard(chartData: List<MonthlyTotal>) {
    if (chartData.isEmpty()) return

    val chartEntries: List<Pair<Float, Float>>
    val bottomAxisFormatter: AxisValueFormatter<AxisPosition.Horizontal.Bottom>

    // This is the new logic to handle the single-month case
    if (chartData.size == 1) {
        val singleMonth = chartData.first()
        // Create two points: one at 0 and one at the month's total
        chartEntries = listOf(
            0f to 0f,
            1f to singleMonth.total.toFloat()
        )
        // Create a custom formatter for the two points
        bottomAxisFormatter = AxisValueFormatter { value, _ ->
            when (value) {
                0f -> "Start"
                1f -> {
                    val monthParser = SimpleDateFormat("yyyy-MM", Locale.getDefault())
                    val monthFormatter = SimpleDateFormat("MMM", Locale.getDefault())
                    try {
                        monthParser.parse(singleMonth.yearMonth)?.let { monthFormatter.format(it) } ?: ""
                    } catch (e: Exception) { "" }
                }
                else -> ""
            }
        }
    } else {
        // This is the original logic for multiple months
        chartEntries = chartData.mapIndexed { index, monthlyTotal ->
            index.toFloat() to monthlyTotal.total.toFloat()
        }
        bottomAxisFormatter = AxisValueFormatter { value, _ ->
            val monthParser = SimpleDateFormat("yyyy-MM", Locale.getDefault())
            val monthFormatter = SimpleDateFormat("MMM", Locale.getDefault())
            try {
                chartData.getOrNull(value.toInt())?.yearMonth?.let {
                    monthParser.parse(it)?.let { date -> monthFormatter.format(date) }
                } ?: ""
            } catch (e: Exception) { "" }
        }
    }

    val chartEntryModelProducer = ChartEntryModelProducer(
        chartEntries.map { (x, y) -> entryOf(x, y) }
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Monthly Spending Trend", style = MaterialTheme.typography.titleMedium)
            Chart(
                // This is the only line that changed
                chart = lineChart(),
                chartModelProducer = chartEntryModelProducer,
                startAxis = rememberStartAxis(),
                bottomAxis = rememberBottomAxis(valueFormatter = bottomAxisFormatter),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
        }
    }
}