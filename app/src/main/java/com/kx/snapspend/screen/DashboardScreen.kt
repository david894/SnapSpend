package com.kx.snapspend.screen

import android.widget.Toast
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Label
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import com.kx.snapspend.ui.WidgetDialog
import com.kx.snapspend.ui.colorFromHex
import com.kx.snapspend.ui.iconMap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToDetails: (String) -> Unit, // Add this new parameter
    onNavigateToEdit: (Long) -> Unit, // Add this new callback
    onNavigateToReports : () -> Unit,
) {
    val summary by viewModel.dashboardSummary.collectAsState()
    val collections by viewModel.allCollections.observeAsState(initial = emptyList())
    val monthlyExpenses by viewModel.monthlyExpenses.collectAsState() // Use collectAsState for Flow
    // NEW: Observe the viewing date from the ViewModel
    val viewingDate by viewModel.viewingDate.collectAsState()
    val chartData by viewModel.chartData.collectAsState() // <-- Add this line

    var showDialog by remember { mutableStateOf(false) }
    var showAddCollectionDialog by remember { mutableStateOf(false) } // New state for the collection dialog
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
    if (showAddCollectionDialog) {
        AddCollectionDialog(
            onConfirm = { newName ->
                viewModel.addCollection(newName)
                Toast.makeText(context, "Collection '$newName' added!", Toast.LENGTH_SHORT).show()
                showAddCollectionDialog = false
            },
            onDismiss = {
                showAddCollectionDialog = false
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
            if (monthlyExpenses.isEmpty()) {
                // If the list is empty, show the "Empty State" message
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f) // Takes up the remaining space
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "No Transactions",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Add an expense using the 'Quick Add' buttons below to get started.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = Color.Gray
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    item {
                        PieChartCard(
                            summary = summary,
                            onClick = onNavigateToReports // Pass the function reference directly
                        )
                    }
                    item {
                        Text(
                            text = "Transactions",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
                        )
                    }
                    items(monthlyExpenses) { expense ->
                        // Pass the callback to the list item
                        ExpenseListItem(expense, onClick = { onNavigateToEdit(expense.id) })
                    }
                }
            }
            QuickAddSection(
                collections = collections,
                onCollectionClick = { collectionName ->
                    selectedCollection = collectionName
                    showDialog = true
                },
                onAddClick = {
                    showAddCollectionDialog = true
                }
            )
        }
    }
}
// NEW: A dialog specifically for adding a new collection
@Composable
fun AddCollectionDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newName by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Collection") },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("Collection Name") },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done // Important for the keyboard action
                ),
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newName.isNotBlank()) {
                        onConfirm(newName)
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
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
fun ExpenseListItem(expense: Expenses, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick)
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

                // NEW: Display the location category if it's available
                if (expense.locationCategory != null) {
                    Text(
                        text = expense.locationCategory!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                val date = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(expense.timestamp))
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
fun QuickAddSection(
    collections: List<Collections>,
    onCollectionClick: (String) -> Unit,
    onAddClick: () -> Unit // New callback for the add button
) {
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically // Aligns items vertically in the center
            ) {
                // The LazyRow now takes up all available space, pushing the button to the end.
                LazyRow(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 16.dp, end = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(collections) { collection ->
                        Button(onClick = { onCollectionClick(collection.name) }) {
                            Text(collection.name)
                        }
                    }
                }

                // This IconButton is now correctly positioned at the end of the Row.
                IconButton(
                    onClick = onAddClick,
                    modifier = Modifier
                        .padding(end = 8.dp) // Add some padding
                        .background(MaterialTheme.colorScheme.secondaryContainer, shape = CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add new collection",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

