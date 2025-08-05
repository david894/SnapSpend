package com.kx.snapspend.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kx.snapspend.model.Expenses

@Dao
interface ExpensesDao {
    // Inserts a new expense. If it already exists, it will be replaced.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expenses): Long

    // Updates an existing expense.
    @Update
    suspend fun updateExpense(expense: Expenses)

    // Gets a specific expense by its ID.
    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getExpenseById(id: Long): Expenses?

    // Gets all expenses for a given month and year, ordered by the most recent first.
    // Timestamps are stored in milliseconds, so we need to calculate the start and end of the month.
    @Query("SELECT * FROM expenses WHERE strftime('%Y-%m', timestamp / 1000, 'unixepoch') = :yearMonth ORDER BY timestamp DESC")
    fun getExpensesForMonth(yearMonth: String): LiveData<List<Expenses>>

    // Gets expenses with a null location category for background processing.
    @Query("SELECT * FROM expenses WHERE locationCategory IS NULL AND latitude IS NOT NULL")
    suspend fun getExpensesForLocationProcessing(): List<Expenses>

    // Gets total expenses for a specific date range and collection.
    @Query("SELECT SUM(amount) FROM expenses WHERE collectionName = :collectionName AND timestamp BETWEEN :startDate AND :endDate")
    suspend fun getTotalForCollectionInRange(collectionName: String, startDate: Long, endDate: Long): Double?
}