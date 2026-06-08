package com.example.stylemate.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.padding
import androidx.core.app.NotificationManagerCompat
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.stylemate.notification.NotificationBus
import com.example.stylemate.ui.components.AccountMenu
import com.example.stylemate.ui.navigation.BottomNavItem

@Composable
fun MainScreen(onLogout: () -> Unit) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* no-op */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val enabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
            if (!enabled) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LaunchedEffect(Unit) {
        NotificationBus.events.collect { event ->
            snackbarHostState.showSnackbar("${event.title}: ${event.body}")
        }
    }

    val items = listOf(
        BottomNavItem.Closet,
        BottomNavItem.AIStylist,
        BottomNavItem.Weather,
        BottomNavItem.Calendar
    )

    val isEditItemRoute = currentDestination?.route == "edit_item/{itemId}"

    Scaffold(
        bottomBar = {
            if (!isEditItemRoute) {
                NavigationBar {
                    items.forEach { item ->
                        val isSelected = currentDestination
                            ?.hierarchy
                            ?.any { it.route == item.route } == true

                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.title) },
                            label = { Text(item.title) },
                            selected = isSelected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Closet.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Closet.route) { backStackEntry ->
                val refreshSignal =
                    backStackEntry.savedStateHandle.getStateFlow("refresh_items", false)
                ClosetScreen(
                    onEditItem = { itemId ->
                        navController.navigate("edit_item/$itemId")
                    },
                    refreshSignal = refreshSignal,
                    onRefreshConsumed = {
                        backStackEntry.savedStateHandle["refresh_items"] = false
                    },
                    accountMenu = { AccountMenu(onLogout = onLogout) }
                )
            }
            composable(BottomNavItem.AIStylist.route) {
                AIStylistScreen(accountMenu = { AccountMenu(onLogout = onLogout) })
            }
            composable(BottomNavItem.Weather.route) {
                WeatherScreen(
                    accountMenu = {
                        AccountMenu(
                            onLogout = onLogout,
                            iconTint = Color.White
                        )
                    }
                )
            }
            composable(BottomNavItem.Calendar.route) {
                CalendarScreen(accountMenu = { AccountMenu(onLogout = onLogout) })
            }
            composable(
                route = "edit_item/{itemId}",
                arguments = listOf(navArgument("itemId") { type = NavType.StringType })
            ) { backStackEntry ->
                val itemId = backStackEntry.arguments?.getString("itemId").orEmpty()
                EditItemScreen(navController = navController, itemId = itemId)
            }
        }
    }
}
