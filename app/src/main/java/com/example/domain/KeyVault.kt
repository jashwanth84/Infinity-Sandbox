package com.example.domain

import com.example.BuildConfig
import com.example.util.SecretRedactor

/**
 * Manages access to model inference API keys.
 * Keys are loaded from BuildConfig (configured via .env / Secrets Gradle Plugin)
 * and automatically registered with SecretRedactor so they are never exposed in UI or logs.
 */
object KeyVault {

    enum class Slot {
        KEY1,
        KEY2,
        KEY3,
        KEY4,
        KEY5
    }

    // Default backup keys provided in environment configuration
    private const val FALLBACK_KEY1 = "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"
    private const val FALLBACK_KEY2 = "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"
    private const val FALLBACK_KEY3 = "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"
    private const val FALLBACK_KEY4 = "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"
    private const val FALLBACK_KEY5 = "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"

    init {
        // Register fallback keys with secret redactor immediately
        SecretRedactor.registerSecret(FALLBACK_KEY1)
        SecretRedactor.registerSecret(FALLBACK_KEY2)
        SecretRedactor.registerSecret(FALLBACK_KEY3)
        SecretRedactor.registerSecret(FALLBACK_KEY4)
        SecretRedactor.registerSecret(FALLBACK_KEY5)

        // Register any BuildConfig keys if present
        safeRegister(runCatching { BuildConfig.NV_KEY1 }.getOrNull())
        safeRegister(runCatching { BuildConfig.NV_KEY2 }.getOrNull())
        safeRegister(runCatching { BuildConfig.NV_KEY3 }.getOrNull())
        safeRegister(runCatching { BuildConfig.NV_KEY4 }.getOrNull())
        safeRegister(runCatching { BuildConfig.NV_KEY5 }.getOrNull())
        safeRegister(runCatching { BuildConfig.GEMINI_API_KEY }.getOrNull())
    }

    private fun safeRegister(key: String?) {
        if (!key.isNullOrBlank()) {
            SecretRedactor.registerSecret(key.trim().removeSurrounding("\""))
        }
    }

    /**
     * Resolves the API key for a given key slot.
     */
    fun getKey(slot: Slot): String {
        val candidate = when (slot) {
            Slot.KEY1 -> runCatching { BuildConfig.NV_KEY1 }.getOrNull()?.takeIf { it.isNotBlank() } ?: FALLBACK_KEY1
            Slot.KEY2 -> runCatching { BuildConfig.NV_KEY2 }.getOrNull()?.takeIf { it.isNotBlank() } ?: FALLBACK_KEY2
            Slot.KEY3 -> runCatching { BuildConfig.NV_KEY3 }.getOrNull()?.takeIf { it.isNotBlank() } ?: FALLBACK_KEY3
            Slot.KEY4 -> runCatching { BuildConfig.NV_KEY4 }.getOrNull()?.takeIf { it.isNotBlank() } ?: FALLBACK_KEY4
            Slot.KEY5 -> runCatching { BuildConfig.NV_KEY5 }.getOrNull()?.takeIf { it.isNotBlank() } ?: FALLBACK_KEY5
        }
        val clean = candidate.trim().removeSurrounding("\"")
        SecretRedactor.registerSecret(clean)
        return clean
    }

    /**
     * Retrieves Gemini API key if present.
     */
    fun getGeminiApiKey(): String {
        val key = runCatching { BuildConfig.GEMINI_API_KEY }.getOrNull() ?: ""
        return key.trim().removeSurrounding("\"")
    }
}
