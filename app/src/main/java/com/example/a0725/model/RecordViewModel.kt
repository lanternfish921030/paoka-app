package com.example.a0725.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.a0725.auth.AuthRepository
import com.example.a0725.repository.RecordRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class PbItem(
    val id: String = "", // ★ 新增 ID 以便更新
    val event: String = "",
    val best_text: String = "",
    val best_sec: Double = Double.MAX_VALUE,
    val date: String = "",
    val meet: String = "",
    val note: String = "", // ★ 新增 心得
    val team: Boolean = false
)

data class ScoreItem(
    val id: String = "",
    val event: String = "",
    val performance: String = "",
    val perf_seconds: Double? = null,
    val date: String = "",
    val meet: String = "",
    val note: String = "", // ★ 新增 心得
    val team: Boolean = false
)

data class RecordUiState(
    val loading: Boolean = false,
    val message: String? = null,
    val avatarUrl: String? = null,
    val pbsIndividual: List<PbItem> = emptyList(),
    val pbsTeam: List<PbItem> = emptyList(),
    val historyIndividual: List<ScoreItem> = emptyList(),
    val historyTeam: List<ScoreItem> = emptyList(),
)

class RecordViewModel : ViewModel() {

    private val _state = MutableStateFlow(RecordUiState())
    val state = _state.asStateFlow()

    private val fs by lazy { FirebaseFirestore.getInstance() }

    /** 進頁面自動確保新鮮（抓協會最新成績，寫進 scores/{uid}/history） */
    fun ensureFreshOnEnter() = viewModelScope.launch {
        val user = AuthRepository.currentUser ?: return@launch
        val uid = user.uid

        val meta = fs.collection("scores").document(uid).get().await()
        val last = meta.getLong("last_fetch_ms") ?: 0L
        val tooOld = (System.currentTimeMillis() - last) > 24 * 60 * 60 * 1000L

        if (last == 0L || tooOld) {
            _state.value = _state.value.copy(loading = true)
            val r = RecordRepository.fetchCtaa(force = (last == 0L))
            _state.value = _state.value.copy(
                loading = false,
                message = r.fold(
                    onSuccess = { null },
                    onFailure = { "更新失敗：" + (it.message ?: it::class.simpleName ?: "未知錯誤") }
                )
            )
        }
    }

    /** 手動同步協會成績（右下角「同步」按鈕） */
    fun manualRefresh() = viewModelScope.launch {
        val user = AuthRepository.currentUser ?: run {
            _state.value = _state.value.copy(message = "尚未登入")
            return@launch
        }
        val u = fs.collection("users").document(user.uid).get().await()
        val realName = (u.getString("realName") ?: "").trim()
        val ctaaId   = (u.getString("ctaa_id") ?: "").trim()
        if (realName.isEmpty() || ctaaId.isEmpty()) {
            _state.value = _state.value.copy(message = "請先在個人資料填寫「真實姓名」與「協會系統編號」")
            return@launch
        }

        _state.value = _state.value.copy(loading = true)
        val r = RecordRepository.fetchCtaa(force = true)
        _state.value = _state.value.copy(
            loading = false,
            message = r.fold(
                onSuccess = { null },
                onFailure = { "更新失敗：" + (it.message ?: it::class.simpleName ?: "未知錯誤") }
            )
        )
    }

    /** ★ 更新賽後心得 */
    fun updateScoreNote(scoreId: String, note: String) = viewModelScope.launch {
        val user = AuthRepository.currentUser ?: return@launch
        try {
            fs.collection("scores")
                .document(user.uid)
                .collection("history")
                .document(scoreId)
                .update("note", note)
                .await()
            _state.value = _state.value.copy(message = "心得已儲存")
        } catch (e: Exception) {
            _state.value = _state.value.copy(message = "儲存失敗: ${e.message}")
        }
    }

    /** 只要呼叫一次，建立 Firestore 監聽 */
    fun attachListenersOnce() {
        val user = AuthRepository.currentUser ?: return
        val uid = user.uid

        // 頭像
        fs.collection("users").document(uid)
            .addSnapshotListener { d, _ ->
                val fromUsers = d?.getString("avatarUrl")?.takeIf { !it.isNullOrBlank() }
                val fromAuth = user.photoUrl?.toString()
                _state.value = _state.value.copy(avatarUrl = fromUsers ?: fromAuth)
            }

        // ⭐ 重點：只聽 history，並從 history 重新算出 PB
        fs.collection("scores").document(uid).collection("history")
            .orderBy("time_dt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                val all = snap?.documents?.map { d ->
                    ScoreItem(
                        id = d.id,
                        event = d.getString("event") ?: "",
                        performance = d.getString("performance") ?: "",
                        perf_seconds = d.getDouble("perf_seconds"),
                        date = d.getString("date") ?: "",
                        meet = d.getString("meet") ?: "",
                        note = d.getString("note") ?: "", // ★ 讀取心得
                        team = d.getBoolean("team") ?: false
                    )
                }.orEmpty()

                val indiv = all.filter { !it.team }
                val team  = all.filter { it.team }

                // 從 history 算出各項目 PB（個人）
                val pbsIndividual = indiv
                    .groupBy { it.event }
                    .mapNotNull { (event, list) ->
                        val best = list.minByOrNull { score ->
                            // 優先用 perf_seconds，沒有就現場再 parse 一次
                            score.perf_seconds ?: parseToSecondsForVm(score.performance) ?: Double.MAX_VALUE
                        } ?: return@mapNotNull null

                        val bestSec = best.perf_seconds
                            ?: parseToSecondsForVm(best.performance)
                            ?: Double.MAX_VALUE

                        PbItem(
                            id = best.id, // ★ 帶入 ID
                            event = event,
                            best_text = best.performance,
                            best_sec = bestSec,
                            date = best.date,
                            meet = best.meet,
                            note = best.note, // ★ 帶入心得
                            team = false
                        )
                    }
                    .sortedBy { it.event }

                // 從 history 算出各項目 PB（團體）
                val pbsTeam = team
                    .groupBy { it.event }
                    .mapNotNull { (event, list) ->
                        val best = list.minByOrNull { score ->
                            score.perf_seconds ?: parseToSecondsForVm(score.performance) ?: Double.MAX_VALUE
                        } ?: return@mapNotNull null

                        val bestSec = best.perf_seconds
                            ?: parseToSecondsForVm(best.performance)
                            ?: Double.MAX_VALUE

                        PbItem(
                            id = best.id, // ★ 帶入 ID
                            event = event,
                            best_text = best.performance,
                            best_sec = bestSec,
                            date = best.date,
                            meet = best.meet,
                            note = best.note, // ★ 帶入心得
                            team = true
                        )
                    }
                    .sortedBy { it.event }

                _state.value = _state.value.copy(
                    historyIndividual = indiv,
                    historyTeam = team,
                    pbsIndividual = pbsIndividual,
                    pbsTeam = pbsTeam
                )
            }
    }

    fun consumeMessage() {
        if (_state.value.message != null) {
            _state.value = _state.value.copy(message = null)
        }
    }

    // ---- 小工具：ViewModel 內用的秒數 parsing（跟畫面那個邏輯一樣） ----
    private fun parseToSecondsForVm(perf: String?): Double? {
        val s = perf?.trim()?.lowercase() ?: return null
        if (s.isBlank()) return null
        if (s in setOf("dq", "dns", "dnf", "—", "-")) return null

        val cleaned = s.removeSuffix("s").removeSuffix("sec").trim()
        return try {
            if (":" in cleaned) {
                val parts = cleaned.split(":")
                val mins = parts.dropLast(1).joinToString(":").toDoubleOrNull() ?: parts[0].toDouble()
                val secs = parts.last().toDouble()
                mins * 60 + secs
            } else {
                cleaned.toDouble()
            }
        } catch (_: Throwable) {
            null
        }
    }
}