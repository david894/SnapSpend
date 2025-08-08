package com.kx.snapspend.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.kx.snapspend.BudgetTrackerApplication
import com.kx.snapspend.ui.theme.SnapSpendTheme
import com.kx.snapspend.viewmodel.MainViewModel
import com.kx.snapspend.viewmodel.MainViewModelFactory

class WidgetEntryActivity : ComponentActivity() {

    companion object {
        const val EXTRA_COLLECTION_NAME = "extra_collection_name"
    }

    private val mainViewModel: MainViewModel by viewModels {
        MainViewModelFactory((application as BudgetTrackerApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val collectionName = intent.data?.lastPathSegment ?: intent.getStringExtra(EXTRA_COLLECTION_NAME)
        if (collectionName == null) {
            Toast.makeText(this, "Could not identify collection.", Toast.LENGTH_SHORT).show()
            finish() // Close if we still can't find the name
            return
        }

        setContent {
            SnapSpendTheme {
                WidgetDialog(
                    collectionName = collectionName,
                    onConfirm = { amount ->
                        mainViewModel.addExpense(collectionName, amount, this)
                        Toast.makeText(this, "Expense Added!", Toast.LENGTH_SHORT).show()
                        finish()
                    },
                    onDismiss = {
                        finish()
                    }
                )
            }
        }
    }
}