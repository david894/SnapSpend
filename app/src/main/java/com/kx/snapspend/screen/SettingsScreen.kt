package com.kx.snapspend.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kx.snapspend.model.Collections
import com.kx.snapspend.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MainViewModel, onNavigateUp: () -> Unit) {
    val collections by viewModel.allCollections.observeAsState(initial = emptyList())
    var newCollectionName by remember { mutableStateOf("") }

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
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            // Section to add a new collection
            OutlinedTextField(
                value = newCollectionName,
                onValueChange = { newCollectionName = it },
                label = { Text("New Collection Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    if (newCollectionName.isNotBlank()) {
                        viewModel.addCollection(newCollectionName)
                        newCollectionName = "" // Clear the input field
                    }
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Add Collection")
            }

            Divider(modifier = Modifier.padding(vertical = 24.dp))

            // List of existing collections
            Text("Manage Collections", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn {
                items(collections) { collection ->
                    CollectionListItem(collection) {
                        viewModel.deleteCollection(collection.name)
                    }
                }
            }
        }
    }
}

@Composable
fun CollectionListItem(collection: Collections, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
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
