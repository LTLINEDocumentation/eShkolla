package com.ltline.eshkolla.features.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ltline.eshkolla.domain.model.User
import com.ltline.eshkolla.domain.model.UserRole

private data class DashboardModule(val title: String, val description: String)

@Composable
fun DashboardScreen(user: User?) {
    val role = user?.role ?: UserRole.NXENES
    val modules = modulesFor(role)

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Paneli kryesor", style = MaterialTheme.typography.headlineMedium)
            Text(
                text = "Mirë se vini${user?.let { ", ${it.fullName}" } ?: ""}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Roli", style = MaterialTheme.typography.labelLarge)
                    Text(roleLabel(role), style = MaterialTheme.typography.titleLarge)
                }
            }
        }
        items(modules) { module ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(module.title, style = MaterialTheme.typography.titleLarge)
                    Text(module.description, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}

private fun modulesFor(role: UserRole): List<DashboardModule> = when (role) {
    UserRole.ADMINISTRATOR -> listOf(
        DashboardModule("Përdoruesit", "Menaxhimi i llogarive dhe roleve."),
        DashboardModule("Shkolla", "Parametrat dhe të dhënat e institucionit."),
        DashboardModule("Klasat", "Klasat dhe organizimi i nxënësve."),
        DashboardModule("Mësimdhënësit", "Regjistri i stafit mësimor."),
        DashboardModule("Nxënësit", "Regjistri qendror i nxënësve."),
        DashboardModule("Lëndët", "Lëndët dhe ngarkesa javore.")
    )
    UserRole.DREJTOR -> listOf(
        DashboardModule("Klasat", "Pasqyra e klasave."),
        DashboardModule("Mësimdhënësit", "Stafi dhe ngarkesa."),
        DashboardModule("Nxënësit", "Lista dhe të dhënat."),
        DashboardModule("Orari", "Orari mësimor."),
        DashboardModule("Njoftimet", "Komunikimet e shkollës."),
        DashboardModule("Raportet", "Raporte dhe statistika.")
    )
    UserRole.MESIMDHENES -> listOf(
        DashboardModule("Klasat e mia", "Klasat që i mëson."),
        DashboardModule("Nxënësit", "Nxënësit sipas klasës."),
        DashboardModule("Notat", "Regjistrimi i vlerësimeve."),
        DashboardModule("Orari", "Orari yt mësimor."),
        DashboardModule("Njoftimet", "Njoftimet e shkollës.")
    )
    UserRole.NXENES -> listOf(
        DashboardModule("Orari", "Orari yt mësimor."),
        DashboardModule("Notat", "Notat dhe suksesi."),
        DashboardModule("Njoftimet", "Njoftimet e shkollës."),
        DashboardModule("Profili", "Të dhënat e profilit.")
    )
    UserRole.PRIND -> listOf(
        DashboardModule("Fëmijët", "Fëmijët e lidhur me llogarinë."),
        DashboardModule("Notat", "Suksesi dhe vlerësimet."),
        DashboardModule("Orari", "Orari i fëmijës."),
        DashboardModule("Njoftimet", "Komunikimet e shkollës."),
        DashboardModule("Profili", "Të dhënat e llogarisë.")
    )
}

private fun roleLabel(role: UserRole): String = when (role) {
    UserRole.ADMINISTRATOR -> "Administrator"
    UserRole.DREJTOR -> "Drejtor"
    UserRole.MESIMDHENES -> "Mësimdhënës"
    UserRole.NXENES -> "Nxënës"
    UserRole.PRIND -> "Prind"
}
