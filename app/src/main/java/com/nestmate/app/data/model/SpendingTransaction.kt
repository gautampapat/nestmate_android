package com.nestmate.app.data.model

data class SpendingTransaction(
    val id: String = "",
    val userId: String = "",
    val type: String = "EXPENSE",           // INCOME | EXPENSE
    val amount: Long = 0L,                  // in paise
    val category: String = "",
    val date: Long = System.currentTimeMillis(),
    val notes: String? = null,
    val isRecurring: Boolean = false,
    val recurrenceInterval: String? = null, // WEEKLY | MONTHLY
    val nextOccurrenceDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
