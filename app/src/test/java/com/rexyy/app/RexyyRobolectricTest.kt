package com.rexyy.app

import android.app.Application
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import com.rexyy.app.data.local.SecureStorage
import com.rexyy.app.repository.AssistantRepository
import com.rexyy.app.ui.chat.ChatViewModel
import com.rexyy.app.voice.VoiceCommand
import com.rexyy.app.voice.VoiceCommandParser
import com.rexyy.app.voice.VoiceState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RexyyRobolectricTest {

    @Test
    fun testAppNameResource() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("REXYY", appName)
    }

    @Test
    fun testSecureStorage() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val storage = SecureStorage(context)

        assertFalse(storage.hasApiKey())
        storage.saveApiKey("sk-test-sample-api-key-12345")
        assertTrue(storage.hasApiKey())
        assertEquals("sk-test-sample-api-key-12345", storage.getApiKey())

        storage.clearApiKey()
        assertFalse(storage.hasApiKey())
    }

    @Test
    fun testSecureStorageVoicePreferences() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val storage = SecureStorage(context)

        // Defaults
        assertTrue(storage.isVoiceCommandsEnabled())
        assertTrue(storage.isVoiceRepliesEnabled())
        assertEquals(SecureStorage.VOICE_LANG_DEFAULT, storage.getVoiceLanguage())

        // Updates
        storage.setVoiceCommandsEnabled(false)
        assertFalse(storage.isVoiceCommandsEnabled())

        storage.setVoiceRepliesEnabled(false)
        assertFalse(storage.isVoiceRepliesEnabled())

        storage.setVoiceLanguage(SecureStorage.VOICE_LANG_HI)
        assertEquals(SecureStorage.VOICE_LANG_HI, storage.getVoiceLanguage())
    }

    @Test
    fun testVoiceCommandParserAppsAndActions() {
        // YouTube open
        val ytEnglish = VoiceCommandParser.parse("Open YouTube")
        assertTrue(ytEnglish is VoiceCommand.OpenApp)
        assertEquals("YouTube", (ytEnglish as VoiceCommand.OpenApp).appName)

        val ytHindi = VoiceCommandParser.parse("YouTube kholo")
        assertTrue(ytHindi is VoiceCommand.OpenApp)
        assertEquals("YouTube", (ytHindi as VoiceCommand.OpenApp).appName)

        // Settings
        val settingsEn = VoiceCommandParser.parse("Open settings")
        assertTrue(settingsEn is VoiceCommand.OpenSettings)

        val settingsHi = VoiceCommandParser.parse("Settings kholo")
        assertTrue(settingsHi is VoiceCommand.OpenSettings)

        // Bluetooth
        val btOn = VoiceCommandParser.parse("Turn on bluetooth")
        assertTrue(btOn is VoiceCommand.BluetoothSettings)
        assertTrue((btOn as VoiceCommand.BluetoothSettings).turnOn)

        val btHi = VoiceCommandParser.parse("Bluetooth on karo")
        assertTrue(btHi is VoiceCommand.BluetoothSettings)

        // Volume
        val volUp = VoiceCommandParser.parse("Volume badhao")
        assertTrue(volUp is VoiceCommand.AdjustVolume)
        assertTrue((volUp as VoiceCommand.AdjustVolume).raise)

        val volDown = VoiceCommandParser.parse("Decrease volume")
        assertTrue(volDown is VoiceCommand.AdjustVolume)
        assertFalse((volDown as VoiceCommand.AdjustVolume).raise)
    }

    @Test
    fun testVoiceCommandParserAlarm() {
        val alarm1 = VoiceCommandParser.parse("Set an alarm for 7:30 AM")
        assertTrue(alarm1 is VoiceCommand.SetAlarm)
        val cmd1 = alarm1 as VoiceCommand.SetAlarm
        assertEquals(7, cmd1.hour)
        assertEquals(30, cmd1.minute)

        val alarm2 = VoiceCommandParser.parse("Alarm 8 baje lagao")
        assertTrue(alarm2 is VoiceCommand.SetAlarm)
        val cmd2 = alarm2 as VoiceCommand.SetAlarm
        assertEquals(8, cmd2.hour)
    }

    @Test
    fun testVoiceCommandParserSearchAndChat() {
        val search = VoiceCommandParser.parse("Search Google for Android updates")
        assertTrue(search is VoiceCommand.GoogleSearch)
        assertTrue((search as VoiceCommand.GoogleSearch).query.contains("Android updates"))

        // Conversational queries should map to AiChat
        val chat1 = VoiceCommandParser.parse("What is the weather today?")
        assertTrue(chat1 is VoiceCommand.AiChat)

        val chat2 = VoiceCommandParser.parse("Tell me a funny joke")
        assertTrue(chat2 is VoiceCommand.AiChat)
    }

    @Test
    fun testChatViewModelCreationWithFactory() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val viewModel = ViewModelProvider(activity, ChatViewModel.Factory)[ChatViewModel::class.java]
        assertNotNull(viewModel)
        assertNotNull(viewModel.uiState.value)
        assertEquals(VoiceState.IDLE, viewModel.uiState.value.voiceState)
        assertTrue(viewModel.uiState.value.isVoiceCommandsEnabled)
        assertTrue(viewModel.uiState.value.isVoiceRepliesEnabled)
    }

    @Test
    fun testChatViewModelDirectApplicationConstructor() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val constructor = ChatViewModel::class.java.getConstructor(Application::class.java)
        assertNotNull(constructor)
        val viewModel = constructor.newInstance(app)
        assertNotNull(viewModel)
        assertEquals(VoiceState.IDLE, viewModel.uiState.value.voiceState)
    }

    @Test
    fun testChatViewModelFullConstructorInjection() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val repository = AssistantRepository(app)
        val viewModel = ChatViewModel(app, repository)
        assertNotNull(viewModel)

        // Test Voice Settings in ViewModel
        viewModel.setVoiceCommandsEnabled(false)
        assertFalse(viewModel.uiState.value.isVoiceCommandsEnabled)

        viewModel.setVoiceRepliesEnabled(false)
        assertFalse(viewModel.uiState.value.isVoiceRepliesEnabled)

        viewModel.setVoiceLanguage(SecureStorage.VOICE_LANG_HI)
        assertEquals(SecureStorage.VOICE_LANG_HI, viewModel.uiState.value.voiceLanguage)
    }
}
