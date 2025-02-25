package com.example.pc_activitytracker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    minSessionDuration: Float,
    onMinSessionDurationChange: (Float) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Button(onClick = onBack) {
            Text("Takaisin")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Asetukset",
            style = androidx.compose.material3.MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Näytä sessiot, joiden kesto on vähintään: ${minSessionDuration.toInt()} sekuntia")
        Spacer(modifier = Modifier.height(8.dp))
        Slider(
            value = minSessionDuration,
            onValueChange = onMinSessionDurationChange,
            valueRange = 0f..60f,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
