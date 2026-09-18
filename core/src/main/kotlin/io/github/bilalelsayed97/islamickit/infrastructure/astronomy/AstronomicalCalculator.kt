package io.github.bilalelsayed97.islamickit.infrastructure.astronomy

import io.github.bilalelsayed97.islamickit.domain.enums.HighLatitudeRule
import io.github.bilalelsayed97.islamickit.domain.enums.MidnightMode
import io.github.bilalelsayed97.islamickit.domain.enums.Prayer
import io.github.bilalelsayed97.islamickit.domain.ports.TwilightStrategy
import io.github.bilalelsayed97.islamickit.domain.valueobjects.CalculationParameters
import io.github.bilalelsayed97.islamickit.domain.valueobjects.Coordinates
import io.github.bilalelsayed97.islamickit.domain.valueobjects.MethodParams
import io.github.bilalelsayed97.islamickit.time.CivilDate
import kotlin.math.floor

/** Seconds in a day. The engine works in whole seconds from 00:00 UTC of the requested civil date. */
private const val SECONDS_PER_DAY: Int = 86400

/**
 * Computes raw prayer times (fractional local hours) for a date and location.
 *
 * Values may be negative or exceed 24 when an event rolls into the adjacent
 * day; `null` marks a time that cannot be computed (polar day or polar
 * night).
 *
 * Times are rounded to the nearest minute here, so a formatted result never
 * depends on formatting-time rounding.
 */
class AstronomicalCalculator {
    /**
     * Returns raw fractional local hours for every [Prayer], in [Prayer] order.
     *
     * [twilight] supplies the Moonsighting Committee's seasonal bounds; it is
     * required only when `params.method.usesMoonsighting` is set.
     *
     * [ramadan] selects the method's Ramadan Isha interval where it defines one
     * (Umm al-Qura lengthens 90 minutes to 120). The caller decides, because
     * resolving the Hijri month is a calendar concern, not an astronomical one.
     *
     * @throws IllegalStateException when the method needs a [TwilightStrategy] and none was given.
     */
    @JvmOverloads
    fun compute(
        date: CivilDate,
        coordinates: Coordinates,
        params: CalculationParameters,
        twilight: TwilightStrategy? = null,
        ramadan: Boolean = false,
    ): Map<Prayer, Double?> = Computation(date, coordinates, params, twilight, ramadan).run()
}

private class Computation(
    private val date: CivilDate,
    private val coordinates: Coordinates,
    private val params: CalculationParameters,
    private val twilight: TwilightStrategy?,
    ramadan: Boolean,
) {
    private val mp: MethodParams =
        if (ramadan) params.effectiveParams.forRamadan() else params.effectiveParams

    private val latitude: Double
        get() = coordinates.latitude

    fun run(): Map<Prayer, Double?> {
        val solar = SolarTime(date, coordinates, params.elevation)

        val transit = seconds(solar.transit)
        val sunrise = seconds(solar.sunrise)
        val sunset = seconds(solar.sunset)
        val asr = seconds(solar.afternoon(params.resolvedShadowFactor))

        // Polar day or polar night: the sun never crosses the horizon, so no time
        // can be anchored and the whole day is invalid.
        if (transit == null || sunrise == null || sunset == null || asr == null) {
            val invalid = LinkedHashMap<Prayer, Double?>()
            for (p in Prayer.entries) invalid[p] = null
            return invalid
        }

        val night = (sunrise + SECONDS_PER_DAY) - sunset
        val fajr = fajr(solar, sunrise = sunrise, night = night)
        val maghrib = maghribBase(solar, sunset = sunset)
        val isha = isha(solar, sunset = sunset, maghrib = maghrib, night = night)

        val imsak = if (fajr == null) null else fajr - params.imsakMinutes * 60
        val dhuhr = transit + params.dhuhrMinutes * 60

        val adjustments = mp.adjustments
        val result = LinkedHashMap<Prayer, Int?>()
        result[Prayer.IMSAK] = imsak
        result[Prayer.FAJR] = shift(fajr, adjustments.fajr)
        result[Prayer.SUNRISE] = shift(sunrise, adjustments.sunrise)
        result[Prayer.DHUHR] = shift(dhuhr, adjustments.dhuhr)
        result[Prayer.ASR] = shift(asr, adjustments.asr)
        result[Prayer.SUNSET] = sunset
        result[Prayer.MAGHRIB] = shift(maghrib, adjustments.maghrib)
        result[Prayer.ISHA] = shift(isha, adjustments.isha)

        // Midnight and the night thirds are derived from the *unadjusted* anchors.
        result.putAll(nightTimes(sunset = sunset, sunrise = sunrise, fajr = fajr))

        return finalize(result)
    }

    // ---------------------------------------------------------------------------
    // Individual times
    // ---------------------------------------------------------------------------

    /** Fajr, floored by the high-latitude safe bound. */
    private fun fajr(solar: SolarTime, sunrise: Int, night: Int): Int? {
        var candidate = seconds(solar.hourAngle(-mp.fajrAngle, afterTransit = false))

        // Above 55°N the Moonsighting method abandons the angle entirely.
        if (usesMoonsighting && latitude >= 55) {
            candidate = sunrise - night / 7
        }

        val rule = params.resolvedHighLatitudeRule
        if (rule == HighLatitudeRule.NONE) return candidate

        val safe = if (usesMoonsighting) {
            sunrise - requireTwilight().fajrSecondsBeforeSunrise(date, latitude)
        } else {
            sunrise - (night * nightPortion(rule, mp.fajrAngle)).toInt()
        }

        if (candidate == null || candidate < safe) return safe
        return candidate
    }

    /** The Maghrib anchor: sunset, or the method's own interval/angle. */
    private fun maghribBase(solar: SolarTime, sunset: Int): Int {
        val minutes = mp.maghribMinutesAfterSunset
        if (minutes != null) return sunset + minutes * 60

        val angle = mp.maghribAngle
        if (angle != null) {
            val byAngle = seconds(solar.hourAngle(-angle, afterTransit = true))
            if (byAngle != null) return byAngle
        }
        return sunset
    }

    /** Isha, capped by the high-latitude safe bound when angle-based. */
    private fun isha(solar: SolarTime, sunset: Int, maghrib: Int, night: Int): Int? {
        // A fixed interval is definitional: no high-latitude bound applies.
        val interval = mp.ishaMinutesAfterMaghrib
        if (interval != null) return maghrib + interval * 60

        val ishaAngle = mp.ishaAngle ?: 0.0
        var candidate = seconds(solar.hourAngle(-ishaAngle, afterTransit = true))

        if (usesMoonsighting && latitude >= 55) {
            candidate = sunset + night / 7
        }

        val rule = params.resolvedHighLatitudeRule
        if (rule == HighLatitudeRule.NONE) return candidate

        val safe = if (usesMoonsighting) {
            sunset + requireTwilight().ishaSecondsAfterSunset(date, latitude, params.shafaq)
        } else {
            sunset + (night * nightPortion(rule, ishaAngle)).toInt()
        }

        if (candidate != null && candidate <= safe) return candidate
        return safe
    }

    /**
     * Midnight and the night thirds.
     *
     * The night runs from sunset to the following Fajr
     * ([MidnightMode.JAFARI], the default) or to the following sunrise
     * ([MidnightMode.STANDARD]).
     */
    private fun nightTimes(sunset: Int, sunrise: Int, fajr: Int?): Map<Prayer, Int?> {
        val anchor = if (params.resolvedMidnightMode == MidnightMode.STANDARD) sunrise else fajr
        val out = LinkedHashMap<Prayer, Int?>()
        if (anchor == null) {
            out[Prayer.MIDNIGHT] = null
            out[Prayer.FIRST_THIRD] = null
            out[Prayer.LAST_THIRD] = null
            return out
        }

        val night = (anchor + SECONDS_PER_DAY) - sunset
        out[Prayer.MIDNIGHT] = sunset + night / 2
        out[Prayer.FIRST_THIRD] = sunset + night / 3
        out[Prayer.LAST_THIRD] = sunset + (night * 2) / 3
        return out
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private val usesMoonsighting: Boolean
        get() = params.method.usesMoonsighting

    /**
     * The Moonsighting strategy. Only read when the method needs it, so a
     * caller that never selects Moonsighting need not supply one.
     */
    private fun requireTwilight(): TwilightStrategy =
        twilight ?: throw IllegalStateException(
            "The ${params.method.code} method needs a TwilightStrategy. " +
                "Pass one to AstronomicalCalculator.compute().",
        )

    /** The fraction of the night that bounds a twilight time under [rule]. */
    private fun nightPortion(rule: HighLatitudeRule, angle: Double): Double = when (rule) {
        HighLatitudeRule.ANGLE_BASED -> angle / 60.0
        HighLatitudeRule.ONE_SEVENTH -> 1.0 / 7
        HighLatitudeRule.MIDDLE_OF_NIGHT, HighLatitudeRule.NONE -> 1.0 / 2
    }

    /**
     * Truncates fractional UTC hours to whole seconds, or `null` when the value
     * is not a real time (the sun never reached the requested angle).
     */
    private fun seconds(hours: Double): Int? {
        if (hours.isNaN() || hours.isInfinite()) return null
        return floor(hours * 3600).toInt()
    }

    private fun shift(seconds: Int?, minutes: Int): Int? = if (seconds == null) null else seconds + minutes * 60

    /**
     * Applies user tuning, rounds to the nearest minute and converts to
     * fractional local hours.
     */
    private fun finalize(times: Map<Prayer, Int?>): Map<Prayer, Double?> {
        val tune = params.tune.toMap()
        val offsetSeconds = params.utcOffset.totalSeconds

        val out = LinkedHashMap<Prayer, Double?>()
        for ((prayer, seconds) in times) {
            out[prayer] = toLocalHours(seconds, tune[prayer] ?: 0, offsetSeconds)
        }
        return out
    }

    private fun toLocalHours(seconds: Int?, tuneMinutes: Int, offsetSeconds: Int): Double? {
        if (seconds == null) return null
        val tuned = seconds + tuneMinutes * 60
        // Round to the nearest minute: seconds >= 30 advance the minute.
        val minutes = Math.floorDiv(tuned, 60) + (if (Math.floorMod(tuned, 60) >= 30) 1 else 0)
        return (minutes * 60 + offsetSeconds) / 3600.0
    }
}
