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
        get() = provider == AiProviderKind.OPENAI &&
            apiKeyConfigured &&
            AiProviderSafetyPolicy.validateModel(model) == null

    fun status(): String = when {
        provider == AiProviderKind.LOCAL_ONLY -> "Local assistant active"
        !apiKeyConfigured -> "OpenAI key required"
        AiProviderSafetyPolicy.validateModel(model) != null -> "Choose a valid OpenAI model"
        lastConnectionSuccessAt > 0L -> "OpenAI connected · ${AiProviderSafetyPolicy.normalizeModel(model)}"
        else -> "OpenAI configured · connection not verified"
    }

    fun validationMessage(apiKeyInput: String?): String? {
        if (provider == AiProviderKind.LOCAL_ONLY) return null
        AiProviderSafetyPolicy.validateModel(model)?.let { return it }
        if (!apiKeyConfigured && apiKeyInput.isNullOrBlank()) return "Enter an OpenAI API key."
        if (!apiKeyInput.isNullOrBlank()) {
            AiProviderSafetyPolicy.validateNewApiKey(apiKeyInput)?.let { return it }
        }
        return null
    }

    companion object {
        const val DEFAULT_OPENAI_MODEL = "gpt-5-mini"
    }
}

class AiProviderSettingsStore(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val secretStore = AndroidKeystoreSecretStore(preferences)

    fun read(): AiProviderConfig {
        val keyAvailable = secretStore.hasReadableSecret()
        return AiProviderConfig(
            provider = runCatching {
                AiProviderKind.valueOf(preferences.getString(KEY_PROVIDER, null) ?: AiProviderKind.LOCAL_ONLY.name)
            }.getOrDefault(AiProviderKind.LOCAL_ONLY),
            model = AiProviderSafetyPolicy.normalizeModel(
                preferences.getString(KEY_MODEL, AiProviderConfig.DEFAULT_OPENAI_MODEL).orEmpty()
            ).ifBlank { AiProviderConfig.DEFAULT_OPENAI_MODEL },
            apiKeyConfigured = keyAvailable,
            lastConnectionSuccessAt = if (keyAvailable) preferences.getLong(KEY_LAST_SUCCESS_AT, 0L) else 0L,
            lastConnectionMessage = AiProviderSafetyPolicy.sanitizeConnectionMessage(
                preferences.getString(KEY_LAST_MESSAGE, "Not tested")
            )
        )
    }

    fun save(config: AiProviderConfig, apiKey: String? = null) {
        apiKey?.takeIf(String::isNotBlank)?.let {
            val normalized = AiProviderSafetyPolicy.normalizeApiKey(it)
            require(AiProviderSafetyPolicy.validateNewApiKey(normalized) == null) {
                "OpenAI API key format is not valid."
            }
            secretStore.write(normalized)
        }
        val normalizedModel = AiProviderSafetyPolicy.normalizeModel(config.model)
            .ifBlank { AiProviderConfig.DEFAULT_OPENAI_MODEL }
        preferences.edit()
            .putString(KEY_PROVIDER, config.provider.name)
            .putString(KEY_MODEL, normalizedModel)
            .apply()
    }

    fun apiKey(): String? = secretStore.read()

    fun recordConnection(success: Boolean, message: String, now: Long = System.currentTimeMillis()) {
        preferences.edit()
            .putLong(KEY_LAST_SUCCESS_AT, if (success) now else 0L)
            .putString(KEY_LAST_MESSAGE, AiProviderSafetyPolicy.sanitizeConnectionMessage(message))
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
    fun hasReadableSecret(): Boolean = read()?.isNotBlank() == true

    fun write(value: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        check(
            preferences.edit()
                .putString(KEY_CIPHERTEXT, Base64.encodeToString(encrypted, Base64.NO_WRAP))
                .putString(KEY_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
                .commit()
        ) { "Could not persist encrypted API key." }
    }

    fun read(): String? {
        val ciphertext = preferences.getString(KEY_CIPHERTEXT, null) ?: return null
        val iv = preferences.getString(KEY_IV, null) ?: return clearCorrupted()
        return runCatching {
            val decodedIv = Base64.decode(iv, Base64.NO_WRAP)
            require(decodedIv.size == EXPECTED_GCM_IV_BYTES) { "Invalid encrypted-key IV." }
            val decodedCiphertext = Base64.decode(ciphertext, Base64.NO_WRAP)
            require(decodedCiphertext.isNotEmpty()) { "Encrypted key is empty." }
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                secretKey(),
                GCMParameterSpec(128, decodedIv)
            )
            String(cipher.doFinal(decodedCiphertext), Charsets.UTF_8)
                .takeIf { AiProviderSafetyPolicy.validateNewApiKey(it) == null }
                ?: error("Decrypted API key is invalid.")
        }.getOrElse { clearCorrupted() }
    }

    fun clear() {
        preferences.edit().remove(KEY_CIPHERTEXT).remove(KEY_IV).commit()
    }

    private fun clearCorrupted(): String? {
        clear()
        return null
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
        const val EXPECTED_GCM_IV_BYTES = 12
    }
}
