package com.kx.snapspend

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import com.kx.snapspend.data.database.AppDatabase
import com.kx.snapspend.data.repository.BudgetTrackerRepository
import androidx.work.*
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.kx.snapspend.data.repository.FirestoreRepository
import kotlinx.coroutines.tasks.await
import com.kx.snapspend.workers.BudgetAlertWorker
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Custom Application class for the Budget Tracker app.
 * This class is the entry point of the application and is used to initialize
 * application-level components like the database and repository.
 */
class BudgetTrackerApplication : Application() {

    val firestoreRepository by lazy { FirestoreRepository() }

    // Using a SupervisorJob so that if one coroutine fails, others are not cancelled.
    val applicationScope = CoroutineScope(SupervisorJob())

    // Lazy initialization of the database.
    // The database is created only when it's first accessed.
    val database by lazy { AppDatabase.getDatabase(this, applicationScope) }

    // Lazy initialization of the repository.
    // The repository is created only when it's first accessed, using the database DAOs.
    val repository by lazy { BudgetTrackerRepository(database.expenseDao(), database.collectionsDao()) }

    // Add a lazy-initialized property for Firebase Auth
    val firebaseAuth: FirebaseAuth by lazy { Firebase.auth }

    override fun onCreate() {
        super.onCreate()

        // Sign the user in anonymously when the app starts
        applicationScope.launch {
            try {
                if (firebaseAuth.currentUser == null) {
                    firebaseAuth.signInAnonymously().await()
                }
            } catch (e: Exception) {
                // Handle error, e.g., no internet connection
            }
        }

        scheduleBudgetAlerts()
    }

    private fun scheduleBudgetAlerts() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED) // Optional: only run with internet
            .build()

        val repeatingRequest = PeriodicWorkRequestBuilder<BudgetAlertWorker>(
            1, TimeUnit.DAYS // Run once a day
        ).setConstraints(constraints).build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "budget-alert-worker",
            ExistingPeriodicWorkPolicy.KEEP, // Keep the existing schedule if it's already running
            repeatingRequest
        )
    }
}
