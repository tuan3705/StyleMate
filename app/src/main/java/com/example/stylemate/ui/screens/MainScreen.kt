package com.example.stylemate.ui.screens

import android.Manifest
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
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
import com.example.stylemate.ui.navigation.StyleMateRoutes
import com.example.stylemate.ui.screens.ai_stylist.AIChatScreen
import com.example.stylemate.ui.screens.ai_stylist.PersonalStylistScreen
import com.example.stylemate.ui.screens.ai_stylist.OutfitSuggestionWizard
import com.example.stylemate.ui.screens.ai_stylist.AISettingsScreen
import com.example.stylemate.ui.screens.ai_stylist.AILocationSettingsScreen
import com.example.stylemate.ui.screens.ai_stylist.AIClosetSettingsScreen
import com.example.stylemate.ui.screens.ai_stylist.AINotesSettingsScreen

@Composable
fun MainScreen(onLogout: () -> Unit) {
    Log.d("MainScreen", "Rendering MainScreen...")
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

    val bottomNavItems = listOf(
        BottomNavItem.Closet,
        BottomNavItem.AIStylist,
        BottomNavItem.Weather,
        BottomNavItem.Calendar
    )

    val currentRoute = currentDestination?.route ?: ""
    val isFullScreenRoute = currentRoute == "edit_item/{itemId}" ||
                            currentRoute == StyleMateRoutes.AI_CHAT || 
                            currentRoute == StyleMateRoutes.AI_PERSONAL_STYLIST ||
                            currentRoute == StyleMateRoutes.OUTFIT_SUGGESTION ||
                            currentRoute.startsWith("ai_settings")

    Scaffold(
        bottomBar = {
            if (!isFullScreenRoute) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val isSelected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.title) },
                            label = { Text(item.title) },
                            selected = isSelected,
                            onClick = {
                                Log.d("Navigation", "BottomNav Click: ${item.route}")
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
                Log.d("MainScreen", "Route: CLOSET")
                val refreshSignal = backStackEntry.savedStateHandle.getStateFlow("refresh_items", false)
                ClosetScreen(
                    onEditItem = { itemId -> navController.navigate("edit_item/$itemId") },
                    refreshSignal = refreshSignal,
                    onRefreshConsumed = { backStackEntry.savedStateHandle["refresh_items"] = false },
                    accountMenu = { AccountMenu(onLogout = onLogout) }
                )
            }

            composable(BottomNavItem.AIStylist.route) {
                Log.d("MainScreen", "Route: AI_HUB")
                AIStylistScreen(
                    onNavigateToChat = {
                        navController.navigate(StyleMateRoutes.AI_CHAT)
                    },
                    onNavigateToPersonalStylist = {
                        navController.navigate(StyleMateRoutes.AI_PERSONAL_STYLIST)
                    },
                    accountMenu = { AccountMenu(onLogout = onLogout) }
                )
            }

            composable(StyleMateRoutes.AI_CHAT) {
                Log.d("MainScreen", "Route: AI_CHAT")
                AIChatScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToSettings = { navController.navigate(StyleMateRoutes.AI_SETTINGS) }
                )
            }

            composable(StyleMateRoutes.AI_PERSONAL_STYLIST) { backStackEntry ->
                Log.d("MainScreen", "Route: PERSONAL_STYLIST")
                val wizardResult = backStackEntry.savedStateHandle.get<String>("wizard_result")
                PersonalStylistScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToWizard = { navController.navigate(StyleMateRoutes.OUTFIT_SUGGESTION) },
                    onNavigateToSettings = { navController.navigate(StyleMateRoutes.AI_SETTINGS) },
                    wizardResult = wizardResult,
                    onWizardResultConsumed = { backStackEntry.savedStateHandle.remove<String>("wizard_result") }
                )
            }

            composable(StyleMateRoutes.AI_SETTINGS) {
                Log.d("MainScreen", "Route: AI_SETTINGS")
                AISettingsScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToLocation = { navController.navigate(StyleMateRoutes.AI_SETTINGS_LOCATION) },
                    onNavigateToCloset = { navController.navigate(StyleMateRoutes.AI_SETTINGS_CLOSET) },
                    onNavigateToNotes = { navController.navigate(StyleMateRoutes.AI_SETTINGS_NOTES) }
                )
            }

            composable(StyleMateRoutes.AI_SETTINGS_LOCATION) {
                AILocationSettingsScreen(onBack = { navController.popBackStack() })
            }

            composable(StyleMateRoutes.AI_SETTINGS_CLOSET) {
                AIClosetSettingsScreen(onBack = { navController.popBackStack() })
            }

            composable(StyleMateRoutes.AI_SETTINGS_NOTES) {
                AINotesSettingsScreen(onBack = { navController.popBackStack() })
            }

            composable(StyleMateRoutes.OUTFIT_SUGGESTION) {
                Log.d("MainScreen", "Route: WIZARD")
                OutfitSuggestionWizard(
                    onBack = { navController.popBackStack() },
                    onFinish = { items, occasion, style, theme ->
                        val message = "Hãy gợi ý phối đồ cho dịp $occasion với phong cách $style ${if (theme != "Không có") "tại $theme" else ""}."
                        navController.previousBackStackEntry?.savedStateHandle?.set("wizard_result", message)
                        navController.popBackStack()
                    }
                )
            }

            composable(BottomNavItem.Weather.route) {
                WeatherScreen(accountMenu = { AccountMenu(onLogout = onLogout, iconTint = Color.White) })
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

