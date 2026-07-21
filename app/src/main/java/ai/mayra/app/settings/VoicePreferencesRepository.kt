package ai.mayra.app.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import ai.mayra.app.voice.ConversationMode
import ai.mayra.app.voice.VoiceSettings
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.voicePreferencesDataStore by preferencesDataStore(
    name = "voice_preferences"
)

class VoicePreferencesRepository(
    private val context: Context
) {
    val settings: Flow<VoiceSettings> = context.voicePreferencesDataStore.data.map { preferences ->
        VoiceSettings(
            mode = preferences[Keys.MODE]
                ?.let { stored ->
                    runCatching { ConversationMode.valueOf(stored) }.getOrNull()
                }
                ?: ConversationMode.CONTINUOUS,
            autoSpeak = preferences[Keys.AUTO_SPEAK] ?: true,
            restartListeningAfterSpeech =
                preferences[Keys.RESTART_LISTENING_AFTER_SPEECH] ?: true,
            languageTag = preferences[Keys.LANGUAGE_TAG]
                ?: Locale.getDefault().toLanguageTag()
        )
    }

    suspend fun setConversationMode(mode: ConversationMode) {
        context.voicePreferencesDataStore.edit { preferences ->
            preferences[Keys.MODE] = mode.name
        }
    }

    suspend fun setAutoSpeak(enabled: Boolean) {
        context.voicePreferencesDataStore.edit { preferences ->
            preferences[Keys.AUTO_SPEAK] = enabled
        }
    }

    suspend fun setRestartListeningAfterSpeech(enabled: Boolean) {
        context.voicePreferencesDataStore.edit { preferences ->
            preferences[Keys.RESTART_LISTENING_AFTER_SPEECH] = enabled
        }
    }

    suspend fun setLanguageTag(languageTag: String) {
        val normalizedTag = languageTag.trim().ifBlank {
            Locale.getDefault().toLanguageTag()
        }
        context.voicePreferencesDataStore.edit { preferences ->
            preferences[Keys.LANGUAGE_TAG] = normalizedTag
        }
    }

    suspend fun update(settings: VoiceSettings) {
        context.voicePreferencesDataStore.edit { preferences ->
            preferences[Keys.MODE] = settings.mode.name
            preferences[Keys.AUTO_SPEAK] = settings.autoSpeak
            preferences[Keys.RESTART_LISTENING_AFTER_SPEECH] =
                settings.restartListeningAfterSpeech
            preferences[Keys.LANGUAGE_TAG] = settings.languageTag
        }
    }

    private object Keys {
        val MODE = stringPreferencesKey("conversation_mode")
        val AUTO_SPEAK = booleanPreferencesKey("auto_speak")
        val RESTART_LISTENING_AFTER_SPEECH =
            booleanPreferencesKey("restart_listening_after_speech")
        val LANGUAGE_TAG = stringPreferencesKey("language_tag")
    }
}
