package com.kx.snapspend.model

data class SharedExpense(
    val id: String = "", // A unique ID for the expense
    val amount: Double = 0.0,
    val timestamp: Long = 0,
    val adderUserId: String = "",
    val adderUserName: String = "",
    val latitude: Double? = null,  // Add this
    val longitude: Double? = null, // Add this
    val locationCategory: String? = null // Add this new field
    // Note: We don't store the collection name here, as it's part of the parent document
)