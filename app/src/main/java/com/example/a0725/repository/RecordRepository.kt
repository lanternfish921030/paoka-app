// ScoresRepository.kt
package com.example.a0725.repository

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit
import org.json.JSONObject
import okhttp3.logging.HttpLoggingInterceptor
object RecordRepository {
    private const val BASE =
        "https://ctaa-svc-134800518696.asia-east1.run.app/scores/ctaa/fetch"

    private val client by lazy {
        val log = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
        OkHttpClient.Builder()
            .addInterceptor(log)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    suspend fun fetchCtaa(force: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val user = FirebaseAuth.getInstance().currentUser
                ?: throw IllegalStateException("尚未登入")
            val token = user.getIdToken(true).await().token
                ?: throw IllegalStateException("取得 ID Token 失敗")

            val body = """{"force": $force}""".toRequestBody("application/json".toMediaType())
            val req = Request.Builder()
                .url(BASE)
                .post(body)
                .addHeader("Authorization", "Bearer $token")
                .build()

            client.newCall(req).execute().use { resp ->
                val raw = resp.body?.string()
                if (!resp.isSuccessful) {
                    val msg = try { JSONObject(raw ?: "").optString("error") } catch (_: Exception) { null }
                    val finalMsg = when {
                        !msg.isNullOrBlank() -> msg
                        !raw.isNullOrBlank() -> raw.take(200)
                        else -> "HTTP ${resp.code} ${resp.message}"
                    }
                    throw IOException(finalMsg)
                }
            }
            // 成功就回 Result.success(Unit)
        }
    }
}
