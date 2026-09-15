package com.ltline.eshkolla.app

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ltline.eshkolla.domain.auth.AuthState
import com.ltline.eshkolla.features.auth.LoginScreen
import com.ltline.eshkolla.features.auth.RealAuthViewModel
import com.ltline.eshkolla.features.dashboard.DashboardScreen
import com.ltline.eshkolla.features.students.StudentScreen
import com.ltline.eshkolla.features.students.StudentViewModel

private object Routes {
    const val LOGIN = "login"
    const val DASHBOARD = "dashboard"
    const val STUDENTS = "students"
}

@Composable
fun EShkollaApp() {
    val navController = rememberNavController()
    val authViewModel: RealAuthViewModel = viewModel()

    NavHost(navController = navController, startDestination = Routes.LOGIN) {
        composable(Routes.LOGIN) {
            LoginScreen(
                state = authViewModel.state.value,
                onLogin = authViewModel::login,
                onLoginSuccess = {
                    navController.navigate(Routes.DASHBOARD) { popUpTo(Routes.LOGIN) { inclusive = true } }
                }
            )
        }
        composable(Routes.DASHBOARD) {
            val user = (authViewModel.state.value as? AuthState.LoggedIn)?.user
            DashboardScreen(user = user, onModuleClick = { module -> if (module == "students") navController.navigate(Routes.STUDENTS) })
        }
        composable(Routes.STUDENTS) {
            val studentViewModel: StudentViewModel = viewModel()
            StudentScreen(viewModel = studentViewModel, onBack = { navController.popBackStack() })
        }
    }
}
