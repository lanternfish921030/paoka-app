package com.example.a0725.health



import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
import java.time.ZoneId
import java.time.temporal.ChronoUnit




// 定義資料類型列舉
enum class HealthMetricType {
    STEPS, DISTANCE, HEART_RATE, RESTING_HEART_RATE, SPEED, VO2_MAX, HRV, NONE
}


data class HealthData(
    val steps: Long = 0,
    val distanceMeters: Double = 0.0,
    val heartRateBpm: Long? = null,
    val restingHeartRateBpm: Long? = null,
    val vo2Max: Double? = null,
    val hrvRmssd: Double? = null,
    val speed: Double? = null
)


// 用來回傳歷史數據的格式
data class DailyDataPoint(
    val date: LocalDate,
    val value: Double,
    val label: String
)


object HealthConnectUtils {


    private var client: HealthConnectClient? = null


    val PERMISSIONS = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(RestingHeartRateRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getReadPermission(Vo2MaxRecord::class),
        HealthPermission.getReadPermission(SpeedRecord::class),
        HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class)
    )


    fun createPermissionContract() = PermissionController.createRequestPermissionResultContract()


    suspend fun hasAllPermissions(context: Context): Boolean {
        val c = client ?: HealthConnectClient.getOrCreate(context).also { client = it }
        val granted = c.permissionController.getGrantedPermissions()
        return granted.containsAll(PERMISSIONS)
    }


    // ── 1. 讀取當日即時數據 ──
    suspend fun readCurrentHealthData(context: Context): HealthData {
        val c = client ?: HealthConnectClient.getOrCreate(context).also { client = it }
        val zoneId = ZoneId.systemDefault()
        val todayDate = LocalDate.now()


        // 讀取整天 (00:00 到 明日 00:00) 確保不漏資料
        val todayStart = todayDate.atStartOfDay(zoneId).toInstant()
        val todayEnd = todayDate.plusDays(1).atStartOfDay(zoneId).toInstant()
        val now = Instant.now()


        val aggregateRequest = AggregateRequest(
            metrics = setOf(StepsRecord.COUNT_TOTAL, DistanceRecord.DISTANCE_TOTAL),
            timeRangeFilter = TimeRangeFilter.between(todayStart, todayEnd)
        )
        val aggregateResponse = c.aggregate(aggregateRequest)
        val totalSteps = aggregateResponse[StepsRecord.COUNT_TOTAL] ?: 0L
        val totalDist = aggregateResponse[DistanceRecord.DISTANCE_TOTAL]?.inMeters ?: 0.0


        suspend fun <T : Record> readLatest(recordType: kotlin.reflect.KClass<T>): T? {
            val response = c.readRecords(
                ReadRecordsRequest(
                    recordType = recordType,
                    timeRangeFilter = TimeRangeFilter.between(now.minus(24, ChronoUnit.HOURS), now),
                    ascendingOrder = false,
                    pageSize = 1
                )
            )
            return response.records.firstOrNull()
        }


        return HealthData(
            steps = totalSteps,
            distanceMeters = totalDist,
            heartRateBpm = readLatest(HeartRateRecord::class)?.samples?.lastOrNull()?.beatsPerMinute,
            restingHeartRateBpm = readLatest(RestingHeartRateRecord::class)?.beatsPerMinute,
            vo2Max = readLatest(Vo2MaxRecord::class)?.vo2MillilitersPerMinuteKilogram,
            hrvRmssd = readLatest(HeartRateVariabilityRmssdRecord::class)?.heartRateVariabilityMillis,
            speed = readLatest(SpeedRecord::class)?.samples?.lastOrNull()?.speed?.inMetersPerSecond
        )
    }


    // ── 2. 通用歷史數據讀取 ──
    suspend fun readHistoricalData(context: Context, type: HealthMetricType): List<DailyDataPoint> {
        val c = client ?: HealthConnectClient.getOrCreate(context).also { client = it }


        val today = LocalDate.now()
        val endLocal = today.plusDays(1).atStartOfDay()
        val startLocal = endLocal.minusDays(7)


        val zoneId = ZoneId.systemDefault()
        // 請求時轉為 Instant (符合舊版 SDK 要求)
        val startInstant = startLocal.atZone(zoneId).toInstant()
        val endInstant = endLocal.atZone(zoneId).toInstant()


        val metricToFetch = when (type) {
            HealthMetricType.STEPS -> StepsRecord.COUNT_TOTAL
            HealthMetricType.DISTANCE -> DistanceRecord.DISTANCE_TOTAL
            HealthMetricType.HEART_RATE -> HeartRateRecord.BPM_AVG
            HealthMetricType.RESTING_HEART_RATE -> RestingHeartRateRecord.BPM_AVG
            HealthMetricType.SPEED -> SpeedRecord.SPEED_AVG
            else -> null
        }


        if (metricToFetch == null) return emptyList()


        try {
            val request = AggregateGroupByPeriodRequest(
                metrics = setOf(metricToFetch),
                timeRangeFilter = TimeRangeFilter.between(startInstant, endInstant),
                timeRangeSlicer = Period.ofDays(1)
            )
            val response = c.aggregateGroupByPeriod(request)


            val resultList = MutableList(7) { i ->
                val date = today.minusDays((6 - i).toLong())
                val dayName = date.dayOfWeek.getDisplayName(java.time.format.TextStyle.NARROW, java.util.Locale.TAIWAN)
                DailyDataPoint(date, 0.0, dayName)
            }


            response.forEach { periodResult ->
                // ★ [修正處]：periodResult.startTime 已經是 LocalDateTime 了，直接轉 LocalDate
                val resultDate = periodResult.startTime.toLocalDate()


                val daysAgo = ChronoUnit.DAYS.between(resultDate, today).toInt()
                val index = 6 - daysAgo


                if (index in 0..6) {
                    val value: Double = when (type) {
                        HealthMetricType.STEPS -> (periodResult.result[StepsRecord.COUNT_TOTAL] ?: 0L).toDouble()
                        HealthMetricType.DISTANCE -> (periodResult.result[DistanceRecord.DISTANCE_TOTAL]?.inMeters ?: 0.0) / 1000.0
                        HealthMetricType.HEART_RATE -> (periodResult.result[HeartRateRecord.BPM_AVG] ?: 0L).toDouble()
                        HealthMetricType.RESTING_HEART_RATE -> (periodResult.result[RestingHeartRateRecord.BPM_AVG] ?: 0L).toDouble()
                        HealthMetricType.SPEED -> (periodResult.result[SpeedRecord.SPEED_AVG]?.inMetersPerSecond ?: 0.0) * 3.6
                        else -> 0.0
                    }
                    resultList[index] = resultList[index].copy(value = value)
                }
            }
            return resultList


        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
    }
}



