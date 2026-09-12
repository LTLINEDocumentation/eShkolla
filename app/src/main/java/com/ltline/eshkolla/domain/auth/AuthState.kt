package com.ltline.eshkolla.domain.auth

import com.ltline.eshkolla.domain.model.User

sealed interface AuthState {
    data object LoggedOut : AuthState
    data object Loading : AuthState
    data class LoggedIn(val user: User) : AuthState
    data class Error(val message: String) : AuthState
}
