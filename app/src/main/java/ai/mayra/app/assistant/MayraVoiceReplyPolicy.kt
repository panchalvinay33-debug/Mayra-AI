package ai.mayra.app.assistant

import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Small deterministic bridge for the engineering voice session. It never executes an action.
 * Full Mayra action routing remains behind the existing typed confirmation/runtime boundary.
 */
object MayraVoiceReplyPolicy {
    data class Reply(val text: String, val containsPrivateContent: Boolean = false)

    fun replyFor(transcript: String, now: LocalTime = LocalTime.now()): Reply {
        val clean = transcript.trim()
        val normalized = clean.lowercase(Locale.ROOT)

        return when {
            clean.isBlank() -> Reply("Mujhe awaaz saaf nahi mili.")
            normalized.contains("namaste") || normalized.contains("hello") || normalized.contains("hi mayra") ->
                Reply("Namaste. Main sun rahi hoon.")
            normalized.contains("time") || normalized.contains("samay") || normalized.contains("kitne baje") ->
                Reply("Abhi ${now.format(DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH))} hue hain.")
            normalized.contains("open whatsapp") || normalized.contains("whatsapp kholo") || normalized.contains("ओपन व्हाट्सएप") ->
                Reply("WhatsApp kholne ka request samajh gaya. Full Mayra mein confirmation ke baad action hoga.")
            normalized.contains("reminder") || normalized.contains("yaad dila") || normalized.contains("याद") ->
                Reply("Reminder request samajh gaya. Full Mayra mein time confirm karke save karungi.", containsPrivateContent = true)
            normalized.contains("kya kar sak") || normalized.contains("what can you do") ->
                Reply("Main offline sun sakti hoon, baat samajh sakti hoon, aur safe commands ko confirmation ke saath chalaungi.")
            else -> Reply("Maine suna: $clean", containsPrivateContent = true)
        }
    }
}
