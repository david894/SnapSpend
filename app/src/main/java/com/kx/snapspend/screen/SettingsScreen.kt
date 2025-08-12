package com.kx.snapspend.screen

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kx.snapspend.model.Collections
import com.kx.snapspend.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigateUp: () -> Unit,
    onNavigateToEditCollection: (String) -> Unit
) {
    val collections by viewModel.allCollections.observeAsState(initial = emptyList())
    val userName by viewModel.userName.collectAsState()
    var newCollectionName by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf(userName) }
    var pinInput by remember { mutableStateOf("") }

    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(userName) {
        nameInput = userName
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        // Use a single LazyColumn for the entire screen's content
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .fillMaxSize()
        ) {
            // Item 1: Profile Section
            item {
                Text("Your Profile", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Your Name") },
                    placeholder = { Text("e.g., David") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        viewModel.saveUserName(nameInput)
                        Toast.makeText(context, "Name saved!", Toast.LENGTH_SHORT).show()
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    }
                ) {
                    Text("Save Name")
                }
                Divider(modifier = Modifier.padding(vertical = 24.dp))
            }

            // Item 2: Join a Collection Section
            item {
                Text("Join a Collection", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = pinInput,
                    onValueChange = { pinInput = it },
                    label = { Text("Enter 10-Digit PIN") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = userName.isNotBlank()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        // First, check if the user's name is set.
                        if (viewModel.userName.value.isBlank()) {
                            Toast.makeText(context, "Please set your name in the Profile section first.", Toast.LENGTH_LONG).show()
                            return@Button // Stop the function here
                        }

                        val pin = pinInput.trim()
                        if (pin.length == 10) {
                            scope.launch {
                                val success = viewModel.joinCollection(pin)
                                if (success) {
                                    Toast.makeText(context, "Successfully joined collection!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "PIN not found or error occurred.", Toast.LENGTH_SHORT).show()
                                }
                            }
                            pinInput = ""
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        } else {
                            Toast.makeText(context, "Please enter a valid 10-digit PIN.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = userName.isNotBlank(),
                ) {
                    Text("Join")
                }
                if(!userName.isNotBlank()){
                    Text(
                        text = "Please set your name in the Profile section first before joining a collection.",
                        fontSize = 12.sp
                    )
                }
                Divider(modifier = Modifier.padding(vertical = 24.dp))
            }

            // Item 4: Manage Collections Header
            item {
                Text("Manage Collections", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Items 5...N: The list of existing collections
            items(collections) { collection ->
                CollectionListItem(
                    collection = collection,
                    onDelete = { viewModel.deleteCollection(collection.name) },
                    onClick = { onNavigateToEditCollection(collection.name) }
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun CollectionListItem(
    collection: Collections,
    onDelete: () -> Unit,
    onClick: () -> Unit // Add onClick parameter
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick) // Make the whole card clickable
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = collection.name, modifier = Modifier.weight(1f))
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Collection")
            }
        }
    }
}
