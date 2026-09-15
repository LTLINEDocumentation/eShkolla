package com.ltline.eshkolla.features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ltline.eshkolla.data.auth.ApiAuthRepository
import com.ltline.eshkolla.domain.auth.AuthState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RealAuthViewModel : ViewModel() {
    private val repository = ApiAuthRepository()
    private val _state = MutableStateFlow<AuthState>(AuthState.LoggedOut)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            _state.value = repository.login(username, password).fold(
                onSuccess = { AuthState.LoggedIn(it) },
                onFailure = { AuthState.Error(it.message ?: "Gabim gjatë hyrjes.") }
            )
        }
    }

    fun logout() {
        viewModelScope.launch { repository.logout(); _state.value = AuthState.LoggedOut }
    }
}
