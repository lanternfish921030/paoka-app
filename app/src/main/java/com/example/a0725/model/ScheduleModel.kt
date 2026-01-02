package com.example.a0725.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.a0725.repository.ProfileRepository
import com.example.a0725.data.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


// Firestore 友善：全部都是可序列化的基本型別
data class ProjectDetail(
    var project: String = "",
    var group: String = "",
    var category: String = "",
    var checkInTime: String = "",
    var lane: String = "",
    var matchTime: String = ""
)

data class DayDetail(
    var date: String = "",
    var note: String = "",
    var startTime: String = "",                 // Day1 起始時間
    var projects: List<ProjectDetail> = emptyList()
)

data class ScheduleEvent(
    val id: String = "",
    val year: String = "",
    val name: String = "",
    val dateRange: String = "",
    val colorHex: String = "#8B95FF",
    val days: List<DayDetail> = emptyList()
)

data class ProfileUiState(
    val loading: Boolean = true,
    val profile: UserProfile = UserProfile(),
    val error: String? = null
)

class ScheduleViewModel(
    private val repo: ProfileRepository = ProfileRepository()
) : ViewModel() {

    private val _ui = MutableStateFlow(ProfileUiState())
    val ui: StateFlow<ProfileUiState> = _ui

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _ui.value = _ui.value.copy(loading = true, error = null)
        try {
            val p = repo.load()
            _ui.value = ProfileUiState(loading = false, profile = p)
        } catch (e: Exception) {
            _ui.value = _ui.value.copy(loading = false, error = e.message)
        }
    }

    fun updateName(name: String) = updateBothNames(name)
    fun updatePhone(phone: String?) = update("phone", phone?.ifBlank { null }) {
        it.copy(phone = phone?.ifBlank { null })
    }
    fun updateEmail(email: String) = update("email", email) { it.copy(email = email) }
    fun updateCtaaId(raw: String) {
        val digits = raw.filter { it.isDigit() }
        update("ctaa_id", digits.ifBlank { null }) { it.copy(ctaaId = digits.ifBlank { null }) }
    }

    // Firestore 你的結構同時有 realName 與 displayName，一起更新
    private fun updateBothNames(name: String) = viewModelScope.launch {
        try {
            repo.updateField("realName", name)
            repo.updateField("displayName", name)
            _ui.value = _ui.value.copy(profile = _ui.value.profile.copy(name = name), error = null)
        } catch (e: Exception) {
            _ui.value = _ui.value.copy(error = e.message)
        }
    }

    private fun update(field: String, value: Any?, reduce: (UserProfile) -> UserProfile) =
        viewModelScope.launch {
            try {
                repo.updateField(field, value)
                _ui.value = _ui.value.copy(profile = reduce(_ui.value.profile), error = null)
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(error = e.message)
            }
        }
}