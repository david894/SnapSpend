package com.kx.snapspend.screen

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.kx.snapspend.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditExpenseScreen(
    viewModel: MainViewModel,
    expenseId: Long,
    onNavigateUp: () -> Unit
) {
    val expense by viewModel.getExpenseById(expenseId).collectAsState(initial = null)
    val collections by viewModel.allCollections.observeAsState(initial = emptyList())
    val context = LocalContext.current

    // State for the editable fields
    var amount by remember { mutableStateOf("") }
    var selectedCollection by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") } // <-- New state for the category

    // When the expense data loads, update our local state
    LaunchedEffect(expense) {
        expense?.let {
            amount = it.amount.toString()
            selectedCollection = it.collectionName
            category = it.locationCategory ?: "" // <-- Populate the category field
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Transaction", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        expense?.let {
                            viewModel.deleteExpense(it)
                            onNavigateUp()
                        }
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            )
        }
    ) { innerPadding ->
        expense?.let { currentExpense ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(16.dp)
                    .fillMaxSize()
            ) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = selectedCollection,
                    onValueChange = { selectedCollection = it },
                    label = { Text("Collection") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                // NEW: Text field for the location category
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category (Optional)") },
                    placeholder = { Text("e.g., Restaurant, Shopping") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))
                // NEW: Location Section - only shows if coordinates exist
                if (currentExpense.latitude != null && currentExpense.longitude != null) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Location", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Hyperlink to open Google Maps
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val gmmIntentUri = Uri.parse("geo:${currentExpense.latitude},${currentExpense.longitude}?q=${currentExpense.latitude},${currentExpense.longitude}(Expense Location)")
                                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                mapIntent.setPackage("com.google.android.apps.maps")
                                context.startActivity(mapIntent)
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Location Pin",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "View on Google Maps",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            textDecoration = TextDecoration.Underline
                        )
                    }

                    // Placeholder for the future embedded map
                    Spacer(modifier = Modifier.height(16.dp))
                    /*
                    // TODO: UNCOMMENT THIS WHEN YOU HAVE A MAPS API KEY
                    // 1. Add this dependency to your app's build.gradle.kts:
                    //    implementation("com.google.maps.android:maps-compose:4.3.3")
                    // 2. Add your API key to the AndroidManifest.xml:
                    //    <meta-data android:name="com.google.android.geo.API_KEY" android:value="YOUR_KEY_HERE" />

                    val cameraPositionState = rememberCameraPositionState {
                        position = CameraPosition.fromLatLngZoom(
                            LatLng(currentExpense.latitude, currentExpense.longitude), 15f
                        )
                    }
                    GoogleMap(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        cameraPositionState = cameraPositionState
                    ) {
                        Marker(
                            state = MarkerState(position = LatLng(currentExpense.latitude, currentExpense.longitude)),
                            title = "Expense Location"
                        )
                    }
                    */
                }

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        val updatedAmount = amount.toDoubleOrNull()
                        if (updatedAmount != null) {
                            val updatedExpense = currentExpense.copy(
                                amount = updatedAmount,
                                collectionName = selectedCollection,
                                locationCategory = category.ifBlank { null } // <-- Save the new category
                            )
                            viewModel.updateExpense(updatedExpense)
                            onNavigateUp()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Changes")
                }
            }
        }
    }
}