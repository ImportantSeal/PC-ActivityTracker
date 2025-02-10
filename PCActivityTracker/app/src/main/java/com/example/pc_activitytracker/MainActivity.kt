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
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


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
    var pcStatus by remember { mutableStateOf("Press Fetch to check status") }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(pcStatus)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            coroutineScope.launch {
                pcStatus = fetchPcStatus()  // Call network request inside coroutine
            }
        }) {
            Text("Fetch PC Status")
        }
    }
}


suspend fun fetchPcStatus(): String {
    return withContext(Dispatchers.IO) {  // moves network request to a background thread/ otherwise main crashes app
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("http://192.168.1.103:5000/status")
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


