package com.example.a0725.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    // TODO：把 BASE_URL 改成你部署成績爬蟲的 Cloud Run/Cloud Shell 入口（尾端要有 /）
    private const val BASE_URL = "https://<your-cloud-run-domain>/"

    private val client by lazy {
        val log = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .addInterceptor(log)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    val scoresApi: ScoresApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ScoresApi::class.java)
    }
}
