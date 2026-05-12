package com.nestmate.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.nestmate.app.ui.navigation.RootNavGate
import com.nestmate.app.ui.theme.NestMateTheme
import com.nestmate.app.utils.SeedDataUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var auth: FirebaseAuth
    @Inject lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Seed Firestore with initial data once per install (non-blocking)
        lifecycleScope.launch {
            if (auth.currentUser != null) {
                SeedDataUtil.seedIfNeeded(firestore, auth, applicationContext)
            }
        }

        setContent {
            NestMateTheme {
                val navController = rememberNavController()
                RootNavGate(navController = navController)
            }
        }
    }
}
