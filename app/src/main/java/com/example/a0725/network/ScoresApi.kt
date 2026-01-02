package com.example.a0725.network

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface ScoresApi {
    @GET("scores/ctaa/fetch")
    suspend fun fetchCtaa(
        @Query("uid") uid: String,
        @Query("name") name: String,
        @Query("id") systemId: String
    ): Response<FetchResp>
}

data class FetchResp(
    val status: String,
    val uid: String,
    val system_id: String,
    val rows_total: Int,
    val rows_track: Int,
    val pb_events: Int
)
