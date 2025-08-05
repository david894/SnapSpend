package com.kx.snapspend

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.kx.snapspend.screen.DashboardScreen
import com.kx.snapspend.ui.theme.SnapSpendTheme
import com.kx.snapspend.viewmodel.MainViewModel
import com.kx.snapspend.viewmodel.MainViewModelFactory

class MainActivity : ComponentActivity() {
    // Lazily initialize the MainViewModel using our custom factory.
    // This ensures the ViewModel is created with the repository from our Application class.
    private val mainViewModel: MainViewModel by viewModels {
        MainViewModelFactory((application as BudgetTrackerApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SnapSpendTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Set the main screen of the app.
                    // We pass the ViewModel instance to our composable screen.
                    DashboardScreen(viewModel = mainViewModel)
                }
            }
        }
    }
}

// You'll need to create a theme file, e.g., ui/theme/Theme.kt
// For now, you can use a default theme to preview.
@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    SnapSpendTheme {
        // This is a preview and won't have a real ViewModel.
        // We can create a dummy screen for visualization.
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            // You can add dummy data here to see the preview
        }
    }
}

