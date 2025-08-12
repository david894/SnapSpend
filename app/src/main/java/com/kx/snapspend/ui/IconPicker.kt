package com.kx.snapspend.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector

// A map of icon names to their actual ImageVector objects
val iconMap: Map<String, ImageVector> = mapOf(
    "Label" to Icons.Default.Label,
    "ShoppingCart" to Icons.Default.ShoppingCart,
    "Fastfood" to Icons.Default.Fastfood,
    "DirectionsCar" to Icons.Default.DirectionsCar,
    "Home" to Icons.Default.Home,
    "Flight" to Icons.Default.Flight,
    "CardGiftcard" to Icons.Default.CardGiftcard,
    "LocalHospital" to Icons.Default.LocalHospital,
    "School" to Icons.Default.School,
    "Pets" to Icons.Default.Pets,

    "Favorite" to Icons.Default.Favorite, // Love/Favorites
    "FitnessCenter" to Icons.Default.FitnessCenter, // Gym/Fitness
    "Movie" to Icons.Default.Movie, // Entertainment
    "Coffee" to Icons.Default.Coffee, // Cafes/Drinks
    "GasStation" to Icons.Default.LocalGasStation, // Gas
    "Bills" to Icons.Default.ReceiptLong, // Bills/Utilities
    "Savings" to Icons.Default.Savings, // Savings/Investment
    "Phone" to Icons.Default.PhoneAndroid // Phone Bill
)

// A predefined list of colors for the user to choose from
val colorList: List<Color> = listOf(
    Color(0xFF6200EE), // Purple
    Color(0xFF03DAC5), // Teal
    Color(0xFFB00020), // Red
    Color(0xFF00C853), // Green
    Color(0xFFFFD600), // Yellow
    Color(0xFF2962FF), // Blue
    Color(0xFFFF6D00), // Orange
    Color(0xFFd500f9), // Magenta

    Color(0xFFEF6351), // Coral Red
    Color(0xFF4F5D75), // Slate Blue
    Color(0xFF7D98A1), // Sage Green
    Color(0xFFF7B267), // Warm Gold
    Color(0xFF6A057F), // Royal Purple
    Color(0xFF3A86FF), // Bright Blue
    Color(0xFF2EC4B6), // Turquoise
    Color(0xFFCDB4DB),  // Soft Lilac

    Color(0xFFF4B6C2), // Pastel Pink
    Color(0xFFB5EAD7), // Mint Green
    Color(0xFFA2D2FF), // Baby Blue
    Color(0xFFC7CEEA), // Lavender
    Color(0xFFFFF1E6), // Soft Peach
    Color(0xFFFFD1BA), // Apricot
    Color(0xFFFFFACD), // Lemon Chiffon
    Color(0xFFE2F0CB),  // Pistachio
)

// Helper function to convert a hex string to a Color object (this one is correct)
fun colorFromHex(hex: String): Color {
    return Color(android.graphics.Color.parseColor(hex))
}

// THIS IS THE CORRECTED FUNCTION
// It now uses the official .toArgb() method for reliable conversion.
fun colorToHex(color: Color): String {
    return String.format("#%08X", color.toArgb())
}