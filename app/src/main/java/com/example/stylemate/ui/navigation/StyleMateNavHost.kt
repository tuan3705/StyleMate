package com.example.stylemate.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.stylemate.ui.screens.AIStylistScreen
import com.example.stylemate.ui.screens.ai_stylist.AIStylistChatScreen
import com.example.stylemate.ui.screens.ai_stylist.OutfitSuggestionWizard
import com.example.stylemate.ui.screens.style_assess.StyleAssessScreen
import com.example.stylemate.ui.screens.virtual_tryon.TryOnSetupScreen
import com.example.stylemate.ui.screens.item_upload.ItemUploadScreen
import com.example.stylemate.ui.screens.find_colors.ColorIntroScreen

object StyleMateRoutes {
    const val AI_STYLIST = "ai_stylist"
    const val STYLE_ASSESS = "style_assess"
    const val VIRTUAL_TRY_ON = "virtual_try_on"
    const val ITEM_UPLOAD = "item_upload"
    const val FIND_COLORS = "find_colors"
    const val AI_CHAT = "ai_chat"
    const val OUTFIT_SUGGESTION = "outfit_suggestion"
}

@Composable
fun StyleMateNavHost(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = StyleMateRoutes.AI_STYLIST
    ) {
        composable(StyleMateRoutes.AI_STYLIST) {
            AIStylistScreen(
                onNavigateToWizard = {
                    navController.navigate(StyleMateRoutes.AI_CHAT)
                }
            )
        }

        composable(StyleMateRoutes.AI_CHAT) { backStackEntry ->
            val wizardResult = backStackEntry.savedStateHandle.get<String>("wizard_result")
            AIStylistChatScreen(
                onBack = { navController.popBackStack() },
                onNavigateToWizard = {
                    navController.navigate(StyleMateRoutes.OUTFIT_SUGGESTION)
                },
                wizardResult = wizardResult,
                onWizardResultConsumed = {
                    backStackEntry.savedStateHandle.remove<String>("wizard_result")
                }
            )
        }
        
        composable(StyleMateRoutes.STYLE_ASSESS) {
            StyleAssessScreen(onBack = { navController.popBackStack() })
        }
        
        composable(StyleMateRoutes.VIRTUAL_TRY_ON) {
            TryOnSetupScreen(onBack = { navController.popBackStack() })
        }
        
        composable(StyleMateRoutes.ITEM_UPLOAD) {
            ItemUploadScreen(onBack = { navController.popBackStack() })
        }
        
        composable(StyleMateRoutes.FIND_COLORS) {
            ColorIntroScreen(onBack = { navController.popBackStack() })
        }

        composable(StyleMateRoutes.OUTFIT_SUGGESTION) {
            OutfitSuggestionWizard(
                onBack = { navController.popBackStack() },
                onFinish = { items, occasion, style, theme ->
                    // Navigate back to Chat with the selected context
                    val message = "Hãy gợi ý phối đồ cho dịp $occasion với phong cách $style ${if (theme != "Không có") "tại $theme" else ""}."
                    // We can pass this through a shared ViewModel or SavedStateHandle
                    // For now, let's navigate back and we'll ensure the Chat screen can receive this.
                    navController.previousBackStackEntry?.savedStateHandle?.set("wizard_result", message)
                    navController.popBackStack()
                }
            )
        }
    }
}
