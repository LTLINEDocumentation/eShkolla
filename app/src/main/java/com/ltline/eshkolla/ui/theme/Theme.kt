package com.ltline.eshkolla.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val EShkollaColors = lightColorScheme()

@Composable
fun EShkollaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EShkollaColors,
        content = content
    )
}
