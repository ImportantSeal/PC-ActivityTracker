package com.example.pc_activitytracker

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.example.pc_activitytracker.ui.AppNavigation
import com.example.pc_activitytracker.ui.theme.PcActivityTrackerTheme
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContent {
            var darkThemeEnabled by rememberSaveable { mutableStateOf(true) }

            PcActivityTrackerTheme(darkTheme = darkThemeEnabled) {
                AppNavigation(
                    darkThemeEnabled = darkThemeEnabled,
                    onDarkThemeToggle = { darkThemeEnabled = it }
                )
            }
        }
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }
            val token = task.result
            Log.d("FCM", "FCM Registration token: $token")
        }

    }

}

