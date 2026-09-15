package com.ltline.eshkolla.app

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ltline.eshkolla.domain.auth.AuthState
import com.ltline.eshkolla.features.absences.AbsenceScreen
import com.ltline.eshkolla.features.auth.LoginScreen
import com.ltline.eshkolla.features.auth.RealAuthViewModel
import com.ltline.eshkolla.features.classes.TeacherClassesScreen
import com.ltline.eshkolla.features.dashboard.DashboardScreen
import com.ltline.eshkolla.features.grades.GradeScreen
import com.ltline.eshkolla.features.students.StudentScreen
import com.ltline.eshkolla.features.students.StudentViewModel

private object Routes {
    const val LOGIN = "login"
    const val DASHBOARD = "dashboard"
    const val STUDENTS = "students"
    const val CLASSES = "classes"
    const val GRADES = "grades"
    const val ABSENCES = "absences"
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
                onLoginSuccess = { navController.navigate(Routes.DASHBOARD) { popUpTo(Routes.LOGIN) { inclusive = true } } }
            )
        }
        composable(Routes.DASHBOARD) {
            val user = (authViewModel.state.value as? AuthState.LoggedIn)?.user
            DashboardScreen(user = user, onModuleClick = { module ->
                when (module) {
                    "students" -> navController.navigate(Routes.STUDENTS)
                    "classes" -> navController.navigate(Routes.CLASSES)
                    "grades" -> navController.navigate(Routes.GRADES)
                    "absences" -> navController.navigate(Routes.ABSENCES)
                }
            })
        }
        composable(Routes.STUDENTS) {
            val studentViewModel: StudentViewModel = viewModel()
            StudentScreen(viewModel = studentViewModel, onBack = { navController.popBackStack() })
        }
        composable(Routes.CLASSES) {
            TeacherClassesScreen(
                onBack = { navController.popBackStack() },
                onGrades = { navController.navigate(Routes.GRADES) },
                onAbsences = { navController.navigate(Routes.ABSENCES) }
            )
        }
        composable(Routes.GRADES) {
            GradeScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.ABSENCES) {
            AbsenceScreen(onBack = { navController.popBackStack() })
        }
    }
}
