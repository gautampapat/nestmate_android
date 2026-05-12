package com.nestmate.app

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.nestmate.app.data.repository.AuthRepository
import com.nestmate.app.data.repository.AuthSessionState
import com.nestmate.app.data.repository.UserActivityRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class NestMateApplication : Application(), DefaultLifecycleObserver {

    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var userActivityRepository: UserActivityRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastStoppedAt: Long = 0L

    override fun onCreate() {
        super<Application>.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        // Cold-start trigger: when auth state becomes Authenticated, touch lastActiveAt.
        // Fires once on cold start if already logged in, and again on any future sign-in.
        appScope.launch {
            authRepository.observeAuthState()
                .filterIsInstance<AuthSessionState.Authenticated>()
                .distinctUntilChangedBy { it.uid }
                .collect { state ->
                    userActivityRepository.touchLastActive(state.uid)
                }
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        lastStoppedAt = System.currentTimeMillis()
    }

    override fun onStart(owner: LifecycleOwner) {
        // Skip the first foreground transition on cold start — handled by the auth observer.
        if (lastStoppedAt == 0L) return
        val now = System.currentTimeMillis()
        if (now - lastStoppedAt > BACKGROUND_THRESHOLD_MS) {
            val uid = authRepository.getCurrentUserId() ?: return
            appScope.launch { userActivityRepository.touchLastActive(uid) }
        }
    }

    companion object {
        private const val BACKGROUND_THRESHOLD_MS = 30L * 60L * 1000L
    }
}
