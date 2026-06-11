package com.example.stylemate.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.stylemate.R

sealed class BottomNavItem(val route: String, val titleResId: Int, val icon: ImageVector) {
    object Closet : BottomNavItem("closet", R.string.nav_closet, Icons.Filled.Home)
    object AIStylist : BottomNavItem("ai_stylist", R.string.nav_ai_stylist, Icons.Filled.Star)
    object Weather : BottomNavItem("weather", R.string.nav_weather, Icons.Filled.Place)
    object Calendar : BottomNavItem("calendar", R.string.nav_calendar, Icons.Filled.DateRange)
}
