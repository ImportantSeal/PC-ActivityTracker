package com.example.pc_activitytracker.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import com.example.pc_activitytracker.models.ActiveSession
import com.example.pc_activitytracker.models.Session
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketTimeoutException

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
            // Filtteröidään sessiot, jotka kestävät alle 2 sekuntia
            if (durationSec < 2.0) continue

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
