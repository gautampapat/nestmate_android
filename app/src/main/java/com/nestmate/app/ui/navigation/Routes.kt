package com.nestmate.app.ui.navigation

sealed class Screen(val route: String) {
    // Auth Flow
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object Login : Screen("login")
    object Register : Screen("register?role={role}") {
        fun createRoute(role: String = "student") = "register?role=$role"
    }
    object CollegeVerification : Screen("college_verification")
    object RoleSelection : Screen("role_selection")

    // Student Main
    object StudentHome : Screen("student_home")
    object Home : Screen("student_home")   // alias kept for NavGraph references
    object Profile : Screen("profile")
    object EditProfile : Screen("edit_profile")
    object StudentVerification : Screen("student_verification")

    // Housing
    object HousingList : Screen("housing_list")
    object HousingDetail : Screen("housing_detail/{listingId}") {
        fun createRoute(listingId: String) = "housing_detail/$listingId"
    }
    object AddListing : Screen("add_listing")

    // Mess
    object MessList : Screen("mess_list")
    object MessDetail : Screen("mess_detail/{messId}") {
        fun createRoute(messId: String) = "mess_detail/$messId"
    }

    // Marketplace
    object MarketplaceList : Screen("marketplace_list")
    object MarketplaceDetail : Screen("marketplace_detail/{itemId}") {
        fun createRoute(itemId: String) = "marketplace_detail/$itemId"
    }
    object AddMarketplaceItem : Screen("add_marketplace_item?editItemId={editItemId}") {
        const val BASE = "add_marketplace_item"
        fun createRoute(editItemId: String? = null): String =
            if (editItemId == null) BASE else "$BASE?editItemId=$editItemId"
    }
    object MarketplaceWantedCreate : Screen("marketplace_wanted_create")
    object MarketplaceBundleCreate : Screen("marketplace_bundle_create")
    object MarketplaceChats : Screen("marketplace_chats")
    object MarketplaceChat : Screen("marketplace_chat/{chatId}") {
        fun createRoute(chatId: String) = "marketplace_chat/$chatId"
    }

    // Services & Transport
    object ServicesHub : Screen("services_hub")
    object RideSharing : Screen("ride_sharing")
    object RideSharedMatch : Screen("ride_shared_match?pickup={pickup}&drop={drop}&scheduledAt={scheduledAt}") {
        const val BASE = "ride_shared_match"
        fun createRoute(pickup: String, drop: String, scheduledAt: Long?): String {
            val encPickup = java.net.URLEncoder.encode(pickup, "UTF-8")
            val encDrop = java.net.URLEncoder.encode(drop, "UTF-8")
            val schedPart = scheduledAt?.let { "&scheduledAt=$it" }.orEmpty()
            return "$BASE?pickup=$encPickup&drop=$encDrop$schedPart"
        }
    }
    object RideActive : Screen("ride_active")
    object RideHistory : Screen("ride_history")
    object RideRating : Screen("ride_rating/{rideId}") {
        fun createRoute(rideId: String) = "ride_rating/$rideId"
    }
    object RideScheduled : Screen("ride_scheduled")

    // Health
    object HealthHub : Screen("health_hub")

    // Community
    object CommunityHub : Screen("community_hub")
    object PostDetail : Screen("post_detail/{postId}") {
        fun createRoute(postId: String) = "post_detail/$postId"
    }
    object AddPost : Screen("add_post")

    // Finance
    object FinanceHub : Screen("finance_hub")
    object AddExpense : Screen("add_expense")
    object AddSplitExpense : Screen("add_split_expense")
    object BillList : Screen("bill_list")
    object BillCreate : Screen("bill_create?userId={userId}") {
        fun createRoute(userId: String? = null) = if (userId == null) "bill_create" else "bill_create?userId=$userId"
    }
    object BillDetail : Screen("bill_detail/{billId}") {
        fun createRoute(billId: String) = "bill_detail/$billId"
    }

    // Roommate Matching
    object RoommateMatching : Screen("roommate_matching")
    object RoommateSetup : Screen("roommate_setup")
    object RoommateProfileDetail : Screen("roommate_profile/{targetUserId}") {
        fun createRoute(targetUserId: String) = "roommate_profile/$targetUserId"
    }
    object Connections : Screen("connections")
    object RoommateGroups : Screen("roommate_groups")
    object RoommateChat : Screen("roommate_chat/{chatId}") {
        fun createRoute(chatId: String) = "roommate_chat/$chatId"
    }

    // Safety & Emergency
    object SafetyHub : Screen("safety_hub")
    object EmergencyContacts : Screen("emergency_contacts")

    // Buddy System
    object BuddyHome : Screen("buddy_home")
    object BuddyMatching : Screen("buddy_matching")
    object BuddyChat : Screen("buddy_chat/{pairId}") {
        fun createRoute(pairId: String) = "buddy_chat/$pairId"
    }

    // Provider Routes (provider/ prefix — only in providerNavGraph)
    object ProviderDashboard : Screen("provider/dashboard")
    object ProviderVerification : Screen("provider/verification")
    object ProviderMyListings : Screen("provider/my_listings")
    object ProviderAddListing : Screen("provider/add_listing/{listingType}") {
        fun createRoute(type: String) = "provider/add_listing/$type"
    }
    object ProviderEditListing : Screen("provider/edit_listing/{listingId}") {
        fun createRoute(id: String) = "provider/edit_listing/$id"
    }
    object ProviderListingDetail : Screen("provider/listing_detail/{listingId}") {
        fun createRoute(id: String) = "provider/listing_detail/$id"
    }
    object ProviderInquiries : Screen("provider/inquiries")
    object ProviderInquiryChat : Screen("provider/inquiry_chat/{inquiryId}") {
        fun createRoute(inquiryId: String) = "provider/inquiry_chat/$inquiryId"
    }
    object ProviderProfile : Screen("provider/profile")
    object ProviderEditProfile : Screen("provider/edit_profile")

    // Restaurants (Phase 4.1)
    object RestaurantBrowse : Screen("restaurants")
    object RestaurantDetail : Screen("restaurants/{restaurantId}") {
        fun createRoute(restaurantId: String) = "restaurants/$restaurantId"
    }

    // Spending Tracker (Phase 4.2)
    object SpendingDashboard : Screen("spending")
    object SpendingBudget : Screen("spending/budget")
    object SpendingInsights : Screen("spending/insights")

    // Other User Profile (Phase 4.3)
    object OtherUserProfile : Screen("profile/{userId}") {
        fun createRoute(userId: String) = "profile/$userId"
    }

    // Lost & Found
    object LostFound : Screen("lost_found")
    object LostFoundDetail : Screen("lost_found_detail/{itemId}") {
        fun createRoute(itemId: String) = "lost_found_detail/$itemId"
    }
    object LostFoundPost : Screen("lost_found_post")
}
