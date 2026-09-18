package com.rexyy.app.repository

import android.content.Context
import com.rexyy.app.data.local.AppDatabase
import com.rexyy.app.data.local.ChatMessageEntity
import com.rexyy.app.data.local.SecureStorage
import com.rexyy.app.model.ChatMessage
import com.rexyy.app.model.MessageSender
import com.rexyy.app.network.ApiClientFactory
import com.rexyy.app.network.ApiMessage
import com.rexyy.app.network.ChatCompletionRequest
import com.rexyy.app.network.NetworkResult
import com.rexyy.app.network.OpenAiApi
import com.rexyy.app.utils.NetworkUtils
import com.rexyy.app.utils.SecurityUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class AssistantRepository(
    private val context: Context,
    private val secureStorage: SecureStorage = SecureStorage(context),
    private val database: AppDatabase = AppDatabase.getInstance(context)
) {
    private val chatDao = database.chatMessageDao()

    @Volatile
    private var openAiApi: OpenAiApi = ApiClientFactory.createOpenAiApi(secureStorage.getBaseUrl())

    fun refreshApiBaseUrl() {
        openAiApi = ApiClientFactory.createOpenAiApi(secureStorage.getBaseUrl())
    }

    fun getConversationHistory(): Flow<List<ChatMessage>> {
        return chatDao.getAllMessages().map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun clearConversation() = withContext(Dispatchers.IO) {
        chatDao.clearAllMessages()
    }

    fun hasApiKey(): Boolean = secureStorage.hasApiKey()

    fun getMaskedApiKey(): String {
        val rawKey = secureStorage.getApiKey()
        return SecurityUtils.maskApiKey(rawKey)
    }

    fun saveApiKey(apiKey: String) {
        secureStorage.saveApiKey(apiKey)
    }

    fun clearApiKey() {
        secureStorage.clearApiKey()
    }

    fun getSelectedModel(): String = secureStorage.getSelectedModel()

    fun saveSelectedModel(model: String) {
        secureStorage.saveSelectedModel(model)
    }

    fun getBaseUrl(): String = secureStorage.getBaseUrl()

    fun saveBaseUrl(url: String) {
        secureStorage.saveBaseUrl(url)
        refreshApiBaseUrl()
    }

    fun isVoiceCommandsEnabled(): Boolean = secureStorage.isVoiceCommandsEnabled()
    fun setVoiceCommandsEnabled(enabled: Boolean) = secureStorage.setVoiceCommandsEnabled(enabled)

    fun isVoiceRepliesEnabled(): Boolean = secureStorage.isVoiceRepliesEnabled()
    fun setVoiceRepliesEnabled(enabled: Boolean) = secureStorage.setVoiceRepliesEnabled(enabled)

    fun getVoiceLanguage(): String = secureStorage.getVoiceLanguage()
    fun setVoiceLanguage(language: String) = secureStorage.setVoiceLanguage(language)

    /**
     * Records a local voice command interaction directly into local Room database history:
     * 1. Inserts the user's spoken command.
     * 2. Inserts REXYY's confirmation or action reply.
     */
    suspend fun recordCommandInteraction(userText: String, replyText: String, isError: Boolean = false): ChatMessage = withContext(Dispatchers.IO) {
        val userEntity = ChatMessageEntity(
            content = userText.trim(),
            sender = MessageSender.USER.name,
            timestamp = System.currentTimeMillis()
        )
        chatDao.insertMessage(userEntity)

        val assistantEntity = ChatMessageEntity(
            content = replyText.trim(),
            sender = MessageSender.ASSISTANT.name,
            timestamp = System.currentTimeMillis() + 1,
            isError = isError
        )
        val id = chatDao.insertMessage(assistantEntity)
        assistantEntity.copy(id = id).toDomain()
    }

    /**
     * Sends a user message to the AI Assistant:
     * 1. Saves user message in local Room DB.
     * 2. Checks network availability.
     * 3. Calls AI completions endpoint with conversation context.
     * 4. Saves AI response in local Room DB.
     * 5. Returns NetworkResult.
     */
    suspend fun sendMessage(userText: String): NetworkResult<ChatMessage> = withContext(Dispatchers.IO) {
        val apiKey = secureStorage.getApiKey()
        if (apiKey.isNullOrBlank()) {
            return@withContext NetworkResult.Error(
                userFriendlyMessage = "No API key configured. Please add your key in Settings or the setup screen.",
                errorType = NetworkResult.ErrorType.INVALID_API_KEY
            )
        }

        // 1. Insert user message locally
        val userEntity = ChatMessageEntity(
            content = userText.trim(),
            sender = MessageSender.USER.name,
            timestamp = System.currentTimeMillis()
        )
        chatDao.insertMessage(userEntity)

        // 2. Check network connectivity
        if (!NetworkUtils.isNetworkAvailable(context)) {
            val errorMsg = "No internet connection detected. Please connect to Wi-Fi or mobile data and try again."
            val errorEntity = ChatMessageEntity(
                content = errorMsg,
                sender = MessageSender.ASSISTANT.name,
                timestamp = System.currentTimeMillis(),
                isError = true
            )
            chatDao.insertMessage(errorEntity)
            return@withContext NetworkResult.Error(
                userFriendlyMessage = errorMsg,
                errorType = NetworkResult.ErrorType.NO_INTERNET
            )
        }

        // 3. Build context from recent messages
        val existingHistory = chatDao.getAllMessages().first()
        val contextMessages = mutableListOf<ApiMessage>()
        
        // System prompt establishing REXYY persona
        contextMessages.add(
            ApiMessage(
                role = "system",
                content = "You are REXYY, an intelligent, helpful, and concise native Android AI assistant. " +
                        "Provide clear, accurate, and beautifully structured responses."
            )
        )

        // Take last 10 messages for conversational context
        val recentHistory = existingHistory.takeLast(10)
        for (item in recentHistory) {
            val role = when (item.sender) {
                MessageSender.USER.name -> "user"
                MessageSender.ASSISTANT.name -> "assistant"
                else -> "system"
            }
            if (!item.isError && item.content.isNotBlank()) {
                contextMessages.add(ApiMessage(role = role, content = item.content))
            }
        }

        val request = ChatCompletionRequest(
            model = secureStorage.getSelectedModel(),
            messages = contextMessages
        )

        val formattedAuthHeader = if (apiKey.startsWith("Bearer ", ignoreCase = true)) {
            apiKey
        } else {
            "Bearer $apiKey"
        }

        try {
            val response = openAiApi.createChatCompletion(
                authorization = formattedAuthHeader,
                request = request
            )

            if (response.isSuccessful) {
                val body = response.body()
                val replyContent = body?.choices?.firstOrNull()?.message?.content?.trim()

                if (!replyContent.isNullOrBlank()) {
                    val assistantEntity = ChatMessageEntity(
                        content = replyContent,
                        sender = MessageSender.ASSISTANT.name,
                        timestamp = System.currentTimeMillis(),
                        isError = false
                    )
                    val insertedId = chatDao.insertMessage(assistantEntity)
                    val domainResult = assistantEntity.copy(id = insertedId).toDomain()
                    NetworkResult.Success(domainResult)
                } else {
                    val errorMsg = "The AI model returned an empty response. Please try rephrasing your request."
                    val errorEntity = ChatMessageEntity(
                        content = errorMsg,
                        sender = MessageSender.ASSISTANT.name,
                        timestamp = System.currentTimeMillis(),
                        isError = true
                    )
                    chatDao.insertMessage(errorEntity)
                    NetworkResult.Error(
                        userFriendlyMessage = errorMsg,
                        errorType = NetworkResult.ErrorType.MALFORMED_RESPONSE
                    )
                }
            } else {
                val errorBodyStr = try {
                    response.errorBody()?.string()
                } catch (_: Exception) {
                    null
                }
                val errorResult = ApiClientFactory.mapHttpError(response.code(), errorBodyStr)
                val errorEntity = ChatMessageEntity(
                    content = errorResult.userFriendlyMessage,
                    sender = MessageSender.ASSISTANT.name,
                    timestamp = System.currentTimeMillis(),
                    isError = true
                )
                chatDao.insertMessage(errorEntity)
                errorResult
            }
        } catch (e: Exception) {
            val mappedError = ApiClientFactory.mapExceptionToError(e)
            val errorEntity = ChatMessageEntity(
                content = mappedError.userFriendlyMessage,
                sender = MessageSender.ASSISTANT.name,
                timestamp = System.currentTimeMillis(),
                isError = true
            )
            chatDao.insertMessage(errorEntity)
            mappedError
        }
    }
}
