package com.nestmate.app.utils

object FirebaseConstants {
    // CollegeId — single source of truth used everywhere
    const val DEFAULT_COLLEGE_ID = "wce_sangli"

    // User & Auth
    const val COLLECTION_USERS = "users"
    const val COLLECTION_COLLEGES = "colleges"

    // Housing (legacy — student-posted)
    const val COLLECTION_LISTINGS = "listings"

    // Phase 1 — Provider System
    const val COLLECTION_PROVIDER_LISTINGS = "providerListings"
    const val COLLECTION_INQUIRIES = "inquiries"
    const val COLLECTION_INQUIRY_CHATS = "inquiryChats"
    const val COLLECTION_PROVIDER_VERIFICATION = "providerVerificationRequests"

    // Mess
    const val COLLECTION_MESSES = "messes"
    const val COLLECTION_REVIEWS = "reviews"

    // Marketplace
    const val COLLECTION_MARKETPLACE = "marketplace"

    // Services & Transport
    const val COLLECTION_SERVICES = "services"
    const val COLLECTION_RIDES = "rides"

    // Safety
    const val COLLECTION_ALERTS = "alerts"

    // Buddy System
    const val COLLECTION_BUDDY_PAIRS = "buddyPairs"

    // Community
    const val COLLECTION_FORUMS = "forums"
    const val COLLECTION_EVENTS = "events"

    // Health
    const val COLLECTION_HEALTH_CONTACTS = "healthContacts"

    // Finance
    const val COLLECTION_EXPENSES = "expenses"

    // Roommate Matching
    const val COLLECTION_ROOMMATE_PROFILES = "roommateProfiles"
    const val COLLECTION_CONNECTIONS = "connections"

    // Lost & Found
    const val COLLECTION_LOST_FOUND = "lostFound"
}
