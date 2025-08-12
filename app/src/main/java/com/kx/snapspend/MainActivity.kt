package com.kx.snapspend

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.kx.snapspend.screen.DashboardScreen
import com.kx.snapspend.screen.EditCollectionScreen
import com.kx.snapspend.screen.EditExpenseScreen
import com.kx.snapspend.screen.SettingsScreen // You will create this file in the next steps
import com.kx.snapspend.screen.TransactionListScreen
import com.kx.snapspend.ui.screens.ReportsScreen
import com.kx.snapspend.ui.theme.SnapSpendTheme
import com.kx.snapspend.viewmodel.MainViewModel
import com.kx.snapspend.viewmodel.MainViewModelFactory

class MainActivity : ComponentActivity() {
    // In MainActivity.kt
    private val mainViewModel: MainViewModel by viewModels {
        MainViewModelFactory(
            (application as BudgetTrackerApplication).repository,
            (application as BudgetTrackerApplication).firestoreRepository, // Pass it
            applicationContext
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SnapSpendTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // This now handles both permissions and navigation
                    AppContent()
                }
            }
        }
    }

    @OptIn(ExperimentalPermissionsApi::class)
    @Composable
    fun AppContent() {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val permissionsState = rememberMultiplePermissionsState(permissions = permissionsToRequest)

        // Based on the permission status, show either the permission screen or the main app navigation
        if (permissionsState.allPermissionsGranted) {
            // If permissions are granted, set up the NavHost with your screens
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = "dashboard") {
                // Route for the Dashboard
                // In MainActivity.kt, inside NavHost
                composable("dashboard") {
                    DashboardScreen(
                        viewModel = mainViewModel,
                        onNavigateToSettings = { navController.navigate("settings") },
                        onNavigateToDetails = { collectionName -> navController.navigate("details/$collectionName") },
                        // Pass the new navigation action
                        onNavigateToEdit = { expenseId -> navController.navigate("edit/$expenseId") },
                        onNavigateToReports = { navController.navigate("reports") }
                    )
                }
                // In MainActivity.kt, inside NavHost
                composable("settings") {
                    SettingsScreen(
                        viewModel = mainViewModel,
                        onNavigateUp = { navController.popBackStack() },
                        // Pass the new navigation action
                        onNavigateToEditCollection = { collectionName ->
                            navController.navigate("edit_collection/$collectionName")
                        }
                    )
                }
                // NEW: Route for the Transaction Details screen
                composable(
                    route = "details/{collectionName}",
                    arguments = listOf(navArgument("collectionName") { type = NavType.StringType })
                ) { backStackEntry ->
                    val collectionName = backStackEntry.arguments?.getString("collectionName") ?: ""
                    TransactionListScreen(
                        viewModel = mainViewModel,
                        collectionName = collectionName,
                        onNavigateUp = { navController.popBackStack() },
                        onNavigateToEdit = { expenseId -> navController.navigate("edit/$expenseId") }
                    )
                }
                composable(
                    route = "edit/{expenseId}",
                    arguments = listOf(navArgument("expenseId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val expenseId = backStackEntry.arguments?.getLong("expenseId") ?: 0L
                    if (expenseId > 0) {
                        EditExpenseScreen(
                            viewModel = mainViewModel,
                            expenseId = expenseId,
                            onNavigateUp = { navController.popBackStack() }
                        )
                    }
                }
                // In MainActivity.kt, inside the NavHost
                composable(
                    route = "edit_collection/{collectionName}",
                    arguments = listOf(navArgument("collectionName") { type = NavType.StringType })
                ) { backStackEntry ->
                    val collectionName = backStackEntry.arguments?.getString("collectionName") ?: ""
                    EditCollectionScreen(
                        viewModel = mainViewModel,
                        collectionName = collectionName,
                        onNavigateUp = { navController.popBackStack() }
                    )
                }
                composable("reports") {
                    ReportsScreen(
                        viewModel = mainViewModel,
                        onNavigateUp = { navController.popBackStack() }
                    )
                }
            }
        } else {
            // If permissions are not granted, show the screen asking for them
            PermissionDeniedScreen {
                permissionsState.launchMultiplePermissionRequest()
            }
        }
    }

    @Composable
    fun PermissionDeniedScreen(onRequestPermission: () -> Unit) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Location Permission Required",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "This app uses your location to automatically categorize expenses. Please grant permission to enable this feature.",
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onRequestPermission) {
                Text("Grant Permission")
            }
        }
    }
}