package com.nestmate.app.data.model

data class DailyService(
    val serviceId: String = "",
    val name: String = "",
    val type: String = "Tiffin", // Tiffin, Laundry, Maid
    val contact: String = "",
    val priceSummary: String = "",
    val rating: Double = 0.0,
    val isVerified: Boolean = false,
)
