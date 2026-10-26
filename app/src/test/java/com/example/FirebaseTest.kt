package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.diagnostics.FirebaseInitializer
import com.google.firebase.FirebaseApp
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FirebaseTest {

    @Test
    fun testFirebaseInitializationAndOptions() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val initialized = FirebaseInitializer.init(context)
        assertTrue("Firebase must initialize successfully", initialized)

        val app = FirebaseApp.getInstance()
        assertNotNull("FirebaseApp default instance must exist", app)

        val options = app.options
        assertEquals("cinestream-sulo", options.projectId)
        assertEquals("1:979447256418:android:ea5b570266ae1aa6a9883b", options.applicationId)
        assertEquals("AIzaSyC9CvaM9Mw3NiD-KsOiYvHZHj6XZJQJnPs", options.apiKey)
        assertFalse("API key must not be placeholder or remixed", options.apiKey.contains("remix", ignoreCase = true))
    }
}
