package com.rexyy.app.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rexyy.app.network.NetworkResult
import com.rexyy.app.network.provider.AiProviderType
import com.rexyy.app.repository.AssistantRepository
import com.rexyy.app.voice.VoiceCommand
import com.rexyy.app.voice.VoiceCommandExecutor
import com.rexyy.app.voice.VoiceCommandParser
import com.rexyy.app.voice.VoiceCommandResult
import com.rexyy.app.voice.VoiceInputManager
import com.rexyy.app.voice.VoiceState
import com.rexyy.app.voice.VoiceTtsManager
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

    private val voiceInputManager = VoiceInputManager(
        context = application,
        onListeningStateChanged = { listening ->
            _uiState.update {
                it.copy(
                    voiceState = if (listening) VoiceState.LISTENING else if (it.voiceState == VoiceState.LISTENING) VoiceState.IDLE else it.voiceState,
                    voiceStatusMessage = if (listening) "Listening... Speak now" else null
                )
            }
        },
        onSpeechRecognized = { spokenText ->
            handleVoiceInput(spokenText)
        },
        onError = { errorMsg ->
            _uiState.update {
                it.copy(
                    voiceState = VoiceState.IDLE,
                    voiceStatusMessage = null,
                    errorMessage = errorMsg
                )
            }
        }
    )

    private val voiceTtsManager = VoiceTtsManager(
        context = application,
        onSpeakingStateChanged = { speaking ->
            _uiState.update {
                it.copy(
                    voiceState = if (speaking) VoiceState.SPEAKING else if (it.voiceState == VoiceState.SPEAKING) VoiceState.IDLE else it.voiceState,
                    voiceStatusMessage = if (speaking) "REXYY is speaking..." else null
                )
            }
        }
    )

    private val _uiState = MutableStateFlow(
        ChatUiState(
            hasApiKey = repository.hasApiKey(),
            maskedApiKey = repository.getMaskedApiKey(),
            selectedProvider = repository.getSelectedProvider(),
            currentModel = repository.getSelectedModel(),
            openAiModel = repository.getOpenAiModel(),
            geminiModel = repository.getGeminiModel(),
            maskedOpenAiApiKey = repository.getMaskedOpenAiApiKey(),
            maskedGeminiApiKey = repository.getMaskedGeminiApiKey(),
            isAutoFallbackEnabled = repository.isAutoFallbackEnabled(),
            isVoiceCommandsEnabled = repository.isVoiceCommandsEnabled(),
            isVoiceRepliesEnabled = repository.isVoiceRepliesEnabled(),
            voiceLanguage = repository.getVoiceLanguage()
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

        executeAiMessage(currentText, isSpokenResponseRequested = false)
    }

    private fun executeAiMessage(
        promptText: String,
        isSpokenResponseRequested: Boolean,
        providerOverride: AiProviderType? = null
    ) {
        viewModelScope.launch {
            val result = repository.sendMessage(promptText, providerOverride)
            when (result) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            voiceState = if (isSpokenResponseRequested && it.isVoiceRepliesEnabled) VoiceState.SPEAKING else VoiceState.IDLE,
                            voiceStatusMessage = null
                        )
                    }
                    if (isSpokenResponseRequested && _uiState.value.isVoiceRepliesEnabled) {
                        voiceTtsManager.speak(result.data.content, _uiState.value.voiceLanguage)
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            voiceState = VoiceState.IDLE,
                            voiceStatusMessage = null,
                            errorMessage = result.userFriendlyMessage
                        )
                    }
                }
            }
        }
    }

    /**
     * Entry point for speech recognition results:
     * Parses the command and either executes an Android action or passes to selected AI model.
     */
    private fun handleVoiceInput(recognizedText: String) {
        val trimmed = recognizedText.trim()
        if (trimmed.isBlank()) {
            _uiState.update { it.copy(voiceState = VoiceState.IDLE, voiceStatusMessage = null) }
            return
        }

        _uiState.update {
            it.copy(
                inputText = trimmed,
                voiceState = VoiceState.PROCESSING,
                voiceStatusMessage = "Processing: \"$trimmed\""
            )
        }

        // If voice commands mode is enabled, evaluate command patterns
        if (_uiState.value.isVoiceCommandsEnabled) {
            val command = VoiceCommandParser.parse(trimmed)
            when (command) {
                is VoiceCommand.AiChat -> {
                    // Standard conversational prompt -> Route through AI chat
                    _uiState.update { it.copy(inputText = "", isLoading = true) }
                    executeAiMessage(command.prompt, isSpokenResponseRequested = true, providerOverride = command.providerOverride)
                }
                else -> {
                    // Local device command -> Execute intent and record in chat history
                    viewModelScope.launch {
                        val result = VoiceCommandExecutor.execute(command, getApplication())
                        when (result) {
                            is VoiceCommandResult.Handled -> {
                                repository.recordCommandInteraction(trimmed, result.replyText, isError = false)
                                _uiState.update {
                                    it.copy(
                                        inputText = "",
                                        voiceState = if (it.isVoiceRepliesEnabled) VoiceState.SPEAKING else VoiceState.IDLE,
                                        voiceStatusMessage = null
                                    )
                                }
                                if (_uiState.value.isVoiceRepliesEnabled) {
                                    voiceTtsManager.speak(result.replyText, _uiState.value.voiceLanguage)
                                }
                            }
                            is VoiceCommandResult.ForwardToAi -> {
                                _uiState.update { it.copy(inputText = "", isLoading = true) }
                                executeAiMessage(result.prompt, isSpokenResponseRequested = true, providerOverride = result.providerOverride)
                            }
                            is VoiceCommandResult.RequiresConfirmation -> {
                                _uiState.update {
                                    it.copy(
                                        voiceState = VoiceState.IDLE,
                                        voiceStatusMessage = null
                                    )
                                }
                            }
                            is VoiceCommandResult.Error -> {
                                repository.recordCommandInteraction(trimmed, result.errorMessage, isError = true)
                                _uiState.update {
                                    it.copy(
                                        inputText = "",
                                        voiceState = VoiceState.IDLE,
                                        voiceStatusMessage = null,
                                        errorMessage = result.errorMessage
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Voice commands disabled: Treat all voice input as conversational AI query
            _uiState.update { it.copy(inputText = "", isLoading = true) }
            executeAiMessage(trimmed, isSpokenResponseRequested = true)
        }
    }

    fun startVoiceInput() {
        if (voiceTtsManager.isSpeaking()) {
            voiceTtsManager.stop()
        }
        if (!voiceInputManager.isAvailable()) {
            _uiState.update {
                it.copy(errorMessage = "Speech recognition is not available on this device.")
            }
            return
        }
        voiceInputManager.startListening(_uiState.value.voiceLanguage)
    }

    fun stopVoiceInput() {
        voiceInputManager.stopListening()
    }

    fun cancelVoiceInput() {
        voiceInputManager.cancel()
        _uiState.update { it.copy(voiceState = VoiceState.IDLE, voiceStatusMessage = null) }
    }

    fun stopSpeaking() {
        voiceTtsManager.stop()
        _uiState.update { it.copy(voiceState = VoiceState.IDLE, voiceStatusMessage = null) }
    }

    fun onMicrophonePermissionDenied() {
        _uiState.update {
            it.copy(
                voiceState = VoiceState.IDLE,
                voiceStatusMessage = null,
                errorMessage = "Microphone permission is required to use voice input and voice commands."
            )
        }
    }

    fun setVoiceCommandsEnabled(enabled: Boolean) {
        repository.setVoiceCommandsEnabled(enabled)
        _uiState.update { it.copy(isVoiceCommandsEnabled = enabled) }
    }

    fun setVoiceRepliesEnabled(enabled: Boolean) {
        repository.setVoiceRepliesEnabled(enabled)
        _uiState.update { it.copy(isVoiceRepliesEnabled = enabled) }
        if (!enabled) {
            voiceTtsManager.stop()
        }
    }

    fun setVoiceLanguage(language: String) {
        repository.setVoiceLanguage(language)
        _uiState.update { it.copy(voiceLanguage = language) }
        voiceTtsManager.applyLanguage(language)
    }

    fun clearConversation() {
        viewModelScope.launch {
            repository.clearConversation()
        }
    }

    // --- Provider and Key Configuration Actions ---

    fun selectProvider(provider: AiProviderType) {
        repository.setSelectedProvider(provider)
        _uiState.update {
            it.copy(
                selectedProvider = provider,
                currentModel = repository.getSelectedModel(),
                hasApiKey = repository.hasApiKey(),
                maskedApiKey = repository.getMaskedApiKey()
            )
        }
    }

    fun updateOpenAiApiKey(apiKey: String) {
        repository.saveOpenAiApiKey(apiKey)
        _uiState.update {
            it.copy(
                hasApiKey = repository.hasApiKey(),
                maskedApiKey = repository.getMaskedApiKey(),
                maskedOpenAiApiKey = repository.getMaskedOpenAiApiKey()
            )
        }
    }

    fun clearOpenAiApiKey() {
        repository.clearOpenAiApiKey()
        _uiState.update {
            it.copy(
                hasApiKey = repository.hasApiKey(),
                maskedApiKey = repository.getMaskedApiKey(),
                maskedOpenAiApiKey = ""
            )
        }
    }

    fun updateGeminiApiKey(apiKey: String) {
        repository.saveGeminiApiKey(apiKey)
        _uiState.update {
            it.copy(
                hasApiKey = repository.hasApiKey(),
                maskedApiKey = repository.getMaskedApiKey(),
                maskedGeminiApiKey = repository.getMaskedGeminiApiKey()
            )
        }
    }

    fun clearGeminiApiKey() {
        repository.clearGeminiApiKey()
        _uiState.update {
            it.copy(
                hasApiKey = repository.hasApiKey(),
                maskedApiKey = repository.getMaskedApiKey(),
                maskedGeminiApiKey = ""
            )
        }
    }

    fun updateOpenAiModel(model: String) {
        repository.saveOpenAiModel(model)
        _uiState.update {
            it.copy(
                openAiModel = model,
                currentModel = if (it.selectedProvider == AiProviderType.OPENAI) model else it.currentModel
            )
        }
    }

    fun updateGeminiModel(model: String) {
        repository.saveGeminiModel(model)
        _uiState.update {
            it.copy(
                geminiModel = model,
                currentModel = if (it.selectedProvider == AiProviderType.GEMINI) model else it.currentModel
            )
        }
    }

    fun setAutoFallbackEnabled(enabled: Boolean) {
        repository.setAutoFallbackEnabled(enabled)
        _uiState.update { it.copy(isAutoFallbackEnabled = enabled) }
    }

    fun saveApiKey(apiKey: String) {
        repository.saveApiKey(apiKey)
        _uiState.update {
            it.copy(
                hasApiKey = true,
                maskedApiKey = repository.getMaskedApiKey(),
                maskedOpenAiApiKey = repository.getMaskedOpenAiApiKey(),
                maskedGeminiApiKey = repository.getMaskedGeminiApiKey()
            )
        }
    }

    fun clearApiKey() {
        repository.clearApiKey()
        _uiState.update {
            it.copy(
                hasApiKey = repository.hasApiKey(),
                maskedApiKey = repository.getMaskedApiKey(),
                maskedOpenAiApiKey = repository.getMaskedOpenAiApiKey(),
                maskedGeminiApiKey = repository.getMaskedGeminiApiKey()
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

    override fun onCleared() {
        super.onCleared()
        voiceInputManager.destroy()
        voiceTtsManager.shutdown()
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
