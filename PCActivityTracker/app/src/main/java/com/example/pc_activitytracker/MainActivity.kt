package com.example.pc_activitytracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.pc_activitytracker.ui.AppNavigation
import com.example.pc_activitytracker.ui.theme.PcActivityTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {

            PcActivityTrackerTheme(darkTheme = false) {
                AppNavigation()
            }
        }
    }
}
