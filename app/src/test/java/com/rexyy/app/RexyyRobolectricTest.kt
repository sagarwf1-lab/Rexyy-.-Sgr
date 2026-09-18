package com.rexyy.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.rexyy.app.data.local.SecureStorage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
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
}
