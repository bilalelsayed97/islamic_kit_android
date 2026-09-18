package io.github.bilalelsayed97.islamickit.internal

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.math.truncate

/**
 * Numeric helpers that reproduce Dart semantics where the JVM differs.
 *
 * The JDK's half-up `round` and Kotlin's `roundTo*` extensions are deliberately
 * absent from the engine: they round half up (or half even), while Dart's
 * `round()` rounds half *away from zero* and the astronomy's `javaRound` is
 * `floor(x + 0.5)`.
 */
internal object DartMath {
    /**
     * Dart `double.round()`: the nearest integer, ties away from zero.
     *
     * `x - truncate(x)` is exact for every finite double, so the tie test is
     * exact as well.
     *
     * @throws IllegalArgumentException for NaN or infinite input (Dart throws
     *   `UnsupportedError`).
     */
    fun round(x: Double): Long {
        require(!x.isNaN() && !x.isInfinite()) { "Cannot round $x" }
        val t = truncate(x)
        val frac = abs(x - t)
        return if (frac >= 0.5) (if (x < 0) t - 1 else t + 1).toLong() else t.toLong()
    }

    /**
     * Dart `double.toString()`: the ECMAScript `Number::toString` algorithm
     * (shortest digit string that round-trips, laid out in fixed notation for
     * exponents in `[-6, 21)` and in `d.ddde±x` form outside) plus Dart's
     * trailing `.0` on integral values.
     *
     * The digits are found by rounding the exact binary value to 1..17
     * significant decimals and keeping the first that parses back to the same
     * double, which is the shortest round-trip form the JDK's `Double.toString`
     * only guarantees from JDK 19 onwards.
     */
    fun doubleToString(value: Double): String {
        if (value.isNaN()) return "NaN"
        if (value == Double.POSITIVE_INFINITY) return "Infinity"
        if (value == Double.NEGATIVE_INFINITY) return "-Infinity"
        if (value == 0.0) return if (1.0 / value < 0) "-0.0" else "0.0"

        val negative = value < 0
        val exact = BigDecimal(abs(value))
        var shortest: BigDecimal = exact
        for (precision in 1..17) {
            val candidate = exact.round(MathContext(precision, RoundingMode.HALF_EVEN))
            if (candidate.toDouble() == abs(value)) {
                shortest = candidate
                break
            }
        }
        val normalized = shortest.stripTrailingZeros()
        // value = 0.d1d2...dk × 10^n
        val digits = normalized.unscaledValue().toString()
        val k = digits.length
        val n = k - normalized.scale()

        val sb = StringBuilder()
        if (negative) sb.append('-')
        when {
            n in k..21 -> {
                sb.append(digits)
                repeat(n - k) { sb.append('0') }
                sb.append(".0")
            }
            n in 1 until k -> {
                sb.append(digits, 0, n).append('.').append(digits, n, k)
            }
            n in -5..0 -> {
                sb.append("0.")
                repeat(-n) { sb.append('0') }
                sb.append(digits)
            }
            else -> {
                val exp = n - 1
                sb.append(digits[0])
                if (k > 1) sb.append('.').append(digits, 1, k)
                sb.append('e').append(if (exp >= 0) "+" else "-").append(abs(exp))
            }
        }
        return sb.toString()
    }
}
