package com.nestmate.app.data.model

data class FareEstimate(
    val minRupees: Long,
    val maxRupees: Long,
    val perPassengerRupees: Long? = null,
    val currency: String = "Rs",
)
