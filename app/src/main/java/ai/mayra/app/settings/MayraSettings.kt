package ai.mayra.app.settings

import android.content.Context

enum class MayraLanguage(val label: String, val speechTag: String) {
    HINGLISH("Hindi + English", "en-IN"),
    HINDI("Hindi", "hi-IN"),
    ENGLISH("English", "en-IN")
}

enum class MayraVoiceStyle(
    val label: String,
    val description: String,
    val defaultRate: Float,
    val defaultPitch: Float
) {
    WARM("Warm Mayra", "Soft, friendly and slightly relaxed.", 0.92f, 1.03f),
    NATURAL("Natural", "Balanced everyday conversation.", 0.98f, 1.00f),
    CLEAR("Clear", "A little slower and easier to understand.", 0.86f, 1.00f)
}

data class MayraSettings(
    val userName: String = "",
    val language: MayraLanguage = MayraLanguage.HINGLISH,
    val speakResponses: Boolean = true,
    val continuousVoiceByDefault: Boolean = false,
    val voiceStyle: MayraVoiceStyle = MayraVoiceStyle.WARM,
    val voiceRate: Float = MayraVoiceStyle.WARM.defaultRate,
    val voicePitch: Float = MayraVoiceStyle.WARM.defaultPitch,
    val preferHighQualityOfflineVoice: Boolean = true,
    val memoryEnabled: Boolean = true,
    val personalizationEnabled: Boolean = true,
    val diagnosticsSharingEnabled: Boolean = false,
    val onboardingCompleted: Boolean = false
) {
    val normalizedName: String
        get() = userName.trim().take(MAX_NAME_LENGTH)

    val normalizedVoiceRate: Float get() = voiceRate.coerceIn(MIN_VOICE_RATE, MAX_VOICE_RATE)
    val normalizedVoicePitch: Float get() = voicePitch.coerceIn(MIN_VOICE_PITCH, MAX_VOICE_PITCH)

    fun validationMessage(): String? = when {
        normalizedName.isBlank() -> "Tell Mayra what to call you."
        normalizedName.length < 2 -> "Name must contain at least 2 characters."
        !voiceRate.isFinite() || voiceRate !in MIN_VOICE_RATE..MAX_VOICE_RATE -> "Choose a valid speaking speed."
        !voicePitch.isFinite() || voicePitch !in MIN_VOICE_PITCH..MAX_VOICE_PITCH -> "Choose a valid voice pitch."
        else -> null
    }

    fun summary(): String = buildString {
        append(if (normalizedName.isBlank()) "Profile not configured" else normalizedName)
        append(" · ")
        append(language.label)
        append(" · ")
        append(voiceStyle.label)
        append(" · Memory ")
        append(if (memoryEnabled) "on" else "off")
    }

    companion object {
        const val MAX_NAME_LENGTH = 40
        const val MIN_VOICE_RATE = 0.70f
        const val MAX_VOICE_RATE = 1.20f
        const val MIN_VOICE_PITCH = 0.80f
        const val MAX_VOICE_PITCH = 1.20f
    }
}

class MayraSettingsStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun read(): MayraSettings {
        val style = runCatching {
            MayraVoiceStyle.valueOf(preferences.getString(KEY_VOICE_STYLE, null) ?: MayraVoiceStyle.WARM.name)
        }.getOrDefault(MayraVoiceStyle.WARM)
        return MayraSettings(
            userName = preferences.getString(KEY_USER_NAME, "").orEmpty(),
            language = runCatching {
                MayraLanguage.valueOf(preferences.getString(KEY_LANGUAGE, null) ?: MayraLanguage.HINGLISH.name)
            }.getOrDefault(MayraLanguage.HINGLISH),
            speakResponses = preferences.getBoolean(KEY_SPEAK_RESPONSES, true),
            continuousVoiceByDefault = preferences.getBoolean(KEY_CONTINUOUS_VOICE, false),
            voiceStyle = style,
            voiceRate = preferences.getFloat(KEY_VOICE_RATE, style.defaultRate).coerceIn(MayraSettings.MIN_VOICE_RATE, MayraSettings.MAX_VOICE_RATE),
            voicePitch = preferences.getFloat(KEY_VOICE_PITCH, style.defaultPitch).coerceIn(MayraSettings.MIN_VOICE_PITCH, MayraSettings.MAX_VOICE_PITCH),
            preferHighQualityOfflineVoice = preferences.getBoolean(KEY_PREFER_HIGH_QUALITY_VOICE, true),
            memoryEnabled = preferences.getBoolean(KEY_MEMORY, true),
            personalizationEnabled = preferences.getBoolean(KEY_PERSONALIZATION, true),
            diagnosticsSharingEnabled = preferences.getBoolean(KEY_DIAGNOSTICS_SHARING, false),
            onboardingCompleted = preferences.getBoolean(KEY_ONBOARDING_COMPLETED, false)
        )
    }

    fun save(settings: MayraSettings) {
        val normalized = settings.copy(
            userName = settings.normalizedName,
            voiceRate = settings.normalizedVoiceRate,
            voicePitch = settings.normalizedVoicePitch
        )
        preferences.edit()
            .putString(KEY_USER_NAME, normalized.userName)
            .putString(KEY_LANGUAGE, normalized.language.name)
            .putBoolean(KEY_SPEAK_RESPONSES, normalized.speakResponses)
            .putBoolean(KEY_CONTINUOUS_VOICE, normalized.continuousVoiceByDefault)
            .putString(KEY_VOICE_STYLE, normalized.voiceStyle.name)
            .putFloat(KEY_VOICE_RATE, normalized.voiceRate)
            .putFloat(KEY_VOICE_PITCH, normalized.voicePitch)
            .putBoolean(KEY_PREFER_HIGH_QUALITY_VOICE, normalized.preferHighQualityOfflineVoice)
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
        const val KEY_VOICE_STYLE = "voice_style"
        const val KEY_VOICE_RATE = "voice_rate"
        const val KEY_VOICE_PITCH = "voice_pitch"
        const val KEY_PREFER_HIGH_QUALITY_VOICE = "prefer_high_quality_voice"
        const val KEY_MEMORY = "memory"
        const val KEY_PERSONALIZATION = "personalization"
        const val KEY_DIAGNOSTICS_SHARING = "diagnostics_sharing"
        const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
    }
}