package com.rexyy.app.ui.chat

import com.rexyy.app.model.ChatMessage

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val currentModel: String = "gpt-4o-mini",
    val maskedApiKey: String = "",
    val hasApiKey: Boolean = false
)
