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
import java.io.Reader
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
        val boundedMessage = AiProviderSafetyPolicy.boundUserMessage(message)
        if (boundedMessage.isBlank()) return Result.failure(IllegalArgumentException("Message cannot be empty."))
        val intent = intentEngine.parse(boundedMessage)
        if (intent !is AssistantIntent.Chat) return localAssistant.reply(boundedMessage, conversation)

        val config = providerStore.read()
        val key = providerStore.apiKey()
        if (!config.onlineEnabled || key.isNullOrBlank()) {
            return localAssistant.reply(boundedMessage, conversation)
        }

        val remote = runCatching { remoteFactory(config, key) }
            .getOrElse { error -> return offlineFallback(boundedMessage, conversation, error) }
        return remote.reply(boundedMessage, conversation)
            .recoverCatching { error ->
                offlineFallback(boundedMessage, conversation, error).getOrThrow()
            }
    }

    private suspend fun offlineFallback(
        message: String,
        conversation: List<MayraMessage>,
        cause: Throwable? = null
    ): Result<String> = localAssistant.reply(message, conversation).map { answer ->
        val reason = cause?.message
            ?.let(AiProviderSafetyPolicy::sanitizeConnectionMessage)
            ?.takeIf { it.isNotBlank() && it != "Connection test failed." }
        buildString {
            append(answer)
            append("\n\nOnline AI was unavailable, so I answered with Mayra's offline brain.")
            if (reason != null) append("\nReason: ").append(reason)
        }
    }
}

class OpenAiResponsesAssistant(
    apiKey: String,
    model: String,
    private val endpoint: String = RESPONSES_ENDPOINT,
    private val connectionFactory: (URL) -> HttpURLConnection = { url ->
        url.openConnection() as HttpURLConnection
    }
) : MayraAssistant {
    private val providerKey = AiProviderSafetyPolicy.normalizeApiKey(apiKey)
    private val model = AiProviderSafetyPolicy.normalizeModel(model)

    init {
        require(AiProviderSafetyPolicy.validateNewApiKey(this.providerKey) == null) { "OpenAI API key format is not valid." }
        require(AiProviderSafetyPolicy.validateModel(this.model) == null) { "OpenAI model name is not valid." }
        AiProviderSafetyPolicy.requireHttpsEndpoint(endpoint)
    }

    override suspend fun reply(message: String, conversation: List<MayraMessage>): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val boundedMessage = AiProviderSafetyPolicy.boundUserMessage(message)
                require(boundedMessage.isNotBlank()) { "Message cannot be empty." }
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
                    val boundedText = AiProviderSafetyPolicy.boundContextMessage(item.text)
                    if (boundedText.isNotBlank()) {
                        input.put(
                            JSONObject()
                                .put("role", if (item.sender == MayraMessage.Sender.USER) "user" else "assistant")
                                .put("content", boundedText)
                        )
                    }
                }
                if (conversation.lastOrNull()?.text?.trim() != boundedMessage) {
                    input.put(JSONObject().put("role", "user").put("content", boundedMessage))
                }

                val payload = JSONObject()
                    .put("model", model)
                    .put("input", input)
                    .put("max_output_tokens", MAX_OUTPUT_TOKENS)
                    .put("store", false)
                    .toString()
                require(payload.length <= MAX_REQUEST_CHARACTERS) { "Online AI request is too large." }

                val response = request("POST", endpoint, providerKey, payload)
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
        return parts.joinToString("\n").trim().take(MAX_ASSISTANT_TEXT_LENGTH).ifBlank {
            error("OpenAI returned an empty response.")
        }
    }

    private fun request(method: String, target: String, key: String, body: String?): String {
        AiProviderSafetyPolicy.requireHttpsEndpoint(target)
        val connection = connectionFactory(URL(target)).apply {
            requestMethod = method
            connectTimeout = CONNECT_TIMEOUT_MILLIS
            readTimeout = READ_TIMEOUT_MILLIS
            instanceFollowRedirects = false
            setRequestProperty("Authorization", "Bearer $key")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            if (body != null) {
                require(body.length <= MAX_REQUEST_CHARACTERS) { "Online AI request is too large." }
                doOutput = true
                outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(body) }
            }
        }
        return connection.useBoundedResponse()
    }

    companion object {
        const val RESPONSES_ENDPOINT = "https://api.openai.com/v1/responses"
        private const val MAX_CONTEXT_MESSAGES = 16
        private const val MAX_OUTPUT_TOKENS = 900
        private const val MAX_REQUEST_CHARACTERS = 140_000
        private const val MAX_RESPONSE_CHARACTERS = 220_000
        private const val MAX_ASSISTANT_TEXT_LENGTH = 20_000
        private const val CONNECT_TIMEOUT_MILLIS = 15_000
        private const val READ_TIMEOUT_MILLIS = 60_000

        internal fun maxResponseCharacters(): Int = MAX_RESPONSE_CHARACTERS
    }
}

class AiProviderConnectionTester(
    private val assistantFactory: (String, String) -> MayraAssistant = { apiKey, model ->
        OpenAiResponsesAssistant(apiKey = apiKey, model = model)
    }
) {
    suspend fun test(apiKey: String, model: String): Result<String> {
        val normalizedKey = AiProviderSafetyPolicy.normalizeApiKey(apiKey)
        val normalizedModel = AiProviderSafetyPolicy.normalizeModel(model)
        return runCatching {
            require(AiProviderSafetyPolicy.validateNewApiKey(normalizedKey) == null) { "API key is invalid." }
            require(AiProviderSafetyPolicy.validateModel(normalizedModel) == null) { "Model name is invalid." }
            val response = assistantFactory(normalizedKey, normalizedModel)
                .reply("Reply with only the word OK.", emptyList())
                .getOrThrow()
            require(response.trim().isNotBlank()) { "OpenAI returned an empty response." }
            "Connected successfully · $normalizedModel · generation verified"
        }.recoverCatching { error ->
            throw IllegalStateException(
                AiProviderSafetyPolicy.sanitizeConnectionMessage(error.message),
                null
            )
        }
    }
}

private fun HttpURLConnection.useBoundedResponse(): String = try {
    val status = responseCode
    val stream = if (status in 200..299) inputStream else errorStream
    val text = stream?.bufferedReader(Charsets.UTF_8)?.use {
        it.readAtMost(OpenAiResponsesAssistant.maxResponseCharacters())
    }.orEmpty()
    if (status !in 200..299) {
        val message = runCatching {
            JSONObject(text).optJSONObject("error")?.optString("message")
        }.getOrNull().orEmpty().ifBlank { "HTTP $status" }
        error(AiProviderSafetyPolicy.sanitizeConnectionMessage(message))
    }
    text
} finally {
    disconnect()
}

private fun Reader.readAtMost(limit: Int): String {
    require(limit > 0)
    val output = StringBuilder(limit.coerceAtMost(8_192))
    val buffer = CharArray(4_096)
    while (output.length < limit) {
        val count = read(buffer, 0, minOf(buffer.size, limit - output.length))
        if (count < 0) break
        output.append(buffer, 0, count)
    }
    if (read() >= 0) error("Online AI response exceeded the safe size limit.")
    return output.toString()
}
