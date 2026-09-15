package com.ltline.eshkolla.features.management

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ltline.eshkolla.data.management.ApiManagementRepository
import com.ltline.eshkolla.data.management.ManagementClass
import com.ltline.eshkolla.data.management.ManagementTeacher
import com.ltline.eshkolla.data.management.ManagementUser
import com.ltline.eshkolla.domain.model.UserRole
import kotlinx.coroutines.launch

@Composable
fun ManagementScreen(role: UserRole, section: String, onBack: () -> Unit) {
    val repo = remember { ApiManagementRepository() }
    val scope = rememberCoroutineScope()
    var teachers by remember { mutableStateOf<List<ManagementTeacher>>(emptyList()) }
    var classes by remember { mutableStateOf<List<ManagementClass>>(emptyList()) }
    var users by remember { mutableStateOf<List<ManagementUser>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(section, role) {
        loading = true; error = null
        runCatching {
            when (section) {
                "teachers" -> teachers = repo.getTeachers()
                "classes" -> classes = repo.getClasses()
                "users" -> users = repo.getUsers()
            }
        }.onFailure { error = it.message ?: "Gabim gjatë ngarkimit." }
        loading = false
    }

    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Button(onClick = onBack) { Text("← Kthehu") }
        Text(sectionTitle(section), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(vertical = 16.dp))
        if (loading) CircularProgressIndicator()
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 12.dp)) }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            when (section) {
                "teachers" -> items(teachers) { t -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text(t.fullName, style=MaterialTheme.typography.titleLarge); Text("Përdoruesi: ${t.username}"); Text("Lënda: ${t.subjectId}"); Text("Klasa: ${t.classIds.ifEmpty { listOf("Pa caktim") }.joinToString()}") } } }
                "classes" -> items(classes) { c -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text(c.name, style=MaterialTheme.typography.titleLarge); Text("Klasa ${c.gradeLevel} • ${c.studentCount} nxënës"); Text("Mësimdhënës: ${c.teacherIds.ifEmpty { listOf("Pa caktim") }.joinToString()}") } } }
                "users" -> items(users) { u -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text(u.fullName, style=MaterialTheme.typography.titleLarge); Text("${u.username} • ${u.role}"); Text(if (u.active) "Aktiv" else "Joaktiv") } } }
            }
        }
    }
}

private fun sectionTitle(section: String) = when(section) {
    "teachers" -> "Mësimdhënësit"
    "classes" -> "Klasat"
    "users" -> "Përdoruesit"
    else -> "Menaxhimi"
}
