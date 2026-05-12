package com.nestmate.app.ui.navigation

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.nestmate.app.data.repository.AuthSessionState

private val AUTH_ROUTES = setOf(
    Screen.Splash.route,
    Screen.Onboarding.route,
    Screen.Login.route,
    Screen.Register.route,
    Screen.RoleSelection.route,
    Screen.CollegeVerification.route,
)

/**
 * Persistent auth + role gate for the root navigation host.
 *
 * States:
 *  • Unauthenticated + on a protected route → redirect to Login (clears stack)
 *  • Authenticated + userRole == null        → show full-screen loading indicator
 *    (Firestore role fetch in-flight; prevents flash to wrong screen)
 *  • Authenticated + userRole == "service_provider" → navigate to ProviderDashboard
 *  • Authenticated + userRole == "student" (or any other) → navigate to StudentHome
 */
@Composable
fun RootNavGate(
    navController: NavHostController,
    viewModel: RootAuthViewModel = hiltViewModel(),
) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val userRole by viewModel.userRole.collectAsStateWithLifecycle()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    LaunchedEffect(authState, userRole, currentRoute) {
        val state = authState ?: return@LaunchedEffect

        when {
            // Signed-out user on a protected screen → send to Login
            state is AuthSessionState.Unauthenticated &&
                    currentRoute != null &&
                    currentRoute !in AUTH_ROUTES -> {
                navController.navigate(Screen.Login.route) {
                    popUpTo(0) { inclusive = true }
                }
            }

            // Authenticated + role loaded → navigate to the correct home
            state is AuthSessionState.Authenticated && userRole != null -> {
                val target = if (userRole == "service_provider")
                    Screen.ProviderDashboard.route
                else
                    Screen.StudentHome.route

                // Only navigate if we're not already there (prevents re-triggering)
                if (currentRoute == Screen.Splash.route ||
                    currentRoute == Screen.Login.route ||
                    currentRoute == Screen.CollegeVerification.route
                ) {
                    navController.navigate(target) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }

            // Authenticated but userRole is still null → loading; do nothing here.
            // The NavGraph shows the loading composable for this case (see below).
        }
    }

    // Render the nav graph. The loading overlay is shown when auth is confirmed but
    // role has not yet been resolved — this is handled as an overlay.
    NestMateNavGraph(navController = navController)

    if (authState is AuthSessionState.Authenticated && userRole == null) {
        // Full-screen loading indicator on the dark navy background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    }
}
