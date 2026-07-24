package ai.mayra.app.ai

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

enum class AiProviderKind(val label: String) {
    LOCAL_ONLY("Local only"),
    OPENAI("OpenAI")
}

data class AiProviderConfig(
    val provider: AiProviderKind = AiProviderKind.LOCAL_ONLY,
    val model: String = DEFAULT_OPENAI_MODEL,
    val apiKeyConfigured: Boolean = false,
    val lastConnectionSuccessAt: Long = 0L,
    val lastConnectionMessage: String = "Not tested"
) {
    val onlineEnabled: Boolean
        get() = provider == AiProviderKind.OPENAI && apiKeyConfigured && model.isNotBlank()

    fun status(): String = when {
        provider == AiProviderKind.LOCAL_ONLY -> "Local assistant active"
        !apiKeyConfigured -> "OpenAI key required"
        model.isBlank() -> "Choose an OpenAI model"
        lastConnectionSuccessAt > 0L -> "OpenAI connected · $model"
        else -> "OpenAI configured · connection not verified"
    }

    fun validationMessage(apiKeyInput: String?): String? = when {
        provider == AiProviderKind.LOCAL_ONLY -> null
        model.trim().isBlank() -> "Enter an OpenAI model name."
        !apiKeyConfigured && apiKeyInput.isNullOrBlank() -> "Enter an OpenAI API key."
        !apiKeyInput.isNullOrBlank() && !apiKeyInput.trim().startsWith("sk-") ->
            "OpenAI API keys normally start with sk-."
        else -> null
    }

    companion object {
        const val DEFAULT_OPENAI_MODEL = "gpt-5-mini"
    }
}

class AiProviderSettingsStore(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val secretStore = AndroidKeystoreSecretStore(preferences)

    fun read(): AiProviderConfig = AiProviderConfig(
        provider = runCatching {
            AiProviderKind.valueOf(preferences.getString(KEY_PROVIDER, null) ?: AiProviderKind.LOCAL_ONLY.name)
        }.getOrDefault(AiProviderKind.LOCAL_ONLY),
        model = preferences.getString(KEY_MODEL, AiProviderConfig.DEFAULT_OPENAI_MODEL)
            ?.trim().orEmpty().ifBlank { AiProviderConfig.DEFAULT_OPENAI_MODEL },
        apiKeyConfigured = secretStore.hasSecret(),
        lastConnectionSuccessAt = preferences.getLong(KEY_LAST_SUCCESS_AT, 0L),
        lastConnectionMessage = preferences.getString(KEY_LAST_MESSAGE, "Not tested").orEmpty()
    )

    fun save(config: AiProviderConfig, apiKey: String? = null) {
        apiKey?.trim()?.takeIf(String::isNotBlank)?.let(secretStore::write)
        preferences.edit()
            .putString(KEY_PROVIDER, config.provider.name)
            .putString(KEY_MODEL, config.model.trim().ifBlank { AiProviderConfig.DEFAULT_OPENAI_MODEL })
            .apply()
    }

    fun apiKey(): String? = secretStore.read()

    fun recordConnection(success: Boolean, message: String, now: Long = System.currentTimeMillis()) {
        preferences.edit()
            .putLong(KEY_LAST_SUCCESS_AT, if (success) now else 0L)
            .putString(KEY_LAST_MESSAGE, message.take(160))
            .apply()
    }

    fun clearApiKey() {
        secretStore.clear()
        preferences.edit()
            .putLong(KEY_LAST_SUCCESS_AT, 0L)
            .putString(KEY_LAST_MESSAGE, "API key removed")
            .apply()
    }

    fun reset() {
        secretStore.clear()
        preferences.edit().clear().apply()
    }

    private companion object {
        const val PREFS = "mayra_ai_provider"
        const val KEY_PROVIDER = "provider"
        const val KEY_MODEL = "model"
        const val KEY_LAST_SUCCESS_AT = "last_success_at"
        const val KEY_LAST_MESSAGE = "last_message"
    }
}

private class AndroidKeystoreSecretStore(
    private val preferences: android.content.SharedPreferences
) {
    fun hasSecret(): Boolean = preferences.contains(KEY_CIPHERTEXT) && preferences.contains(KEY_IV)

    fun write(value: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        preferences.edit()
            .putString(KEY_CIPHERTEXT, Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .putString(KEY_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .apply()
    }

    fun read(): String? = runCatching {
        val ciphertext = preferences.getString(KEY_CIPHERTEXT, null) ?: return null
        val iv = preferences.getString(KEY_IV, null) ?: return null
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            secretKey(),
            GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP))
        )
        String(cipher.doFinal(Base64.decode(ciphertext, Base64.NO_WRAP)), Charsets.UTF_8)
    }.getOrNull()

    fun clear() {
        preferences.edit().remove(KEY_CIPHERTEXT).remove(KEY_IV).apply()
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
        )
        return generator.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "mayra_ai_provider_key_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val KEY_CIPHERTEXT = "api_key_ciphertext"
        const val KEY_IV = "api_key_iv"
    }
}