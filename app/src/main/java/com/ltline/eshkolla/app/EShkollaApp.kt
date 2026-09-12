package com.ltline.eshkolla.app

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ltline.eshkolla.domain.auth.AuthState
import com.ltline.eshkolla.features.auth.AuthViewModel
import com.ltline.eshkolla.features.auth.LoginScreen
import com.ltline.eshkolla.features.dashboard.DashboardScreen

private object Routes {
    const val LOGIN = "login"
    const val DASHBOARD = "dashboard"
}

@Composable
fun EShkollaApp() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()

    NavHost(navController = navController, startDestination = Routes.LOGIN) {
        composable(Routes.LOGIN) {
            LoginScreen(
                state = authViewModel.state.value,
                onLogin = authViewModel::login,
                onLoginSuccess = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.DASHBOARD) {
            val user = (authViewModel.state.value as? AuthState.LoggedIn)?.user
            DashboardScreen(user = user)
        }
    }
}
