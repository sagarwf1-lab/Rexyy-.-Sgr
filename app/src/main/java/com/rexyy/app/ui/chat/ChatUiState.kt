package com.rexyy.app.ui.chat

import com.rexyy.app.data.local.SecureStorage
import com.rexyy.app.model.ChatMessage
import com.rexyy.app.voice.VoiceState

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val currentModel: String = "gpt-4o-mini",
    val maskedApiKey: String = "",
    val hasApiKey: Boolean = false,
    val voiceState: VoiceState = VoiceState.IDLE,
    val isVoiceCommandsEnabled: Boolean = true,
    val isVoiceRepliesEnabled: Boolean = true,
    val voiceLanguage: String = SecureStorage.VOICE_LANG_DEFAULT,
    val voiceStatusMessage: String? = null
)
