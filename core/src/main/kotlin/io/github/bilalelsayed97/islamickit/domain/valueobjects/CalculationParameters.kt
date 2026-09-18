package io.github.bilalelsayed97.islamickit.domain.valueobjects

import io.github.bilalelsayed97.islamickit.domain.enums.AsrSchool
import io.github.bilalelsayed97.islamickit.domain.enums.CalculationMethod
import io.github.bilalelsayed97.islamickit.domain.enums.CalendarMethod
import io.github.bilalelsayed97.islamickit.domain.enums.HighLatitudeRule
import io.github.bilalelsayed97.islamickit.domain.enums.MidnightMode
import io.github.bilalelsayed97.islamickit.domain.enums.Shafaq
import io.github.bilalelsayed97.islamickit.time.UtcOffset

/**
 * All configuration for a calculation. Immutable; use [copy] to derive a
 * tweaked copy (fluent-builder style).
 *
 * The caller supplies [utcOffset] because the package has no timezone
 * database — this keeps it dependency-free. Include any DST in the offset you
 * pass for the target date.
 */
data class CalculationParameters @JvmOverloads constructor(
    val method: CalculationMethod = CalculationMethod.MWL,
    /** Params used when [method] is [CalculationMethod.CUSTOM]. */
    val customMethod: MethodParams? = null,
    val school: AsrSchool = AsrSchool.STANDARD,
    /** Overrides the school's Asr shadow factor when non-null. */
    val asrShadowFactor: Double? = null,
    /** Overrides the method's implied midnight mode when non-null. */
    val midnightMode: MidnightMode? = null,
    /**
     * How Fajr and Isha are bounded when the sun never reaches their angle.
     *
     * Defaults to [HighLatitudeRule.MIDDLE_OF_NIGHT]. [HighLatitudeRule.NONE]
     * disables the bound entirely, so an unreachable angle yields an invalid
     * time instead.
     */
    val highLatitudeRule: HighLatitudeRule = HighLatitudeRule.MIDDLE_OF_NIGHT,
    /** UTC offset for the target date (caller-provided; include DST if relevant). */
    val utcOffset: UtcOffset = UtcOffset.ZERO,
    /** Observer elevation in metres (affects sunrise/sunset). */
    val elevation: Double = 0.0,
    /** Shafaq used by the Moonsighting method for Isha. */
    val shafaq: Shafaq = Shafaq.GENERAL,
    /** Per-prayer tuning offsets in minutes (aladhan `tune`). */
    val tune: Tune = Tune.NONE,
    /** Minutes before Fajr for Imsak. */
    val imsakMinutes: Int = 10,
    /** Minutes added to Dhuhr. */
    val dhuhrMinutes: Int = 0,
    /** Hijri calendar method for the date block. */
    val calendarMethod: CalendarMethod = CalendarMethod.HJCOSA,
    /** Optional timezone label echoed in `meta.timezone` (informational only). */
    val timezoneName: String? = null,
) {
    /** The params in effect (custom-aware). */
    val effectiveParams: MethodParams
        get() = if (method == CalculationMethod.CUSTOM) (customMethod ?: method.params) else method.params

    /**
     * Midnight mode after resolving overrides and the method default.
     *
     * Defaults to [MidnightMode.JAFARI] — the night measured from sunset to the
     * following Fajr, which is the basis used for Midnight and the night
     * thirds. Pass [MidnightMode.STANDARD] explicitly for the
     * sunset-to-sunrise night used by the aladhan API.
     */
    val resolvedMidnightMode: MidnightMode
        get() = midnightMode ?: effectiveParams.midnightMode ?: MidnightMode.JAFARI

    /** Asr shadow factor after resolving the override / school. */
    val resolvedShadowFactor: Double
        get() = asrShadowFactor ?: school.shadowFactor.toDouble()

    /**
     * High-latitude rule in effect.
     *
     * Kept as a separate getter for the aladhan `meta` echo; the Moonsighting
     * method supplies its own seasonal bounds and only consults this to see
     * whether bounding is switched off entirely.
     */
    val resolvedHighLatitudeRule: HighLatitudeRule
        get() = highLatitudeRule
}
