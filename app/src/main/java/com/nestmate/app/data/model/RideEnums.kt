package com.nestmate.app.data.model

enum class RideType(val label: String) {
    SHARED("Shared"),
    PRIVATE_AUTO("Private Auto"),
    PRIVATE_RICKSHAW("Private Rickshaw"),
}

enum class RideStatus {
    SEARCHING,
    MATCHED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED,
}

enum class VehicleType(val label: String) {
    AUTO("Auto"),
    RICKSHAW("Rickshaw"),
}

enum class PaymentMethod(val label: String) {
    WALLET("Wallet"),
    CASH("Cash"),
}
