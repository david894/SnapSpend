package com.kx.snapspend.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable // Add this import
import com.kx.snapspend.model.Collections
import com.kx.snapspend.model.Expenses
import com.kx.snapspend.viewmodel.ChangeType
import com.kx.snapspend.viewmodel.CollectionSpending
import com.kx.snapspend.viewmodel.DashboardSummary
import com.kx.snapspend.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

import androidx.compose.runtime.collectAsState
import com.kx.snapspend.ui.WidgetDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToDetails: (String) -> Unit // Add this new parameter
) {
    val summary by viewModel.dashboardSummary.collectAsState()
    val collections by viewModel.allCollections.observeAsState(initial = emptyList())
    val monthlyExpenses by viewModel.monthlyExpenses.collectAsState() // Use collectAsState for Flow
    // NEW: Observe the viewing date from the ViewModel
    val viewingDate by viewModel.viewingDate.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var selectedCollection by remember { mutableStateOf<String?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current

    if (showDialog && selectedCollection != null) {
        WidgetDialog(
            collectionName = selectedCollection!!,
            onConfirm = { amount ->
                viewModel.addExpense(selectedCollection!!, amount, context)
                showDialog = false
                selectedCollection = null
            },
            onDismiss = {
                showDialog = false
                selectedCollection = null
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                // Add this actions block
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { /* TODO */ }) {
                Icon(Icons.Filled.Add, "Add Expense")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // NEW: Add the month selector UI
            MonthSelector(
                date = viewingDate,
                onPrevious = { viewModel.previousMonth() },
                onNext = { viewModel.nextMonth() }
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                item { MonthlySummaryCard(summary) }
                if (summary.spendingBreakdown.isNotEmpty()) {
                    item {
                        SpendingBreakdownCard(
                            breakdown = summary.spendingBreakdown,
                            onItemClick = onNavigateToDetails // Pass the callback here
                        )
                    }
                }
                item {
                    Text(
                        text = "Transactions",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
                    )
                }
                items(monthlyExpenses) { expense ->
                    ExpenseListItem(expense)
                }
            }
            QuickAddSection(collections) { collectionName ->
                selectedCollection = collectionName
                showDialog = true
            }
        }
    }
}

// NEW Composable for the month navigation
@Composable
fun MonthSelector(date: Calendar, onPrevious: () -> Unit, onNext: () -> Unit) {
    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous Month")
        }
        Text(
            text = monthFormat.format(date.time),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next Month")
        }
    }
}

// The rest of the file (SpendingBreakdownCard, MonthlySummaryCard, etc.) remains unchanged.
// I'm including them here for completeness.

@Composable
fun SpendingBreakdownCard(breakdown: List<CollectionSpending>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Spending by Category",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            breakdown.forEach { spending ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = spending.collectionName,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "RM ${"%.2f".format(spending.totalAmount)}",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@Composable
fun MonthlySummaryCard(summary: DashboardSummary) {
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
                text = "RM ${"%.2f".format(summary.currentMonthTotal)}",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
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
                    text = "${"%.1f".format(abs(summary.percentageChange))}% vs. Last Month",
                    color = changeColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
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
fun QuickAddSection(collections: List<Collections>, onCollectionClick: (String) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.padding(vertical = 16.dp)) {
            Text(
                text = "Quick Add",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
            )
            // Use LazyRow for a horizontally scrolling list
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp) // Adds space between buttons
            ) {
                items(collections) { collection ->
                    Button(onClick = { onCollectionClick(collection.name) }) {
                        Text(collection.name)
                    }
                }
            }
        }
    }
}

@Composable
fun SpendingBreakdownCard(
    breakdown: List<CollectionSpending>,
    onItemClick: (String) -> Unit // Add a callback for clicks
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Spending by Category",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            breakdown.forEach { spending ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium) // Make the whole row look clickable
                        .clickable { onItemClick(spending.collectionName) } // Make it clickable
                        .padding(vertical = 8.dp, horizontal = 4.dp), // Add padding for the click area
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = spending.collectionName,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "RM ${"%.2f".format(spending.totalAmount)}",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}
//@Composable
//fun ExpenseInputDialog(
//    collectionName: String,
//    onConfirm: (Double) -> Unit,
//    onDismiss: () -> Unit
//) {
//    var text by remember { mutableStateOf("") }
//    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
//    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
//
//    AlertDialog(
//        onDismissRequest = onDismiss,
//        title = { Text("Add to $collectionName") },
//        text = {
//            Column {
//                Text("Enter the amount for your expense.")
//                Spacer(modifier = Modifier.height(16.dp))
//                OutlinedTextField(
//                    value = text,
//                    onValueChange = { text = it },
//                    label = { Text("Amount (RM)") },
//                    singleLine = true,
//                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
//                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
//                        imeAction = androidx.compose.ui.text.input.ImeAction.Done
//                    ),
//                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
//                        onDone = {
//                            val amount = text.toDoubleOrNull()
//                            if (amount != null && amount > 0) {
//                                onConfirm(amount)
//                            }
//                            keyboardController?.hide()
//                        }
//                    ),
//                    modifier = Modifier.focusRequester(focusRequester)
//                )
//                LaunchedEffect(Unit) {
//                    focusRequester.requestFocus()
//                }
//            }
//        },
//        confirmButton = {
//            Button(
//                onClick = {
//                    val amount = text.toDoubleOrNull()
//                    if (amount != null && amount > 0) {
//                        onConfirm(amount)
//                    }
//                }
//            ) {
//                Text("Confirm")
//            }
//        },
//        dismissButton = {
//            TextButton(onClick = onDismiss) {
//                Text("Cancel")
//            }
//        }
//    )
//}