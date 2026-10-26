package com.example.data.remote

import android.util.Log
import com.example.domain.KeyVault
import com.example.util.SecretRedactor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiApiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "GeminiApiClient"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    /**
     * Generates a video from an image using gemini-omni-1.1-flash via the Interactions API.
     * Aspect ratio: "16:9" (landscape) or "9:16" (portrait).
     */
    suspend fun generateVideoFromImage(
        prompt: String,
        base64Image: String,
        aspectRatio: String = "16:9", // "16:9" or "9:16"
        onProgress: (String) -> Unit
    ): String? = withContext(Dispatchers.IO) {
        val apiKey = KeyVault.getGeminiApiKey().trim()
        if (apiKey.isBlank()) {
            throw IllegalStateException("Please provide GEMINI_API_KEY in the AI Studio Secrets panel to generate videos.")
        }

        onProgress("Submitting video animation request to gemini-omni-1.1-flash...")

        val requestObj = JSONObject().apply {
            put("model", "gemini-omni-1.1-flash")

            // Multimodal input or prompt
            val inputArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("type", "text")
                    put("text", prompt.ifBlank { "Animate this mobile UI screen into an interactive demo showcase with smooth motion transitions." })
                })
                put(JSONObject().apply {
                    put("mime_type", "image/jpeg")
                    put("data", base64Image)
                })
            }
            put("input", inputArray)
            put("background", false)
            put("store", true)
            put("stream", false)

            val respFormat = JSONObject().apply {
                put("type", "video")
                put("aspect_ratio", aspectRatio)
                put("duration", "5s")
            }
            put("response_format", respFormat)
        }

        val postRequest = Request.Builder()
            .url("$BASE_URL/interactions?key=$apiKey")
            .post(requestObj.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        val response = client.newCall(postRequest).execute()
        if (!response.isSuccessful) {
            val err = response.body?.string().orEmpty()
            response.close()
            throw IllegalStateException(SecretRedactor.redact("Gemini Interactions API error ($response.code): $err"))
        }

        val respStr = response.body?.string() ?: ""
        response.close()
        val initJson = JSONObject(respStr)
        val interactionId = initJson.getString("id")
        var status = initJson.optString("status", "in_progress")

        // Poll for completion (max 2 minutes)
        var attempts = 0
        while ((status == "in_progress" || status == "queued") && attempts < 24) {
            attempts++
            onProgress("Rendering video animation frames... (${attempts * 5}s)")
            delay(5000L)

            val getReq = Request.Builder()
                .url("$BASE_URL/interactions/$interactionId?key=$apiKey")
                .get()
                .build()

            val pollResp = client.newCall(getReq).execute()
            if (pollResp.isSuccessful) {
                val pollBody = pollResp.body?.string() ?: ""
                pollResp.close()
                val pollJson = JSONObject(pollBody)
                status = pollJson.optString("status", "")

                if (status == "completed" || status == "succeeded") {
                    // Extract video data
                    val steps = pollJson.optJSONArray("steps")
                    if (steps != null) {
                        for (i in 0 until steps.length()) {
                            val step = steps.getJSONObject(i)
                            if (step.optString("type") == "model_output") {
                                val contents = step.optJSONArray("content") ?: continue
                                for (j in 0 until contents.length()) {
                                    val item = contents.getJSONObject(j)
                                    val data = item.optString("data", "")
                                    if (data.isNotEmpty()) {
                                        return@withContext data
                                    }
                                    val uri = item.optString("uri", "")
                                    if (uri.isNotEmpty()) {
                                        return@withContext uri
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                pollResp.close()
            }
        }

        return@withContext null
    }

    /**
     * Direct Gemini Copilot assistant (gemini-3.8-flash) for rapid suggestions and code analysis.
     */
    suspend fun consultCopilot(prompt: String): String = withContext(Dispatchers.IO) {
        val apiKey = KeyVault.getGeminiApiKey()
        if (apiKey.isBlank()) {
            return@withContext "To use Gemini Copilot, configure GEMINI_API_KEY in the Secrets panel."
        }

        val requestObj = JSONObject().apply {
            val contents = JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            }
            put("contents", contents)
        }

        val req = Request.Builder()
            .url("$BASE_URL/models/gemini-3.8-flash:generateContent?key=$apiKey")
            .post(requestObj.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                return@withContext "Gemini Copilot Error: ${resp.code}"
            }
            val body = resp.body?.string().orEmpty()
            val json = JSONObject(body)
            val text = json.optJSONArray("candidates")
                ?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text")
            text ?: "No response from Gemini Copilot"
        }
    }
}
