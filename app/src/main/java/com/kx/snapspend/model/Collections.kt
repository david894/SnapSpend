package com.kx.snapspend.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "collections")
data class Collections(
    @PrimaryKey
    val name: String
)
