package ai.mayra.app.skills

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

/** Performs deterministic offline conversions for common length, weight and temperature units. */
class UnitConversionSkill : MayraSkill {
    override val id: String = "unit-conversion"
    override val description: String = "Converts common length, weight, and temperature units offline"

    private val requestPattern = Regex(
        pattern = """^\s*(?:convert\s+)?(-?\d+(?:\.\d+)?)\s*([a-zA-Z°]+)\s+(?:to|in|into)\s+([a-zA-Z°]+)\s*$""",
        option = RegexOption.IGNORE_CASE
    )

    override fun canHandle(message: String): Boolean = requestPattern.matches(message.trim())

    override suspend fun execute(message: String): SkillResult {
        val match = requestPattern.matchEntire(message.trim())
            ?: return SkillResult("Please use a format like: convert 5 km to miles.", isSuccess = false)

        val value = match.groupValues[1].toDoubleOrNull()
            ?: return SkillResult("I couldn't read that number.", isSuccess = false)
        val from = normalize(match.groupValues[2])
        val to = normalize(match.groupValues[3])

        val converted = convert(value, from, to)
            ?: return SkillResult(
                text = "I can't convert $from to $to yet.",
                isSuccess = false,
                metadata = mapOf("from" to from, "to" to to)
            )

        return SkillResult(
            text = "${format(value)} ${label(from)} = ${format(converted)} ${label(to)}",
            metadata = mapOf(
                "from" to from,
                "to" to to,
                "input" to value.toString(),
                "result" to converted.toString()
            )
        )
    }

    private fun convert(value: Double, from: String, to: String): Double? {
        if (from == to) return value

        return when {
            from in lengthFactors && to in lengthFactors ->
                value * lengthFactors.getValue(from) / lengthFactors.getValue(to)

            from in weightFactors && to in weightFactors ->
                value * weightFactors.getValue(from) / weightFactors.getValue(to)

            from in temperatureUnits && to in temperatureUnits -> convertTemperature(value, from, to)
            else -> null
        }
    }

    private fun convertTemperature(value: Double, from: String, to: String): Double {
        val celsius = when (from) {
            "c" -> value
            "f" -> (value - 32.0) * 5.0 / 9.0
            "k" -> value - 273.15
            else -> value
        }
        return when (to) {
            "c" -> celsius
            "f" -> celsius * 9.0 / 5.0 + 32.0
            "k" -> celsius + 273.15
            else -> celsius
        }
    }

    private fun normalize(raw: String): String {
        val unit = raw.lowercase(Locale.ROOT).replace("°", "")
        return aliases[unit] ?: unit
    }

    private fun label(unit: String): String = when (unit) {
        "c" -> "°C"
        "f" -> "°F"
        "k" -> "K"
        else -> unit
    }

    private fun format(value: Double): String = BigDecimal.valueOf(value)
        .setScale(4, RoundingMode.HALF_UP)
        .stripTrailingZeros()
        .toPlainString()

    private companion object {
        val lengthFactors = mapOf(
            "mm" to 0.001,
            "cm" to 0.01,
            "m" to 1.0,
            "km" to 1000.0,
            "in" to 0.0254,
            "ft" to 0.3048,
            "yd" to 0.9144,
            "mi" to 1609.344
        )

        val weightFactors = mapOf(
            "mg" to 0.001,
            "g" to 1.0,
            "kg" to 1000.0,
            "oz" to 28.349523125,
            "lb" to 453.59237
        )

        val temperatureUnits = setOf("c", "f", "k")

        val aliases = mapOf(
            "millimeter" to "mm", "millimeters" to "mm",
            "centimeter" to "cm", "centimeters" to "cm",
            "meter" to "m", "meters" to "m", "metre" to "m", "metres" to "m",
            "kilometer" to "km", "kilometers" to "km", "kilometre" to "km", "kilometres" to "km",
            "inch" to "in", "inches" to "in",
            "foot" to "ft", "feet" to "ft",
            "yard" to "yd", "yards" to "yd",
            "mile" to "mi", "miles" to "mi",
            "milligram" to "mg", "milligrams" to "mg",
            "gram" to "g", "grams" to "g",
            "kilogram" to "kg", "kilograms" to "kg",
            "ounce" to "oz", "ounces" to "oz",
            "pound" to "lb", "pounds" to "lb",
            "celsius" to "c", "centigrade" to "c",
            "fahrenheit" to "f",
            "kelvin" to "k"
        )
    }
}
