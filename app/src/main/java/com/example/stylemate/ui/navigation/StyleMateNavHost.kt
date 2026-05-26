package com.example.stylemate.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.stylemate.ui.screens.ai_stylist.AIStylistScreen
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
            AIStylistScreen()
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
    }
}
