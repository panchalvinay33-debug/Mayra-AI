package ai.mayra.app.assistant

/**
 * Small lifecycle contract for Mayra speech output.
 *
 * Keeping the Assistant session dependent on this interface lets us retain Android system TTS as
 * a zero-cost fallback while benchmarking a higher-quality on-device neural engine separately.
 */
interface MayraSpeechOutput {
    fun speak(text: String)
    fun stop()
    fun shutdown()
}
