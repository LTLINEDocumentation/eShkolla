package com.ltline.eshkolla.features.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DashboardScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Paneli kryesor",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Mirë se vini në eShkolla",
            style = MaterialTheme.typography.titleMedium
        )

        Card {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Njoftimet", style = MaterialTheme.typography.titleLarge)
                Text("Moduli i njoftimeve do të aktivizohet në fazën tjetër.")
            }
        }

        Card {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Mësimet", style = MaterialTheme.typography.titleLarge)
                Text("Moduli i orarit dhe mësimeve do të ndërtohet më tej.")
            }
        }

        Card {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Profili", style = MaterialTheme.typography.titleLarge)
                Text("Profili do të lidhet me rolin e përdoruesit.")
            }
        }
    }
}
