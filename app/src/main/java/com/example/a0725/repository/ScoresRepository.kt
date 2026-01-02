package com.example.a0725.repository

import android.util.Log
import com.example.a0725.model.Meet
import com.example.a0725.model.ResultRow
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await

// ====== 賽程用資料結構 ======
data class ScheduleFilters(
    val dates: List<String>,
    val groups: List<String>,
    val items: List<String>
)

data class ScheduleRow(
    val seq: Int = 0,          // 場次（由 heat 或排序索引推得）
    val time: String = "",     // 時間（scores.time / 時間）
    val athlete: String = "",  // 新增的選手欄位
    val group: String = "",    // 組別（scores.round / 組別）
    val eventName: String = "",// 項目（scores.event / 項目）
    val live: String? = null,  // 即時（目前沒有 -> "-"）
    val race: String? = null,  // 賽別（目前沒有 -> "-"）
    val people: Int? = null,   // 人數（同一群組的成績筆數）
    val quota: String? = null, // 取數（目前沒有 -> "-"）
    val date: String? = null   // 日期
)


// ====== 成績頁舊有結構（保留） ======
data class FilterOptions(
    val genders: List<String>,
    val groups: List<String>,
    val events: List<String>
)

class ScoresRepository(private val db: FirebaseFirestore) {

    // 後端已建立 meets/{meetId}，這裡只要直接讀
    suspend fun getMeets(): List<Meet> {
        val qs = db.collection("meets")
            .orderBy("lastCrawlAt", Query.Direction.DESCENDING)
            .limit(50)
            .get(Source.SERVER) // ★ 固定走伺服器
            .await()

        return qs.documents.map { d ->
            Meet(
                id = d.id,
                name = d.getString("name") ?: d.id,
                sourceAlias = d.getString("sourceAlias"),
                sourceUrl = d.getString("sourceUrl")
            )
        }
    }

    // ============== ① 下拉選項（從 scores 擷取） ==============
    suspend fun getScheduleFilters(meetId: String): ScheduleFilters = try {
        val snap = db.collection("meets").document(meetId)
            .collection("scores")
            .get(Source.SERVER) // ★ 固定走伺服器，避免快取回空
            .await()
        Log.d("ScoresRepo", "filters meet=$meetId docs=${snap.size()}")

        val dates = linkedSetOf<String>()
        val groups = linkedSetOf<String>()
        val items  = linkedSetOf<String>()

        for (d in snap.documents) {
            d.str("date", "日期")?.takeIf { it.isNotBlank() }?.let { dates += it }
            d.str("round", "組別")?.takeIf { it.isNotBlank() }?.let { groups += it }
            d.str("event", "項目")?.takeIf { it.isNotBlank() }?.let { items  += it }
        }

        ScheduleFilters(
            dates = listOf("所有日期") + dates.toList(),
            groups = listOf("所有組別") + groups.toList(),
            items  = listOf("所有項目") + items.toList()
        )
    } catch (e: Exception) {
        Log.e("ScoresRepo", "getScheduleFilters error", e)
        ScheduleFilters(
            dates = listOf("所有日期"),
            groups = listOf("所有組別"),
            items  = listOf("所有項目")
        )
    }

    // ============== ② 賽程列表（從 scores 客端彙整） ==============
    suspend fun getSchedule(
        meetId: String,
        date: String?,
        group: String?,
        item: String?
    ): List<ScheduleRow> = try {
        val snap = db.collection("meets").document(meetId)
            .collection("scores")
            .get(Source.SERVER) // ★ 固定走伺服器
            .await()
        val docs = snap.documents
        Log.d("ScoresRepo", "schedule meet=$meetId docs=${docs.size}")

        // 客端過濾（支援中英鍵）
        val filtered = docs.filter { d ->
            val okDate  = date.isNullOrBlank()  || date  == "所有日期" || d.str("date", "日期") == date
            val okGroup = group.isNullOrBlank() || group == "所有組別" || d.str("round", "組別") == group
            val okItem  = item.isNullOrBlank()  || item  == "所有項目"   || d.str("event", "項目") == item
            okDate && okGroup && okItem
        }

        // 依「日期 / 時間 / 組別 / 項目 / 場次」彙整
        data class Key(val date: String?, val time: String?, val group: String?, val item: String?, val heat: String?)
        val grouped = filtered.groupBy { d ->
            Key(
                date  = d.str("date", "日期"),
                time  = d.str("time", "時間"),
                group = d.str("round", "組別"),
                item  = d.str("event", "項目"),
                heat  = d.str("heat", "場次")
            )
        }
        // 依群組產列
        val rows = grouped.entries.mapIndexed { idx, (k, list) ->

            // 從整個 list 找到第一個有值的欄位，不只看第一筆
            fun pickStr(vararg keys: String): String? =
                list.firstNotNullOfOrNull { it.str(*keys) }

            val timeText  = k.time ?: pickStr("time", "時間")
            val athlete   = pickStr("athlete", "name", "選手")      // ← Firestore 實際欄位叫 athlete
            val raceRaw   = pickStr("race", "category", "賽別")
            val quotaText = pickStr("quota", "qualifiers", "取數")

            // 賽別：把 "(依成績序)" 當作沒意義的字串丟掉
            val raceText = raceRaw
                ?.takeIf { it.isNotBlank() && it != "(依成績序)" }
                ?: "-"

            ScheduleRow(
                seq       = parseSeqFromHeat(k.heat) ?: (idx + 1),
                time      = timeText ?: "-",
                athlete   = athlete ?: "-",
                group     = k.group.orEmpty(),
                eventName = k.item.orEmpty(),
                live      = pickStr("liveText", "live", "即時"),
                race      = raceText,
                people    = list.size,                  // 這個 OK，本來就算 group 裡的筆數
                quota     = quotaText ?: "-",           // 會拿到「前8」「前16」這種
                date      = k.date
            )
        }





        rows.sortedWith(
            compareBy<ScheduleRow> { it.date ?: "9999-99-99" }
                .thenBy { timeKey(it.time) }
                .thenBy { it.eventName }
                .thenBy { it.group }
                .thenBy { it.seq }
        )
    } catch (e: Exception) {
        Log.e("ScoresRepo", "getSchedule error", e)
        emptyList()
    }

    // ===== 以下是成績（保留舊頁面用；不影響本次功能） =====

    private suspend fun getFirstEventId(meetId: String): String? {
        val qs = db.collection("meets").document(meetId)
            .collection("events")
            .limit(1)
            .get(Source.SERVER)
            .await()
        return qs.documents.firstOrNull()?.id
    }

    suspend fun getFilterOptions(meetId: String): FilterOptions {
        val eventId = getFirstEventId(meetId)
            ?: return FilterOptions(listOf("所有性別"), listOf("所有組別"), listOf("所有項目"))

        val qs = db.collection("meets").document(meetId)
            .collection("events").document(eventId)
            .collection("results")
            .limit(2000)
            .get(Source.SERVER)
            .await()

        val g  = linkedSetOf<String>()
        val gr = linkedSetOf<String>()
        val ev = linkedSetOf<String>()
        for (d in qs.documents) {
            d.getString("gender")?.takeIf { it.isNotBlank() }?.let { g += it }
            d.getString("group") ?.takeIf { it.isNotBlank() }?.let { gr += it }
            d.getString("eventName")?.takeIf { it.isNotBlank() }?.let { ev += it }
        }
        return FilterOptions(
            genders = listOf("所有性別") + g.toList(),
            groups  = listOf("所有組別") + gr.toList(),
            events  = listOf("所有項目") + ev.toList()
        )
    }

    suspend fun getResults(
        meetId: String,
        gender: String?, group: String?, eventName: String?
    ): List<ResultRow> {
        val eventId = getFirstEventId(meetId) ?: return emptyList()

        var q: Query = db.collection("meets").document(meetId)
            .collection("events").document(eventId)
            .collection("results")

        if (!gender.isNullOrBlank())    q = q.whereEqualTo("gender", gender)
        if (!group.isNullOrBlank())     q = q.whereEqualTo("group", group)
        if (!eventName.isNullOrBlank()) q = q.whereEqualTo("eventName", eventName)

        q = q.orderBy("rank", Query.Direction.ASCENDING)

        val qs = q.get(Source.SERVER).await()
        return qs.documents.map { d ->
            ResultRow(
                id = d.id,
                rank = d.getLong("rank")?.toInt(),
                lane = d.getLong("lane")?.toInt(),
                name = d.getString("name") ?: "",
                team = d.getString("team") ?: "",
                result = d.getString("result"),
                resultNorm = d.getDouble("resultNorm"),
                wind = d.getDouble("wind") ?: d.getString("wind")?.toDoubleOrNull(),
                status = d.getString("status"),
                eventName = d.getString("eventName"),
                gender = d.getString("gender"),
                group = d.getString("group"),
                round = d.getString("round"),
                heat = d.getLong("heat")?.toInt()
            )
        }
    }
}

/* ---------- 小工具：欄位容錯/排序 ---------- */

private fun DocumentSnapshot.str(vararg keys: String): String? {
    // 安全版：任何不是 String 的型別都先轉成字串，Map/List 直接忽略
    for (k in keys) {
        val v = this.get(k)
        val s = when (v) {
            null -> null
            is String -> v
            is Number, is Boolean -> v.toString()
            else -> null // Map / List / 其它型別 → 略過，避免 getString() 丟例外
        }
        if (!s.isNullOrBlank()) return s
    }
    return null
}

private fun DocumentSnapshot.int(vararg keys: String): Int? {
    // 需要整數時用這個，既支援 Number 也支援 "123" 字串
    for (k in keys) {
        val v = this.get(k)
        when (v) {
            is Number -> return v.toInt()
            is String -> v.toIntOrNull()?.let { return it }
        }
    }
    return null
}

// "104-01"、"507-02" → 取數字；抓不到回 null
private fun parseSeqFromHeat(heat: String?): Int? {
    if (heat.isNullOrBlank()) return null
    val m = Regex("""\d+""").find(heat) ?: return null
    return m.value.toIntOrNull()
}

// "09:30" → 570；抓不到就放到最後
private fun timeKey(t: String?): Int {
    if (t.isNullOrBlank()) return Int.MAX_VALUE
    val m = Regex("""(\d{1,2}):(\d{2})""").find(t) ?: return Int.MAX_VALUE
    val (hh, mm) = m.destructured
    return (hh.toIntOrNull() ?: 99) * 60 + (mm.toIntOrNull() ?: 0)
}


