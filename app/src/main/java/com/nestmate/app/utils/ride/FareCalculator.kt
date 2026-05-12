package com.nestmate.app.utils.ride

import com.nestmate.app.data.model.FareEstimate
import com.nestmate.app.data.model.PredefinedRoute
import com.nestmate.app.data.model.RideType

object FareCalculator {

    private const val AUTO_BASE = 30.0
    private const val RICKSHAW_BASE = 20.0
    private const val AUTO_PER_KM = 12.0
    private const val RICKSHAW_PER_KM = 8.0

    private const val DEFAULT_MIN_FARE_RS = 40L
    private const val DEFAULT_MAX_FARE_RS = 60L

    fun estimate(
        rideType: RideType,
        route: PredefinedRoute?,
        expectedPassengers: Int = 1,
    ): FareEstimate {
        if (route != null && route.approximateKm > 0.0) {
            val fare = when (rideType) {
                RideType.PRIVATE_RICKSHAW -> RICKSHAW_BASE + route.approximateKm * RICKSHAW_PER_KM
                RideType.PRIVATE_AUTO, RideType.SHARED ->
                    AUTO_BASE + route.approximateKm * AUTO_PER_KM
            }
            val rupees = fare.toLong()
            val perPassenger = if (rideType == RideType.SHARED && expectedPassengers > 1) {
                (rupees + expectedPassengers - 1) / expectedPassengers
            } else null
            return FareEstimate(
                minRupees = rupees,
                maxRupees = rupees,
                perPassengerRupees = perPassenger,
            )
        }

        val min = DEFAULT_MIN_FARE_RS
        val max = DEFAULT_MAX_FARE_RS
        val perPassenger = if (rideType == RideType.SHARED && expectedPassengers > 1) {
            ((min + max) / 2 + expectedPassengers - 1) / expectedPassengers
        } else null
        return FareEstimate(
            minRupees = min,
            maxRupees = max,
            perPassengerRupees = perPassenger,
        )
    }

    fun baseFareForShared(route: PredefinedRoute?): Long {
        if (route == null || route.approximateKm <= 0.0) {
            return (DEFAULT_MIN_FARE_RS + DEFAULT_MAX_FARE_RS) / 2
        }
        return (AUTO_BASE + route.approximateKm * AUTO_PER_KM).toLong()
    }
}
