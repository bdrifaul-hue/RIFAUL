package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.ui.screens.AssistantScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AssistantViewModel

enum class CurrentScreen {
    ASSISTANT,
    SETTINGS
}

class MainActivity : ComponentActivity() {

    private val assistantViewModel: AssistantViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Mandatory Firebase App Check initialization before setContent or ViewModels
        configureAppCheck(this, intent)

        // STEP 2: Full bleed edge-to-edge
        enableEdgeToEdge()

        // STEP 3: Compose UI
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainNavigation(viewModel = assistantViewModel)
                }
            }
        }
    }
}

@Composable
fun MainNavigation(viewModel: AssistantViewModel) {
    var currentScreen by remember { mutableStateOf(CurrentScreen.ASSISTANT) }

    when (currentScreen) {
        CurrentScreen.ASSISTANT -> {
            AssistantScreen(
                viewModel = viewModel,
                onNavigateToSettings = { currentScreen = CurrentScreen.SETTINGS }
            )
        }
        CurrentScreen.SETTINGS -> {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { currentScreen = CurrentScreen.ASSISTANT }
            )
        }
    }
}
