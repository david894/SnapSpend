package com.kx.snapspend.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kx.snapspend.model.Collections
import com.kx.snapspend.model.Expenses
import com.kx.snapspend.viewmodel.MainViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: MainViewModel) {
    // Observe the LiveData from the ViewModel.
    // Compose will automatically recompose this screen when the data changes.
    val collections by viewModel.allCollections.observeAsState(initial = emptyList())
    val monthlyExpenses by viewModel.monthlyExpenses.observeAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        // Floating Action Button for adding expenses (an alternative to the bottom buttons)
        floatingActionButton = {
            FloatingActionButton(onClick = { /* TODO: Open a more detailed expense entry dialog */ }) {
                Icon(Icons.Filled.Add, "Add Expense")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // 1. Monthly Summary Card (Placeholder for now)
            MonthlySummaryCard(monthlyExpenses)

            // 2. Recent Transactions List
            Text(
                text = "Recent Transactions",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // Takes up remaining space
            ) {
                items(monthlyExpenses) { expense ->
                    ExpenseListItem(expense)
                }
            }

            // 3. Quick Add Buttons (simulating the widget)
            QuickAddSection(collections) { collectionName ->
                // For now, let's add a fixed amount for demonstration
                // In a real scenario, this would open the quick-add dialog
                viewModel.addExpense(collectionName, 10.0)
            }
        }
    }
}

@Composable
fun MonthlySummaryCard(expenses: List<Expenses>) {
    val total = expenses.sumOf { it.amount }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Total Spent This Month", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "RM ${"%.2f".format(total)}",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Placeholder for comparison with the previous month
            Text(text = "vs. Last Month: --%", color = Color.Gray)
        }
    }
}


@Composable
fun ExpenseListItem(expense: Expenses) {
    val date = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(expense.timestamp))
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = expense.collectionName, fontWeight = FontWeight.Bold)
                Text(text = date, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Text(
                text = "RM ${"%.2f".format(expense.amount)}",
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun QuickAddSection(collections: List<Collections>, onAdd: (String) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Quick Add",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                collections.take(4).forEach { collection ->
                    Button(onClick = { onAdd(collection.name) }) {
                        Text(collection.name)
                    }
                }
            }
        }
    }
}

