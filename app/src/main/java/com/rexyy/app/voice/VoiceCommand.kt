package com.rexyy.app.voice

import com.rexyy.app.network.provider.AiProviderType

sealed class VoiceCommand {
    data class OpenApp(val appName: String, val rawInput: String) : VoiceCommand()
    data class GoogleSearch(val query: String, val rawInput: String) : VoiceCommand()
    data class SetAlarm(val hour: Int, val minute: Int, val message: String, val rawInput: String) : VoiceCommand()
    data class OpenSettings(val rawInput: String) : VoiceCommand()
    data class BluetoothSettings(val turnOn: Boolean, val rawInput: String) : VoiceCommand()
    data class AdjustVolume(val raise: Boolean, val rawInput: String) : VoiceCommand()
    data class CallContact(val target: String, val rawInput: String) : VoiceCommand()
    data class SendMessage(val target: String, val body: String = "", val rawInput: String) : VoiceCommand()
    data class SetReminder(val title: String = "", val rawInput: String) : VoiceCommand()
    data class AiChat(val prompt: String, val providerOverride: AiProviderType? = null) : VoiceCommand()
}
