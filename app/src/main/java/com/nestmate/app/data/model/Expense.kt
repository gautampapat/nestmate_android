package com.nestmate.app.data.model

data class Expense(
    val expenseId: String = "",
    val groupId: String = "",
    val payerId: String = "",
    val payerName: String = "",
    val description: String = "",
    val amount: Int = 0,
    val category: String = "Other", // Rent, Food/Mess, Travel, Laundry, Utilities, Misc, Other
    val note: String = "",
    val isPersonal: Boolean = true,
    // Map of userId → share amount they owe the payer
    val splits: Map<String, Int> = emptyMap(),
    // Denormalized list of split user IDs for whereArrayContains queries
    val splitUserIds: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)
