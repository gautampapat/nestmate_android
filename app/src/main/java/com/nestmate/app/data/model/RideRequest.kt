package com.nestmate.app.data.model

data class RideRequest(
    val id: String = "",
    val requesterId: String = "",
    val requesterName: String = "",
    val pickupLocation: String = "",
    val dropLocation: String = "",
    val rideType: RideType = RideType.PRIVATE_AUTO,
    val scheduledAt: Long? = null,
    val maxBudget: Long? = null,
    val estimatedFare: Long = 0L,
    val status: RideStatus = RideStatus.SEARCHING,
    val confirmedPassengerIds: List<String> = emptyList(),
    val driverId: String? = null,
    val driverName: String? = null,
    val vehicleNumber: String? = null,
    val vehicleType: VehicleType? = null,
    val driverRating: Float? = null,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val totalFare: Long? = null,
    val farePerPassenger: Long? = null,
    val createdAt: Long = 0L,
)
