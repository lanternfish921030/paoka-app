package com.example.a0725.model

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.a0725.repository.ScheduleFilters
import com.example.a0725.repository.ScheduleRow
import com.example.a0725.repository.ScoresRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class ScoreViewModel(
    private val repo: ScoresRepository = ScoresRepository(FirebaseFirestore.getInstance())
) : ViewModel() {

    var meets by mutableStateOf<List<Meet>>(emptyList()); private set
    var selectedMeet by mutableStateOf<Meet?>(null)

    // 賽程下拉選項（動態）
    var dates by mutableStateOf(listOf("所有日期")); private set
    var groups by mutableStateOf(listOf("所有組別")); private set
    var items  by mutableStateOf(listOf("所有項目")); private set

    // 已選的值
    var selectedDate  by mutableStateOf("所有日期")
    var selectedGroup by mutableStateOf("所有組別")
    var selectedItem  by mutableStateOf("所有項目")

    // 賽程表資料
    var schedule by mutableStateOf<List<ScheduleRow>>(emptyList()); private set

    var error by mutableStateOf<String?>(null);     private set
    var loading by mutableStateOf(false);           private set

    init {
        viewModelScope.launch {
            loading = true
            runCatching {
                meets = repo.getMeets()
                selectedMeet = meets.firstOrNull()
                refreshFiltersAndSchedule()
            }.onFailure { e ->
                error = e.message ?: "載入失敗"
                schedule = emptyList()
            }
            loading = false
        }
    }


    fun onMeetSelected(meetName: String) = viewModelScope.launch {
        selectedMeet = meets.find { it.name == meetName }
        selectedDate  = "所有日期"
        selectedGroup = "所有組別"
        selectedItem  = "所有項目"
        refreshFiltersAndSchedule()
    }

    fun refreshSchedule() = viewModelScope.launch {
        val m = selectedMeet ?: return@launch
        loading = true
        runCatching {
            val d = selectedDate.takeIf  { it != "所有日期" }
            val g = selectedGroup.takeIf { it != "所有組別" }
            val i = selectedItem.takeIf  { it != "所有項目" }
            schedule = repo.getSchedule(m.id, d, g, i)
            error = null
        }.onFailure { e ->
            schedule = emptyList()
            error = e.message ?: "讀取失敗"
        }
        loading = false
    }

    private fun refreshFiltersAndSchedule() = viewModelScope.launch {
        val m = selectedMeet ?: run { schedule = emptyList(); return@launch }
        val f: ScheduleFilters = repo.getScheduleFilters(m.id)
        dates  = f.dates
        groups = f.groups
        items  = f.items

        // 若目前選的值不在新選項內，回到預設第一個
        if (selectedDate  !in dates)  selectedDate  = dates.first()
        if (selectedGroup !in groups) selectedGroup = groups.first()
        if (selectedItem  !in items)  selectedItem  = items.first()

        refreshSchedule()
    }
}
