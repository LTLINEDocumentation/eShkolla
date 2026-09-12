package com.ltline.eshkolla.app

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ltline.eshkolla.features.auth.LoginScreen
import com.ltline.eshkolla.features.dashboard.DashboardScreen

private object Routes {
    const val LOGIN = "login"
    const val DASHBOARD = "dashboard"
}

@Composable
fun EShkollaApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.LOGIN
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.DASHBOARD) {
            DashboardScreen()
        }
    }
}
