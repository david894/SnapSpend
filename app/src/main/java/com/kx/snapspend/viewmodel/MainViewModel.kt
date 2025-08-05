package com.kx.snapspend.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kx.snapspend.data.repository.BudgetTrackerRepository
import com.kx.snapspend.model.Collections
import com.kx.snapspend.model.Expenses
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ViewModel for the main screen of the Budget Tracker app.
 * It prepares and manages the data for the UI.
 */
class MainViewModel(private val repository: BudgetTrackerRepository) : ViewModel() {

    // --- LiveData for UI Observation ---

    // Exposes the list of all user-defined collections.
    // The UI will observe this to display the collection buttons on the widget and in the app.
    val allCollections: LiveData<List<Collections>> = repository.allCollections

    // Exposes the list of all expenses for the current month.
    // The UI will observe this to display the transaction history.
    val monthlyExpenses: LiveData<List<Expenses>>

    init {
        // Get the current year and month in "YYYY-MM" format.
        val currentYearMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        // Initialize the LiveData stream for the current month's expenses.
        monthlyExpenses = repository.getExpensesForMonth(currentYearMonth)
    }

    // --- Public Functions for UI Events ---

    /**
     * Adds a new expense to the database.
     * This will be called when the user enters an amount in the quick-add dialog.
     *
     * @param collectionName The name of the collection the expense belongs to.
     * @param amount The amount of the expense.
     * @param latitude The latitude for location-based categorization (can be null).
     * @param longitude The longitude for location-based categorization (can be null).
     */
    fun addExpense(collectionName: String, amount: Double, latitude: Double? = null, longitude: Double? = null) {
        // Launch a coroutine in the ViewModel's scope to perform the database insert.
        // This ensures the operation is done on a background thread and is lifecycle-aware.
        viewModelScope.launch {
            val newExpense = Expenses(
                amount = amount,
                collectionName = collectionName,
                timestamp = System.currentTimeMillis(),
                latitude = latitude,
                longitude = longitude,
                locationCategory = null // Initially null, will be filled by a background job.
            )
            repository.insertExpense(newExpense)
        }
    }
}
