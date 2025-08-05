package com.kx.snapspend


import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import com.kx.snapspend.data.database.AppDatabase
import com.kx.snapspend.data.repository.BudgetTrackerRepository

/**
 * Custom Application class for the Budget Tracker app.
 * This class is the entry point of the application and is used to initialize
 * application-level components like the database and repository.
 */
class BudgetTrackerApplication : Application() {

    // Using a SupervisorJob so that if one coroutine fails, others are not cancelled.
    val applicationScope = CoroutineScope(SupervisorJob())

    // Lazy initialization of the database.
    // The database is created only when it's first accessed.
    val database by lazy { AppDatabase.getDatabase(this, applicationScope) }

    // Lazy initialization of the repository.
    // The repository is created only when it's first accessed, using the database DAOs.
    val repository by lazy { BudgetTrackerRepository(database.expenseDao(), database.collectionsDao()) }
}
