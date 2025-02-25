package com.example.pc_activitytracker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    darkThemeEnabled: Boolean,
    onDarkThemeToggle: (Boolean) -> Unit,
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
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Tumma teema")
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = darkThemeEnabled,
                onCheckedChange = onDarkThemeToggle
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Näytä sessiot, joiden kesto on vähintään: ${minSessionDuration.toInt()} sekuntia",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Slider(
            value = minSessionDuration,
            onValueChange = onMinSessionDurationChange,
            valueRange = 0f..60f,
            steps = 5,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
