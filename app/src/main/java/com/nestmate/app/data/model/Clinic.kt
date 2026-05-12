package com.nestmate.app.data.model

data class Clinic(
    val clinicId: String = "",
    val name: String = "",
    val address: String = "",
    val contactNumber: String = "",
    val type: String = "General", // General, Hospital, Pharmacy
    val operatingHours: String = "",
    val distanceKm: Double = 0.0,
    val isEmergency24x7: Boolean = false
)
