package com.nestmate.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.nestmate.app.data.model.Clinic
import javax.inject.Inject

class HealthRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    // Returning dummy local data for Phase 1 as health collection wasn't strictly defined
    // but the spec required standard clinics listing. Mapped to WCE Sangli locality
    suspend fun getNearbyClinics(): Result<List<Clinic>> {
        return Result.success(
            listOf(
                Clinic(
                    clinicId = "c1",
                    name = "Civil Hospital, Sangli",
                    address = "Vishrambag, Sangli",
                    contactNumber = "0233-2374333",
                    type = "Hospital",
                    operatingHours = "24x7",
                    distanceKm = 1.2,
                    isEmergency24x7 = true
                ),
                Clinic(
                    clinicId = "c2",
                    name = "Mhetre Clinic",
                    address = "Near College Gate, WCE",
                    contactNumber = "+91 98765 00001",
                    type = "General",
                    operatingHours = "10:00 AM - 9:00 PM",
                    distanceKm = 0.3,
                    isEmergency24x7 = false
                ),
                Clinic(
                    clinicId = "c3",
                    name = "Wellness Medicals",
                    address = "Vishrambag Chowk",
                    contactNumber = "+91 98765 00002",
                    type = "Pharmacy",
                    operatingHours = "8:00 AM - 11:00 PM",
                    distanceKm = 0.8,
                    isEmergency24x7 = false
                )
            )
        )
    }
}
