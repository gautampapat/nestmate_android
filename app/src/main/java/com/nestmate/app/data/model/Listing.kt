package com.nestmate.app.data.model

import com.google.firebase.firestore.PropertyName

// PRIVACY: never expose ownerPhone in listing documents
data class Listing(
    val listingId: String = "",
    val type: String = "", // flat/pg/hostel
    val title: String = "",
    val description: String = "",
    val rent: Int = 0,
    val deposit: Int = 0,
    val address: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val collegeId: String = "",
    val ownerId: String = "",
    @get:PropertyName("isBachelorFriendly") @set:PropertyName("isBachelorFriendly") var isBachelorFriendly: Boolean = false,
    @get:PropertyName("isFemaleOnly") @set:PropertyName("isFemaleOnly") var isFemaleOnly: Boolean = false,
    val photos: List<String> = emptyList(),
    val realityScore: Double = 0.0,
    val greenScore: Double = 0.0,
    @get:PropertyName("isVerified") @set:PropertyName("isVerified") var isVerified: Boolean = false,
    val bhkType: String = "",
    val sharingType: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    @get:PropertyName("isActive") @set:PropertyName("isActive") var isActive: Boolean = true
)
