package com.example.stylemate.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.stylemate.StyleMateApp
import com.example.stylemate.notification.fetchFcmToken
import com.example.stylemate.ui.screens.LoginScreen
import com.example.stylemate.ui.screens.MainScreen
import com.example.stylemate.viewmodel.AuthViewModel
import com.example.stylemate.viewmodel.AuthViewModel.AuthViewModelFactory
import android.util.Log

/**
 * Root Navigation của ứng dụng.
 *
 * Sử dụng MVVM: theo dõi [AuthViewModel.uiState] để quyết định
 * hiển thị LoginScreen hay MainScreen.
 * KHÔNG quản lý state cục bộ - tất cả logic auth đều trong ViewModel.
 */
@Composable
fun AppNavigation() {
    val app = LocalContext.current.applicationContext as StyleMateApp
    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(app.authRepository)
    )
    val uiState by authViewModel.uiState.collectAsState()

    // Khi user đăng nhập → đồng bộ FCM token
    LaunchedEffect(uiState) {
        if (uiState is AuthViewModel.LoginUiState.LoggedIn) {
            Log.d("AppNavigation", "User logged in, fetching FCM token...")
            val token = fetchFcmToken()
            if (!token.isNullOrBlank()) {
                app.fcmRepository.cacheFcmToken(token)
                app.fcmRepository.syncFcmToken(token)
            } else {
                app.fcmRepository.syncFcmToken()
            }
        }
    }

    when (uiState) {
        is AuthViewModel.LoginUiState.Loading -> {
            LoginScreen(
                viewModel = authViewModel,
                onLoggedIn = { /* handled by state change */ }
            )
        }

        is AuthViewModel.LoginUiState.Form -> {
            LoginScreen(
                viewModel = authViewModel,
                onLoggedIn = { /* handled by state change */ }
            )
        }

        is AuthViewModel.LoginUiState.LoggedIn -> {
            MainScreen(onLogout = authViewModel::logout)
        }
    }
}