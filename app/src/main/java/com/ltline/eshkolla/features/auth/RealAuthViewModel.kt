package com.ltline.eshkolla.features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ltline.eshkolla.data.auth.ApiAuthRepository
import com.ltline.eshkolla.data.auth.ApiSession
import com.ltline.eshkolla.domain.auth.AuthState
import com.ltline.eshkolla.domain.model.User
import com.ltline.eshkolla.domain.model.UserRole
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
            val result = repository.login(username, password)
            _state.value = result.fold(
                onSuccess = { AuthState.LoggedIn(it) },
                onFailure = { error ->
                    // Lejojmë hyrjen demo vetëm kur serveri nuk është i arritshëm.
                    // Kjo e bën APK-në të testueshme edhe pa backend të publikuar,
                    // ndërsa kur serveri është aktiv përdoret autentikimi real.
                    if (username.trim().equals("admin", ignoreCase = true) && password == "123456" &&
                        error.message?.contains("server", ignoreCase = true) == true
                    ) {
                        ApiSession.token = "DEMO-OFFLINE"
                        AuthState.LoggedIn(User("1", "admin", "Administrator", UserRole.ADMINISTRATOR))
                    } else {
                        AuthState.Error(error.message ?: "Gabim gjatë hyrjes.")
                    }
                }
            )
        }
    }

    fun logout() {
        viewModelScope.launch { repository.logout(); ApiSession.token = null; _state.value = AuthState.LoggedOut }
    }
}
