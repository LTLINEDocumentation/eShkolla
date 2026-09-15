package com.ltline.eshkolla.features.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.ltline.eshkolla.data.auth.ApiConfig
import com.ltline.eshkolla.domain.auth.AuthState

@Composable
fun LoginScreen(
    state: AuthState,
    onLogin: (String, String) -> Unit,
    onLoginSuccess: () -> Unit
) {
    var username by remember { mutableStateOf("admin") }
    var password by remember { mutableStateOf("") }
    var serverUrl by remember { mutableStateOf(ApiConfig.baseUrl) }

    if (state is AuthState.LoggedIn) {
        onLoginSuccess()
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("eShkolla", style = MaterialTheme.typography.headlineLarge)
        Text(
            "Hyr në sistem",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        )

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Përdoruesi") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Fjalëkalimi") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        )

        OutlinedTextField(
            value = serverUrl,
            onValueChange = {
                serverUrl = it
                ApiConfig.baseUrl = it
            },
            label = { Text("Adresa e serverit") },
            placeholder = { Text("http://10.0.2.2:8080") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        )

        Text(
            text = "Emulator: http://10.0.2.2:8080 • Telefon: http://IP-E-PC-së:8080",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 6.dp)
        )

        if (state is AuthState.Error) {
            Text(
                text = state.message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        Button(
            onClick = {
                ApiConfig.baseUrl = serverUrl.trim().trimEnd('/')
                onLogin(username, password)
            },
            enabled = state !is AuthState.Loading,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp)
        ) {
            if (state is AuthState.Loading) {
                CircularProgressIndicator(strokeWidth = 2.dp)
            } else {
                Text("Hyr")
            }
        }

        Text(
            text = "Demo: admin / 123456",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}
