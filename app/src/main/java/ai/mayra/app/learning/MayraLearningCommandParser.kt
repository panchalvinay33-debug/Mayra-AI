package ai.mayra.app.learning

sealed interface LearningCommand {
    data class Remember(val key: String, val value: String) : LearningCommand
    data class Forget(val key: String) : LearningCommand
    data object ForgetAll : LearningCommand
    data object ListLearned : LearningCommand
    data object ReviewPending : LearningCommand
    data object None : LearningCommand
}

/**
 * Small deterministic command parser. It never asks the model to decide whether a memory write is
 * trusted; parsed writes still pass through MayraSelfLearningPolicy and owner review.
 */
object MayraLearningCommandParser {
    private val rememberPatterns = listOf(
        Regex("^remember(?: that)?\\s+(.+?)\\s*(?:=|is|:)\\s*(.+)$", RegexOption.IGNORE_CASE),
        Regex("^yaad rakho(?: ki)?\\s+(.+?)\\s*(?:=|hai|:)\\s*(.+)$", RegexOption.IGNORE_CASE),
        Regex("^याद रखो(?: कि)?\\s+(.+?)\\s*(?:=|है|:)\\s*(.+)$")
    )
    private val forgetPatterns = listOf(
        Regex("^forget\\s+(.+)$", RegexOption.IGNORE_CASE),
        Regex("^bhool jao\\s+(.+)$", RegexOption.IGNORE_CASE),
        Regex("^भूल जाओ\\s+(.+)$")
    )

    fun parse(raw: String): LearningCommand {
        val text = raw.trim().replace(Regex("\\s+"), " ")
        if (text.isBlank()) return LearningCommand.None

        val lowercase = text.lowercase()
        if (lowercase in setOf("forget everything", "forget all", "sab bhool jao", "सब भूल जाओ")) {
            return LearningCommand.ForgetAll
        }
        if (lowercase in setOf("what have you learned", "show learned memories", "tumne kya seekha", "तुमने क्या सीखा")) {
            return LearningCommand.ListLearned
        }
        if (lowercase in setOf("review pending memories", "pending memories", "pending yaadein", "लंबित यादें")) {
            return LearningCommand.ReviewPending
        }

        rememberPatterns.forEach { pattern ->
            pattern.matchEntire(text)?.let { match ->
                val key = match.groupValues[1].trim()
                val value = match.groupValues[2].trim()
                if (key.isNotBlank() && value.isNotBlank()) return LearningCommand.Remember(key, value)
            }
        }
        forgetPatterns.forEach { pattern ->
            pattern.matchEntire(text)?.let { match ->
                val key = match.groupValues[1].trim()
                if (key.isNotBlank()) return LearningCommand.Forget(key)
            }
        }
        return LearningCommand.None
    }
}
