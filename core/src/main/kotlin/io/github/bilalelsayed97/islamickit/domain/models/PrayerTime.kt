package io.github.bilalelsayed97.islamickit.domain.models

import io.github.bilalelsayed97.islamickit.domain.enums.Language
import io.github.bilalelsayed97.islamickit.domain.enums.Prayer
import io.github.bilalelsayed97.islamickit.domain.enums.TimeFormat
import io.github.bilalelsayed97.islamickit.domain.services.TimeFormatting
import io.github.bilalelsayed97.islamickit.internal.DartMath
import io.github.bilalelsayed97.islamickit.time.CivilDate
import io.github.bilalelsayed97.islamickit.time.UtcOffset

/**
 * A single computed prayer time.
 *
 * [hours] is the raw fractional-hours value in the *local* (offset-adjusted)
 * day. It may be negative or exceed 24 when the event rolls into the previous
 * or next calendar day; formatting and [toEpochMillis] handle the wrap. A
 * `null` (or NaN) [hours] means the time is invalid for the given latitude.
 */
data class PrayerTime(
    val prayer: Prayer,
    val hours: Double?,
    /** The civil calendar date the times were computed for. */
    val date: CivilDate,
    /** The UTC offset used to localize the times. */
    val utcOffset: UtcOffset,
) {
    /** Whether the time could be computed. */
    val isValid: Boolean
        get() = hours != null && !hours.isNaN()

    /** Localized display name of the prayer. */
    fun name(language: Language): String = prayer.localizedName(language)

    /** Formats the time using [format] (defaults to 24-hour). */
    @JvmOverloads
    fun format(format: TimeFormat = TimeFormat.H24): String =
        TimeFormatting.formatHours(hours, format, date, utcOffset)

    /**
     * The absolute instant as milliseconds since the Unix epoch, or `null`
     * if invalid. Equivalent to the Dart `toUtc()`.
     */
    fun toEpochMillis(): Long? {
        if (!isValid) return null
        val wall = date.utcMidnightEpochMillis() + DartMath.round(hours!! * 3_600_000.0)
        return wall - utcOffset.totalSeconds * 1000L
    }

    override fun toString(): String = "${prayer.key}: ${format()}"
}

/** The full set of computed times for a single date and location. */
data class PrayerTimings(
    /** Raw fractional-hours per prayer (null/NaN = invalid), in [Prayer] order. */
    val raw: Map<Prayer, Double?>,
    /** The civil calendar date the times were computed for. */
    val date: CivilDate,
    /** The UTC offset used to localize the times. */
    val utcOffset: UtcOffset,
) {
    /** Returns the [PrayerTime] for [prayer]. */
    fun time(prayer: Prayer): PrayerTime = PrayerTime(prayer, raw[prayer], date, utcOffset)

    /** Formats a single prayer. */
    @JvmOverloads
    fun formatted(prayer: Prayer, format: TimeFormat = TimeFormat.H24): String = time(prayer).format(format)

    /** A map of every present prayer to its formatted string, in [raw] order. */
    @JvmOverloads
    fun toFormattedMap(format: TimeFormat = TimeFormat.H24): Map<Prayer, String> {
        val out = LinkedHashMap<Prayer, String>()
        for (prayer in raw.keys) out[prayer] = formatted(prayer, format)
        return out
    }
}
