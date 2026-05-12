package com.nestmate.app.data.model

data class SpendingCategory(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val isDefault: Boolean = false,
    val transactionType: String = "EXPENSE", // INCOME | EXPENSE
)
