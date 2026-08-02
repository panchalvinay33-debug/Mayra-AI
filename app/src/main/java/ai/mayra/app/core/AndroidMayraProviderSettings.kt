package ai.mayra.app.core

import android.content.Context

/** Non-secret owner settings. Credentials are stored separately in Android Keystore. */
data class MayraProviderSettings(
    val enabled: Boolean = false,
    val endpoint: String = DEFAULT_ENDPOINT,
    val model: String = DEFAULT_MODEL,
    val connectTimeoutMillis: Int = 15_000,
    val readTimeoutMillis: Int = 60_000,
    val maxResponseBytes: Int = 256_000
) {
    fun validatedConfig(): Result<MayraHttpProviderConfig> = runCatching {
        MayraHttpProviderConfig(
            endpoint = endpoint.trim(),
            model = model.trim(),
            enabled = enabled,
            connectTimeoutMillis = connectTimeoutMillis,
            readTimeoutMillis = readTimeoutMillis,
            maxResponseBytes = maxResponseBytes
        )
    }

    companion object {
        const val DEFAULT_ENDPOINT = "https://api.openai.com/v1/responses"
        const val DEFAULT_MODEL = "gpt-5.6"
    }
}

class AndroidMayraProviderSettingsStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun read(): MayraProviderSettings = MayraProviderSettings(
        enabled = preferences.getBoolean(KEY_ENABLED, false),
        endpoint = preferences.getString(KEY_ENDPOINT, null) ?: MayraProviderSettings.DEFAULT_ENDPOINT,
        model = preferences.getString(KEY_MODEL, null) ?: MayraProviderSettings.DEFAULT_MODEL,
        connectTimeoutMillis = preferences.getInt(KEY_CONNECT_TIMEOUT, 15_000),
        readTimeoutMillis = preferences.getInt(KEY_READ_TIMEOUT, 60_000),
        maxResponseBytes = preferences.getInt(KEY_MAX_RESPONSE, 256_000)
    )

    fun write(settings: MayraProviderSettings): Result<Unit> = settings.validatedConfig().mapCatching {
        check(
            preferences.edit()
                .putBoolean(KEY_ENABLED, settings.enabled)
                .putString(KEY_ENDPOINT, settings.endpoint.trim())
                .putString(KEY_MODEL, settings.model.trim())
                .putInt(KEY_CONNECT_TIMEOUT, settings.connectTimeoutMillis)
                .putInt(KEY_READ_TIMEOUT, settings.readTimeoutMillis)
                .putInt(KEY_MAX_RESPONSE, settings.maxResponseBytes)
                .commit()
        ) { "Unable to persist provider settings." }
    }

    fun disable(): Boolean = preferences.edit().putBoolean(KEY_ENABLED, false).commit()

    private companion object {
        const val PREFS = "mayra_provider_settings_v1"
        const val KEY_ENABLED = "enabled"
        const val KEY_ENDPOINT = "endpoint"
        const val KEY_MODEL = "model"
        const val KEY_CONNECT_TIMEOUT = "connect_timeout"
        const val KEY_READ_TIMEOUT = "read_timeout"
        const val KEY_MAX_RESPONSE = "max_response"
    }
}
