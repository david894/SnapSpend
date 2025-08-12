package com.kx.snapspend.data.repository

import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import com.kx.snapspend.model.Collections
import com.kx.snapspend.model.Member
import com.kx.snapspend.model.SharedCollection
import com.kx.snapspend.model.SharedExpense
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreRepository {
    private val db = Firebase.firestore
    private val collectionsRef = db.collection("shared_collections")

    suspend fun createSharedCollection(collection: SharedCollection): Result<Unit> {
        return try {
            collectionsRef.document(collection.pin).set(collection).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSharedCollectionByPin(pin: String): SharedCollection? {
        return try {
            collectionsRef.document(pin).get().await().toObject(SharedCollection::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun addMemberToCollection(pin: String, member: Member): Result<Unit> {
        return try {
            collectionsRef.document(pin).update("members", FieldValue.arrayUnion(member)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // NEW: Update a shared collection's details
    suspend fun updateSharedCollectionDetails(pin: String, collection: Collections): Result<Unit> {
        return try {
            val updates = mapOf(
                "budget" to collection.budget,
                "iconName" to collection.iconName,
                "colorHex" to collection.colorHex
            )
            collectionsRef.document(pin).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // NEW: Listen for real-time changes to a collection's details
    fun listenToSharedCollectionDetails(pin: String): Flow<SharedCollection?> = callbackFlow {
        val listener = collectionsRef.document(pin)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObject(SharedCollection::class.java))
            }
        awaitClose { listener.remove() }
    }


    // NEW: Function to add a new expense to a shared collection
    suspend fun addExpenseToSharedCollection(pin: String, expense: SharedExpense): Result<Unit> {
        return try {
            collectionsRef.document(pin).collection("expenses").document(expense.id).set(expense).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // NEW: Function to listen for real-time expense changes
    fun listenToSharedExpenses(pin: String): Flow<List<SharedExpense>> = callbackFlow {
        val listener = collectionsRef.document(pin).collection("expenses")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val expenses = snapshot.toObjects(SharedExpense::class.java)
                    trySend(expenses)
                }
            }
        awaitClose { listener.remove() } // Clean up the listener when the flow is closed
    }

    suspend fun updateSharedExpenseLocation(pin: String, expenseId: String, lat: Double, lon: Double, category: String?): Result<Unit> {
        return try {
            val updates = mapOf(
                "latitude" to lat,
                "longitude" to lon,
                "locationCategory" to category
            )
            db.collection("shared_collections").document(pin).collection("expenses").document(expenseId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // NEW: Update an entire shared expense document.
    suspend fun updateSharedExpense(pin: String, expense: SharedExpense): Result<Unit> {
        return try {
            collectionsRef.document(pin).collection("expenses").document(expense.id).set(expense).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // NEW: Delete a shared expense document.
    suspend fun deleteSharedExpense(pin: String, expenseId: String): Result<Unit> {
        return try {
            collectionsRef.document(pin).collection("expenses").document(expenseId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}