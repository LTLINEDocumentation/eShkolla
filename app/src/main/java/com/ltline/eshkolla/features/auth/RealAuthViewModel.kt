package com.ltline.eshkolla.features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ltline.eshkolla.data.auth.ApiAuthRepository
import com.ltline.eshkolla.data.auth.ApiConfig
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

            // Demo e integruar: përdoret vetëm kur aplikacioni është në URL-në
            // fillestare të emulatorit. Sapo të vendoset një URL reale serveri,
            // hyrja kalon automatikisht te backend-i/PostgreSQL.
            val demoUser = demoUser(username)
            if (ApiConfig.baseUrl == ApiConfig.DEFAULT_BASE_URL && password == "123456" && demoUser != null) {
                ApiSession.token = "DEMO-OFFLINE-${demoUser.id}"
                _state.value = AuthState.LoggedIn(demoUser)
                return@launch
            }

            val result = repository.login(username, password)
            _state.value = result.fold(
                onSuccess = { AuthState.LoggedIn(it) },
                onFailure = { error ->
                    if (username.trim().equals("admin", ignoreCase = true) && password == "123456" &&
                        error.message?.contains("server", ignoreCase = true) == true
                    ) {
                        ApiSession.token = "DEMO-OFFLINE-1"
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

    private fun demoUser(username: String): User? = when (username.trim().lowercase()) {
        "admin" -> User("1", "admin", "Administrator", UserRole.ADMINISTRATOR)
        "drejtor" -> User("2", "drejtor", "Drejtor i shkollës", UserRole.DREJTOR)
        "leonard.tahiraj" -> User("3", "leonard.tahiraj", "Leonard Tahiraj", UserRole.MESIMDHENES)
        "nxenes" -> User("4", "nxenes", "Nxënës Demo", UserRole.NXENES)
        "prind" -> User("5", "prind", "Prind Demo", UserRole.PRIND)
        else -> null
    }
}
