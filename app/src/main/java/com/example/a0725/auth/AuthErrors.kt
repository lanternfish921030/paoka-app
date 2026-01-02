package com.example.a0725.auth

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.*

fun mapAuthError(e: Exception): String = when (e) {
    is FirebaseAuthInvalidUserException -> when (e.errorCode) {
        "ERROR_USER_NOT_FOUND" -> "該帳號不存在，請確認信箱是否註冊"
        "ERROR_USER_DISABLED" -> "此帳號已被停用，請聯絡客服"
        else -> "無法登入：找不到使用者"
    }
    is FirebaseAuthInvalidCredentialsException -> when (e.errorCode) {
        "ERROR_WRONG_PASSWORD" -> "密碼錯誤，請再試一次"
        "ERROR_INVALID_EMAIL" -> "電子信箱格式不正確，請重新輸入"
        else -> "帳號或密碼不正確"
    }
    is FirebaseTooManyRequestsException -> "嘗試次數過多，請稍候再試"
    is FirebaseNetworkException -> "網路連線異常，請檢查網路後再試"
    is FirebaseAuthException -> when (e.errorCode) {
        "ERROR_OPERATION_NOT_ALLOWED" -> "此登入方式未啟用，請聯絡管理員"
        "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" -> "此信箱已用其他方式註冊，請用原方式登入"
        else -> "登入失敗：請稍後再試（${e.errorCode}）"
    }
    else -> "操作失敗：請稍後再試"
}
fun mapRegisterError(e: Exception): String = when (e) {
    is FirebaseAuthUserCollisionException -> "此電子信箱已被註冊，請直接登入或重設密碼。"
    is FirebaseAuthWeakPasswordException -> "密碼太弱，至少 6 個字元。"
    is FirebaseAuthInvalidCredentialsException -> "電子信箱格式不正確。"
    is FirebaseTooManyRequestsException -> "嘗試次數過多，請稍候再試。"
    is FirebaseNetworkException -> "網路連線異常，請檢查連線。"
    is FirebaseAuthException -> when (e.errorCode) {
        "ERROR_OPERATION_NOT_ALLOWED" -> "此登入方式未啟用，請到 Firebase Console 開啟 Email/Password。"
        else -> "註冊失敗：請稍後再試（${e.errorCode}）"
    }
    else -> "註冊失敗：請稍後再試。"
}
