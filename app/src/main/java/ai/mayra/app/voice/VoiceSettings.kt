package ai.mayra.app.voice

import java.util.Locale

data class VoiceSettings(
    val mode: ConversationMode = ConversationMode.CONTINUOUS,
    val autoSpeak: Boolean = true,
    val restartListeningAfterSpeech: Boolean = true,
    val languageTag: String = Locale.getDefault().toLanguageTag()
)
