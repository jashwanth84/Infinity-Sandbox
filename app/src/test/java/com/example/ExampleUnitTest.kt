package com.example

import com.example.domain.KeyVault
import com.example.util.JsonSanitizer
import com.example.util.SecretRedactor
import com.example.util.ZipExporter
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.zip.ZipFile

class ExampleUnitTest {

    @Test
    fun jsonSanitizer_stripsMarkdownCodeFences() {
        val markdownJson = "```json\n{\"files\": {\"app/build.gradle.kts\": \"// content\"}}\n```"
        val cleaned = JsonSanitizer.cleanJsonString(markdownJson)
        assertTrue(cleaned.startsWith("{"))
        assertTrue(cleaned.endsWith("}"))
        assertTrue(cleaned.contains("\"files\""))
    }

    @Test
    fun jsonSanitizer_extractsOuterBraces() {
        val wrappedText = "Here is the plan:\n{\"subtasks\": [{\"id\": \"s1\"}]}\nHope this helps!"
        val cleaned = JsonSanitizer.cleanJsonString(wrappedText)
        assertTrue(cleaned.startsWith("{"))
        assertTrue(cleaned.endsWith("}"))
        assertTrue(cleaned.contains("\"subtasks\""))
    }

    @Test
    fun secretRedactor_masksNvapiKeys() {
        val sample = "Connecting using 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 for model"
        val redacted = SecretRedactor.redact(sample)
        assertFalse(redacted.contains("nvapi-2FAFrxr"))
        assertTrue(redacted.contains("[REDACTED_API_KEY]"))
    }

    @Test
    fun secretRedactor_masksBearerHeaders() {
        val sample = "Authorization: Bearer nvapi-1234567890abcdef1234567890"
        val redacted = SecretRedactor.redact(sample)
        assertFalse(redacted.contains("nvapi-12345"))
        assertTrue(redacted.contains("Bearer [REDACTED_API_KEY]"))
    }

    @Test
    fun zipExporter_buildsValidZipFromMap() {
        val tempDir = File(System.getProperty("java.io.tmpdir"), "test_sandbox_exports")
        tempDir.mkdirs()

        val files = mapOf(
            "settings.gradle.kts" to "rootProject.name = \"TestApp\"",
            "app/src/main/AndroidManifest.xml" to "<manifest></manifest>"
        )

        val zipFile = ZipExporter.exportProjectToZip(files, tempDir)
        assertTrue(zipFile.exists())
        assertTrue(zipFile.length() > 0)

        val zip = ZipFile(zipFile)
        assertNotNull(zip.getEntry("settings.gradle.kts"))
        assertNotNull(zip.getEntry("app/src/main/AndroidManifest.xml"))
        zip.close()

        zipFile.delete()
    }

    @Test
    fun keyVault_resolvesAllSlots() {
        for (slot in KeyVault.Slot.values()) {
            val key = KeyVault.getKey(slot)
            assertNotNull(key)
            assertTrue(key.isNotEmpty())
            assertTrue(key.startsWith("nvapi-"))
        }
    }
}
