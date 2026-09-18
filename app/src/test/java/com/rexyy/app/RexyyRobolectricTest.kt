package com.rexyy.app

import android.app.Application
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import com.rexyy.app.data.local.SecureStorage
import com.rexyy.app.repository.AssistantRepository
import com.rexyy.app.ui.chat.ChatViewModel
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
    fun testChatViewModelCreationWithFactory() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val viewModel = ViewModelProvider(activity, ChatViewModel.Factory)[ChatViewModel::class.java]
        assertNotNull(viewModel)
        assertNotNull(viewModel.uiState.value)
    }

    @Test
    fun testChatViewModelDirectApplicationConstructor() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        // Verify reflection getConstructor(Application::class.java) succeeds without NoSuchMethodException
        val constructor = ChatViewModel::class.java.getConstructor(Application::class.java)
        assertNotNull(constructor)
        val viewModel = constructor.newInstance(app)
        assertNotNull(viewModel)
    }

    @Test
    fun testChatViewModelFullConstructorInjection() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val repository = AssistantRepository(app)
        val viewModel = ChatViewModel(app, repository)
        assertNotNull(viewModel)
    }
}
