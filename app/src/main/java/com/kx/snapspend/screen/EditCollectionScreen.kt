package com.kx.snapspend.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext // Add this import
import com.kx.snapspend.ui.colorList
import com.kx.snapspend.ui.colorToHex
import com.kx.snapspend.ui.iconMap
import com.kx.snapspend.viewmodel.MainViewModel
import android.widget.Toast // Add this import
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCollectionScreen(
    viewModel: MainViewModel,
    collectionName: String,
    onNavigateUp: () -> Unit
) {
    val collection by viewModel.getCollectionByName(collectionName).collectAsState(initial = null)

    // State for the editable fields
    var budget by remember { mutableStateOf("") }
    var selectedIconName by remember { mutableStateOf("Label") }
    var selectedColorHex by remember { mutableStateOf("#FF6200EE") }
    var sharePin by remember { mutableStateOf<String?>(null) } // State for the PIN
    var showConsentDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Show the dialog when the state is true
    if (showConsentDialog) {
        ConsentDialog(
            onConfirm = {
                viewModel.saveConsent(true)
                showConsentDialog = false
                // For now, just show a confirmation toast
                Toast.makeText(context, "Sharing enabled! Ready to generate PIN.", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showConsentDialog = false }
        )
    }

    // When the collection data loads from the database, update our local state
    LaunchedEffect(collection) {
        collection?.let {
            budget = if (it.budget > 0) it.budget.toString() else ""
            selectedIconName = it.iconName
            selectedColorHex = it.colorHex
            sharePin = it.sharePin // Update the PIN state
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(collectionName, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        collection?.let { currentCollection ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(16.dp)
                    .fillMaxSize()
            ) {
                // Budget Field
                OutlinedTextField(
                    value = budget,
                    onValueChange = { budget = it },
                    label = { Text("Monthly Budget (RM)") },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))

                // Icon Picker
                Text("Icon", style = MaterialTheme.typography.titleMedium)
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 48.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    items(iconMap.keys.toList()) { iconName ->
                        val isSelected = iconName == selectedIconName
                        Icon(
                            imageVector = iconMap[iconName]!!,
                            contentDescription = iconName,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { selectedIconName = iconName }
                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .padding(8.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))

                // Color Picker
                Text("Color", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    colorList.chunked(8).forEach { rowColors ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowColors.forEach { color ->
                                val isSelected = colorToHex(color) == selectedColorHex
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(CircleShape)
                                        .background(color)
                                        .clickable { selectedColorHex = colorToHex(color) }
                                        .border(
                                            width = if (isSelected) 2.dp else 0.dp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            shape = CircleShape
                                        )
                                ) {
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, "Selected", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.align(Alignment.Center))
                                    }
                                }
                            }
                            repeat(8 - rowColors.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                        Spacer(modifier = Modifier.padding(5.dp))
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                if(!viewModel.hasUserConsented()){
                    Button(
                        onClick = {
                            if (viewModel.hasUserConsented()) {
                                // If already consented, proceed with sharing logic (for next step)
                                Toast.makeText(context, "Sharing enabled! Ready to generate PIN.", Toast.LENGTH_SHORT).show()
                            } else {
                                // If not consented, show the dialog
                                showConsentDialog = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Share Collection")
                    }
                }

                if (viewModel.hasUserConsented() && sharePin == null) {
                    // Show the Share button if there's no PIN yet
                    Button(
                        onClick = {
                            if (viewModel.userName.value.isBlank()) {
                                Toast.makeText(context, "Please set your name in Settings first.", Toast.LENGTH_LONG).show()
                                return@Button
                            }
                            // Call the ViewModel to generate the PIN and save to Firebase
                            val newPin = viewModel.shareCollection(currentCollection)
                            if (newPin != null) {
                                sharePin = newPin // Update the UI to show the PIN
                                Toast.makeText(context, "Collection is now shared!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Share Collection & Generate PIN")
                    }
                } else if (viewModel.hasUserConsented()) {
                    // Show the PIN if the collection is already shared
                    OutlinedTextField(
                        value = sharePin!!,
                        onValueChange = {},
                        label = { Text("Share PIN") },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                // Save Button
                Button(
                    onClick = {
                        val updatedBudget = budget.toDoubleOrNull() ?: 0.0
                        val updatedCollection = currentCollection.copy(
                            budget = updatedBudget,
                            iconName = selectedIconName,
                            colorHex = selectedColorHex
                        )
                        viewModel.updateCollection(updatedCollection)
                        onNavigateUp()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Changes")
                }
            }
        }
    }
}

@Composable
fun ConsentDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enable Cloud Sync?") },
        text = { Text("To share collections, your data will be stored securely using Firebase. This allows real-time syncing with other users. Do you agree to proceed?") },
        confirmButton = {
            Button(onClick = onConfirm) { Text("Agree & Proceed") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}