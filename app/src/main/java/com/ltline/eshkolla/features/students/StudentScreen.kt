package com.ltline.eshkolla.features.students

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun StudentScreen(
    viewModel: StudentViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Button(onClick = onBack) { Text("← Paneli") }
        Text(
            "Nxënësit",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        when (val current = state) {
            StudentListState.Loading -> Text("Duke ngarkuar…")
            is StudentListState.Error -> Text(current.message, color = MaterialTheme.colorScheme.error)
            is StudentListState.Success -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(current.students) { student ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(student.fullName, style = MaterialTheme.typography.titleLarge)
                                Text("ID: ${student.id}")
                                Text("Klasa: ${student.classId}")
                                Text("Statusi: ${if (student.isActive) "Aktiv" else "Joaktiv"}")
                            }
                        }
                    }
                }
            }
        }
    }
}
