package com.kx.snapspend.screen

import android.view.RoundedCorner
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kx.snapspend.ui.colorFromHex
import com.kx.snapspend.viewmodel.ChangeType
import com.kx.snapspend.viewmodel.DashboardSummary
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun PieChartCard(
    summary: DashboardSummary,
    onClick: () -> Unit
) {
    val changeColor = when (summary.changeType) {
        ChangeType.INCREASE -> MaterialTheme.colorScheme.error
        ChangeType.DECREASE -> Color(0xFF008000)
        ChangeType.SAME -> Color.Gray
    }
    val changeIcon = when (summary.changeType) {
        ChangeType.INCREASE -> Icons.Default.KeyboardArrowUp
        ChangeType.DECREASE -> Icons.Default.KeyboardArrowDown
        ChangeType.SAME -> null
    }

    if (summary.spendingBreakdown.isEmpty() || summary.currentMonthTotal <= 0) return

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable(onClick = onClick), // <-- Move the click action here
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Monthly Breakdown", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(40.dp))

            // **FIX 1: The container is now a responsive square.**
            // It fills the width of the card and its height will match.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .align(Alignment.CenterHorizontally),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .height(200.dp) // Set the desired height
                        .aspectRatio(1f),
                    contentAlignment = Alignment.Center
                ) {
                    DonutChart(summary = summary)

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Spent",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                        Text(
                            text = "RM ${"%.2f".format(summary.currentMonthTotal)}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (changeIcon != null) {
                                Icon(
                                    imageVector = changeIcon,
                                    contentDescription = "Change indicator",
                                    tint = changeColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = "${"%.1f".format(abs(summary.percentageChange))}%",
                                color = changeColor,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                        Text(
                            text = "vs. Last Month",
                            color = changeColor,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun DonutChart(summary: DashboardSummary) {
    val breakdown = summary.spendingBreakdown
    if (breakdown.isEmpty() || summary.currentMonthTotal <= 0) return

    val totalSpent = summary.currentMonthTotal.toFloat()
    val density = LocalDensity.current

    val strokeWidth = with(density) { 25.dp.toPx() }
    val spacerDegrees = 2f
    val fontColor = MaterialTheme.colorScheme.inverseSurface

    Canvas(modifier = Modifier.fillMaxSize()) {
        val chartRadius = (size.minDimension - strokeWidth) / 2f
        var startAngle = -90f

        breakdown.forEach { spending ->
            val sweepAngle = (spending.totalAmount.toFloat() / totalSpent) * 360f
            var color = colorFromHex(spending.colorHex)

            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweepAngle - spacerDegrees,
                useCenter = false,
                topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                size = Size(chartRadius * 2, chartRadius * 2),
                style = Stroke(width = strokeWidth)
            )

            // --- Label Drawing Logic ---
            val angleInRad = (startAngle + sweepAngle / 2).toDouble() * (Math.PI / 180)
            // Increased the radius to push the text further away
            val labelRadius = chartRadius + with(density) { 50.dp.toPx() }
            val labelX = center.x + labelRadius * cos(angleInRad).toFloat()
            val labelY = center.y + labelRadius * sin(angleInRad).toFloat()
            val percentage = (spending.totalAmount / totalSpent) * 100

            val paint = android.graphics.Paint().apply {
                // ** THIS IS THE FIX **
                // Convert the Compose Color to an integer ARGB color
                this.color = fontColor.toArgb()
                textSize = 14.sp.toPx()
                textAlign = android.graphics.Paint.Align.CENTER
                isFakeBoldText = true
            }
            val fontSpacing = paint.fontSpacing

            // Draw the three lines of text, centered around the middle line
            drawContext.canvas.nativeCanvas.apply {
                // Line 1: Collection Name (Top)
                drawText(
                    "${"%.0f".format(percentage)}%",
                    labelX,
                    labelY - fontSpacing, // Position above the middle line
                    paint
                )
                // Line 2: Percentage (Middle)
                drawText(
                    "RM ${"%.0f".format(spending.totalAmount)}",
                    labelX,
                    labelY, // Center position
                    paint
                )
                // Line 3: Amount (Bottom)
                drawText(
                    spending.collectionName,
                    labelX,
                    labelY + fontSpacing, // Position below the middle line
                    paint
                )
            }

            startAngle += sweepAngle
        }
    }
}