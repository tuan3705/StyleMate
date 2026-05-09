package com.example.stylemate.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    object Closet : BottomNavItem("closet", "Closet", Icons.Filled.Home)
    object AIStylist : BottomNavItem("ai_stylist", "AI Stylist", Icons.Filled.Star)
    object Weather : BottomNavItem("weather", "Thời tiết", Icons.Filled.Place)
    object Calendar : BottomNavItem("calendar", "Lịch", Icons.Filled.DateRange)
}

