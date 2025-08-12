package com.kx.snapspend.workers // Use your actual package name

import android.annotation.SuppressLint
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.kx.snapspend.BudgetTrackerApplication
import com.kx.snapspend.data.UserPreferences
import com.kx.snapspend.model.SharedExpense
import com.kx.snapspend.network.GeocodingApiService
import com.kx.snapspend.network.GeocodingResponse
import kotlinx.coroutines.tasks.await
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.UUID
import kotlinx.coroutines.flow.*

class LocationProcessingWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val KEY_EXPENSE_ID = "key_expense_id"
        const val API_KEY = "YOUR_GOOGLE_MAPS_API_KEY_HERE"
    }

    @SuppressLint("MissingPermission")
    override suspend fun doWork(): Result {
        val expenseId = inputData.getLong(KEY_EXPENSE_ID, -1)
        if (expenseId == -1L) return Result.failure()

        val application = applicationContext as BudgetTrackerApplication
        val repository = application.repository
        val firestoreRepository = application.firestoreRepository

        val expense = repository.getExpenseById(expenseId) ?: return Result.failure()

        // --- Step 1: Get Location & Category (Same as before) ---
        val location = try {
            LocationServices.getFusedLocationProviderClient(applicationContext)
                .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
        } catch (e: Exception) {
            return Result.retry()
        }

        val retrofit = Retrofit.Builder().baseUrl("https://maps.googleapis.com/").addConverterFactory(GsonConverterFactory.create()).build()
        val service = retrofit.create(GeocodingApiService::class.java)
        val latlng = "${location.latitude},${location.longitude}"
        val categoryResponse = service.reverseGeocode(latlng, API_KEY)
        val category = if (categoryResponse.isSuccessful) parseCategoryFromResponse(categoryResponse.body()) else "Uncategorized"

        // --- Step 2: Update the LOCAL expense with all data ---
        val cloudId = expense.cloudId ?: UUID.randomUUID().toString()
        val updatedLocalExpense = expense.copy(
            latitude = location.latitude,
            longitude = location.longitude,
            locationCategory = category,
            cloudId = cloudId
        )
        repository.updateExpense(updatedLocalExpense)

        // --- Step 3: Check if the collection is shared and SYNC to Firestore ---
        val collection = repository.getCollectionByName(expense.collectionName).first()
        if (collection?.sharePin != null && expense.cloudId != null) {
            firestoreRepository.updateSharedExpenseLocation(
                pin = collection.sharePin,
                expenseId = expense.cloudId,
                lat = location.latitude,
                lon = location.longitude,
                category = category
            )
        }

        return Result.success()
    }

    private fun parseCategoryFromResponse(response: GeocodingResponse?): String {
        val result = response?.results?.firstOrNull() ?: return "Uncategorized"
        val preferredTypes = listOf("restaurant", "cafe", "store", "supermarket", "shopping_mall", "gas_station", "food", "point_of_interest")
        val foundType = result.types.find { it in preferredTypes }
        return foundType?.replace("_", " ")?.replaceFirstChar { it.uppercase() } ?: "Location"
    }
}