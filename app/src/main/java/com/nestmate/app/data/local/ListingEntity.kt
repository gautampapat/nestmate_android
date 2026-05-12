package com.nestmate.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "listings")
data class ListingEntity(
    @PrimaryKey val listingId: String,
    val type: String,
    val title: String,
    val rent: Int,
    val deposit: Int,
    val address: String,
    val realityScore: Double,
    val greenScore: Double,
    val isBachelorFriendly: Boolean,
    val isFemaleOnly: Boolean,
    val bhkType: String,
    val photoUrl: String // Caching first image for thumbnail offline
)
