package com.nestmate.app.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nestmate.app.ui.components.NestMatePrimaryButton
import com.nestmate.app.ui.components.NestMateTextButton
import com.nestmate.app.ui.components.GlassSurface
import com.nestmate.app.ui.components.GlowButton

@Composable
fun OnboardingScreen(
    onNavigateToRoleSelection: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    GlassSurface {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
        Text(
            text = "Welcome to NestMate",
            style = MaterialTheme.typography.displayLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "1. Find Verified Flats\n2. Discover Food & Services\n3. Connect with Community",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 32.dp),
        )

        GlowButton(
            text = "Get Started",
            onClick = {
                viewModel.completeOnboarding()
                onNavigateToRoleSelection()
            },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        )
        NestMateTextButton(
            text = "Skip",
            onClick = {
                viewModel.completeOnboarding()
                onNavigateToRoleSelection()
            },
        )
    }
    }
}
