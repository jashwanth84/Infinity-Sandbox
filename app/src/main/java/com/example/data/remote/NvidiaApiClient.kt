package com.example.data.remote

import android.util.Log
import com.example.domain.AgentConfig
import com.example.domain.KeyVault
import com.example.util.SecretRedactor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

sealed class AgentStreamChunk {
    data class Content(val text: String) : AgentStreamChunk()
    data class Thinking(val text: String) : AgentStreamChunk()
}

class NvidiaApiClient {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(90, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(90, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    companion object {
        private const val TAG = "NvidiaApiClient"
        private const val API_URL = "https://integrate.api.nvidia.com/v1/chat/completions"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    /**
     * Executes a streaming chat completion with automatic retry and model fallback.
     */
    suspend fun streamChatCompletion(
        config: AgentConfig,
        systemPrompt: String,
        userPrompt: String,
        base64Image: String? = null,
        onChunk: (AgentStreamChunk) -> Unit
    ): String = withContext(Dispatchers.IO) {
        // Attempt 1 with primary config
        try {
            return@withContext executeStream(config, systemPrompt, userPrompt, base64Image, onChunk)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val safeMsg = SecretRedactor.redact(e.message ?: "Unknown error")
            Log.w(TAG, "Attempt 1 failed for model ${config.model}: $safeMsg. Retrying once...")
            delay(1200L)

            // Attempt 2 (retry once)
            try {
                return@withContext executeStream(config, systemPrompt, userPrompt, base64Image, onChunk)
            } catch (e2: CancellationException) {
                throw e2
            } catch (e2: Exception) {
                val safeMsg2 = SecretRedactor.redact(e2.message ?: "Unknown error")
                Log.w(TAG, "Attempt 2 failed: $safeMsg2. Falling back to glm-5.3-flash with key5...")

                // Fallback to glm-5.3-flash with key5
                try {
                    return@withContext executeStream(
                        AgentConfig.FALLBACK_CONFIG,
                        systemPrompt,
                        userPrompt,
                        base64Image = null, // vision fallback to text description
                        onChunk = onChunk
                    )
                } catch (e3: Exception) {
                    val safeMsg3 = SecretRedactor.redact(e3.message ?: "Fallback failed")
                    throw IllegalStateException("Model execution failed after retry and fallback: $safeMsg3")
                }
            }
        }
    }

    private fun executeStream(
        config: AgentConfig,
        systemPrompt: String,
        userPrompt: String,
        base64Image: String?,
        onChunk: (AgentStreamChunk) -> Unit
    ): String {
        val apiKey = KeyVault.getKey(config.slot)

        val rootJson = JSONObject().apply {
            put("model", config.model)
            put("temperature", config.temperature)
            put("top_p", config.topP)
            put("max_tokens", config.maxTokens)
            put("stream", true)

            if (config.enableThinking || config.model.contains("nemotron")) {
                val templateKwargs = JSONObject().apply {
                    put("enable_thinking", true)
                }
                put("chat_template_kwargs", templateKwargs)
            }

            val messagesArray = JSONArray()

            // System message
            if (systemPrompt.isNotBlank()) {
                val sysObj = JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                }
                messagesArray.put(sysObj)
            }

            // User message
            val userObj = JSONObject().apply {
                put("role", "user")

                if (!base64Image.isNullOrBlank()) {
                    // Vision multimodal payload
                    val partsArray = JSONArray().apply {
                        put(JSONObject().apply {
                            put("type", "text")
                            put("text", userPrompt)
                        })
                        put(JSONObject().apply {
                            put("type", "image_url")
                            put("image_url", JSONObject().apply {
                                put("url", "data:image/jpeg;base64,$base64Image")
                            })
                        })
                    }
                    put("content", partsArray)
                } else {
                    put("content", userPrompt)
                }
            }
            messagesArray.put(userObj)

            put("messages", messagesArray)
        }

        val requestBody = rootJson.toString().toRequestBody(JSON_MEDIA_TYPE)

        val request = Request.Builder()
            .url(API_URL)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "text/event-stream")
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            val errorBody = response.body?.string().orEmpty()
            val safeError = SecretRedactor.redact("HTTP ${response.code}: $errorBody")
            response.close()
            throw IllegalStateException(safeError)
        }

        val body = response.body ?: throw IllegalStateException("Empty response body from NVIDIA API")
        val accumulatedContent = StringBuilder()

        try {
            BufferedReader(InputStreamReader(body.byteStream(), StandardCharsets.UTF_8)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val rawLine = line?.trim() ?: continue
                    if (rawLine.isEmpty()) continue
                    if (!rawLine.startsWith("data:")) continue

                    val jsonPayload = rawLine.removePrefix("data:").trim()
                    if (jsonPayload.isEmpty()) continue
                    if (jsonPayload == "[DONE]") break

                    try {
                        val chunkObj = JSONObject(jsonPayload)
                        val choices = chunkObj.optJSONArray("choices") ?: continue
                        if (choices.length() == 0) continue

                        val firstChoice = choices.getJSONObject(0)
                        val delta = firstChoice.optJSONObject("delta") ?: continue

                        // Extract reasoning / thinking content if present
                        val reasoning = delta.optString("reasoning_content", "")
                        if (reasoning.isNotEmpty()) {
                            onChunk(AgentStreamChunk.Thinking(reasoning))
                        }

                        // Extract main content chunk
                        val content = delta.optString("content", "")
                        if (content.isNotEmpty()) {
                            accumulatedContent.append(content)
                            onChunk(AgentStreamChunk.Content(content))
                        }
                    } catch (e: Exception) {
                        // Skip malformed chunk line
                    }
                }
            }
        } finally {
            body.close()
            response.close()
        }

        return accumulatedContent.toString()
    }

    // --- Role-Specific Agent Request Methods ---

    suspend fun sendOrchestratorRequest(
        userPrompt: String,
        existingFilesSummary: String = "",
        onChunk: (AgentStreamChunk) -> Unit = {}
    ): String {
        val config = com.example.domain.AgentConfig.forAgent(com.example.domain.AgentType.ORCHESTRATOR)
        val fullPrompt = if (existingFilesSummary.isNotBlank()) {
            "User Goal: $userPrompt\nExisting Files:\n$existingFilesSummary\nPlan modifications and subtasks."
        } else {
            "User Goal: $userPrompt\nPlan the project subtasks."
        }
        return streamChatCompletion(config, com.example.domain.Prompts.ORCHESTRATOR, fullPrompt, onChunk = onChunk)
    }

    suspend fun sendResearcherRequest(
        goal: String,
        onChunk: (AgentStreamChunk) -> Unit = {}
    ): String {
        val config = com.example.domain.AgentConfig.forAgent(com.example.domain.AgentType.RESEARCHER)
        return streamChatCompletion(config, com.example.domain.Prompts.RESEARCHER, goal, onChunk = onChunk)
    }

    suspend fun sendCoderRequest(
        prompt: String,
        onChunk: (AgentStreamChunk) -> Unit = {}
    ): String {
        val config = com.example.domain.AgentConfig.forAgent(com.example.domain.AgentType.CODER)
        return streamChatCompletion(config, com.example.domain.Prompts.CODER, prompt, onChunk = onChunk)
    }

    suspend fun sendVisionRequest(
        prompt: String,
        base64Image: String,
        onChunk: (AgentStreamChunk) -> Unit = {}
    ): String {
        val config = com.example.domain.AgentConfig.forAgent(com.example.domain.AgentType.VISION)
        return streamChatCompletion(config, com.example.domain.Prompts.VISION, prompt, base64Image = base64Image, onChunk = onChunk)
    }

    suspend fun sendDebuggerRequest(
        codeFilesSummary: String,
        onChunk: (AgentStreamChunk) -> Unit = {}
    ): String {
        val config = com.example.domain.AgentConfig.forAgent(com.example.domain.AgentType.DEBUGGER)
        return streamChatCompletion(config, com.example.domain.Prompts.DEBUGGER, "Review these files:\n$codeFilesSummary", onChunk = onChunk)
    }

    suspend fun sendHelperRequest(
        codeFilesSummary: String,
        onChunk: (AgentStreamChunk) -> Unit = {}
    ): String {
        val config = com.example.domain.AgentConfig.forAgent(com.example.domain.AgentType.HELPER)
        return streamChatCompletion(config, com.example.domain.Prompts.HELPER, "Generate tests and README for:\n$codeFilesSummary", onChunk = onChunk)
    }

    suspend fun sendMergerRequest(
        allFilesSummary: String,
        onChunk: (AgentStreamChunk) -> Unit = {}
    ): String {
        val config = com.example.domain.AgentConfig.forAgent(com.example.domain.AgentType.MERGER)
        return streamChatCompletion(config, com.example.domain.Prompts.MERGER, "Merge all files into a complete Android project:\n$allFilesSummary", onChunk = onChunk)
    }
}
