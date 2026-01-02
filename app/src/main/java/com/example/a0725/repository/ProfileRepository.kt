package com.example.a0725.repository

import com.example.a0725.data.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class ProfileRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private fun doc() = db.collection("users").document(
        requireNotNull(auth.currentUser?.uid) { "Firebase 使用者尚未登入" }
    )

    /** 讀取個人資料（包含 photoUrl） */
    suspend fun load(): UserProfile {
        val s = doc().get().await()
        if (!s.exists()) return UserProfile()

        val name = (s.getString("realName") ?: s.getString("displayName")).orEmpty()
        val email = s.getString("email").orEmpty()
        val ctaa = s.getString("ctaa_id")?.takeIf { it.isNotBlank() }
        val phone = s.getString("phone")?.takeIf { it.isNotBlank() }
        val photo = s.getString("photoUrl")?.takeIf { it.isNotBlank() }

        return UserProfile(
            name = name,
            ctaaId = ctaa,
            phone = phone,
            email = email,
            photoUrl = photo
        )
    }

    /** 單一欄位更新；value == null 或空字串會刪除該欄位 */
    suspend fun updateField(field: String, value: Any?) {
        val payload = if (value == null || (value is String && value.isBlank())) {
            mapOf(field to FieldValue.delete(), "updatedAt" to FieldValue.serverTimestamp())
        } else {
            mapOf(field to value, "updatedAt" to FieldValue.serverTimestamp())
        }
        doc().set(payload, SetOptions.merge()).await()
    }

    /** 更新名字時，同步 realName / displayName */
    suspend fun updateName(name: String) {
        val map = mapOf(
            "realName" to name,
            "displayName" to name,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        doc().set(map, SetOptions.merge()).await()
    }

    /** 直接設定 photoUrl */
    suspend fun setPhotoUrl(url: String?) {
        val payload = if (url.isNullOrBlank()) {
            mapOf("photoUrl" to com.google.firebase.firestore.FieldValue.delete(),
                "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp())
        } else {
            mapOf("photoUrl" to url,
                "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp())
        }
        doc().set(payload, com.google.firebase.firestore.SetOptions.merge()).await()
    }


    /** 一次上傳多個欄位（包含 photoUrl 的 merge 刪除邏輯） */
    suspend fun upsert(profile: UserProfile) {
        val map = mapOf(
            "realName" to profile.name,
            "displayName" to profile.name,
            "ctaa_id" to (profile.ctaaId ?: FieldValue.delete()),
            "phone" to (profile.phone ?: FieldValue.delete()),
            "email" to profile.email,
            "photoUrl" to (profile.photoUrl ?: FieldValue.delete()),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        doc().set(map, SetOptions.merge()).await()
    }
}
