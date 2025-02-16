package com.example.pc_activitytracker

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.SocketTimeoutException

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
    var pcStatus by remember { mutableStateOf("Press Fetch Status to update") }
    var sessionList by remember { mutableStateOf<List<String>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            pcIp = receivePcIp()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Detected PC IP: $pcIp")
        Spacer(modifier = Modifier.height(16.dp))
        Text(pcStatus)
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            coroutineScope.launch {
                pcStatus = fetchPcStatus(pcIp)
            }
        }) {
            Text("Fetch PC Status")
        }
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            coroutineScope.launch {
                sessionList = fetchSessionList(pcIp)
            }
        }) {
            Text("Fetch Sessions")
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
                    Text(session, modifier = Modifier.padding(12.dp))
                }
            }
        }
    }
}


suspend fun receivePcIp(): String = withContext(Dispatchers.IO) {
    try {
        val socket = DatagramSocket(5001)
        socket.broadcast = true
        socket.soTimeout = 10000  // Lisää pidempi timeout

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


suspend fun fetchPcStatus(ip: String): String = withContext(Dispatchers.IO) {
    if (ip.startsWith("No PC detected") || ip.isBlank()) return@withContext "Invalid IP address: $ip"
    val client = OkHttpClient()
    val request = Request.Builder().url("http://$ip:5000/status").build()
    try {
        val response = client.newCall(request).execute()
        if (response.isSuccessful) {
            val json = JSONObject(response.body?.string() ?: "{}")
            var status = "PC Status: ${json.getString("status")} at ${json.getString("timestamp")}"
            if (json.has("current_session")) {
                val cs = json.getJSONObject("current_session")
                status += "\nCurrently Active: ${cs.getString("process")} - ${cs.getString("window")} (since ${cs.getString("start_time")})"
            }
            status
        } else "HTTP Error: ${response.code}"
    } catch (e: IOException) {
        "Connection failed: ${e.message}"
    }
}

suspend fun fetchSessionList(ip: String): List<String> = withContext(Dispatchers.IO) {
    if (ip.startsWith("No PC detected") || ip.isBlank())
        return@withContext listOf("Invalid IP address: $ip")

    val client = OkHttpClient()
    val request = Request.Builder().url("http://$ip:5000/sessions").build()

    try {
        val response = client.newCall(request).execute()
        if (!response.isSuccessful)
            return@withContext listOf("HTTP Error: ${response.code}")

        val jsonStr = response.body?.string() ?: "[]"
        val jsonArray = JSONArray(jsonStr)

        if (jsonArray.length() == 0)
            return@withContext listOf("No usage sessions available")

        val sessionList = mutableListOf<String>()

        for (i in 0 until jsonArray.length()) {
            if (i >= jsonArray.length()) break

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
            val durationText = if (durationSec >= 60) {
                "${(durationSec / 60).toInt()} min"
            } else {
                "${durationSec.toInt()} sec"
            }

            sessionList.add("$appName ($windowName)\n⏳ $durationText | ⏰ $startTime - $endTime")
        }

        sessionList
    } catch (e: IOException) {
        listOf("Connection failed: ${e.message}")
    } catch (e: Exception) {
        listOf("Error parsing session data: ${e.message}")
    }
}

