package com.ltline.eshkolla.features.dashboard

import androidx.compose.foundation.clickable
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

private data class DashboardModule(val key: String, val title: String, val description: String)

@Composable
fun DashboardScreen(
    user: User?,
    onModuleClick: (String) -> Unit = {}
) {
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
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onModuleClick(module.key) }
            ) {
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
        DashboardModule("users", "Përdoruesit", "Menaxhimi i llogarive dhe roleve."),
        DashboardModule("school", "Shkolla", "Parametrat dhe të dhënat e institucionit."),
        DashboardModule("classes", "Klasat", "Klasat dhe organizimi i nxënësve."),
        DashboardModule("teachers", "Mësimdhënësit", "Regjistri i stafit mësimor."),
        DashboardModule("students", "Nxënësit", "Regjistri qendror i nxënësve."),
        DashboardModule("subjects", "Lëndët", "Lëndët dhe ngarkesa javore.")
    )
    UserRole.DREJTOR -> listOf(
        DashboardModule("classes", "Klasat", "Pasqyra e klasave."),
        DashboardModule("teachers", "Mësimdhënësit", "Stafi dhe ngarkesa."),
        DashboardModule("students", "Nxënësit", "Lista dhe të dhënat."),
        DashboardModule("schedule", "Orari", "Orari mësimor."),
        DashboardModule("announcements", "Njoftimet", "Komunikimet e shkollës."),
        DashboardModule("reports", "Raportet", "Raporte dhe statistika.")
    )
    UserRole.MESIMDHENES -> listOf(
        DashboardModule("classes", "Klasat e mia", "Klasat që i mëson."),
        DashboardModule("students", "Nxënësit", "Nxënësit sipas klasës."),
        DashboardModule("grades", "Notat", "Regjistrimi i vlerësimeve."),
        DashboardModule("schedule", "Orari", "Orari yt mësimor."),
        DashboardModule("announcements", "Njoftimet", "Njoftimet e shkollës.")
    )
    UserRole.NXENES -> listOf(
        DashboardModule("schedule", "Orari", "Orari yt mësimor."),
        DashboardModule("grades", "Notat", "Notat dhe suksesi."),
        DashboardModule("announcements", "Njoftimet", "Njoftimet e shkollës."),
        DashboardModule("profile", "Profili", "Të dhënat e profilit.")
    )
    UserRole.PRIND -> listOf(
        DashboardModule("children", "Fëmijët", "Fëmijët e lidhur me llogarinë."),
        DashboardModule("grades", "Notat", "Suksesi dhe vlerësimet."),
        DashboardModule("schedule", "Orari", "Orari i fëmijës."),
        DashboardModule("announcements", "Njoftimet", "Komunikimet e shkollës."),
        DashboardModule("profile", "Profili", "Të dhënat e llogarisë.")
    )
}

private fun roleLabel(role: UserRole): String = when (role) {
    UserRole.ADMINISTRATOR -> "Administrator"
    UserRole.DREJTOR -> "Drejtor"
    UserRole.MESIMDHENES -> "Mësimdhënës"
    UserRole.NXENES -> "Nxënës"
    UserRole.PRIND -> "Prind"
}
