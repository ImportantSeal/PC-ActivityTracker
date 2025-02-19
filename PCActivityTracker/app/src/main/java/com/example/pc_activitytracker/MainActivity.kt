package com.example.pc_activitytracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.*
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.SocketTimeoutException
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.ui.graphics.asImageBitmap

data class Session(val text: String, val iconUrl: String)
data class ActiveSession(val text: String, val iconUrl: String)

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
    var activeSession by remember { mutableStateOf(ActiveSession("Waiting for PC...", "")) }
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

    // Automaattinen statuspäivitys vain, jos pcIp on kelvollinen
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

    // Automaattinen sessiolistan päivitys vain, jos pcIp on kelvollinen
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

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Detected PC IP: $pcIp")
        Spacer(modifier = Modifier.height(16.dp))

        // Näytetään aktiivinen sessio kuvakkeella
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
            Text(activeSession.text)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            coroutineScope.launch {
                if (ipRegex.matches(pcIp)) {
                    activeSession = fetchPcStatus(pcIp)
                }
            }
        }) {
            Text("Refresh Status")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            coroutineScope.launch {
                if (ipRegex.matches(pcIp)) {
                    sessionList = fetchSessionList(pcIp)
                }
            }
        }) {
            Text("Refresh Sessions")
        }

        Spacer(modifier = Modifier.height(16.dp))
        Divider()

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(sessionList) { session ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
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
        return@withContext ActiveSession("Invalid IP address: $ip", "")
    val client = OkHttpClient()
    val request = Request.Builder().url("http://$ip:5000/status").build()
    try {
        val response = client.newCall(request).execute()
        if (response.isSuccessful) {
            val json = JSONObject(response.body?.string() ?: "{}")
            var statusText = "PC Status: ${json.getString("status")}"
            var iconUrl = ""
            if (json.has("current_session")) {
                val cs = json.getJSONObject("current_session")
                val appName = cs.optString("process", "Unknown App")
                val windowName = cs.optString("window", "Unknown Window")
                val startTime = cs.optString("start_time", "Unknown Start").split(" ").getOrElse(1) { "??:??" }.substring(0, 5)
                iconUrl = cs.optString("icon_url", "")
                statusText += "\nCurrently Active: $appName - $windowName (since $startTime)"
            }
            return@withContext ActiveSession(statusText, iconUrl)
        } else {
            return@withContext ActiveSession("HTTP Error: ${response.code}", "")
        }
    } catch (e: IOException) {
        return@withContext ActiveSession("Connection failed: ${e.message}", "")
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
            val appName = session.optString("process", "Unknown App")
            val windowName = session.optString("window", "Unknown Window")
            val startTime = session.optString("start_time", "Unknown Start").split(" ").getOrElse(1) { "??:??" }.substring(0, 5)
            val endTime = if (session.has("end_time")) {
                session.getString("end_time").split(" ").getOrElse(1) { "??:??" }.substring(0, 5)
            } else {
                "Still Running"
            }
            val durationSec = session.optDouble("duration_seconds", 0.0)
            val durationText = if (durationSec >= 60) "${(durationSec / 60).toInt()} min" else "${durationSec.toInt()} sec"
            val iconUrl = session.optString("icon_url", "")
            val text = "$appName ($windowName)\n⏳ $durationText | ⏰ $startTime - $endTime"
            sessionList.add(Session(text, iconUrl))
        }
        sessionList.reversed()
    } catch (e: IOException) {
        listOf(Session("Connection failed: ${e.message}", ""))
    } catch (e: Exception) {
        listOf(Session("Error parsing session data: ${e.message}", ""))
    }
}
