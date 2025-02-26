package com.example.pc_activitytracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.example.pc_activitytracker.ui.AppNavigation
import com.example.pc_activitytracker.ui.theme.PcActivityTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var darkThemeEnabled by rememberSaveable { mutableStateOf(false) }

            PcActivityTrackerTheme(darkTheme = darkThemeEnabled) {
                AppNavigation(
                    darkThemeEnabled = darkThemeEnabled,
                    onDarkThemeToggle = { darkThemeEnabled = it }
                )
            }
        }
    }
}
