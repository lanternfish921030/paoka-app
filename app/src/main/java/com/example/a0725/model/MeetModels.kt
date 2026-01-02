package com.example.a0725.model

import com.google.firebase.Timestamp

data class Meet(
    val id: String = "",
    val name: String = "",
    val sourceAlias: String? = null,
    val sourceUrl: String? = null,
    val lastCrawlAt: Timestamp? = null
)

data class Event(
    val id: String = "",
    val eventName: String = "",
    val gender: String? = null,   // 先保留不影響
    val group: String? = null,
    val round: String? = null,
    val heat: Int? = null,
    val dateText: String? = null,   // ← 新增
    val timeText: String? = null,   // ← 新增
    val headcount: Int? = null,     // ← 新增
    val qualifiers: String? = null, // ← 新增
    val category: String? = null    // ← 新增
)

data class ResultRow(
    val id: String = "",
    val rank: Int? = null,
    val lane: Int? = null,
    val name: String = "",
    val team: String = "",
    val result: String? = null,
    val resultNorm: Double? = null,
    val wind: Double? = null,
    val status: String? = null,

    // ↓ 這些都是爬蟲寫進 results 的欄位，用來做篩選
    val eventName: String? = null,
    val gender: String? = null,
    val group: String? = null,
    val round: String? = null,
    val heat: Int? = null,
    val date: Timestamp? = null
)
