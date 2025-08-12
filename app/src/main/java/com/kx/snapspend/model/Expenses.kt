package com.kx.snapspend.model
import androidx.room.Entity
import androidx.room.PrimaryKey

// Represents a single expense entry
@Entity(tableName = "expenses")
data class Expenses(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val collectionName: String,
    val timestamp: Long,
    val latitude: Double?,
    val longitude: Double?,
    var locationCategory: String? = null,  // e.g., "Food", "Shopping"
    val cloudId: String? = null // Add this new field
)
