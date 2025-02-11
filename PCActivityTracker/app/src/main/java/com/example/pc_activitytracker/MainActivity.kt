package com.example.pc_activitytracker
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import java.net.DatagramPacket
import java.net.DatagramSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
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
    var pcStatus by remember { mutableStateOf("Press Fetch to check status") }
    val coroutineScope = rememberCoroutineScope()

    // start listening for UDP broadcasts when the app launches
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            pcIp = receivePcIp()  // get PC IP automatically
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Detected PC IP: $pcIp")
        Spacer(modifier = Modifier.height(16.dp))
        Text(pcStatus)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            coroutineScope.launch {
                pcStatus = fetchPcStatus(pcIp)  // auto-detected IP
            }
        }) {
            Text("Fetch PC Status")
        }
    }
}

// UDP listener to get PC IP
suspend fun receivePcIp(): String {
    return withContext(Dispatchers.IO) {
        try {
            val socket = DatagramSocket(null)
            socket.reuseAddress = true
            socket.bind(InetSocketAddress(5001)) //ensure bound properly
            socket.soTimeout = 5000 // timeout after 5 seconds

            val buffer = ByteArray(1024)
            val packet = DatagramPacket(buffer, buffer.size)

            socket.receive(packet)  // wait for a broadcast message
            val receivedData = String(packet.data, 0, packet.length)

            if (receivedData.startsWith("PC_IP:")) {
                return@withContext receivedData.split(":")[1]  // extract IP
            }
            "No PC detected"
        }catch (e: SocketTimeoutException) {
            "No PC detected"
        } catch (e: Exception) {
            "Error receiving IP"
        }
    }
}

// auto detected IP fetching
suspend fun fetchPcStatus(ip: String): String {
    return withContext(Dispatchers.IO) {  // running network request in background thread

        if (ip.startsWith("No PC detected")|| ip.isBlank()){
            return@withContext "Invalid IP address: $ip"
        }

        val client = OkHttpClient()
        val request = Request.Builder()
            .url("http://$ip:5000/status") // use the detected IP
            .build()

        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val json = JSONObject(response.body?.string() ?: "{}")
                "PC Status: ${json.getString("status")} at ${json.getString("timestamp")}"
            } else {
                "HTTP Error: ${response.code}"
            }
        } catch (e: IOException) {
            "Connection failed: ${e.message}"
        }
    }
}