package ai.mayra.app.voice

import ai.mayra.app.settings.MayraLanguage

/** Pure text preparation used before Android TextToSpeech. */
object MayraSpeechTextPolicy {
    private const val MAX_SPOKEN_CHARACTERS = 6_000

    fun prepare(raw: String): String = raw
        .replace(Regex("```[\\s\\S]*?```"), " Code block omitted. ")
        .replace(Regex("https?://\\S+", RegexOption.IGNORE_CASE), " link ")
        .replace(Regex("(?m)^\\s*[-*•]+\\s*"), "")
        .replace(Regex("(?m)^\\s*#{1,6}\\s*"), "")
        .replace(Regex("[*_`]+"), "")
        .replace(Regex("[\\r\\n]+"), ". ")
        .replace(Regex("\\s+"), " ")
        .replace(Regex("\\.{2,}"), ".")
        .trim()
        .take(MAX_SPOKEN_CHARACTERS)

    fun languageTag(text: String, preferred: MayraLanguage): String = when {
        text.any { it in '\u0900'..'\u097F' } -> "hi-IN"
        preferred == MayraLanguage.HINDI -> "hi-IN"
        else -> "en-IN"
    }
}