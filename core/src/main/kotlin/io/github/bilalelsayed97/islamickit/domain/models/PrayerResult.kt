package io.github.bilalelsayed97.islamickit.domain.models

import io.github.bilalelsayed97.islamickit.domain.enums.Prayer
import io.github.bilalelsayed97.islamickit.domain.enums.TimeFormat

/**
 * The result of a timings calculation for a single date and location.
 *
 * Bundles the computed [timings], the [date] block (Gregorian + Hijri) and the
 * [meta] echo — the same triple aladhan returns under `data`. A calendar is a
 * list of these.
 */
data class PrayerResult(
    val timings: PrayerTimings,
    val date: DateInfo,
    val meta: CalculationMeta,
) {
    /** Shortcut to a single [PrayerTime]. */
    fun time(prayer: Prayer): PrayerTime = timings.time(prayer)

    /** Shortcut to a single formatted prayer string. */
    @JvmOverloads
    fun formatted(prayer: Prayer, format: TimeFormat = TimeFormat.H24): String = timings.formatted(prayer, format)
}
