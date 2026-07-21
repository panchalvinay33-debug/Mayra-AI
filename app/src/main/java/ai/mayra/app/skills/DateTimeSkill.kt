package ai.mayra.app.skills

import java.time.Clock
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Answers simple date and time questions locally without calling a remote AI provider. */
class DateTimeSkill(
    private val clock: Clock = Clock.systemDefaultZone(),
    private val locale: Locale = Locale.getDefault()
) : MayraSkill {

    override val id: String = "date-time"
    override val description: String = "Tells the current local date or time"

    override fun canHandle(message: String): Boolean {
        val normalized = message.trim().lowercase(locale)
        return normalized in dateQueries || normalized in timeQueries || normalized in dateTimeQueries
    }

    override suspend fun execute(message: String): SkillResult {
        val normalized = message.trim().lowercase(locale)
        val now = ZonedDateTime.now(clock)

        val response = when {
            normalized in dateQueries -> "Today is ${now.format(dateFormatter)}."
            normalized in timeQueries -> "The current time is ${now.format(timeFormatter)}."
            else -> "It is ${now.format(timeFormatter)} on ${now.format(dateFormatter)}."
        }

        return SkillResult(
            text = response,
            metadata = mapOf(
                "action" to "show_date_time",
                "zone" to now.zone.id
            )
        )
    }

    private val dateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", locale)

    private val timeFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("h:mm a", locale)

    private companion object {
        val dateQueries = setOf(
            "date",
            "today's date",
            "what is today's date",
            "what is the date",
            "aaj ki date",
            "aaj tarikh kya hai"
        )

        val timeQueries = setOf(
            "time",
            "current time",
            "what time is it",
            "what is the time",
            "abhi kitne baje hain",
            "abhi time kya hai"
        )

        val dateTimeQueries = setOf(
            "date and time",
            "current date and time",
            "what is the date and time",
            "aaj ki date aur time"
        )
    }
}
