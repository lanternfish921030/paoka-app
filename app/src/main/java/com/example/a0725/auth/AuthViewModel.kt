package com.example.a0725.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuthException // 8/22
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

class AuthViewModel : ViewModel() {
    private val _loginState = MutableStateFlow(AuthUiState())
    val loginState = _loginState.asStateFlow()

    private val _registerState = MutableStateFlow(AuthUiState())
    val registerState = _registerState.asStateFlow()

    // 8/22
    private val _changePwState = MutableStateFlow(AuthUiState())
    val changePwState = _changePwState.asStateFlow()

    // 8/23
    private val _resetPwState = MutableStateFlow(AuthUiState())
    val resetPwState = _resetPwState.asStateFlow()

    fun login(email: String, password: String) {
        _loginState.value = AuthUiState(isLoading = true)
        viewModelScope.launch {
            try {
                AuthRepository.signIn(email, password)
                if (AuthRepository.isEmailVerified()) {
                    _loginState.value = AuthUiState(success = true)
                } else {
                    // 尚未驗證：先寄一封，再登出，提醒去收信
                    try { AuthRepository.resendVerificationEmail() } catch (_: Exception) {}
                    AuthRepository.signOut()
                    _loginState.value = AuthUiState(
                        error = "你的帳號尚未完成信箱驗證。已寄出驗證信到：$email\n請到信箱點擊驗證連結後再登入。"
                    )
                }
            } catch (e: Exception) {
                _loginState.value = AuthUiState(error = mapAuthError(e))
            }
        }
    }

    // ★ 新增 ctaaId: String? 8/17
    fun register(username: String, account: String, email: String, password: String, ctaaId: String?) {
        _registerState.value = AuthUiState(isLoading = true)
        viewModelScope.launch {
            try {
                AuthRepository.register(username, account, email, password, ctaaId)
                _registerState.value = AuthUiState(success = true)
            } catch (e: Exception) {
                _registerState.value = AuthUiState(error = mapRegisterError(e))
            }
        }
    }

    // 讓 UI 「重新寄送驗證信」用（需先登入該帳號；或你在 login() 裡已經寄過）
    fun resendVerification() {
        _loginState.value = AuthUiState(isLoading = true)
        viewModelScope.launch {
            try {
                AuthRepository.resendVerificationEmail()
                _loginState.value = AuthUiState(error = "已重新寄送驗證信，請查收。")
            } catch (e: Exception) {
                _loginState.value = AuthUiState(error = "寄送驗證信失敗：${e.message ?: "請稍後再試"}")
            }
        }
    }

    // 驗證完回到 App，按「我已驗證」可再嘗試登入
    fun refreshVerificationAndLogin(email: String, password: String) {
        login(email, password)
    }

    // 8/22
    fun changePassword(current: String, new: String) {
        _changePwState.value = AuthUiState(isLoading = true)
        viewModelScope.launch {
            try {
                AuthRepository.changePassword(current, new)
                _changePwState.value = AuthUiState(success = true)
            } catch (e: Exception) {
                _changePwState.value = AuthUiState(error = mapChangePasswordError(e))
            }
        }
    }

    fun resetChangePwState() {
        _changePwState.value = AuthUiState()
    }

    private fun mapChangePasswordError(e: Exception): String {
        val code = (e as? FirebaseAuthException)?.errorCode ?: ""
        return when {
            code.contains("ERROR_WRONG_PASSWORD", true) -> "原有密碼不正確。"
            code.contains("ERROR_WEAK_PASSWORD", true) -> "新密碼強度不足（至少 6 碼）。"
            code.contains("ERROR_REQUIRES_RECENT_LOGIN", true) -> "此操作需要近期登入，請先重新登入後再試。"
            else -> e.message ?: "變更密碼失敗，請稍後再試。"
        }
    }

    // 8/23
    fun resetPassword(email: String) {
        _resetPwState.value = AuthUiState(isLoading = true)
        viewModelScope.launch {
            try {
                AuthRepository.sendPasswordReset(email)
                _resetPwState.value = AuthUiState(success = true)
            } catch (e: Exception) {
                _resetPwState.value = AuthUiState(error = mapAuthError(e))
            }
        }
    }

    fun clearResetPwState() {
        _resetPwState.value = AuthUiState()
    }
}
