package com.example.a0725.location

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

// 你之前的資料結構
data class TwAddress(
    val city: String = "未知縣市",
    val district: String = "未知區鄉鎮市",
    val village: String? = null
)

private fun parseTwCityDistrict(components: JSONArray): TwAddress {
    var admin1: String? = null
    var admin2: String? = null
    var admin3: String? = null
    var locality: String? = null
    var sublocality1: String? = null
    var sublocality2: String? = null

    for (i in 0 until components.length()) {
        val c = components.getJSONObject(i)
        val longName = c.optString("long_name")
        val types = c.optJSONArray("types") ?: continue
        val tset = (0 until types.length()).map { types.getString(it) }.toSet()

        when {
            "administrative_area_level_1" in tset -> admin1 = longName
            "administrative_area_level_2" in tset -> admin2 = longName
            "administrative_area_level_3" in tset -> admin3 = longName
            "locality" in tset -> locality = longName
            "sublocality_level_1" in tset || "sublocality" in tset -> sublocality1 = longName
            "sublocality_level_2" in tset -> sublocality2 = longName
        }
    }

    fun endsWithAny(s: String?, suf: List<String>) =
        s != null && suf.any { s.endsWith(it) }

    val city = listOf(admin1, admin2, locality)
        .firstOrNull { endsWithAny(it, listOf("市", "縣")) }
        ?: admin1 ?: admin2 ?: locality ?: "未知縣市"

    val districtCandidate = listOf(admin3, sublocality1, locality, sublocality2, admin2)
        .firstOrNull {
            it != null && it != city && endsWithAny(it, listOf("區", "鎮", "鄉", "市"))
        } ?: "未知區鄉鎮市"

    val village = listOf(sublocality2, sublocality1)
        .firstOrNull { it != null && (it.endsWith("里") || it.endsWith("村")) }

    return TwAddress(city = city, district = districtCandidate, village = village)
}

/** 用 Google Geocoding API 反查台灣地址 */
suspend fun getTwAddressFromLatLng(
    lat: Double,
    lon: Double,
    apiKey: String
): TwAddress = withContext(Dispatchers.IO) {
    try {
        val url =
            "https://maps.googleapis.com/maps/api/geocode/json?latlng=$lat,$lon&language=zh-TW&key=$apiKey"
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        val body = response.body?.string()
        val json = JSONObject(body ?: "")
        val results = json.optJSONArray("results") ?: JSONArray()

        if (results.length() > 0) {
            val components =
                results.getJSONObject(0).optJSONArray("address_components") ?: JSONArray()
            parseTwCityDistrict(components)
        } else {
            TwAddress()
        }
    } catch (e: Exception) {
        Log.e("定位", "反向地理解析失敗", e)
        TwAddress()
    }
}
