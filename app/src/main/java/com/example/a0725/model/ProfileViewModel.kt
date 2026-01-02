package com.example.a0725.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.a0725.data.UserProfile
import com.example.a0725.repository.ProfileRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProfileViewModel(
    private val repository: ProfileRepository = ProfileRepository()
) : ViewModel() {

    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile

    private val _photoUrl = MutableStateFlow<String?>(null)
    val photoUrl: StateFlow<String?> = _photoUrl

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        // 進入頁面時自動載入
        loadProfile()
    }

    fun loadProfile() {
        _loading.value = true
        viewModelScope.launch {
            try {
                val profile = repository.load()
                _userProfile.value = profile
                _photoUrl.value = profile.photoUrl
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _loading.value = false
            }
        }
    }

    /** UI 用：單純把 photoUrl 設到 VM，並同步到 Firestore */
    fun updatePhotoUrl(url: String?) {
        _photoUrl.value = url
    }

    fun savePhotoUrl(url: String?) = viewModelScope.launch {
        try {
            repository.setPhotoUrl(url)
        } catch (e: Exception) {
            _error.value = e.message
        }
    }

    /** UI 用：更新姓名（realName/displayName 會一起改） */
    fun updateName(name: String) = viewModelScope.launch {
        _loading.value = true
        try { repository.updateField("realName", name); _userProfile.value = _userProfile.value.copy(name = name) }
        catch (e: Exception) { _error.value = e.message }
        finally { _loading.value = false }
    }

    /** * ★ 一般更新 ID (不涉及清空，保留給初始化用)
     */
    fun updateCtaaId(ctaaId: String?) = viewModelScope.launch {
        _loading.value = true
        try {
            repository.updateField("ctaa_id", ctaaId?.ifBlank { null })
            _userProfile.value = _userProfile.value.copy(ctaaId = ctaaId?.ifBlank { null })
        }
        catch (e: Exception) { _error.value = e.message }
        finally { _loading.value = false }
    }

    /** * ★★★ 新增功能：更新 ID 並清空舊成績紀錄 ★★★
     * 當使用者確認更換 ID 時呼叫此函式
     */
    fun updateCtaaIdAndClearHistory(ctaaId: String?) = viewModelScope.launch {
        _loading.value = true
        val newId = ctaaId?.trim()?.ifBlank { null }
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid == null) {
            _error.value = "尚未登入"
            _loading.value = false
            return@launch
        }

        try {
            val db = FirebaseFirestore.getInstance()

            // 1. 更新使用者 ID
            repository.updateField("ctaa_id", newId)
            _userProfile.value = _userProfile.value.copy(ctaaId = newId)

            // 2. 清空舊的成績紀錄 (scores/{uid}/history)
            val historyRef = db.collection("scores").document(uid).collection("history")
            val snapshot = historyRef.get().await()

            if (!snapshot.isEmpty) {
                // Firestore Batch 寫入 (每次最多 500 筆，這裡簡單處理，若資料極多建議分批)
                val batch = db.batch()
                for (doc in snapshot.documents) {
                    batch.delete(doc.reference)
                }
                batch.commit().await()
            }

            // 3. 重置同步狀態 (last_fetch_ms = 0)，強制下次進入成績頁面時自動重爬
            // 同時更新 synced_ctaa_id 為新 ID，避免 RecordViewModel 又跳一次提示
            val metaUpdate = mapOf(
                "last_fetch_ms" to 0L,
                "synced_ctaa_id" to (newId ?: "")
            )
            db.collection("scores").document(uid).set(metaUpdate, SetOptions.merge()).await()

        } catch (e: Exception) {
            _error.value = "更新失敗: ${e.message}"
        } finally {
            _loading.value = false
        }
    }

    fun updateEmail(email: String) = viewModelScope.launch {
        _loading.value = true
        try { repository.updateField("email", email); _userProfile.value = _userProfile.value.copy(email = email) }
        catch (e: Exception) { _error.value = e.message }
        finally { _loading.value = false }
    }

    /** 整包寫回（較少用到） */
    fun updateProfile(profile: UserProfile) {
        _loading.value = true
        viewModelScope.launch {
            try {
                repository.upsert(profile)
                _userProfile.value = profile
                _photoUrl.value = profile.photoUrl
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _loading.value = false
            }
        }
    }
}