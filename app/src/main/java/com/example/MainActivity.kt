package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.ProjectViewerScreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.InfinitySandboxTheme
import com.example.ui.viewmodel.SandboxViewModel

enum class CurrentScreen {
    CHAT,
    PROJECT_VIEWER
}

class MainActivity : ComponentActivity() {

    private val viewModel: SandboxViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()
            val projectFiles by viewModel.projectFiles.collectAsState()
            var currentScreen by remember { mutableStateOf(CurrentScreen.CHAT) }

            // Handle system back button to exit project viewer
            BackHandler(enabled = currentScreen != CurrentScreen.CHAT) {
                currentScreen = CurrentScreen.CHAT
            }

            InfinitySandboxTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    when (currentScreen) {
                        CurrentScreen.CHAT -> {
                            ChatScreen(
                                viewModel = viewModel,
                                onOpenProjectViewer = {
                                    currentScreen = CurrentScreen.PROJECT_VIEWER
                                }
                            )
                        }
                        CurrentScreen.PROJECT_VIEWER -> {
                            ProjectViewerScreen(
                                files = projectFiles,
                                onBack = {
                                    currentScreen = CurrentScreen.CHAT
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
