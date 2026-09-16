package com.ltline.eshkolla.features.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ltline.eshkolla.domain.model.User

@Composable
fun ProfileScreen(
    user: User?,
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Profili", style = MaterialTheme.typography.headlineMedium)

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(user?.fullName ?: "Përdorues", style = MaterialTheme.typography.titleLarge)
                Text("Përdoruesi: ${user?.username.orEmpty()}")
                Text("Roli: ${user?.role?.name.orEmpty()}")
                Text("Statusi: ${if (user?.isActive == true) "Aktiv" else "Joaktiv"}")
            }
        }

        Button(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
            Text("Dil nga llogaria")
        }
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Kthehu")
        }
    }
}
