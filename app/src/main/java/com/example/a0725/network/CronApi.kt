package com.example.a0725.network

import com.example.a0725.BuildConfig
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface CronApi {
    @GET("cron/weather")
    suspend fun weather(@Query("only") uid: String): Response<Unit>

    @GET("cron/aqi")
    suspend fun aqi(@Query("only") uid: String): Response<Unit>
}

object CronService {
    val api: CronApi by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.CRON_BASE_URL) // e.g. https://xxx-uc.a.run.app/
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CronApi::class.java)
    }
}
