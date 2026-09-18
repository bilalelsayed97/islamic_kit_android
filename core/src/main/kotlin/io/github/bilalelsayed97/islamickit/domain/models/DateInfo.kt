package io.github.bilalelsayed97.islamickit.domain.models

/**
 * The combined date block returned with each result: a human-readable label,
 * a Unix timestamp, and both the Gregorian and Hijri representations.
 */
data class DateInfo(
    /** e.g. `"01 Jan 2025"`. */
    val readable: String,
    /** Unix timestamp (seconds) of the civil date at the used UTC offset. */
    val timestamp: Long,
    val gregorian: GregorianDate,
    val hijri: HijriDate,
)
