package com.example.a0725.data

data class UserProfile(
    val name: String = "",          // 來自 realName 或 displayName（擇一）
    val ctaaId: String? = null,     // 來自 ctaa_id
    val phone: String? = null,      // 來自 phone（可選）
    val email: String = "",          // 來自 email
    val photoUrl: String? = null    // 新增：用來儲存頭像 URL
)
