package com.example.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.viewmodel.AttendanceViewModel

@Composable
fun MainNavigation(
    viewModel: AttendanceViewModel = viewModel()
) {
    val navController = rememberNavController()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()

    // Determine starting screen based on login state
    val startDestination = if (isLoggedIn) "form" else "login"

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // 1. Login Screen
        composable("login") {
            LoginScreen(
                onLoginSuccess = { isAdmin ->
                    navController.navigate("form") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onLoginClick = { username, password ->
                    viewModel.prosesLogin(username, password)
                }
            )
        }

        // 2. Form Input Screen
        composable("form") {
            FormScreen(
                viewModel = viewModel,
                onLogout = {
                    viewModel.logout()
                    navController.navigate("login") {
                        popUpTo("form") { inclusive = true }
                    }
                },
                onNavigateToHistory = {
                    navController.navigate("history")
                }
            )
        }

        // 3. History Screen
        composable("history") {
            HistoryScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.navigateUp()
                }
            )
        }
    }
}
