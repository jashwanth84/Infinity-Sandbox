package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.JsonSanitizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Infinity Sandbox", appName)
    }

    @Test
    fun `jsonSanitizer parses real json with robolectric shadows`() {
        val markdownJson = "```json\n{\"files\": {\"app/build.gradle.kts\": \"// content\"}}\n```"
        val parsed = JsonSanitizer.parseJsonObject(markdownJson)
        assertTrue(parsed.has("files"))

        val malformed = "{\"ok\": true, \"issues\": [],}"
        val repaired = JsonSanitizer.parseJsonObject(malformed)
        assertTrue(repaired.getBoolean("ok"))
    }
}
