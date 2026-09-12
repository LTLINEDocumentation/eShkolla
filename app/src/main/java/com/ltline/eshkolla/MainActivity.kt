package com.ltline.eshkolla

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ltline.eshkolla.app.EShkollaApp
import com.ltline.eshkolla.ui.theme.EShkollaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EShkollaTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    EShkollaApp()
                }
            }
        }
    }
}
