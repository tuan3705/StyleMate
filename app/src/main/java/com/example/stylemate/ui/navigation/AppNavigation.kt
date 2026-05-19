package com.example.stylemate.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.stylemate.StyleMateApp
import com.example.stylemate.ui.screens.LoadingScreen
import com.example.stylemate.ui.screens.LoginScreen
import com.example.stylemate.ui.screens.MainScreen
import com.example.stylemate.viewmodel.AuthViewModel
import com.example.stylemate.viewmodel.AuthViewModelFactory

@Composable
fun AppNavigation() {
    val app = LocalContext.current.applicationContext as StyleMateApp
    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(app.authRepository)
    )
    val uiState by authViewModel.uiState.collectAsState()

    when {
        uiState.isLoading -> LoadingScreen()
        uiState.isLoggedIn -> MainScreen(onLogout = authViewModel::logout)
        else -> LoginScreen(
            uiState = uiState,
            onLogin = authViewModel::login,
            onRegister = authViewModel::register,
            onClearError = authViewModel::clearError
        )
    }
}
