package com.rexyy.app.voice

sealed class VoiceCommandResult {
    data class Handled(val replyText: String, val openIntentSuccess: Boolean = true) : VoiceCommandResult()
    data class ForwardToAi(val prompt: String) : VoiceCommandResult()
    data class RequiresConfirmation(
        val prompt: String,
        val commandToExecute: VoiceCommand
    ) : VoiceCommandResult()
    data class Error(val errorMessage: String) : VoiceCommandResult()
}
