package com.example.pc_activitytracker.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64

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
