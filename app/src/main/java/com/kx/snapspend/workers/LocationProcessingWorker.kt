package com.kx.snapspend.workers // Use your actual package name

import android.annotation.SuppressLint
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.kx.snapspend.BudgetTrackerApplication
import com.kx.snapspend.network.GeocodingApiService
import com.kx.snapspend.network.GeocodingResponse
import kotlinx.coroutines.tasks.await
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class LocationProcessingWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val KEY_EXPENSE_ID = "key_expense_id"
        // IMPORTANT: Replace with your own Google API Key
        const val API_KEY = "YOUR_GOOGLE_MAPS_API_KEY_HERE"
    }

    @SuppressLint("MissingPermission") // Permissions are checked in MainActivity before this is called
    override suspend fun doWork(): Result {
        val expenseId = inputData.getLong(KEY_EXPENSE_ID, -1)
        if (expenseId == -1L) return Result.failure()

        val repository = (applicationContext as BudgetTrackerApplication).repository
        val expense = repository.getExpenseById(expenseId) ?: return Result.failure()

        // --- Step 1: Get Current Location ---
        val location = try {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(applicationContext)
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
        } catch (e: Exception) {
            // If location fails, retry later
            return Result.retry()
        }

        // --- Step 2: Update Expense with Location Coordinates ---
        val expenseWithLocation = expense.copy(latitude = location.latitude, longitude = location.longitude)
        repository.updateExpense(expenseWithLocation)

        // --- Step 3: Use Coordinates to Get Category from API ---
        val retrofit = Retrofit.Builder()
            .baseUrl("https://maps.googleapis.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val service = retrofit.create(GeocodingApiService::class.java)
        val latlng = "${location.latitude},${location.longitude}"

        return try {
            val response = service.reverseGeocode(latlng, API_KEY)
            if (response.isSuccessful && response.body() != null) {
                val category = parseCategoryFromResponse(response.body()!!)
                val finalExpense = expenseWithLocation.copy(locationCategory = category)
                repository.updateExpense(finalExpense)
                Result.success()
            } else {
                Result.retry() // API call failed, retry later
            }
        } catch (e: Exception) {
            Result.retry() // Network or other error, retry later
        }
    }

    private fun parseCategoryFromResponse(response: GeocodingResponse): String {
        val result = response.results.firstOrNull() ?: return "Uncategorized"
        val preferredTypes = listOf("restaurant", "cafe", "store", "supermarket", "shopping_mall", "gas_station", "food", "point_of_interest")
        val foundType = result.types.find { it in preferredTypes }
        return foundType?.replace("_", " ")?.replaceFirstChar { it.uppercase() } ?: "Location"
    }
}