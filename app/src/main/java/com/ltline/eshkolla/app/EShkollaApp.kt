package com.ltline.eshkolla.app

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ltline.eshkolla.domain.auth.AuthState
import com.ltline.eshkolla.domain.model.UserRole
import com.ltline.eshkolla.features.absences.AbsenceScreen
import com.ltline.eshkolla.features.auth.LoginScreen
import com.ltline.eshkolla.features.auth.RealAuthViewModel
import com.ltline.eshkolla.features.classes.TeacherClassesScreen
import com.ltline.eshkolla.features.dashboard.DashboardScreen
import com.ltline.eshkolla.features.grades.GradeScreen
import com.ltline.eshkolla.features.management.ManagementScreen
import com.ltline.eshkolla.features.profile.ProfileScreen
import com.ltline.eshkolla.features.students.StudentScreen
import com.ltline.eshkolla.features.students.StudentViewModel

private object Routes {
    const val LOGIN = "login"
    const val DASHBOARD = "dashboard"
    const val PROFILE = "profile"
    const val STUDENTS = "students"
    const val CLASSES = "classes"
    const val GRADES = "grades"
    const val GRADE_FOR_STUDENT = "grades/{studentId}"
    const val ABSENCES = "absences"
    const val ABSENCE_FOR_STUDENT = "absences/{studentId}"
    const val MANAGEMENT = "management/{section}"
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
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.DASHBOARD) {
            val user = (authViewModel.state.value as? AuthState.LoggedIn)?.user
            DashboardScreen(
                user = user,
                onModuleClick = { module ->
                    when (module) {
                        "students" -> navController.navigate(Routes.STUDENTS)
                        "classes" -> navController.navigate(Routes.CLASSES)
                        "grades" -> navController.navigate(Routes.GRADES)
                        "absences" -> navController.navigate(Routes.ABSENCES)
                        "teachers" -> navController.navigate("management/teachers")
                        "users" -> navController.navigate("management/users")
                        "profile" -> navController.navigate(Routes.PROFILE)
                    }
                }
            )
        }

        composable(Routes.PROFILE) {
            val user = (authViewModel.state.value as? AuthState.LoggedIn)?.user
            ProfileScreen(
                user = user,
                onBack = { navController.popBackStack() },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(
            Routes.MANAGEMENT,
            arguments = listOf(navArgument("section") { type = NavType.StringType })
        ) { backStack ->
            val role = (authViewModel.state.value as? AuthState.LoggedIn)?.user?.role ?: UserRole.NXENES
            ManagementScreen(
                role = role,
                section = backStack.arguments?.getString("section").orEmpty(),
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.STUDENTS) {
            val vm: StudentViewModel = viewModel()
            StudentScreen(viewModel = vm, onBack = { navController.popBackStack() })
        }

        composable(Routes.CLASSES) {
            TeacherClassesScreen(
                onBack = { navController.popBackStack() },
                onGrades = { id -> navController.navigate("grades/$id") },
                onAbsences = { id -> navController.navigate("absences/$id") }
            )
        }

        composable(Routes.GRADES) {
            GradeScreen(onBack = { navController.popBackStack() })
        }

        composable(
            Routes.GRADE_FOR_STUDENT,
            arguments = listOf(navArgument("studentId") { type = NavType.StringType })
        ) {
            GradeScreen(
                onBack = { navController.popBackStack() },
                studentId = it.arguments?.getString("studentId")
            )
        }

        composable(Routes.ABSENCES) {
            AbsenceScreen(onBack = { navController.popBackStack() })
        }

        composable(
            Routes.ABSENCE_FOR_STUDENT,
            arguments = listOf(navArgument("studentId") { type = NavType.StringType })
        ) {
            AbsenceScreen(
                onBack = { navController.popBackStack() },
                studentId = it.arguments?.getString("studentId")
            )
        }
    }
}
