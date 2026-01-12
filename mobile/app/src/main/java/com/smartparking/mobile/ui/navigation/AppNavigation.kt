package com.smartparking.mobile.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.smartparking.mobile.ui.screens.admin.AdminScreen
import com.smartparking.mobile.ui.screens.dashboard.DashboardScreen
import com.smartparking.mobile.ui.screens.devices.DevicesScreen
import com.smartparking.mobile.ui.screens.entrylogs.EntryLogsScreen
import com.smartparking.mobile.ui.screens.login.LoginScreen
import com.smartparking.mobile.ui.screens.login.LoginViewModel
import com.smartparking.mobile.ui.screens.register.RegisterScreen

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Dashboard : Screen("dashboard")
    object EntryLogs : Screen("entry_logs")
    object Devices : Screen("devices")
    object Admin : Screen("admin")
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val loginViewModel: LoginViewModel = hiltViewModel()
    val isLoggedIn by loginViewModel.isLoggedIn.collectAsState()

    // Determine start destination based on login state
    val startDestination = if (isLoggedIn) Screen.Dashboard.route else Screen.Login.route

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                onLoginSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                onRegisterSuccess = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToEntryLogs = {
                    navController.navigate(Screen.EntryLogs.route)
                },
                onNavigateToDevices = {
                    navController.navigate(Screen.Devices.route)
                },
                onNavigateToAdmin = {
                    navController.navigate(Screen.Admin.route)
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.EntryLogs.route) {
            EntryLogsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Devices.route) {
            DevicesScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Admin.route) {
            AdminScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
