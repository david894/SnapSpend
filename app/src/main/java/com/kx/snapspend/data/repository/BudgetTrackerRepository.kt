package com.kx.snapspend.data.repository

import androidx.lifecycle.LiveData
import com.kx.snapspend.data.dao.CollectionsDao
import com.kx.snapspend.data.dao.ExpensesDao
import com.kx.snapspend.model.Collections
import com.kx.snapspend.model.Expenses
import kotlinx.coroutines.flow.Flow

/**
 * Repository module for handling data operations.
 * It abstracts the data sources (Room database) from the rest of the app.
 * This is the single source of truth for all app data.
 */
class BudgetTrackerRepository(
    private val expensesDao: ExpensesDao,
    private val collectionsDao: CollectionsDao
) {

    // --- Collection Operations ---

    /**
     * Retrieves all collections from the database as LiveData.
     * LiveData will automatically update the UI when the data changes.
     */
    val allCollections: LiveData<List<Collections>> = collectionsDao.getAllCollections()

    /**
     * Inserts a new collection into the database.
     * This is a suspend function, so it must be called from a coroutine.
     * @param collections The collection to insert.
     */
    suspend fun insertCollection(collections: Collections) {
        collectionsDao.insertCollection(collections)
    }

    /**
     * Deletes a collection from the database.
     * @param collectionName The name of the collection to delete.
     */
    suspend fun deleteCollection(collectionName: String) {
        collectionsDao.deleteCollection(collectionName)
    }


    // --- Expense Operations ---

    /**
     * Inserts a new expense into the database.
     * @param expenses The expense to insert.
     * @return The ID of the newly inserted expense.
     */
    suspend fun insertExpense(expenses: Expenses): Long {
        return expensesDao.insertExpense(expenses)
    }

    /**
     * Updates an existing expense.
     * @param expenses The expense to update.
     */
    suspend fun updateExpense(expenses: Expenses) {
        expensesDao.updateExpense(expenses)
    }

    /**
     * Retrieves all expenses for a given month and year (e.g., "2024-08").
     * @param yearMonth The year and month in "YYYY-MM" format.
     * @return LiveData list of expenses for that month.
     */
    fun getExpensesForMonth(yearMonth: String): Flow<List<Expenses>> { // <-- Change LiveData to Flow
        return expensesDao.getExpensesForMonth(yearMonth)
    }

    /**
     * Retrieves expenses that need location processing.
     * These are expenses that were saved without a location category, likely due to being offline.
     * @return A list of expenses to be processed.
     */
    suspend fun getExpensesForLocationProcessing(): List<Expenses> {
        return expensesDao.getExpensesForLocationProcessing()
    }

    /**
     * Gets the total spending for a specific collection within a date range.
     * @param collectionName The name of the collection.
     * @param startDate The start of the date range in milliseconds.
     * @param endDate The end of the date range in milliseconds.
     * @return The total amount spent.
     */
    suspend fun getTotalForCollectionInRange(collectionName: String, startDate: Long, endDate: Long): Double {
        return expensesDao.getTotalForCollectionInRange(collectionName, startDate, endDate) ?: 0.0
    }

    suspend fun getExpenseById(id: Long): Expenses? {
        return expensesDao.getExpenseById(id)
    }

    /**
     * Gets the total spending for a specific date range.
     * @param startDate The start of the date range in milliseconds.
     * @param endDate The end of the date range in milliseconds.
     * @return The total amount spent, or 0.0 if null.
     */
    suspend fun getTotalForDateRange(startDate: Long, endDate: Long): Double {
        return expensesDao.getTotalForDateRange(startDate, endDate) ?: 0.0
    }

    fun getAllCollectionsSync(): List<Collections> {
        return collectionsDao.getAllCollectionsSync()
    }

    // In BudgetTrackerRepository.kt
    fun getTransactionsForCollectionInMonth(yearMonth: String, collectionName: String): Flow<List<Expenses>> {
        return expensesDao.getTransactionsForCollectionInMonth(yearMonth, collectionName)
    }
}
