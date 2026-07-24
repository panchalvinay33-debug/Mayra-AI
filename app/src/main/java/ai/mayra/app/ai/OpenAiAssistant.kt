package ai.mayra.app.ai

import ai.mayra.app.core.AssistantIntent
import ai.mayra.app.core.AssistantIntentEngine
import ai.mayra.app.core.LocalMayraAssistant
import ai.mayra.app.core.MayraAssistant
import ai.mayra.app.core.MayraMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class HybridMayraAssistant(
    private val providerStore: AiProviderSettingsStore,
    private val localAssistant: MayraAssistant = LocalMayraAssistant(),
    private val intentEngine: AssistantIntentEngine = AssistantIntentEngine(),
    private val remoteFactory: (AiProviderConfig, String) -> MayraAssistant = { config, key ->
        OpenAiResponsesAssistant(apiKey = key, model = config.model)
    }
) : MayraAssistant {
    override suspend fun reply(message: String, conversation: List<MayraMessage>): Result<String> {
        val intent = intentEngine.parse(message.trim())
        if (intent !is AssistantIntent.Chat) return localAssistant.reply(message, conversation)

        val config = providerStore.read()
        val key = providerStore.apiKey()
        if (!config.onlineEnabled || key.isNullOrBlank()) {
            return localAssistant.reply(message, conversation)
        }

        return remoteFactory(config, key).reply(message, conversation)
            .recoverCatching {
                localAssistant.reply(message, conversation).getOrThrow() +
                    "\n\nOnline AI was unavailable, so I answered with Mayra's offline brain."
            }
    }
}

class OpenAiResponsesAssistant(
    private val apiKey: String,
    private val model: String,
    private val endpoint: String = RESPONSES_ENDPOINT,
    private val connectionFactory: (URL) -> HttpURLConnection = { url ->
        url.openConnection() as HttpURLConnection
    }
) : MayraAssistant {
    override suspend fun reply(message: String, conversation: List<MayraMessage>): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val input = JSONArray()
                input.put(
                    JSONObject()
                        .put("role", "system")
                        .put(
                            "content",
                            "You are Mayra, a warm, concise personal AI assistant for an Indian user. " +
                                "Reply in the user's language or natural Hinglish. Never claim a phone action happened unless the app confirms it."
                        )
                )
                conversation.takeLast(MAX_CONTEXT_MESSAGES).forEach { item ->
                    input.put(
                        JSONObject()
                            .put("role", if (item.sender == MayraMessage.Sender.USER) "user" else "assistant")
                            .put("content", item.text)
                    )
                }
                if (conversation.lastOrNull()?.text != message) {
                    input.put(JSONObject().put("role", "user").put("content", message))
                }

                val payload = JSONObject()
                    .put("model", model)
                    .put("input", input)
                    .put("max_output_tokens", MAX_OUTPUT_TOKENS)

                val response = request("POST", endpoint, apiKey, payload.toString())
                parseResponseText(JSONObject(response))
            }
        }

    private fun parseResponseText(json: JSONObject): String {
        val output = json.optJSONArray("output") ?: error("OpenAI returned no output.")
        val parts = mutableListOf<String>()
        for (i in 0 until output.length()) {
            val item = output.optJSONObject(i) ?: continue
            val content = item.optJSONArray("content") ?: continue
            for (j in 0 until content.length()) {
                val part = content.optJSONObject(j) ?: continue
                if (part.optString("type") == "output_text") {
                    part.optString("text").takeIf(String::isNotBlank)?.let(parts::add)
                }
            }
        }
        return parts.joinToString("\n").trim().ifBlank {
            error("OpenAI returned an empty response.")
        }
    }

    private fun request(method: String, target: String, key: String, body: String?): String {
        val connection = connectionFactory(URL(target)).apply {
            requestMethod = method
            connectTimeout = CONNECT_TIMEOUT_MILLIS
            readTimeout = READ_TIMEOUT_MILLIS
            setRequestProperty("Authorization", "Bearer $key")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            if (body != null) {
                doOutput = true
                outputStream.bufferedWriter().use { it.write(body) }
            }
        }
        return connection.useResponse()
    }

    companion object {
        const val RESPONSES_ENDPOINT = "https://api.openai.com/v1/responses"
        private const val MAX_CONTEXT_MESSAGES = 16
        private const val MAX_OUTPUT_TOKENS = 900
        private const val CONNECT_TIMEOUT_MILLIS = 15_000
        private const val READ_TIMEOUT_MILLIS = 60_000
    }
}

class AiProviderConnectionTester(
    private val connectionFactory: (URL) -> HttpURLConnection = { url ->
        url.openConnection() as HttpURLConnection
    }
) {
    suspend fun test(apiKey: String, model: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            require(apiKey.isNotBlank()) { "API key is missing." }
            require(model.isNotBlank()) { "Model name is missing." }
            val target = "https://api.openai.com/v1/models/${model.trim()}"
            val connection = connectionFactory(URL(target)).apply {
                requestMethod = "GET"
                connectTimeout = 15_000
                readTimeout = 30_000
                setRequestProperty("Authorization", "Bearer ${apiKey.trim()}")
                setRequestProperty("Accept", "application/json")
            }
            val json = JSONObject(connection.useResponse())
            val resolvedModel = json.optString("id", model.trim())
            "Connected successfully · $resolvedModel"
        }
    }
}

private fun HttpURLConnection.useResponse(): String = try {
    val status = responseCode
    val stream = if (status in 200..299) inputStream else errorStream
    val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
    if (status !in 200..299) {
        val message = runCatching {
            JSONObject(text).optJSONObject("error")?.optString("message")
        }.getOrNull().orEmpty().ifBlank { "HTTP $status" }
        error(message.take(240))
    }
    text
} finally {
    disconnect()
}