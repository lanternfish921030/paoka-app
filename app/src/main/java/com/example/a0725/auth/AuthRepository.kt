package com.example.a0725.auth

import com.google.firebase.auth.EmailAuthProvider // 8/22
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.tasks.await


object AuthRepository {
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    init {
        // 讓寄出的信依系統語系顯示；如要固定繁中可改 "zh-TW"
        auth.useAppLanguage()
        auth.setLanguageCode("zh-TW")
    }

    val currentUser get() = auth.currentUser

    suspend fun signIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).await()
    }

    suspend fun register(
        username: String,
        account: String,
        email: String,
        password: String,
        ctaaId: String? = null
    ) {
        // 建立 Auth user
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val user = result.user ?: error("Auth user is null after registration")
        val uid = user.uid

        // 建議用 server timestamp；欄位名依你 Firestore 規則（若走方案A）
        val userData = hashMapOf<String, Any?>(
            "realName"   to username,
            "displayName" to username,
            "account"    to account,
            "email"      to email,
            "createdAt"  to FieldValue.serverTimestamp(),
            "phone" to ""
        ).apply {
            ctaaId?.takeIf { it.isNotBlank() }?.let { put("ctaa_id", it) }
        }

        try {
            // 建 users/{uid}
            db.collection("users").document(uid).set(userData).await()
            // 註冊成功後立刻寄驗證信
            user.sendEmailVerification().await()
        } catch (e: Exception) {
            // Firestore 失敗 → 回滾 Auth 帳號，避免半完成
            try { user.delete().await() } catch (_: Exception) { }
            throw e
        }
    }

    // 重新寄送驗證信（需是已登入的該帳號）
    suspend fun resendVerificationEmail() {
        val user = auth.currentUser ?: error("尚未登入，無法寄驗證信")
        user.sendEmailVerification().await()
    }

    // 重新抓取使用者資料（驗證完回到App可呼叫）
    suspend fun reloadUser() {
        auth.currentUser?.reload()?.await()
    }
    // 變更密碼：會先以 email + currentPassword 重新驗證，再更新為 newPassword 8/22
    suspend fun changePassword(currentPassword: String, newPassword: String) {
        val user = auth.currentUser ?: error("尚未登入")
        val email = user.email ?: error("此帳號沒有綁定 Email，無法變更密碼")

        // 先 reauthenticate（敏感操作必須要「近期登入」）
        val cred = EmailAuthProvider.getCredential(email, currentPassword)
        user.reauthenticate(cred).await()

        // 再更新密碼
        user.updatePassword(newPassword).await()
    }

    fun isEmailVerified(): Boolean = auth.currentUser?.isEmailVerified == true

    suspend fun sendPasswordReset(email: String) {
        auth.sendPasswordResetEmail(email).await()
    }

    fun signOut() = auth.signOut()
}
