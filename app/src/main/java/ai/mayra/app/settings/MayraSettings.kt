package ai.mayra.app.settings

import android.content.Context

enum class MayraLanguage(val label: String) {
    HINGLISH("Hindi + English"),
    HINDI("Hindi"),
    ENGLISH("English")
}

data class MayraSettings(
    val userName: String = "",
    val language: MayraLanguage = MayraLanguage.HINGLISH,
    val speakResponses: Boolean = true,
    val continuousVoiceByDefault: Boolean = false,
    val memoryEnabled: Boolean = true,
    val personalizationEnabled: Boolean = true,
    val diagnosticsSharingEnabled: Boolean = false,
    val onboardingCompleted: Boolean = false
) {
    val normalizedName: String
        get() = userName.trim().take(MAX_NAME_LENGTH)

    fun validationMessage(): String? = when {
        normalizedName.isBlank() -> "Tell Mayra what to call you."
        normalizedName.length < 2 -> "Name must contain at least 2 characters."
        else -> null
    }

    fun summary(): String = buildString {
        append(if (normalizedName.isBlank()) "Profile not configured" else normalizedName)
        append(" · ")
        append(language.label)
        append(" · Memory ")
        append(if (memoryEnabled) "on" else "off")
    }

    companion object {
        const val MAX_NAME_LENGTH = 40
    }
}

class MayraSettingsStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun read(): MayraSettings = MayraSettings(
        userName = preferences.getString(KEY_USER_NAME, "").orEmpty(),
        language = runCatching {
            MayraLanguage.valueOf(preferences.getString(KEY_LANGUAGE, null) ?: MayraLanguage.HINGLISH.name)
        }.getOrDefault(MayraLanguage.HINGLISH),
        speakResponses = preferences.getBoolean(KEY_SPEAK_RESPONSES, true),
        continuousVoiceByDefault = preferences.getBoolean(KEY_CONTINUOUS_VOICE, false),
        memoryEnabled = preferences.getBoolean(KEY_MEMORY, true),
        personalizationEnabled = preferences.getBoolean(KEY_PERSONALIZATION, true),
        diagnosticsSharingEnabled = preferences.getBoolean(KEY_DIAGNOSTICS_SHARING, false),
        onboardingCompleted = preferences.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    )

    fun save(settings: MayraSettings) {
        val normalized = settings.copy(userName = settings.normalizedName)
        preferences.edit()
            .putString(KEY_USER_NAME, normalized.userName)
            .putString(KEY_LANGUAGE, normalized.language.name)
            .putBoolean(KEY_SPEAK_RESPONSES, normalized.speakResponses)
            .putBoolean(KEY_CONTINUOUS_VOICE, normalized.continuousVoiceByDefault)
            .putBoolean(KEY_MEMORY, normalized.memoryEnabled)
            .putBoolean(KEY_PERSONALIZATION, normalized.personalizationEnabled)
            .putBoolean(KEY_DIAGNOSTICS_SHARING, normalized.diagnosticsSharingEnabled)
            .putBoolean(KEY_ONBOARDING_COMPLETED, normalized.onboardingCompleted)
            .apply()
    }

    fun completeOnboarding(settings: MayraSettings) {
        save(settings.copy(onboardingCompleted = true))
    }

    fun reset() {
        preferences.edit().clear().apply()
    }

    private companion object {
        const val PREFS = "mayra_settings"
        const val KEY_USER_NAME = "user_name"
        const val KEY_LANGUAGE = "language"
        const val KEY_SPEAK_RESPONSES = "speak_responses"
        const val KEY_CONTINUOUS_VOICE = "continuous_voice"
        const val KEY_MEMORY = "memory"
        const val KEY_PERSONALIZATION = "personalization"
        const val KEY_DIAGNOSTICS_SHARING = "diagnostics_sharing"
        const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
    }
}
