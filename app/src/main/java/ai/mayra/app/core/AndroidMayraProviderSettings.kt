package ai.mayra.app.core

import android.content.Context

/** Non-secret owner settings. Credentials are intentionally excluded from persistence. */
data class MayraProviderSettings(
    val enabled: Boolean = false,
    val endpoint: String = "https://example.invalid/mayra",
    val model: String = "mayra-default",
    val connectTimeoutMillis: Int = 10_000,
    val readTimeoutMillis: Int = 20_000,
    val maxResponseBytes: Int = 256_000
) {
    fun validatedConfig(): Result<MayraHttpProviderConfig> = runCatching {
        MayraHttpProviderConfig(endpoint.trim(), model.trim(), enabled, connectTimeoutMillis, readTimeoutMillis, maxResponseBytes)
    }
}

class AndroidMayraProviderSettingsStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun read(): MayraProviderSettings = MayraProviderSettings(
        enabled = preferences.getBoolean(KEY_ENABLED, false),
        endpoint = preferences.getString(KEY_ENDPOINT, null) ?: MayraProviderSettings().endpoint,
        model = preferences.getString(KEY_MODEL, null) ?: MayraProviderSettings().model,
        connectTimeoutMillis = preferences.getInt(KEY_CONNECT_TIMEOUT, 10_000),
        readTimeoutMillis = preferences.getInt(KEY_READ_TIMEOUT, 20_000),
        maxResponseBytes = preferences.getInt(KEY_MAX_RESPONSE, 256_000)
    )

    fun write(settings: MayraProviderSettings): Result<Unit> = settings.validatedConfig().mapCatching {
        check(preferences.edit()
            .putBoolean(KEY_ENABLED, settings.enabled)
            .putString(KEY_ENDPOINT, settings.endpoint.trim())
            .putString(KEY_MODEL, settings.model.trim())
            .putInt(KEY_CONNECT_TIMEOUT, settings.connectTimeoutMillis)
            .putInt(KEY_READ_TIMEOUT, settings.readTimeoutMillis)
            .putInt(KEY_MAX_RESPONSE, settings.maxResponseBytes)
            .commit()) { "Unable to persist provider settings." }
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
