package com.kx.snapspend

import android.Manifest
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
import com.kx.snapspend.screen.SettingsScreen // You will create this file in the next steps
import com.kx.snapspend.screen.TransactionListScreen
import com.kx.snapspend.ui.theme.SnapSpendTheme
import com.kx.snapspend.viewmodel.MainViewModel
import com.kx.snapspend.viewmodel.MainViewModelFactory

class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels {
        MainViewModelFactory((application as BudgetTrackerApplication).repository)
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
        val locationPermissionsState = rememberMultiplePermissionsState(
            listOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
            )
        )

        // Based on the permission status, show either the permission screen or the main app navigation
        if (locationPermissionsState.allPermissionsGranted) {
            // If permissions are granted, set up the NavHost with your screens
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = "dashboard") {
                // Route for the Dashboard
                composable("dashboard") {
                    DashboardScreen(
                        viewModel = mainViewModel,
                        onNavigateToSettings = {
                            navController.navigate("settings")
                        },
                        // Add a new callback for navigating to the details screen
                        onNavigateToDetails = { collectionName ->
                            navController.navigate("details/$collectionName")
                        }
                    )
                }
                // Route for the Settings screen
                composable("settings") {
                    SettingsScreen(
                        viewModel = mainViewModel,
                        onNavigateUp = { navController.popBackStack() }
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
                        onNavigateUp = { navController.popBackStack() }
                    )
                }
            }
        } else {
            // If permissions are not granted, show the screen asking for them
            PermissionDeniedScreen {
                locationPermissionsState.launchMultiplePermissionRequest()
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