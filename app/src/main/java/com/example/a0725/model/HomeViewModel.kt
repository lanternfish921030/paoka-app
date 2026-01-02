package com.example.a0725.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.a0725.repository.EnvRepository
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repo: EnvRepository = EnvRepository()
) : ViewModel() {

    // 避免重複呼叫
    private var bootstrapped = false

    fun bootstrapIfNeeded(city: String, district: String, lat: Double, lon: Double) {
        if (bootstrapped) return
        bootstrapped = true
        viewModelScope.launch {
            try {
                repo.saveLocation(city, district, lat, lon)
                repo.refreshEnvForMe()
            } catch (_: Exception) {
                // TODO: 發出 UI 訊息（Snackbar）或記錄 log
            }
        }
    }

    fun manualRefresh() = viewModelScope.launch {
        try { repo.refreshEnvForMe() } catch (_: Exception) {}
    }
}