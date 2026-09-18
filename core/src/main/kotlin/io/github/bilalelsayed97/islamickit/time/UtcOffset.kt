package io.github.bilalelsayed97.islamickit.time

import io.github.bilalelsayed97.islamickit.internal.DartStrings
import java.util.TimeZone

/**
 * A fixed offset from UTC, stored as whole seconds (the Dart package uses a
 * `Duration`, which the engine reads in seconds).
 *
 * The library never consults a timezone database: the caller supplies the
 * offset that applies on the target date, including any daylight saving.
 * [of] derives it from a [java.util.TimeZone] when one is at hand.
 */
data class UtcOffset(
    /** Signed offset from UTC in seconds. */
    val totalSeconds: Int,
) : Comparable<UtcOffset> {
    /** Signed offset in whole minutes, truncated toward zero. */
    val totalMinutes: Int
        get() = totalSeconds / 60

    /** Signed offset in whole hours, truncated toward zero. */
    val hours: Int
        get() = totalSeconds / 3600

    /** The minutes part of the absolute offset (`0..59`). */
    val minutesPart: Int
        get() = (abs().totalSeconds / 60) % 60

    /** Whether the offset is west of UTC. */
    val isNegative: Boolean
        get() = totalSeconds < 0

    /** The magnitude of this offset. */
    fun abs(): UtcOffset = if (totalSeconds < 0) UtcOffset(-totalSeconds) else this

    /** `±HH:MM`, e.g. `"+05:30"`, `"-03:30"`, `"+00:00"`. */
    fun toIsoString(): String {
        val sign = if (isNegative) "-" else "+"
        val magnitude = abs()
        return "$sign${DartStrings.two(magnitude.hours)}:${DartStrings.two(magnitude.minutesPart)}"
    }

    override fun compareTo(other: UtcOffset): Int = totalSeconds.compareTo(other.totalSeconds)

    override fun toString(): String = "UtcOffset(${toIsoString()})"

    companion object {
        /** UTC itself. */
        @JvmField
        val ZERO: UtcOffset = UtcOffset(0)

        /** An offset of [hours] and [minutes] (both carry the sign of the offset, e.g. `ofHours(-3, -30)`). */
        @JvmStatic
        @JvmOverloads
        fun ofHours(hours: Int, minutes: Int = 0): UtcOffset = UtcOffset(hours * 3600 + minutes * 60)

        /** An offset of [minutes]. */
        @JvmStatic
        fun ofMinutes(minutes: Int): UtcOffset = UtcOffset(minutes * 60)

        /**
         * Parses `±HH:MM`, `±HHMM`, `±HH` or `Z`.
         *
         * @throws IllegalArgumentException when [text] is not an offset.
         */
        @JvmStatic
        fun parse(text: String): UtcOffset {
            val s = text.trim()
            if (s == "Z" || s == "z") return ZERO
            require(s.length >= 2 && (s[0] == '+' || s[0] == '-')) { "Invalid UTC offset: $text" }
            val sign = if (s[0] == '-') -1 else 1
            val body = s.substring(1).replace(":", "")
            require(body.length == 2 || body.length == 4) { "Invalid UTC offset: $text" }
            val h = body.substring(0, 2).toIntOrNull()
            val m = if (body.length == 4) body.substring(2, 4).toIntOrNull() else 0
            require(h != null && m != null && m in 0..59) { "Invalid UTC offset: $text" }
            return UtcOffset(sign * (h * 3600 + m * 60))
        }

        /** The offset [timeZone] applies at the instant [epochMillis]. */
        @JvmStatic
        fun of(timeZone: TimeZone, epochMillis: Long): UtcOffset =
            UtcOffset(timeZone.getOffset(epochMillis) / 1000)

        /**
         * The offset [timeZone] applies at local noon of [date] — the value a
         * caller passes as `CalculationParameters.utcOffset` for that day.
         */
        @JvmStatic
        fun of(timeZone: TimeZone, date: CivilDate): UtcOffset {
            val utcNoon = date.utcMidnightEpochMillis() + 12L * 3_600_000L
            // Local noon is UTC noon minus the offset in force around then.
            val approx = timeZone.getOffset(utcNoon)
            return of(timeZone, utcNoon - approx)
        }
    }
}
