package com.nestmate.app.data.model

data class SpendingBudget(
    val id: String = "",
    val userId: String = "",
    val category: String = "",
    val monthlyLimit: Long = 0L, // paise
    val month: Int = 1,          // 1–12
    val year: Int = 2026,
)
