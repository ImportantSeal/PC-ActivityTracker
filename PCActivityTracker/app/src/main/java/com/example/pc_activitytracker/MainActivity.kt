package com.example.pc_activitytracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.*
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketTimeoutException
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64

data class Session(val text: String, val iconUrl: String)
data class ActiveSession(val onlineStatus: String, val details: String, val iconUrl: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainScreen()
        }
    }
}

@Composable
fun MainScreen() {
    var pcIp by remember { mutableStateOf("Searching for PC...") }
    var activeSession by remember { mutableStateOf(ActiveSession("Waiting...", "", "")) }
    var sessionList by remember { mutableStateOf<List<Session>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()
    val ipRegex = Regex("""\d+\.\d+\.\d+\.\d+""")

    // Haetaan PC:n IP kerran käynnistyksessä
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            val ip = receivePcIp()
            if (ipRegex.matches(ip)) {
                pcIp = ip
            }
        }
    }

    // Automaattinen statuspäivitys
    LaunchedEffect(pcIp) {
        if (ipRegex.matches(pcIp)) {
            while (true) {
                coroutineScope.launch {
                    activeSession = fetchPcStatus(pcIp)
                }
                delay(10000)
            }
        }
    }

    // Automaattinen sessiolistan päivitys
    LaunchedEffect(pcIp) {
        if (ipRegex.matches(pcIp)) {
            while (true) {
                coroutineScope.launch {
                    sessionList = fetchSessionList(pcIp)
                }
                delay(30000)
            }
        }
    }

    // UI
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Yläosa: PC online status ja IP-osoite
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = activeSession.onlineStatus,
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Detected PC IP: $pcIp",
                style = MaterialTheme.typography.bodySmall
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Current Session -osio
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val bitmap = decodeBase64Image(activeSession.iconUrl)
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
                    Text(text = activeSession.details.ifEmpty { "No active session" })
                }
                IconButton(onClick = {
                    coroutineScope.launch {
                        if (ipRegex.matches(pcIp)) {
                            activeSession = fetchPcStatus(pcIp)
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

        // Usage Log -osio
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
                    if (ipRegex.matches(pcIp)) {
                        sessionList = fetchSessionList(pcIp)
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
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(sessionList) { session ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
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

fun decodeBase64Image(base64String: String?): Bitmap? {
    if (base64String.isNullOrEmpty()) return null
    return try {
        val base64Data = base64String.substringAfter("base64,", "")
        val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    } catch (e: Exception) {
        null
    }
}

suspend fun receivePcIp(): String = withContext(Dispatchers.IO) {
    try {
        val socket = DatagramSocket(5001)
        socket.broadcast = true
        socket.soTimeout = 15000
        val buffer = ByteArray(1024)
        val packet = DatagramPacket(buffer, buffer.size)
        socket.receive(packet)
        val receivedData = String(packet.data, 0, packet.length)
        if (receivedData.startsWith("PC_IP:")) {
            return@withContext receivedData.split(":")[1]
        }
        "No PC detected"
    } catch (e: SocketTimeoutException) {
        "No PC detected (Timeout)"
    } catch (e: Exception) {
        "Error receiving IP: ${e.message}"
    }
}

suspend fun fetchPcStatus(ip: String): ActiveSession = withContext(Dispatchers.IO) {
    val ipRegex = Regex("""\d+\.\d+\.\d+\.\d+""")
    if (!ipRegex.matches(ip))
        return@withContext ActiveSession("Invalid IP address: $ip", "", "")
    val client = OkHttpClient()
    val request = Request.Builder().url("http://$ip:5000/status").build()
    try {
        val response = client.newCall(request).execute()
        if (response.isSuccessful) {
            val json = JSONObject(response.body?.string() ?: "{}")
            val serverStatus = json.getString("status")
            val onlineStatus = if (serverStatus.equals("Online", ignoreCase = true)
                || serverStatus.equals("PC is online", ignoreCase = true)
            ) {
                "PC is online"
            } else {
                "PC is offline"
            }
            var detailsText = ""
            var iconUrl = ""
            if (json.has("current_session")) {
                val cs = json.getJSONObject("current_session")
                val windowName = cs.optString("window", "Unknown Window")
                val startTime = cs.optString("start_time", "Unknown Start")
                    .split("T").getOrElse(1) { "??:??" }
                    .substring(0, 5)
                detailsText = "Currently Active: $windowName since $startTime"
                iconUrl = cs.optString("icon_url", "")
            }
            return@withContext ActiveSession(onlineStatus, detailsText, iconUrl)
        } else {
            return@withContext ActiveSession("HTTP Error: ${response.code}", "", "")
        }
    } catch (e: IOException) {
        return@withContext ActiveSession("Connection failed: ${e.message}", "", "")
    }
}

suspend fun fetchSessionList(ip: String): List<Session> = withContext(Dispatchers.IO) {
    val ipRegex = Regex("""\d+\.\d+\.\d+\.\d+""")
    if (!ipRegex.matches(ip))
        return@withContext listOf(Session("Invalid IP address: $ip", ""))
    val client = OkHttpClient()
    val request = Request.Builder().url("http://$ip:5000/sessions").build()
    try {
        val response = client.newCall(request).execute()
        if (!response.isSuccessful)
            return@withContext listOf(Session("HTTP Error: ${response.code}", ""))
        val jsonStr = response.body?.string() ?: "[]"
        val jsonArray = JSONArray(jsonStr)
        if (jsonArray.length() == 0)
            return@withContext listOf(Session("No usage sessions available", ""))
        val sessionList = mutableListOf<Session>()
        for (i in 0 until jsonArray.length()) {
            val session = jsonArray.getJSONObject(i)
            val durationSec = session.optDouble("duration_seconds", 0.0)
            // filtteröidään lyhyet sessiot
            if (durationSec < 10.0) continue

            val windowName = session.optString("window", "Unknown Window")
            val startTime = session.optString("start_time", "Unknown Start")
                .split("T").getOrElse(1) { "??:??" }
                .substring(0, 5)
            val endTime = if (session.has("end_time")) {
                session.getString("end_time")
                    .split("T").getOrElse(1) { "??:??" }
                    .substring(0, 5)
            } else {
                "Still Running"
            }
            val durationText = if (durationSec >= 60) "${(durationSec / 60).toInt()} min" else "${durationSec.toInt()} sec"
            val iconUrl = session.optString("icon_url", "")
            val text = "$windowName\n⏳ $durationText | ⏰ $startTime - $endTime"
            sessionList.add(Session(text, iconUrl))
        }
        sessionList.reversed()
    } catch (e: IOException) {
        listOf(Session("Connection failed: ${e.message}", ""))
    } catch (e: Exception) {
        listOf(Session("Error parsing session data: ${e.message}", ""))
    }
}
