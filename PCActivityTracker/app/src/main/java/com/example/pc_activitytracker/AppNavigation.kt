package com.example.pc_activitytracker.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun AppNavigation(
    darkThemeEnabled: Boolean,
    onDarkThemeToggle: (Boolean) -> Unit
) {
    val navController = rememberNavController()
    val minSessionDuration = remember { mutableStateOf(10f) }

    NavHost(navController = navController, startDestination = "main") {
        composable("main") {
            MainScreen(navController = navController)
        }
        composable("settings") {
            SettingsScreen(
                darkThemeEnabled = darkThemeEnabled,
                onDarkThemeToggle = onDarkThemeToggle,
                minSessionDuration = minSessionDuration.value,
                onMinSessionDurationChange = { minSessionDuration.value = it },
                onBack = { navController.navigateUp() }
            )
        }
    }
}
