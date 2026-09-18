package com.rexyy.app.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rexyy.app.network.NetworkResult
import com.rexyy.app.repository.AssistantRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    application: Application,
    private val repository: AssistantRepository
) : AndroidViewModel(application) {

    // Secondary constructor to ensure Java bytecode compatibility for AndroidViewModelFactory reflection
    constructor(application: Application) : this(
        application = application,
        repository = AssistantRepository(application)
    )

    private val _uiState = MutableStateFlow(
        ChatUiState(
            hasApiKey = repository.hasApiKey(),
            maskedApiKey = repository.getMaskedApiKey(),
            currentModel = repository.getSelectedModel()
        )
    )
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        loadConversationHistory()
    }

    private fun loadConversationHistory() {
        viewModelScope.launch {
            repository.getConversationHistory().collect { messageList ->
                _uiState.update { it.copy(messages = messageList) }
            }
        }
    }

    fun onInputChange(newText: String) {
        _uiState.update { it.copy(inputText = newText, errorMessage = null) }
    }

    fun sendMessage() {
        val currentText = _uiState.value.inputText.trim()
        if (currentText.isBlank() || _uiState.value.isLoading) return

        _uiState.update {
            it.copy(
                inputText = "",
                isLoading = true,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            val result = repository.sendMessage(currentText)
            when (result) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.userFriendlyMessage
                        )
                    }
                }
            }
        }
    }

    fun clearConversation() {
        viewModelScope.launch {
            repository.clearConversation()
        }
    }

    fun saveApiKey(apiKey: String) {
        repository.saveApiKey(apiKey)
        _uiState.update {
            it.copy(
                hasApiKey = true,
                maskedApiKey = repository.getMaskedApiKey()
            )
        }
    }

    fun clearApiKey() {
        repository.clearApiKey()
        _uiState.update {
            it.copy(
                hasApiKey = false,
                maskedApiKey = ""
            )
        }
    }

    fun updateModel(modelName: String) {
        repository.saveSelectedModel(modelName)
        _uiState.update { it.copy(currentModel = modelName) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as? Application)
                    ?: throw IllegalStateException("Application must be provided in CreationExtras to instantiate ChatViewModel")
                val repository = AssistantRepository(application)
                ChatViewModel(application, repository)
            }
        }
    }
}
