package com.rexyy.app.network.provider

enum class AiProviderType(val id: String, val displayName: String) {
    OPENAI("openai", "OpenAI"),
    GEMINI("gemini", "Google Gemini");

    val isGemini: Boolean get() = this == GEMINI
    val isOpenAi: Boolean get() = this == OPENAI

    companion object {
        fun fromId(id: String?): AiProviderType {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: OPENAI
        }
    }
}
