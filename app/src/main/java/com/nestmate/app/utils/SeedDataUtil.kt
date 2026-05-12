package com.nestmate.app.utils

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.nestmate.app.data.model.DailyService
import com.nestmate.app.data.model.ItemCategory
import com.nestmate.app.data.model.ItemCondition
import com.nestmate.app.data.model.ItemStatus
import com.nestmate.app.data.model.Listing
import com.nestmate.app.data.model.MarketplaceItem
import com.nestmate.app.data.model.Mess
import com.nestmate.app.data.model.User
import kotlinx.coroutines.tasks.await

object SeedDataUtil {

    private const val SEED_FLAG = "seed_data_v3_complete"

    /**
     * Call this from MainActivity after the user is confirmed logged in.
     * Seeds all collections once using a SharedPreferences flag — never runs again.
     */
    suspend fun seedIfNeeded(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth,
        context: Context
    ) {
        val prefs = context.getSharedPreferences("nestmate_seed", Context.MODE_PRIVATE)
        if (prefs.getBoolean(SEED_FLAG, false)) return // Already seeded

        try {
            seedMesses(firestore)
            seedListings(firestore, auth)
            seedMarketplace(firestore, auth)
            seedServices(firestore)
            seedSeniorUsers(firestore)
            seedRoommateProfiles(firestore)
            seedCommunityPosts(firestore, auth)

            // Mark as done so this never runs again
            prefs.edit().putBoolean(SEED_FLAG, true).commit()
        } catch (e: Exception) {
            // Seed failure is non-fatal — app still works, will retry next launch
        }
    }

    private suspend fun seedMesses(db: FirebaseFirestore) {
        val col = db.collection(FirebaseConstants.COLLECTION_MESSES)

        val messes = listOf(
            Mess(
                name = "Sharma Mess",
                address = "Vishrambag, Sangli",
                collegeId = FirebaseConstants.DEFAULT_COLLEGE_ID,
                ownerId = "seed",
                vegNonVeg = "Both",
                menu = mapOf("Lunch" to "Roti, Sabji, Dal, Rice", "Dinner" to "Roti, Sabji, Sweet"),
                pricing = mapOf("PerMeal" to 60, "Monthly" to 1500),
                timings = mapOf("Lunch" to "12:00-14:30", "Dinner" to "19:30-22:00"),
                crowdLevel = "medium",
                isActive = true,
                rating = 4.2
            ),
            Mess(
                name = "Laxmi Tiffin Centre",
                address = "Near College Gate, WCE",
                collegeId = FirebaseConstants.DEFAULT_COLLEGE_ID,
                ownerId = "seed",
                vegNonVeg = "Veg",
                menu = mapOf("Lunch" to "Jowar Bhakri, Amti, Rice", "Dinner" to "Chapati, Vegetable"),
                pricing = mapOf("PerMeal" to 50, "Monthly" to 1200),
                timings = mapOf("Lunch" to "12:00-14:00", "Dinner" to "19:00-21:30"),
                crowdLevel = "low",
                isActive = true,
                rating = 4.5
            ),
            Mess(
                name = "WCE Canteen",
                address = "Inside Campus, WCE Sangli",
                collegeId = FirebaseConstants.DEFAULT_COLLEGE_ID,
                ownerId = "seed",
                vegNonVeg = "Both",
                menu = mapOf("Breakfast" to "Poha, Chai", "Lunch" to "Thali", "Snacks" to "Samosa, Vada Pav"),
                pricing = mapOf("PerMeal" to 40, "Monthly" to 1000),
                timings = mapOf("Breakfast" to "08:00-10:00", "Lunch" to "12:00-14:00", "Snacks" to "16:00-17:00"),
                crowdLevel = "high",
                isActive = true,
                rating = 3.8
            )
        )

        messes.forEach { mess ->
            val ref = col.document()
            ref.set(mess.copy(messId = ref.id)).await()
        }
    }

    private suspend fun seedListings(db: FirebaseFirestore, auth: FirebaseAuth) {
        val col = db.collection(FirebaseConstants.COLLECTION_LISTINGS)
        val userId = auth.currentUser?.uid ?: "seed_user"

        val listings = listOf(
            Listing(
                title = "2BHK Flat near WCE",
                description = "Well-furnished 2BHK near Walchand College gate. All amenities, 24/7 water.",
                address = "Vishrambag, Sangli — 5 min walk to WCE",
                rent = 4500,
                deposit = 9000,
                type = "flat",
                bhkType = "2BHK",
                isBachelorFriendly = true,
                isFemaleOnly = false,
                realityScore = 7.8,
                greenScore = 6.5,
                collegeId = FirebaseConstants.DEFAULT_COLLEGE_ID,
                ownerId = userId,
                isVerified = true,
                isActive = true
            ),
            Listing(
                title = "PG for Girls — Vishrambag",
                description = "Safe and clean PG for girls. Meals included. CCTV. 10 min to college.",
                address = "Vishrambag Chowk, Sangli",
                rent = 3500,
                deposit = 7000,
                type = "pg",
                bhkType = "Single",
                isBachelorFriendly = false,
                isFemaleOnly = true,
                realityScore = 8.2,
                greenScore = 7.9,
                collegeId = FirebaseConstants.DEFAULT_COLLEGE_ID,
                ownerId = userId,
                isVerified = true,
                isActive = true
            )
        )

        listings.forEach { listing ->
            val ref = col.document()
            ref.set(listing.copy(listingId = ref.id)).await()
        }
    }

    private suspend fun seedMarketplace(db: FirebaseFirestore, auth: FirebaseAuth) {
        val col = db.collection("marketplace").document("root").collection("items")
        val sellerId = auth.currentUser?.uid ?: "seed_user"
        val now = System.currentTimeMillis()

        val items = listOf(
            MarketplaceItem(
                sellerId = sellerId,
                sellerName = "Seed Seller",
                title = "Study Table — Wooden",
                description = "2-year-old wooden study table in good condition. Moving out sale.",
                price = 80000L,
                isNegotiable = true,
                condition = ItemCondition.USED,
                category = ItemCategory.FURNITURE,
                status = ItemStatus.ACTIVE,
                createdAt = now,
                updatedAt = now,
            ),
            MarketplaceItem(
                sellerId = sellerId,
                sellerName = "Seed Seller",
                title = "Engineering Drawing Kit",
                description = "Complete set with drafter, compass, scale. Used for 1 semester only.",
                price = 15000L,
                isNegotiable = false,
                condition = ItemCondition.USED,
                category = ItemCategory.BOOKS,
                status = ItemStatus.ACTIVE,
                createdAt = now,
                updatedAt = now,
            ),
        )

        items.forEach { item ->
            val ref = col.document()
            ref.set(item.copy(id = ref.id)).await()
        }
    }

    private suspend fun seedServices(db: FirebaseFirestore) {
        val col = db.collection(FirebaseConstants.COLLECTION_SERVICES)

        val services = listOf(
            DailyService(
                name = "Raju Laundry",
                type = "Laundry",
                contact = "+91 98765 12345",
                priceSummary = "₹10-30/item",
                rating = 4.0,
                isVerified = true
            ),
            DailyService(
                name = "Vishrambag Xerox",
                type = "Xerox",
                contact = "+91 87654 23456",
                priceSummary = "₹1/page (B&W), ₹5/page (Colour)",
                rating = 4.3,
                isVerified = true
            ),
            DailyService(
                name = "Krishna Grocery",
                type = "Grocery",
                contact = "+91 76543 34567",
                priceSummary = "Home delivery available",
                rating = 4.1,
                isVerified = false
            ),
            DailyService(
                name = "Sangli Gas Agency",
                type = "Gas",
                contact = "+91 65432 45678",
                priceSummary = "₹950/cylinder",
                rating = 3.9,
                isVerified = false
            )
        )

        services.forEach { service ->
            val ref = col.document()
            ref.set(service.copy(serviceId = ref.id)).await()
        }
    }

    private suspend fun seedSeniorUsers(db: FirebaseFirestore) {
        val col = db.collection(FirebaseConstants.COLLECTION_USERS)

        val seniors = listOf(
            User(
                name = "Rahul Patil",
                email = "rahul.patil@walchandsangli.ac.in",
                collegeId = FirebaseConstants.DEFAULT_COLLEGE_ID,
                year = "TY",
                isSenior = true,
                buddyBadge = true,
                gender = "Male",
                role = "student",
                verificationStatus = "Verified",
                linkedinUrl = "https://linkedin.com/in/rahulpatil",
                instagramHandle = "@rahul_p"
            ),
            User(
                name = "Sneha Desai",
                email = "sneha.desai@walchandsangli.ac.in",
                collegeId = FirebaseConstants.DEFAULT_COLLEGE_ID,
                year = "BE",
                isSenior = true,
                buddyBadge = true,
                gender = "Female",
                role = "student",
                verificationStatus = "Verified",
                linkedinUrl = "https://linkedin.com/in/snehadesai",
                instagramHandle = "@sneha.desai"
            )
        )

        seniors.forEach { user ->
            val ref = col.document()
            ref.set(user.copy(userId = ref.id)).await()
        }
    }

    private suspend fun seedRoommateProfiles(db: FirebaseFirestore) {
        val col = db.collection("roommate").document("root").collection("profiles")
        val nowMs = System.currentTimeMillis()
        val nowTimestamp = com.google.firebase.Timestamp.now()

        val profiles = listOf(
            com.nestmate.app.data.model.RoommateProfile(
                name = "Amit Kumar",
                age = 20,
                gender = com.nestmate.app.data.model.Gender.MALE,
                collegeName = "WCE Sangli",
                course = "Computer Engineering",
                preferredLocation = "Vishrambag",
                minBudget = 3000L,
                maxBudget = 5000L,
                roomTypePreference = com.nestmate.app.data.model.RoomType.SHARED_ROOM,
                sleepingSchedule = com.nestmate.app.data.model.SleepSchedule.NIGHT_OWL,
                cleanlinessLevel = 3,
                studyHabits = com.nestmate.app.data.model.StudyHabit.SOCIAL,
                foodPreference = com.nestmate.app.data.model.FoodPreference.NON_VEG,
                smokingHabit = com.nestmate.app.data.model.HabitPreference.NO,
                drinkingHabit = com.nestmate.app.data.model.HabitPreference.NO,
                bio = "I have a flat in Vishrambag. Looking for 1 chill flatmate.",
                isActivelySearching = true,
                lastActiveAt = nowMs,
                createdAt = nowMs,
                updatedAt = nowMs,
            ),
            com.nestmate.app.data.model.RoommateProfile(
                name = "Priya Singh",
                age = 21,
                gender = com.nestmate.app.data.model.Gender.FEMALE,
                collegeName = "WCE Sangli",
                course = "Electronics Engineering",
                preferredLocation = "Vishrambag",
                minBudget = 2000L,
                maxBudget = 4000L,
                roomTypePreference = com.nestmate.app.data.model.RoomType.SHARED_ROOM,
                sleepingSchedule = com.nestmate.app.data.model.SleepSchedule.EARLY_BIRD,
                cleanlinessLevel = 4,
                studyHabits = com.nestmate.app.data.model.StudyHabit.QUIET_STUDIER,
                foodPreference = com.nestmate.app.data.model.FoodPreference.VEG,
                smokingHabit = com.nestmate.app.data.model.HabitPreference.NO,
                drinkingHabit = com.nestmate.app.data.model.HabitPreference.NO,
                bio = "Need a peaceful environment for studying.",
                isActivelySearching = true,
                lastActiveAt = nowMs,
                createdAt = nowMs,
                updatedAt = nowMs,
            ),
        )

        profiles.forEach { profile ->
            val ref = col.document()
            ref.set(profile.copy(userId = ref.id)).await()
        }
    }

    private suspend fun seedCommunityPosts(db: FirebaseFirestore, auth: FirebaseAuth) {
        // Mock seeding for Community Hub just to show lists. 
        // Real implementation would use Post model.
    }
}
