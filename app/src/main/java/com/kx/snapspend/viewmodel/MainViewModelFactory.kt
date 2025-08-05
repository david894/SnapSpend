package com.kx.snapspend.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kx.snapspend.data.repository.BudgetTrackerRepository

/**
 * Factory for creating a MainViewModel with a constructor that takes a BudgetTrackerRepository.
 * This is the standard way to instantiate a ViewModel that has dependencies.
 */
class MainViewModelFactory(private val repository: BudgetTrackerRepository) : ViewModelProvider.Factory {

    /**
     * Creates a new instance of the given `Class`.
     * @param modelClass a `Class` whose instance is requested
     * @return a newly created ViewModel
     */
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
