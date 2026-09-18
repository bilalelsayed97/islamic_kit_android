package io.github.bilalelsayed97.islamickit.domain.services

import io.github.bilalelsayed97.islamickit.domain.enums.TimeFormat
import io.github.bilalelsayed97.islamickit.internal.DartMath
import io.github.bilalelsayed97.islamickit.internal.DartStrings
import io.github.bilalelsayed97.islamickit.time.CivilDate
import io.github.bilalelsayed97.islamickit.time.UtcOffset
import kotlin.math.ceil
import kotlin.math.floor

/** Formatting of raw fractional-hours values into the aladhan time formats. */
object TimeFormatting {
    /**
     * Sentinel returned when a time cannot be computed (e.g. sun never reaches
     * the required angle at extreme latitudes). Matches the PHP `INVALID_TIME`.
     */
    const val INVALID_TIME: String = "-----"

    /** Positive-modulo wrap to the range `[0, 24)`. */
    @JvmStatic
    fun fixHour(a: Double): Double {
        val r = a - 24 * floor(a / 24)
        return if (r < 0) r + 24 else r
    }

    /**
     * Formats a raw fractional-hours value using [format].
     *
     * Faithful port of `PrayerTimes::getFormattedTime`: adds 0.5 minutes for
     * rounding before truncating clock formats; the ISO-8601 path uses the raw
     * (pre-rounding) value offset from midnight of [date] and appends
     * [utcOffset]. The ISO date arithmetic is purely civil (no host timezone).
     */
    @JvmStatic
    fun formatHours(hours: Double?, format: TimeFormat, date: CivilDate, utcOffset: UtcOffset): String {
        if (hours == null || hours.isNaN()) return INVALID_TIME
        if (format == TimeFormat.FLOAT) return DartMath.doubleToString(hours)

        // 0.5-minute rounding, applied before truncation for every non-float
        // format (including ISO-8601), matching the PHP original.
        val t = hours + 0.5 / 60

        if (format == TimeFormat.ISO8601) {
            val totalMinutes = if (t > 0) floor(t * 60).toInt() else -ceil(-t * 60).toInt()
            val day = date.plusDays(Math.floorDiv(totalMinutes, 1440))
            val minuteOfDay = Math.floorMod(totalMinutes, 1440)
            val stamp = "${DartStrings.padYear(day.year)}-${DartStrings.two(day.month)}-" +
                "${DartStrings.two(day.day)}T${DartStrings.two(minuteOfDay / 60)}:" +
                "${DartStrings.two(minuteOfDay % 60)}:00"
            return "$stamp${utcOffset.toIsoString()}"
        }

        val ft = fixHour(t)
        val h = floor(ft).toInt()
        val m = floor((ft - h) * 60).toInt()
        if (format == TimeFormat.H24) return "${DartStrings.two(h)}:${DartStrings.two(m)}"

        val hour12 = (h + 12 - 1) % 12 + 1
        val body = "$hour12:${DartStrings.two(m)}"
        if (format == TimeFormat.H12) return "$body ${if (h < 12) "am" else "pm"}"
        return body // H12_NO_SUFFIX
    }
}
