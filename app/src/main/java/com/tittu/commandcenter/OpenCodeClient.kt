package com.tittu.commandcenter

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class OpenCodeClient(
    private val baseUrl: String = "http://127.0.0.1:4096"
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun health(): Result<String> = ioRequest {
        val request = Request.Builder().url("$baseUrl/global/health").get().build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("OpenCode returned HTTP ${response.code}")
            response.body?.string().orEmpty().ifBlank { "OpenCode is responding." }
        }
    }

    suspend fun createSession(): Result<String> = ioRequest {
        val body = "{}".toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url("$baseUrl/session").post(body).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Session creation failed: HTTP ${response.code}")
            response.body?.string().orEmpty()
        }
    }

    fun extractSessionId(raw: String): String = runCatching {
        JSONObject(raw).optString("id").takeIf { it.isNotBlank() }
            ?: JSONObject(raw).optJSONObject("info")?.optString("id").orEmpty()
    }.getOrDefault("")

    suspend fun sendPrompt(sessionId: String, prompt: String): Result<String> = ioRequest {
        require(prompt.isNotBlank()) { "Prompt cannot be empty." }
        val json = JSONObject().put(
            "parts",
            JSONArray().put(JSONObject().put("type", "text").put("text", prompt))
        )
        val body = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$baseUrl/session/$sessionId/message")
            .post(body)
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Prompt failed: HTTP ${response.code}")
            val raw = response.body?.string().orEmpty()
            extractAssistantText(raw).ifBlank { "OpenCode completed the request but returned no displayable text." }
        }
    }

    /**
     * OpenCode message responses include metadata and multiple part types.
     * Only user-facing text parts are rendered; reasoning and lifecycle events
     * must never leak into the chat transcript.
     */
    internal fun extractAssistantText(raw: String): String {
        val root = runCatching { JSONObject(raw) }.getOrNull() ?: return raw
        val parts = root.optJSONArray("parts")
            ?: root.optJSONObject("message")?.optJSONArray("parts")
            ?: root.optJSONObject("info")?.optJSONArray("parts")
            ?: return root.optString("text").ifBlank { "" }

        val visible = buildList {
            for (index in 0 until parts.length()) {
                val part = parts.optJSONObject(index) ?: continue
                if (part.optString("type") == "text") {
                    val text = part.optString("text")
                    if (text.isNotBlank()) add(text)
                }
            }
        }
        return visible.joinToString("\n\n")
    }

    private suspend fun <T> ioRequest(block: () -> T): Result<T> =
        withContext(Dispatchers.IO) { runCatching(block) }
}
