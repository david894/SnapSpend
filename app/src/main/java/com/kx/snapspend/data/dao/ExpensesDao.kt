package com.kx.snapspend.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kx.snapspend.model.Expenses
import com.kx.snapspend.model.MonthlyTotal
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpensesDao {
    // Inserts a new expense. If it already exists, it will be replaced.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expenses): Long

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getExpenseById(id: Long): Expenses?

    @Update
    suspend fun updateExpense(expense: Expenses)

    // Gets all expenses for a given month and year, ordered by the most recent first.
    // Timestamps are stored in milliseconds, so we need to calculate the start and end of the month.
    @Query("SELECT * FROM expenses WHERE strftime('%Y-%m', timestamp / 1000, 'unixepoch') = :yearMonth ORDER BY timestamp DESC")
    fun getExpensesForMonth(yearMonth: String): Flow<List<Expenses>> // <-- Change LiveData to Flow

    // Gets expenses with a null location category for background processing.
    @Query("SELECT * FROM expenses WHERE locationCategory IS NULL AND latitude IS NOT NULL")
    suspend fun getExpensesForLocationProcessing(): List<Expenses>

    // Gets total expenses for a specific date range and collection.
    @Query("SELECT SUM(amount) FROM expenses WHERE collectionName = :collectionName AND timestamp BETWEEN :startDate AND :endDate")
    suspend fun getTotalForCollectionInRange(collectionName: String, startDate: Long, endDate: Long): Double?

    // In ExpensesDao.kt
    @Query("SELECT SUM(amount) FROM expenses WHERE timestamp BETWEEN :startDate AND :endDate AND collectionName = :collectionName")
    suspend fun getTotalForDateRange(startDate: Long, endDate: Long, collectionName: String): Double?

    @Query("SELECT SUM(amount) FROM expenses WHERE timestamp BETWEEN :startDate AND :endDate")
    suspend fun getTotalForDateRange(startDate: Long, endDate: Long): Double?

    // In ExpensesDao.kt
    @Query("SELECT * FROM expenses WHERE strftime('%Y-%m', timestamp / 1000, 'unixepoch') = :yearMonth AND collectionName = :collectionName ORDER BY timestamp DESC")
    fun getTransactionsForCollectionInMonth(yearMonth: String, collectionName: String): Flow<List<Expenses>>

    @Query("SELECT * FROM expenses WHERE id = :expenseId")
    fun getExpenseByIdFlow(expenseId: Long): Flow<Expenses?>

    @Delete
    suspend fun deleteExpense(expense: Expenses)

    @Query("""
    SELECT strftime('%Y-%m', timestamp / 1000, 'unixepoch') as yearMonth, SUM(amount) as total
    FROM expenses
    WHERE timestamp >= :sixMonthsAgo
    GROUP BY yearMonth
    ORDER BY yearMonth ASC
    """)
    fun getSpendingForLastSixMonths(sixMonthsAgo: Long): Flow<List<MonthlyTotal>>

    // In ExpensesDao.kt
    @Query("SELECT * FROM expenses WHERE strftime('%Y-%m', timestamp / 1000, 'unixepoch') = :yearMonth AND collectionName = :collectionName")
    fun getTransactionsForCollectionInMonthSync(yearMonth: String, collectionName: String): List<Expenses>

    @Query("SELECT * FROM expenses WHERE collectionName = :collectionName")
    fun getExpensesForCollectionSync(collectionName: String): List<Expenses>
}