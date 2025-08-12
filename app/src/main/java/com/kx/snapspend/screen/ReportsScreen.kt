package com.kx.snapspend.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Label
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kx.snapspend.screen.ChartCard
import com.kx.snapspend.ui.colorFromHex
import com.kx.snapspend.ui.iconMap
import com.kx.snapspend.viewmodel.CollectionSpending
import com.kx.snapspend.viewmodel.MainViewModel
import kotlin.collections.forEach

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: MainViewModel,
    onNavigateUp: () -> Unit
) {
    val summary by viewModel.dashboardSummary.collectAsState()
    val chartData by viewModel.chartData.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Monthly Reports", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            item { ChartCard(chartData = chartData) }

            item {
                // We can reuse the SpendingBreakdownCard here.
                // Note: You might need to make it public if it's in another file.
                SpendingBreakdownCard(
                    breakdown = summary.spendingBreakdown,
                    onItemClick = { /* No navigation from this screen */ }
                )
            }
        }
    }
}

@Composable
fun SpendingBreakdownCard(
    breakdown: List<CollectionSpending>,
    onItemClick: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Budget by Category",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))

            breakdown.forEach { spending ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .clickable { onItemClick(spending.collectionName) }
                        .padding(vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Display the selected icon with its color
                        Icon(
                            imageVector = iconMap[spending.iconName] ?: Icons.Default.Label,
                            contentDescription = spending.collectionName,
                            tint = colorFromHex(spending.colorHex),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = spending.collectionName,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        if(spending.budget > 0){
                            Text(
                                text = "RM ${"%.0f".format(spending.totalAmount)} / ${"%.0f".format(spending.budget)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }else{
                            Text(
                                text = "RM ${"%.0f".format(spending.totalAmount)} / ∞",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    val progressColor = if (spending.progress > 1f) {
                        MaterialTheme.colorScheme.error
                    } else {
                        colorFromHex(spending.colorHex) // Use the collection's color
                    }

                    LinearProgressIndicator(
                        progress = { spending.progress.coerceAtMost(1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = progressColor,
                        trackColor = progressColor.copy(alpha = 0.2f), // Make the track a lighter shade
                        strokeCap = StrokeCap.Round
                    )
                }

            }
        }
    }
}
