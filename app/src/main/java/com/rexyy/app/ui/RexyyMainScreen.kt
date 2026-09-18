package com.rexyy.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rexyy.app.ui.chat.ChatScreen
import com.rexyy.app.ui.chat.ChatViewModel
import com.rexyy.app.ui.navigation.Screen
import com.rexyy.app.ui.settings.SettingsScreen
import com.rexyy.app.ui.setup.ApiKeySetupScreen
import com.rexyy.app.ui.theme.RexyyDarkBackground

@Composable
fun RexyyMainScreen(
    viewModel: ChatViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    // Determine initial screen based on whether an API key already exists
    var currentScreen by remember(uiState.hasApiKey) {
        mutableStateOf(if (uiState.hasApiKey) Screen.Chat else Screen.Setup)
    }

    // Handle system back button when in Settings
    BackHandler(enabled = currentScreen is Screen.Settings) {
        currentScreen = if (uiState.hasApiKey) Screen.Chat else Screen.Setup
    }

    Surface(
        color = RexyyDarkBackground,
        modifier = modifier.fillMaxSize()
    ) {
        when (currentScreen) {
            is Screen.Setup -> {
                ApiKeySetupScreen(
                    onApiKeySaved = { key ->
                        viewModel.saveApiKey(key)
                        currentScreen = Screen.Chat
                    }
                )
            }
            is Screen.Chat -> {
                ChatScreen(
                    uiState = uiState,
                    onInputChange = { viewModel.onInputChange(it) },
                    onSendMessage = { viewModel.sendMessage() },
                    onClearChat = { viewModel.clearConversation() },
                    onSettingsClick = { currentScreen = Screen.Settings },
                    onDismissError = { viewModel.clearError() }
                )
            }
            is Screen.Settings -> {
                SettingsScreen(
                    currentMaskedKey = uiState.maskedApiKey,
                    currentModel = uiState.currentModel,
                    onBackClick = {
                        currentScreen = if (uiState.hasApiKey) Screen.Chat else Screen.Setup
                    },
                    onUpdateApiKey = { newKey ->
                        viewModel.saveApiKey(newKey)
                    },
                    onClearApiKey = {
                        viewModel.clearApiKey()
                        currentScreen = Screen.Setup
                    },
                    onUpdateModel = { newModel ->
                        viewModel.updateModel(newModel)
                    },
                    onClearAllHistory = {
                        viewModel.clearConversation()
                    }
                )
            }
        }
    }
}
