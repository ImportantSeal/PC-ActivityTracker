package com.example.pc_activitytracker.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.example.pc_activitytracker.models.ActiveSession
import com.example.pc_activitytracker.models.Session
import com.example.pc_activitytracker.network.fetchPcStatus
import com.example.pc_activitytracker.network.fetchSessionList
import com.example.pc_activitytracker.network.receivePcIp
import com.example.pc_activitytracker.utils.decodeBase64Image
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MainScreen() {
    var pcIp = remember { mutableStateOf("Searching for PC...") }
    var activeSession = remember { mutableStateOf(ActiveSession("Waiting...", "", "")) }
    var sessionList = remember { mutableStateOf<List<Session>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()
    val ipRegex = Regex("""\d+\.\d+\.\d+\.\d+""")

    // Haetaan PC:n IP kerran
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            val ip = receivePcIp()
            if (ipRegex.matches(ip)) {
                pcIp.value = ip
            }
        }
    }

    // Automaattinen statuspäivitys
    LaunchedEffect(pcIp.value) {
        if (ipRegex.matches(pcIp.value)) {
            while (true) {
                coroutineScope.launch {
                    activeSession.value = fetchPcStatus(pcIp.value)
                }
                delay(10000)
            }
        }
    }

    // Automaattinen sessiolistan päivitys
    LaunchedEffect(pcIp.value) {
        if (ipRegex.matches(pcIp.value)) {
            while (true) {
                coroutineScope.launch {
                    sessionList.value = fetchSessionList(pcIp.value)
                }
                delay(30000)
            }
        }
    }

    // UI
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Yläosa: Online status ja IP
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = activeSession.value.onlineStatus,
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Detected PC IP: ${pcIp.value}",
                style = MaterialTheme.typography.bodySmall
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Current Session
        Text(
            text = "Current Session",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val bitmap = decodeBase64Image(activeSession.value.iconUrl)
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "App Icon",
                            modifier = Modifier.size(40.dp)
                        )
                    } else {
                        Text("❌")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = activeSession.value.details.ifEmpty { "No active session" })
                }
                IconButton(onClick = {
                    coroutineScope.launch {
                        if (ipRegex.matches(pcIp.value)) {
                            activeSession.value = fetchPcStatus(pcIp.value)
                        }
                    }
                }) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Refresh Status"
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Usage Log
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Usage Log",
                style = MaterialTheme.typography.titleMedium
            )
            IconButton(onClick = {
                coroutineScope.launch {
                    if (ipRegex.matches(pcIp.value)) {
                        sessionList.value = fetchSessionList(pcIp.value)
                    }
                }
            }) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "Refresh Sessions"
                )
            }
        }
        Divider()
        Spacer(modifier = Modifier.height(8.dp))

        // Sessioiden lista
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            items(sessionList.value) { session ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val bitmap = decodeBase64Image(session.iconUrl)
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "App Icon",
                                modifier = Modifier.size(40.dp)
                            )
                        } else {
                            Text("❌")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(session.text)
                    }
                }
            }
        }
    }
}
