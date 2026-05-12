package com.nestmate.app.data.repository

sealed class AuthSessionState {
    data object Unauthenticated : AuthSessionState()
    data class Authenticated(val uid: String) : AuthSessionState()
}
