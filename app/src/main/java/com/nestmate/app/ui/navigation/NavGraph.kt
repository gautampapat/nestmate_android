package com.nestmate.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.nestmate.app.ui.screens.auth.CollegeVerificationScreen
import com.nestmate.app.ui.screens.auth.LoginScreen
import com.nestmate.app.ui.screens.auth.OnboardingScreen
import com.nestmate.app.ui.screens.auth.RegisterScreen
import com.nestmate.app.ui.screens.auth.RoleSelectionScreen
import com.nestmate.app.ui.screens.auth.SplashScreen
import com.nestmate.app.ui.screens.bill.BillDetailScreen
import com.nestmate.app.ui.screens.bill.BillListScreen
import com.nestmate.app.ui.screens.bill.CreateBillScreen
import com.nestmate.app.ui.screens.buddy.BuddyChatScreen
import com.nestmate.app.ui.screens.buddy.BuddyHomeScreen
import com.nestmate.app.ui.screens.buddy.BuddyMatchingScreen
import com.nestmate.app.ui.screens.community.AddPostScreen
import com.nestmate.app.ui.screens.community.CommunityHubScreen
import com.nestmate.app.ui.screens.community.PostDetailScreen
import com.nestmate.app.ui.screens.finance.AddExpenseScreen
import com.nestmate.app.ui.screens.finance.AddSplitExpenseScreen
import com.nestmate.app.ui.screens.finance.FinanceHubScreen
import com.nestmate.app.ui.screens.health.HealthHubScreen
import com.nestmate.app.ui.screens.home.HomeScreen
import com.nestmate.app.ui.screens.housing.AddListingScreen as AddHousingListingScreen
import com.nestmate.app.ui.screens.housing.HousingDetailScreen
import com.nestmate.app.ui.screens.housing.HousingListScreen
import com.nestmate.app.ui.screens.marketplace.AddListingScreen
import com.nestmate.app.ui.screens.marketplace.CreateBundleScreen
import com.nestmate.app.ui.screens.marketplace.CreateWantedPostScreen
import com.nestmate.app.ui.screens.marketplace.MarketplaceBrowseScreen
import com.nestmate.app.ui.screens.marketplace.MarketplaceChatScreen
import com.nestmate.app.ui.screens.marketplace.MarketplaceChatsListScreen
import com.nestmate.app.ui.screens.marketplace.MarketplaceItemDetailScreen
import com.nestmate.app.ui.screens.lostandfound.LostFoundHubScreen
import com.nestmate.app.ui.screens.lostandfound.LostFoundDetailScreen
import com.nestmate.app.ui.screens.lostandfound.LostFoundPostScreen
import com.nestmate.app.ui.screens.mess.MessDetailScreen
import com.nestmate.app.ui.screens.mess.MessListScreen
import com.nestmate.app.ui.screens.profile.EditProfileScreen
import com.nestmate.app.ui.screens.profile.ProfileScreen
import com.nestmate.app.ui.screens.provider.ProviderAddEditListingScreen
import com.nestmate.app.ui.screens.provider.ProviderDashboardScreen
import com.nestmate.app.ui.screens.provider.ProviderInquiriesScreen
import com.nestmate.app.ui.screens.provider.ProviderInquiryChatScreen
import com.nestmate.app.ui.screens.provider.ProviderVerificationScreen
import com.nestmate.app.ui.screens.profile.StudentVerificationScreen
import com.nestmate.app.ui.screens.rides.ActiveRideScreen
import com.nestmate.app.ui.screens.rides.RideHistoryScreen
import com.nestmate.app.ui.screens.rides.RideHomeScreen
import com.nestmate.app.ui.screens.rides.RideRatingScreen
import com.nestmate.app.ui.screens.rides.ScheduledRidesScreen
import com.nestmate.app.ui.screens.rides.SharedRideMatchScreen
import com.nestmate.app.ui.screens.roommate.RoommateBrowseScreen
import com.nestmate.app.ui.screens.roommate.RoommateChatScreen
import com.nestmate.app.ui.screens.connections.ConnectionsScreen
import com.nestmate.app.ui.screens.roommate.RoommateGroupsScreen
import com.nestmate.app.ui.screens.roommate.RoommateProfileDetailScreen
import com.nestmate.app.ui.screens.roommate.RoommateSetupScreen
import com.nestmate.app.ui.screens.safety.EmergencyContactsScreen
import com.nestmate.app.ui.screens.safety.SafetyHubScreen
import com.nestmate.app.ui.screens.services.ServicesHubScreen
import com.nestmate.app.ui.screens.restaurant.RestaurantBrowseScreen
import com.nestmate.app.ui.screens.restaurant.RestaurantDetailScreen
import com.nestmate.app.ui.screens.spending.SpendingDashboardScreen
import com.nestmate.app.ui.screens.spending.BudgetScreen
import com.nestmate.app.ui.screens.spending.InsightsScreen

@Composable
fun NestMateNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Splash.route,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {

        // ─────────────────────────────────────────────────────────────────────
        // AUTH GRAPH — accessible to everyone, no role restriction
        // ─────────────────────────────────────────────────────────────────────

        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToOnboarding = {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    // RootNavGate will handle routing to the correct home after role fetch
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
            )
        }

        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onNavigateToRoleSelection = {
                    navController.navigate(Screen.RoleSelection.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                },
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToHome = { },
                onNavigateToRegister = {
                    navController.navigate(Screen.RoleSelection.route)
                },
            )
        }

        // RoleSelection — shown before RegisterScreen for new users
        composable(Screen.RoleSelection.route) {
            RoleSelectionScreen(
                onNavigateToRegister = { role ->
                    navController.navigate(Screen.Register.createRoute(role))
                },
            )
        }

        // Register — accepts a role nav argument (default "student")
        composable(
            route = Screen.Register.route,
            arguments = listOf(
                navArgument("role") {
                    type = NavType.StringType
                    defaultValue = "student"
                },
            ),
        ) { backStackEntry ->
            val role = backStackEntry.arguments?.getString("role") ?: "student"
            RegisterScreen(
                role = role,
                onNavigateToVerification = {
                    navController.navigate(Screen.CollegeVerification.route) {
                        popUpTo(Screen.RoleSelection.route) { inclusive = true }
                    }
                },
                onNavigateToProviderDashboard = {
                    navController.navigate(Screen.ProviderDashboard.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                },
            )
        }

        // CollegeVerification — students only (providers skip this in RegisterScreen)
        composable(Screen.CollegeVerification.route) {
            CollegeVerificationScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.StudentHome.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }

        // ─────────────────────────────────────────────────────────────────────
        // STUDENT NAV GRAPH — student-exclusive routes
        // ─────────────────────────────────────────────────────────────────────

        navigation(
            startDestination = Screen.StudentHome.route,
            route = "student_graph",
        ) {
            composable(Screen.StudentHome.route) {
                HomeScreen(
                    onNavigateTo = { route -> navController.navigate(route) },
                    onNavigateToSplash = {
                        navController.navigate(Screen.Splash.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                )
            }

            composable(Screen.EditProfile.route) {
                EditProfileScreen(onNavigateBack = { navController.popBackStack() })
            }
            
            composable(Screen.Profile.route) {
                ProfileScreen(
                    onNavigateToSplash = {
                        navController.navigate(Screen.Splash.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onNavigateToEditProfile = { navController.navigate(Screen.EditProfile.route) },
                    onNavigateToVerification = { navController.navigate(Screen.StudentVerification.route) }
                )
            }
            
            composable(Screen.StudentVerification.route) {
                StudentVerificationScreen(onNavigateBack = { navController.popBackStack() })
            }

            // Housing
            composable(Screen.HousingList.route) {
                HousingListScreen(
                    onNavigateToDetail = { listingId ->
                        navController.navigate(Screen.HousingDetail.createRoute(listingId))
                    },
                    onNavigateToAddListing = { navController.navigate(Screen.AddListing.route) },
                )
            }
            composable(Screen.HousingDetail.route) { backStackEntry ->
                val listingId = backStackEntry.arguments?.getString("listingId") ?: ""
                HousingDetailScreen(
                    listingId = listingId,
                    onNavigateBack = { navController.popBackStack() },
                )
            }
            composable(Screen.AddListing.route) {
                AddHousingListingScreen(onNavigateBack = { navController.popBackStack() })
            }

            // Mess
            composable(Screen.MessList.route) {
                MessListScreen(
                    onNavigateToDetail = { messId ->
                        navController.navigate(Screen.MessDetail.createRoute(messId))
                    },
                )
            }
            composable(Screen.MessDetail.route) { backStackEntry ->
                val messId = backStackEntry.arguments?.getString("messId") ?: ""
                MessDetailScreen(
                    messId = messId,
                    onNavigateBack = { navController.popBackStack() },
                )
            }

            // Marketplace
            composable(Screen.MarketplaceList.route) {
                MarketplaceBrowseScreen(
                    onNavigateToDetail = { itemId ->
                        navController.navigate(Screen.MarketplaceDetail.createRoute(itemId))
                    },
                    onNavigateToAddItem = {
                        navController.navigate(Screen.AddMarketplaceItem.createRoute())
                    },
                    onNavigateToWantedCreate = {
                        navController.navigate(Screen.MarketplaceWantedCreate.route)
                    },
                    onNavigateToChats = {
                        navController.navigate(Screen.MarketplaceChats.route)
                    },
                    onNavigateToWantedChat = { chatId ->
                        navController.navigate(Screen.MarketplaceChat.createRoute(chatId))
                    },
                    onNavigateBack = { navController.popBackStack() },
                )
            }
            composable(Screen.MarketplaceDetail.route) { backStackEntry ->
                val itemId = backStackEntry.arguments?.getString("itemId") ?: ""
                MarketplaceItemDetailScreen(
                    itemId = itemId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToChat = { chatId ->
                        navController.navigate(Screen.MarketplaceChat.createRoute(chatId))
                    },
                    onNavigateToEdit = { id ->
                        navController.navigate(Screen.AddMarketplaceItem.createRoute(id))
                    },
                )
            }
            composable(
                route = Screen.AddMarketplaceItem.route,
                arguments = listOf(
                    navArgument("editItemId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) { backStackEntry ->
                val editId = backStackEntry.arguments?.getString("editItemId")
                AddListingScreen(
                    editItemId = editId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToBundleCreate = {
                        navController.navigate(Screen.MarketplaceBundleCreate.route)
                    },
                )
            }
            composable(Screen.MarketplaceWantedCreate.route) {
                CreateWantedPostScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.MarketplaceBundleCreate.route) {
                CreateBundleScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.MarketplaceChats.route) {
                MarketplaceChatsListScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToChat = { chatId ->
                        navController.navigate(Screen.MarketplaceChat.createRoute(chatId))
                    },
                )
            }
            composable(Screen.MarketplaceChat.route) { backStackEntry ->
                val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
                MarketplaceChatScreen(
                    chatId = chatId,
                    onNavigateBack = { navController.popBackStack() },
                )
            }

            // Services & Rides
            composable(Screen.ServicesHub.route) {
                ServicesHubScreen(
                    onNavigateToRideShare = { navController.navigate(Screen.RideSharing.route) },
                )
            }
            composable(Screen.RideSharing.route) {
                RideHomeScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToSharedMatch = { pickup, drop, scheduledAt ->
                        navController.navigate(
                            Screen.RideSharedMatch.createRoute(pickup, drop, scheduledAt),
                        )
                    },
                    onNavigateToActive = { navController.navigate(Screen.RideActive.route) },
                    onNavigateToHistory = { navController.navigate(Screen.RideHistory.route) },
                    onNavigateToScheduled = { navController.navigate(Screen.RideScheduled.route) },
                )
            }
            composable(
                route = Screen.RideSharedMatch.route,
                arguments = listOf(
                    navArgument("pickup") { type = NavType.StringType; defaultValue = "" },
                    navArgument("drop") { type = NavType.StringType; defaultValue = "" },
                    navArgument("scheduledAt") {
                        type = NavType.StringType; nullable = true; defaultValue = null
                    },
                ),
            ) { backStackEntry ->
                val pickup = java.net.URLDecoder.decode(
                    backStackEntry.arguments?.getString("pickup").orEmpty(), "UTF-8",
                )
                val drop = java.net.URLDecoder.decode(
                    backStackEntry.arguments?.getString("drop").orEmpty(), "UTF-8",
                )
                val scheduledAt =
                    backStackEntry.arguments?.getString("scheduledAt")?.toLongOrNull()
                SharedRideMatchScreen(
                    pickup = pickup,
                    drop = drop,
                    scheduledAt = scheduledAt,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToActive = { navController.navigate(Screen.RideActive.route) },
                )
            }
            composable(Screen.RideActive.route) {
                ActiveRideScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToRating = { rideId ->
                        navController.navigate(Screen.RideRating.createRoute(rideId))
                    },
                )
            }
            composable(Screen.RideHistory.route) {
                RideHistoryScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToRating = { rideId ->
                        navController.navigate(Screen.RideRating.createRoute(rideId))
                    },
                )
            }
            composable(Screen.RideRating.route) { backStackEntry ->
                val rideId = backStackEntry.arguments?.getString("rideId") ?: ""
                RideRatingScreen(
                    rideId = rideId,
                    onNavigateBack = { navController.popBackStack() },
                )
            }
            composable(Screen.RideScheduled.route) {
                ScheduledRidesScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToHome = {
                        navController.navigate(Screen.RideSharing.route) {
                            popUpTo(Screen.RideScheduled.route) { inclusive = true }
                        }
                    },
                )
            }

            // Health
            composable(Screen.HealthHub.route) {
                HealthHubScreen(onNavigateBack = { navController.popBackStack() })
            }

            // Community (student-only)
            composable(Screen.CommunityHub.route) {
                CommunityHubScreen(
                    onNavigateToPostDetail = { postId ->
                        navController.navigate(Screen.PostDetail.createRoute(postId))
                    },
                    onNavigateToAddPost = { navController.navigate(Screen.AddPost.route) },
                )
            }
            composable(Screen.PostDetail.route) { backStackEntry ->
                val postId = backStackEntry.arguments?.getString("postId") ?: ""
                PostDetailScreen(
                    postId = postId,
                    onNavigateBack = { navController.popBackStack() },
                )
            }
            composable(Screen.AddPost.route) {
                AddPostScreen(onNavigateBack = { navController.popBackStack() })
            }

            // Lost & Found
            composable(Screen.LostFound.route) {
                LostFoundHubScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPost = { navController.navigate(Screen.LostFoundPost.route) },
                    onNavigateToDetail = { itemId ->
                        navController.navigate(Screen.LostFoundDetail.createRoute(itemId))
                    }
                )
            }
            composable(
                route = Screen.LostFoundDetail.route,
                arguments = listOf(navArgument("itemId") { type = NavType.StringType })
            ) { backStackEntry ->
                val itemId = backStackEntry.arguments?.getString("itemId") ?: ""
                LostFoundDetailScreen(
                    itemId = itemId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.LostFoundPost.route) {
                LostFoundPostScreen(onNavigateBack = { navController.popBackStack() })
            }

            // Finance
            composable(Screen.FinanceHub.route) {
                FinanceHubScreen(
                    onNavigateToAddExpense = { navController.navigate(Screen.AddExpense.route) },
                    onNavigateToAddSplitExpense = {
                        navController.navigate(Screen.AddSplitExpense.route)
                    },
                    onNavigateToBillSplitter = { navController.navigate(Screen.BillList.route) },
                )
            }
            composable(Screen.AddExpense.route) {
                AddExpenseScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.AddSplitExpense.route) {
                AddSplitExpenseScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.BillList.route) {
                BillListScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToCreate = { navController.navigate(Screen.BillCreate.createRoute(null)) },
                    onNavigateToDetail = { billId ->
                        navController.navigate(Screen.BillDetail.createRoute(billId))
                    },
                )
            }
            composable(
                route = Screen.BillCreate.route,
                arguments = listOf(
                    androidx.navigation.navArgument("userId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val prefilledUserId = backStackEntry.arguments?.getString("userId")
                CreateBillScreen(
                    onNavigateBack = { navController.popBackStack() },
                    prefilledUserId = prefilledUserId
                )
            }
            composable(Screen.BillDetail.route) { backStackEntry ->
                val billId = backStackEntry.arguments?.getString("billId") ?: ""
                BillDetailScreen(
                    billId = billId,
                    onNavigateBack = { navController.popBackStack() },
                )
            }

            // Roommate (student-only)
            composable(Screen.RoommateMatching.route) {
                RoommateBrowseScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToSetup = { navController.navigate(Screen.RoommateSetup.route) },
                    onNavigateToDetail = { id ->
                        navController.navigate(Screen.RoommateProfileDetail.createRoute(id))
                    },
                    onNavigateToConnections = {
                        navController.navigate(Screen.Connections.route)
                    },
                    onNavigateToGroups = { navController.navigate(Screen.RoommateGroups.route) },
                )
            }
            composable(Screen.RoommateSetup.route) {
                RoommateSetupScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.RoommateProfileDetail.route) { backStackEntry ->
                val targetId = backStackEntry.arguments?.getString("targetUserId") ?: ""
                RoommateProfileDetailScreen(
                    targetUserId = targetId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToChat = { chatId ->
                        navController.navigate(Screen.RoommateChat.createRoute(chatId))
                    },
                    onNavigateToCreateBill = { userId ->
                        navController.navigate(Screen.BillCreate.createRoute(userId))
                    }
                )
            }
            composable(Screen.Connections.route) {
                ConnectionsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToChat = { chatId ->
                        navController.navigate(Screen.RoommateChat.createRoute(chatId))
                    },
                    onNavigateToBrowse = { navController.navigate(Screen.RoommateMatching.route) },
                    onNavigateToCreateBill = { userId ->
                        navController.navigate(Screen.BillCreate.createRoute(userId))
                    }
                )
            }
            composable(Screen.RoommateGroups.route) {
                RoommateGroupsScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.RoommateChat.route) { backStackEntry ->
                val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
                RoommateChatScreen(
                    chatId = chatId,
                    onNavigateBack = { navController.popBackStack() },
                )
            }

            // Safety
            composable(Screen.SafetyHub.route) {
                SafetyHubScreen(
                    onNavigateToContacts = {
                        navController.navigate(Screen.EmergencyContacts.route)
                    },
                )
            }
            composable(Screen.EmergencyContacts.route) {
                EmergencyContactsScreen(onNavigateBack = { navController.popBackStack() })
            }

            // Buddy System
            composable(Screen.BuddyHome.route) {
                BuddyHomeScreen(
                    onNavigateToMatching = { navController.navigate(Screen.BuddyMatching.route) },
                    onNavigateToChat = { pairId ->
                        navController.navigate(Screen.BuddyChat.createRoute(pairId))
                    },
                )
            }
            composable(Screen.BuddyMatching.route) {
                BuddyMatchingScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.BuddyChat.route) { backStackEntry ->
                val pairId = backStackEntry.arguments?.getString("pairId") ?: ""
                BuddyChatScreen(
                    pairId = pairId,
                    onNavigateBack = { navController.popBackStack() },
                )
            }
            // Restaurant Suggestions
            composable(Screen.RestaurantBrowse.route) {
                RestaurantBrowseScreen(
                    onNavigateToDetail = { id -> navController.navigate(Screen.RestaurantDetail.createRoute(id)) }
                )
            }
            composable(
                route = Screen.RestaurantDetail.route,
                arguments = listOf(navArgument("restaurantId") { type = NavType.StringType }),
            ) { backStack ->
                val restaurantId = backStack.arguments?.getString("restaurantId") ?: ""
                RestaurantDetailScreen(
                    restaurantId = restaurantId,
                    onNavigateBack = { navController.popBackStack() },
                )
            }

            // Spending Tracker
            composable(Screen.SpendingDashboard.route) {
                SpendingDashboardScreen(
                    onNavigateToBudget = { navController.navigate(Screen.SpendingBudget.route) },
                    onNavigateToInsights = { navController.navigate(Screen.SpendingInsights.route) },
                )
            }
            composable(Screen.SpendingBudget.route) {
                BudgetScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.SpendingInsights.route) {
                InsightsScreen(onNavigateBack = { navController.popBackStack() })
            }
        } // end student_graph

        // ─────────────────────────────────────────────────────────────────────
        // PROVIDER NAV GRAPH — provider-exclusive routes (provider/ prefix)
        // ─────────────────────────────────────────────────────────────────────

        navigation(
            startDestination = Screen.ProviderDashboard.route,
            route = "provider_graph",
        ) {
            composable(Screen.ProviderDashboard.route) {
                ProviderDashboardScreen(
                    onSignOut = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onNavigateToAddListing = { type ->
                        navController.navigate(Screen.ProviderAddListing.createRoute(type))
                    },
                    onNavigateToEditListing = { listingId ->
                        navController.navigate(Screen.ProviderEditListing.createRoute(listingId))
                    },
                    onNavigateToInquiries = {
                        navController.navigate(Screen.ProviderInquiries.route)
                    },
                    onNavigateToChat = { inquiryId ->
                        navController.navigate(Screen.ProviderInquiryChat.createRoute(inquiryId))
                    },
                    onNavigateToVerification = {
                        navController.navigate(Screen.ProviderVerification.route)
                    },
                    onNavigateToSplash = {
                        navController.navigate(Screen.Splash.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onNavigateToEditProfile = {
                        navController.navigate(Screen.EditProfile.route)
                    }
                )
            }
            
            composable(Screen.ProviderVerification.route) {
                ProviderVerificationScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.ProviderAddListing.route,
                arguments = listOf(
                    navArgument("listingType") { type = NavType.StringType; defaultValue = "FLAT_PG_HOSTEL" },
                ),
            ) { backStack ->
                val listingType = backStack.arguments?.getString("listingType") ?: "FLAT_PG_HOSTEL"
                ProviderAddEditListingScreen(
                    listingType = listingType,
                    listingId = null,
                    onNavigateBack = { navController.popBackStack() },
                )
            }

            composable(
                route = Screen.ProviderEditListing.route,
                arguments = listOf(
                    navArgument("listingId") { type = NavType.StringType },
                ),
            ) { backStack ->
                val listingId = backStack.arguments?.getString("listingId") ?: ""
                ProviderAddEditListingScreen(
                    listingType = "FLAT_PG_HOSTEL", // overridden by ViewModel loading existing listing
                    listingId = listingId,
                    onNavigateBack = { navController.popBackStack() },
                )
            }

            composable(Screen.ProviderInquiries.route) {
                ProviderInquiriesScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToChat = { inquiryId ->
                        navController.navigate(Screen.ProviderInquiryChat.createRoute(inquiryId))
                    },
                )
            }

            composable(
                route = Screen.ProviderInquiryChat.route,
                arguments = listOf(
                    navArgument("inquiryId") { type = NavType.StringType },
                ),
            ) { backStack ->
                val inquiryId = backStack.arguments?.getString("inquiryId") ?: ""
                ProviderInquiryChatScreen(
                    inquiryId = inquiryId,
                    onNavigateBack = { navController.popBackStack() },
                )
            }
        } // end provider_graph
    }
}
