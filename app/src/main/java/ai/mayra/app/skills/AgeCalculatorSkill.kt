package ai.mayra.app.skills

import java.time.Clock
import java.time.DateTimeException
import java.time.LocalDate
import java.time.Period
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Calculates age completely offline from a supplied date of birth.
 *
 * The skill understands English, Roman Hindi/Hinglish and Devanagari prompts,
 * while accepting common numeric and month-name date formats.
 */
class AgeCalculatorSkill(
    private val clock: Clock = Clock.systemDefaultZone()
) : MayraSkill {

    override val id: String = "age-calculator"
    override val description: String =
        "Calculates age and next birthday offline in Hindi, Hinglish or English"

    override fun canHandle(message: String): Boolean {
        val normalized = normalizeDigits(message).trim().lowercase(Locale.ROOT)
        return ageKeywords.any(normalized::contains) && extractDateText(normalized) != null
    }

    override suspend fun execute(message: String): SkillResult {
        val normalized = normalizeDigits(message).trim().lowercase(Locale.ROOT)
        val dateText = extractDateText(normalized)
            ?: return failure(message, "Please include your date of birth, for example 15/08/2000.")

        val birthDate = parseDate(dateText)
            ?: return failure(message, "I couldn't understand that birth date. Try 15/08/2000, 2000-08-15, or 15 Aug 2000.")

        val today = LocalDate.now(clock)
        if (birthDate.isAfter(today)) {
            return failure(message, localized(
                message,
                english = "The birth date cannot be in the future.",
                romanHindi = "Janam ki tareekh future ki nahi ho sakti.",
                hindi = "जन्म तारीख भविष्य की नहीं हो सकती।"
            ))
        }

        val age = Period.between(birthDate, today)
        val nextBirthday = nextBirthday(birthDate, today)
        val daysUntilBirthday = ChronoUnit.DAYS.between(today, nextBirthday)

        val text = localized(
            message,
            english = "Age: ${age.years} years, ${age.months} months and ${age.days} days. " +
                if (daysUntilBirthday == 0L) "Happy birthday!" else "Next birthday is in $daysUntilBirthday days.",
            romanHindi = "Umar: ${age.years} saal, ${age.months} mahine aur ${age.days} din. " +
                if (daysUntilBirthday == 0L) "Janmadin mubarak!" else "Agla janmadin $daysUntilBirthday din baad hai.",
            hindi = "उम्र: ${age.years} साल, ${age.months} महीने और ${age.days} दिन। " +
                if (daysUntilBirthday == 0L) "जन्मदिन मुबारक!" else "अगला जन्मदिन $daysUntilBirthday दिन बाद है।"
        )

        return SkillResult(
            text = text,
            metadata = mapOf(
                "skill" to id,
                "birth_date" to birthDate.toString(),
                "age_years" to age.years.toString(),
                "age_months" to age.months.toString(),
                "age_days" to age.days.toString(),
                "next_birthday" to nextBirthday.toString(),
                "days_until_birthday" to daysUntilBirthday.toString()
            )
        )
    }

    private fun failure(original: String, englishFallback: String): SkillResult = SkillResult(
        text = if (containsDevanagari(original)) {
            when {
                englishFallback.startsWith("Please include") -> "कृपया जन्म तारीख भी लिखें, जैसे 15/08/2000।"
                englishFallback.startsWith("I couldn't") -> "मैं जन्म तारीख समझ नहीं पाई। 15/08/2000, 2000-08-15 या 15 Aug 2000 लिखें।"
                else -> englishFallback
            }
        } else if (looksHindiOrHinglish(original)) {
            when {
                englishFallback.startsWith("Please include") -> "Kripya janam ki tareekh bhi likhiye, jaise 15/08/2000."
                englishFallback.startsWith("I couldn't") -> "Main janam ki tareekh samajh nahi paayi. 15/08/2000, 2000-08-15 ya 15 Aug 2000 likhiye."
                else -> englishFallback
            }
        } else {
            englishFallback
        },
        isSuccess = false,
        metadata = mapOf("skill" to id)
    )

    private fun localized(
        original: String,
        english: String,
        romanHindi: String,
        hindi: String
    ): String = when {
        containsDevanagari(original) -> hindi
        looksHindiOrHinglish(original) -> romanHindi
        else -> english
    }

    private fun nextBirthday(birthDate: LocalDate, today: LocalDate): LocalDate {
        fun birthdayFor(year: Int): LocalDate {
            return try {
                birthDate.withYear(year)
            } catch (_: DateTimeException) {
                // A 29 February birthday is observed on the last day of February
                // in non-leap years for the purpose of this countdown.
                YearMonth.of(year, birthDate.month).atEndOfMonth()
            }
        }

        val thisYear = birthdayFor(today.year)
        return if (thisYear.isBefore(today)) birthdayFor(today.year + 1) else thisYear
    }

    private fun parseDate(raw: String): LocalDate? {
        val cleaned = raw
            .trim()
            .replace(Regex("(?i)(st|nd|rd|th)"), "")
            .replace(Regex("\\s+"), " ")

        return formatters.firstNotNullOfOrNull { formatter ->
            try {
                LocalDate.parse(cleaned, formatter)
            } catch (_: DateTimeParseException) {
                null
            }
        }
    }

    private fun extractDateText(message: String): String? {
        return datePatterns.firstNotNullOfOrNull { pattern ->
            pattern.find(message)?.value?.trim()
        }
    }

    private fun normalizeDigits(value: String): String = buildString(value.length) {
        value.forEach { character ->
            append(
                when (character) {
                    '०' -> '0'
                    '१' -> '1'
                    '२' -> '2'
                    '३' -> '3'
                    '४' -> '4'
                    '५' -> '5'
                    '६' -> '6'
                    '७' -> '7'
                    '८' -> '8'
                    '९' -> '9'
                    else -> character
                }
            )
        }
    }

    private fun containsDevanagari(value: String): Boolean =
        value.any { it.code in 0x0900..0x097F }

    private fun looksHindiOrHinglish(value: String): Boolean {
        val normalized = value.lowercase(Locale.ROOT)
        return romanHindiKeywords.any(normalized::contains)
    }

    private companion object {
        val ageKeywords = listOf(
            "age", "old am i", "born", "date of birth", "dob",
            "umar", "umr", "janam", "paida", "उम्र", "जन्म", "पैदा"
        )

        val romanHindiKeywords = listOf(
            "umar", "umr", "janam", "paida", "kitni", "kitna", "meri", "mera", "saal"
        )

        val datePatterns = listOf(
            Regex("\\b\\d{4}[-/.]\\d{1,2}[-/.]\\d{1,2}\\b"),
            Regex("\\b\\d{1,2}[-/.]\\d{1,2}[-/.]\\d{4}\\b"),
            Regex("(?i)\\b\\d{1,2}(?:st|nd|rd|th)?\\s+(?:jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:tember)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)\\s+\\d{4}\\b"),
            Regex("(?i)\\b(?:jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:tember)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)\\s+\\d{1,2}(?:st|nd|rd|th)?,?\\s+\\d{4}\\b")
        )

        val formatters = listOf(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(java.time.format.ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("d-M-uuuu").withResolverStyle(java.time.format.ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("d.M.uuuu").withResolverStyle(java.time.format.ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("d MMM uuuu", Locale.ENGLISH).withResolverStyle(java.time.format.ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("d MMMM uuuu", Locale.ENGLISH).withResolverStyle(java.time.format.ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("MMM d, uuuu", Locale.ENGLISH).withResolverStyle(java.time.format.ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("MMMM d, uuuu", Locale.ENGLISH).withResolverStyle(java.time.format.ResolverStyle.STRICT)
        )
    }
}
