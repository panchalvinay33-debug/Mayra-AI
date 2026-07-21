package ai.mayra.app.skills

import java.math.BigDecimal
import java.math.RoundingMode

/** Handles common percentage calculations fully offline. */
class PercentageSkill : MayraSkill {
    override val id = "percentage"
    override val description = "Calculates percentages, percentage change, increases, and decreases offline"

    override fun canHandle(message: String): Boolean = parse(message) != null

    override suspend fun execute(message: String): SkillResult {
        val request = parse(message)
            ?: return SkillResult(
                text = "Try: 20% of 500, 500 ka 20 percent, what percent is 50 of 200, or increase 500 by 10%.",
                isSuccess = false
            )

        if (request.operation == Operation.WHAT_PERCENT && request.second == 0.0) {
            return SkillResult(
                text = "I can't calculate a percentage with zero as the total.",
                isSuccess = false
            )
        }

        val result = when (request.operation) {
            Operation.PERCENT_OF -> request.first / 100.0 * request.second
            Operation.WHAT_PERCENT -> request.first / request.second * 100.0
            Operation.INCREASE_BY -> request.first * (1.0 + request.second / 100.0)
            Operation.DECREASE_BY -> request.first * (1.0 - request.second / 100.0)
        }

        val text = when (request.operation) {
            Operation.PERCENT_OF ->
                "${format(request.first)}% of ${format(request.second)} is ${format(result)}."
            Operation.WHAT_PERCENT ->
                "${format(request.first)} is ${format(result)}% of ${format(request.second)}."
            Operation.INCREASE_BY ->
                "${format(request.first)} increased by ${format(request.second)}% is ${format(result)}."
            Operation.DECREASE_BY ->
                "${format(request.first)} decreased by ${format(request.second)}% is ${format(result)}."
        }

        return SkillResult(
            text = text,
            metadata = mapOf(
                "operation" to request.operation.name.lowercase(),
                "result" to format(result)
            )
        )
    }

    private fun parse(message: String): Request? {
        val normalized = message.trim().lowercase()
            .replace(",", "")
            .replace("percentage", "percent")

        increasePattern.find(normalized)?.let { match ->
            return Request(
                operation = Operation.INCREASE_BY,
                first = match.groupValues[1].toDouble(),
                second = match.groupValues[2].toDouble()
            )
        }

        decreasePattern.find(normalized)?.let { match ->
            return Request(
                operation = Operation.DECREASE_BY,
                first = match.groupValues[1].toDouble(),
                second = match.groupValues[2].toDouble()
            )
        }

        whatPercentPattern.find(normalized)?.let { match ->
            return Request(
                operation = Operation.WHAT_PERCENT,
                first = match.groupValues[1].toDouble(),
                second = match.groupValues[2].toDouble()
            )
        }

        percentOfPattern.find(normalized)?.let { match ->
            return Request(
                operation = Operation.PERCENT_OF,
                first = match.groupValues[1].toDouble(),
                second = match.groupValues[2].toDouble()
            )
        }

        hindiPercentPattern.find(normalized)?.let { match ->
            return Request(
                operation = Operation.PERCENT_OF,
                first = match.groupValues[2].toDouble(),
                second = match.groupValues[1].toDouble()
            )
        }

        return null
    }

    private fun format(value: Double): String = BigDecimal.valueOf(value)
        .setScale(6, RoundingMode.HALF_UP)
        .stripTrailingZeros()
        .toPlainString()

    private data class Request(
        val operation: Operation,
        val first: Double,
        val second: Double
    )

    private enum class Operation {
        PERCENT_OF,
        WHAT_PERCENT,
        INCREASE_BY,
        DECREASE_BY
    }

    private companion object {
        const val NUMBER = "(-?\\d+(?:\\.\\d+)?)"

        val percentOfPattern = Regex(
            "$NUMBER\\s*(?:%|percent)\\s*(?:of|ka|ki|ke)\\s*$NUMBER"
        )
        val hindiPercentPattern = Regex(
            "$NUMBER\\s*(?:ka|ki|ke)\\s*$NUMBER\\s*(?:%|percent)"
        )
        val whatPercentPattern = Regex(
            "(?:what\\s+percent\\s+is\\s+)?$NUMBER\\s+(?:is\\s+what\\s+percent\\s+of|out\\s+of|is\\s+of)\\s+$NUMBER"
        )
        val increasePattern = Regex(
            "(?:increase|add)\\s+$NUMBER\\s+(?:by|se)\\s+$NUMBER\\s*(?:%|percent)"
        )
        val decreasePattern = Regex(
            "(?:decrease|reduce|subtract)\\s+$NUMBER\\s+(?:by|se)\\s+$NUMBER\\s*(?:%|percent)"
        )
    }
}
