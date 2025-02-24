package com.example.pc_activitytracker.models

data class Session(val text: String, val iconUrl: String)
data class ActiveSession(val onlineStatus: String, val details: String, val iconUrl: String)
