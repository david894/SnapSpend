package com.kx.snapspend.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "collections")
data class Collections(
    @PrimaryKey
    val name: String,
    val budget: Double = 0.0,
    val iconName: String = "Label", // Add this field for the icon name
    val colorHex: String = "#FF6200EE", // Add this field for the color
    val sharePin: String? = null // Add this new field
)