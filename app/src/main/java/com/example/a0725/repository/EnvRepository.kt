package com.example.a0725.repository

import com.example.a0725.auth.AuthRepository
import com.example.a0725.network.CronService
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class EnvRepository(
    private val fs: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val api = CronService.api

    suspend fun saveLocation(city: String, district: String, lat: Double, lon: Double) {
        val uid = AuthRepository.currentUser?.uid ?: return
        val loc = hashMapOf(
            "city" to city,
            "district" to district,
            "lat" to lat,
            "lon" to lon,
            "timestamp" to Timestamp.now()
        )
        fs.collection("users").document(uid)
            .collection("location").document("current")
            .set(loc).await()

        // 讓 /cron/weather 能直接使用 city/district
        fs.collection("users").document(uid)
            .set(mapOf("city" to city, "district" to district), SetOptions.merge()).await()
    }

    suspend fun refreshEnvForMe() {
        val uid = AuthRepository.currentUser?.uid ?: return
        // 簡單發兩個請求；失敗就忽略（UI 可顯示 Snackbar）
        runCatching { api.weather(uid) }
        runCatching { api.aqi(uid) }
    }
}
