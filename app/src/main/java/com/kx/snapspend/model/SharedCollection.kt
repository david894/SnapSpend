package com.kx.snapspend.model

// Represents a user in a shared collection
data class Member(
    val userId: String = "",
    val name: String = ""
)

// Represents the shared collection document in Firestore
data class SharedCollection(
    val pin: String = "",
    val name: String = "",
    val budget: Double = 0.0,
    val iconName: String = "",
    val colorHex: String = "",
    val members: List<Member> = emptyList()
)