package com.kx.snapspend.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kx.snapspend.data.repository.BudgetTrackerRepository
import com.kx.snapspend.data.repository.FirestoreRepository

/**
 * Factory for creating a MainViewModel with a constructor that takes a BudgetTrackerRepository.
 * This is the standard way to instantiate a ViewModel that has dependencies.
 */
class MainViewModelFactory(
    private val repository: BudgetTrackerRepository,
    private val firestoreRepository: FirestoreRepository, // Add this
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository, firestoreRepository, context) as T // Pass it
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}