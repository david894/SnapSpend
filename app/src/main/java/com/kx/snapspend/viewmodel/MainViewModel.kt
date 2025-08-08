package com.kx.snapspend.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.kx.snapspend.data.repository.BudgetTrackerRepository
import com.kx.snapspend.model.Collections
import com.kx.snapspend.model.Expenses
import com.kx.snapspend.workers.LocationProcessingWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// Data classes remain the same
data class CollectionSpending(
    val collectionName: String,
    val totalAmount: Double
)

data class DashboardSummary(
    val currentMonthTotal: Double = 0.0,
    val percentageChange: Double = 0.0,
    val changeType: ChangeType = ChangeType.SAME,
    val spendingBreakdown: List<CollectionSpending> = emptyList()
)

enum class ChangeType { INCREASE, DECREASE, SAME }

class MainViewModel(private val repository: BudgetTrackerRepository) : ViewModel() {

    // This can remain LiveData as it's simple
    val allCollections: LiveData<List<Collections>> = repository.allCollections

    private val _viewingDate = MutableStateFlow(Calendar.getInstance())
    val viewingDate: StateFlow<Calendar> = _viewingDate

    // This is now a StateFlow that gets its data directly from the repository's Flow
    val monthlyExpenses: StateFlow<List<Expenses>> = _viewingDate
        .flatMapLatest { date ->
            val yearMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(date.time)
            repository.getExpensesForMonth(yearMonth)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    // This StateFlow combines the other two flows to produce the final summary
    val dashboardSummary: StateFlow<DashboardSummary> = monthlyExpenses
        .combine(viewingDate) { expenses, date ->
            calculateSummary(expenses, date)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = DashboardSummary()
        )

    fun previousMonth() {
        val newDate = _viewingDate.value.clone() as Calendar
        newDate.add(Calendar.MONTH, -1)
        _viewingDate.value = newDate
    }

    fun nextMonth() {
        val newDate = _viewingDate.value.clone() as Calendar
        newDate.add(Calendar.MONTH, 1)
        _viewingDate.value = newDate
    }

    private suspend fun calculateSummary(currentExpenses: List<Expenses>, viewingDate: Calendar): DashboardSummary {
        val currentMonthTotal = currentExpenses.sumOf { it.amount }
        val calendar = viewingDate.clone() as Calendar
        calendar.set(Calendar.DAY_OF_MONTH, 1)

        calendar.add(Calendar.MONTH, -1)
        val prevMonthEnd = calendar.timeInMillis
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val prevMonthStart = calendar.timeInMillis

        val previousMonthTotal = repository.getTotalForDateRange(prevMonthStart, prevMonthEnd)

        val (percentageChange, changeType) = if (previousMonthTotal > 0) {
            val change = ((currentMonthTotal - previousMonthTotal) / previousMonthTotal) * 100
            val type = when {
                change > 0 -> ChangeType.INCREASE
                change < 0 -> ChangeType.DECREASE
                else -> ChangeType.SAME
            }
            Pair(change, type)
        } else if (currentMonthTotal > 0) {
            Pair(100.0, ChangeType.INCREASE)
        } else {
            Pair(0.0, ChangeType.SAME)
        }

        val spendingBreakdown = currentExpenses
            .groupBy { it.collectionName }
            .map { (name, expenses) ->
                CollectionSpending(
                    collectionName = name,
                    totalAmount = expenses.sumOf { it.amount }
                )
            }
            .sortedByDescending { it.totalAmount }

        return DashboardSummary(
            currentMonthTotal = currentMonthTotal,
            percentageChange = percentageChange,
            changeType = changeType,
            spendingBreakdown = spendingBreakdown
        )
    }

    fun addExpense(collectionName: String, amount: Double, context: Context) {
        viewModelScope.launch {
            val newExpense = Expenses(
                amount = amount,
                collectionName = collectionName,
                timestamp = System.currentTimeMillis(),
                latitude = null,
                longitude = null,
                locationCategory = null
            )
            val newExpenseId = repository.insertExpense(newExpense)
            startLocationProcessingWorker(newExpenseId, context)
        }
    }

    private fun startLocationProcessingWorker(expenseId: Long, context: Context) {
        val workManager = WorkManager.getInstance(context)
        val workData = workDataOf(LocationProcessingWorker.KEY_EXPENSE_ID to expenseId)
        val workRequest = OneTimeWorkRequestBuilder<LocationProcessingWorker>()
            .setInputData(workData)
            .build()
        workManager.enqueue(workRequest)
    }

    fun addCollection(name: String) {
        viewModelScope.launch {
            repository.insertCollection(Collections(name = name))
        }
    }

    fun deleteCollection(name: String) {
        viewModelScope.launch {
            repository.deleteCollection(name)
        }
    }

    fun getTransactionsForCollection(collectionName: String): Flow<List<Expenses>> {
        // This combines the currently viewed date with the requested collection name
        // to get a flow of the correctly filtered transactions.
        return viewingDate.flatMapLatest { date ->
            val yearMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(date.time)
            repository.getTransactionsForCollectionInMonth(yearMonth, collectionName)
        }
    }

}