package ai.mayra.app.core

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.Locale

/** Owner-controlled runtime configuration. Secrets are supplied separately by credential source. */
data class MayraHttpProviderConfig(
    val endpoint: String,
    val model: String,
    val enabled: Boolean = false,
    val connectTimeoutMillis: Int = 10_000,
    val readTimeoutMillis: Int = 20_000,
    val maxResponseBytes: Int = 256_000
) {
    init {
        require(endpoint.startsWith("https://"))
        require(model.isNotBlank())
        require(connectTimeoutMillis in 1_000..30_000)
        require(readTimeoutMillis in 1_000..60_000)
        require(maxResponseBytes in 1_024..1_000_000)
    }
}

enum class MayraProviderHealthState { DISABLED, MISSING_CREDENTIAL, READY, TEMPORARY_FAILURE, PERMANENT_FAILURE }

data class MayraProviderHealth(
    val state: MayraProviderHealthState,
    val detail: String,
    val checkedAtEpochMillis: Long = System.currentTimeMillis()
)

fun interface MayraHttpConnectionFactory {
    fun open(url: URL): HttpURLConnection
}

/**
 * Minimal HTTPS text provider transport. It has no action or memory-write capability and is not
 * installed automatically. The endpoint response must contain a JSON string field named `text`.
 */
class MayraHttpConversationalProvider(
    private val config: MayraHttpProviderConfig,
    private val credentials: MayraProviderCredentialSource,
    private val connectionFactory: MayraHttpConnectionFactory = MayraHttpConnectionFactory {
        it.openConnection() as HttpURLConnection
    }
) : MayraConversationalProvider {

    @Volatile
    private var lastHealth = healthFromConfiguration()

    fun health(): MayraProviderHealth = lastHealth

    override suspend fun answer(request: MayraProviderRequest): MayraProviderResult {
        if (!config.enabled) {
            return permanent(
                "Remote provider is disabled by the owner.",
                MayraProviderHealthState.DISABLED
            )
        }
        val token = credentials.bearerToken()?.trim().orEmpty()
        if (token.isEmpty()) return permanent("Remote provider credential is missing.", MayraProviderHealthState.MISSING_CREDENTIAL)

        val connection = connectionFactory.open(URL(config.endpoint))
        return try {
            connection.requestMethod = "POST"
            connection.connectTimeout = config.connectTimeoutMillis
            connection.readTimeout = config.readTimeoutMillis
            connection.doOutput = true
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.setRequestProperty("Accept", "application/json")
            val body = requestJson(request).toByteArray(StandardCharsets.UTF_8)
            connection.setFixedLengthStreamingMode(body.size)
            connection.outputStream.use { it.write(body) }

            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.use { input ->
                val bytes = input.readNBytes(config.maxResponseBytes + 1)
                if (bytes.size > config.maxResponseBytes) throw ResponseTooLargeException()
                String(bytes, StandardCharsets.UTF_8)
            }.orEmpty()

            when {
                code in 200..299 -> {
                    val text = extractJsonString(response, "text")?.trim()
                    if (text.isNullOrEmpty()) permanent("Provider response did not contain usable text.")
                    else {
                        lastHealth = MayraProviderHealth(MayraProviderHealthState.READY, "Remote provider responded successfully.")
                        MayraProviderResult.Success(text)
                    }
                }
                code == 408 || code == 429 || code in 500..599 -> temporary("Provider temporarily unavailable (HTTP $code).")
                else -> permanent("Provider rejected the request (HTTP $code).")
            }
        } catch (error: ResponseTooLargeException) {
            permanent("Provider response exceeded the configured size limit.")
        } catch (error: IOException) {
            temporary(error.message ?: "Provider network request failed.")
        } finally {
            connection.disconnect()
        }
    }

    private fun healthFromConfiguration(): MayraProviderHealth = when {
        !config.enabled -> MayraProviderHealth(MayraProviderHealthState.DISABLED, "Remote provider is disabled by the owner.")
        credentials.bearerToken().isNullOrBlank() -> MayraProviderHealth(MayraProviderHealthState.MISSING_CREDENTIAL, "Remote provider credential is missing.")
        else -> MayraProviderHealth(MayraProviderHealthState.READY, "Remote provider is configured; live connectivity is not yet verified.")
    }

    private fun temporary(reason: String): MayraProviderResult.TemporaryFailure {
        lastHealth = MayraProviderHealth(MayraProviderHealthState.TEMPORARY_FAILURE, reason)
        return MayraProviderResult.TemporaryFailure(reason)
    }

    private fun permanent(reason: String, state: MayraProviderHealthState = MayraProviderHealthState.PERMANENT_FAILURE): MayraProviderResult.PermanentFailure {
        lastHealth = MayraProviderHealth(state, reason)
        return MayraProviderResult.PermanentFailure(reason)
    }

    private fun requestJson(request: MayraProviderRequest): String {
        val history = request.conversation.takeLast(20).joinToString(",") { message ->
            "{\"role\":\"${if (message.sender == MayraMessage.Sender.USER) "user" else "assistant"}\",\"text\":\"${escape(message.text.take(8_000))}\"}"
        }
        return "{\"model\":\"${escape(config.model)}\",\"locale\":\"${escape(request.localeTag)}\",\"message\":\"${escape(request.message.take(16_000))}\",\"conversation\":[$history]}"
    }

    private fun escape(value: String): String = buildString(value.length + 16) {
        value.forEach { char ->
            when (char) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (char.code < 0x20) append("\\u%04x".format(Locale.ROOT, char.code)) else append(char)
            }
        }
    }

    private fun extractJsonString(json: String, field: String): String? {
        val marker = "\"$field\""
        var index = json.indexOf(marker)
        if (index < 0) return null
        index = json.indexOf(':', index + marker.length)
        if (index < 0) return null
        index++
        while (index < json.length && json[index].isWhitespace()) index++
        if (index >= json.length || json[index] != '"') return null
        index++
        val result = StringBuilder()
        var escaped = false
        while (index < json.length) {
            val char = json[index++]
            if (escaped) {
                when (char) {
                    '"', '\\', '/' -> result.append(char)
                    'b' -> result.append('\b')
                    'f' -> result.append('\u000C')
                    'n' -> result.append('\n')
                    'r' -> result.append('\r')
                    't' -> result.append('\t')
                    'u' -> {
                        if (index + 4 > json.length) return null
                        result.append(json.substring(index, index + 4).toIntOrNull(16)?.toChar() ?: return null)
                        index += 4
                    }
                    else -> return null
                }
                escaped = false
            } else when (char) {
                '\\' -> escaped = true
                '"' -> return result.toString()
                else -> result.append(char)
            }
        }
        return null
    }

    private class ResponseTooLargeException : IOException()
}
