package ai.mayra.app.skills

import java.math.BigDecimal
import java.math.MathContext

/**
 * Deterministic, offline calculator for basic arithmetic.
 *
 * Supported operators: +, -, *, / and parentheses. The parser deliberately
 * rejects any other characters so calculations never need a remote provider.
 */
class CalculatorSkill : MayraSkill {
    override val id: String = "calculator"
    override val description: String = "Solves basic arithmetic locally and offline"

    override fun canHandle(message: String): Boolean = extractExpression(message) != null

    override suspend fun execute(message: String): SkillResult {
        val expression = extractExpression(message)
            ?: return SkillResult(
                text = "Please give me a calculation such as 25 * 4 or (10 + 5) / 3.",
                isSuccess = false
            )

        return runCatching { ExpressionParser(expression).parse() }
            .fold(
                onSuccess = { value ->
                    SkillResult(
                        text = "${expression.trim()} = ${value.stripTrailingZeros().toPlainString()}",
                        metadata = mapOf(
                            "skill" to id,
                            "expression" to expression.trim()
                        )
                    )
                },
                onFailure = {
                    SkillResult(
                        text = "I couldn't calculate that. Please check the expression and try again.",
                        isSuccess = false,
                        metadata = mapOf("skill" to id)
                    )
                }
            )
    }

    private fun extractExpression(message: String): String? {
        val trimmed = message.trim()
        if (trimmed.isEmpty()) return null

        val commandPrefixes = listOf("calculate ", "calc ", "solve ", "kitna hai ")
        val candidate = commandPrefixes.firstOrNull { trimmed.startsWith(it, ignoreCase = true) }
            ?.let { trimmed.drop(it.length).trim() }
            ?: trimmed

        if (!candidate.any(Char::isDigit)) return null
        if (!candidate.any { it in "+-*/" }) return null
        if (candidate.any { !it.isDigit() && !it.isWhitespace() && it !in ".+-*/()" }) return null

        return candidate
    }
}

private class ExpressionParser(private val source: String) {
    private var index: Int = 0

    fun parse(): BigDecimal {
        val result = parseExpression()
        skipWhitespace()
        require(index == source.length) { "Unexpected token" }
        return result
    }

    private fun parseExpression(): BigDecimal {
        var value = parseTerm()
        while (true) {
            skipWhitespace()
            value = when {
                consume('+') -> value.add(parseTerm())
                consume('-') -> value.subtract(parseTerm())
                else -> return value
            }
        }
    }

    private fun parseTerm(): BigDecimal {
        var value = parseFactor()
        while (true) {
            skipWhitespace()
            value = when {
                consume('*') -> value.multiply(parseFactor())
                consume('/') -> {
                    val divisor = parseFactor()
                    require(divisor.compareTo(BigDecimal.ZERO) != 0) { "Division by zero" }
                    value.divide(divisor, MathContext.DECIMAL64)
                }
                else -> return value
            }
        }
    }

    private fun parseFactor(): BigDecimal {
        skipWhitespace()

        if (consume('+')) return parseFactor()
        if (consume('-')) return parseFactor().negate()

        if (consume('(')) {
            val nested = parseExpression()
            skipWhitespace()
            require(consume(')')) { "Missing closing parenthesis" }
            return nested
        }

        return parseNumber()
    }

    private fun parseNumber(): BigDecimal {
        skipWhitespace()
        val start = index
        var decimalSeen = false

        while (index < source.length) {
            val current = source[index]
            when {
                current.isDigit() -> index++
                current == '.' && !decimalSeen -> {
                    decimalSeen = true
                    index++
                }
                else -> break
            }
        }

        require(index > start) { "Number expected" }
        return source.substring(start, index).toBigDecimal()
    }

    private fun consume(expected: Char): Boolean {
        if (index < source.length && source[index] == expected) {
            index++
            return true
        }
        return false
    }

    private fun skipWhitespace() {
        while (index < source.length && source[index].isWhitespace()) index++
    }
}
