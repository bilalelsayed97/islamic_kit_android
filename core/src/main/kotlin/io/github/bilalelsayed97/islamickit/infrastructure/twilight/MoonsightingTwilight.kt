package io.github.bilalelsayed97.islamickit.infrastructure.twilight

import io.github.bilalelsayed97.islamickit.domain.enums.Shafaq
import io.github.bilalelsayed97.islamickit.domain.ports.TwilightStrategy
import io.github.bilalelsayed97.islamickit.infrastructure.astronomy.Astronomical
import io.github.bilalelsayed97.islamickit.time.CivilDate
import kotlin.math.abs

/**
 * Moonsighting Committee Worldwide Fajr/Isha twilight.
 *
 * A piecewise-linear interpolation of "minutes from sunrise/sunset" driven by
 * the day count since the winter (northern) or summer (southern) solstice,
 * with per-latitude coefficients, as published by the committee.
 */
class MoonsightingTwilight : TwilightStrategy {
    override fun fajrSecondsBeforeSunrise(date: CivilDate, latitude: Double): Int {
        val absLat = abs(latitude)
        val minutes = interpolate(
            daysSinceSolstice(date, latitude),
            75 + 28.65 / 55.0 * absLat,
            75 + 19.44 / 55.0 * absLat,
            75 + 32.74 / 55.0 * absLat,
            75 + 48.10 / 55.0 * absLat,
        )
        return Astronomical.javaRound(minutes * 60.0)
    }

    override fun ishaSecondsAfterSunset(date: CivilDate, latitude: Double, shafaq: Shafaq): Int {
        val absLat = abs(latitude)
        val c = ishaCoefficients(shafaq, absLat)
        val minutes = interpolate(daysSinceSolstice(date, latitude), c[0], c[1], c[2], c[3])
        return Astronomical.javaRound(minutes * 60.0)
    }

    /**
     * Whole days since the hemisphere's solstice, wrapped into the year.
     *
     * Derived from the day-of-year rather than a date subtraction so that leap
     * years and the New Year boundary land on the published day.
     */
    private fun daysSinceSolstice(date: CivilDate, latitude: Double): Int {
        val leap = date.isLeapYear
        val daysInYear = if (leap) 366 else 365
        val dayOfYear = date.dayOfYear

        if (latitude >= 0) {
            // The December solstice sits 10 days before year-end.
            val days = dayOfYear + 10
            return if (days >= daysInYear) days - daysInYear else days
        }
        val southernOffset = if (leap) 173 else 172
        val days = dayOfYear - southernOffset
        return if (days < 0) days + daysInYear else days
    }

    private fun interpolate(dyy: Int, a: Double, b: Double, c: Double, d: Double): Double {
        if (dyy < 91) return a + (b - a) / 91 * dyy
        if (dyy < 137) return b + (c - b) / 46 * (dyy - 91)
        if (dyy < 183) return c + (d - c) / 46 * (dyy - 137)
        if (dyy < 229) return d + (c - d) / 46 * (dyy - 183)
        if (dyy < 275) return c + (b - c) / 46 * (dyy - 229)
        return b + (a - b) / 91 * (dyy - 275)
    }

    /** Returns the `[a, b, c, d]` seasonal coefficients for [shafaq]. */
    private fun ishaCoefficients(shafaq: Shafaq, absLat: Double): DoubleArray = when (shafaq) {
        Shafaq.AHMER -> doubleArrayOf(
            62 + 17.4 / 55.0 * absLat,
            62 - 7.16 / 55.0 * absLat,
            62 + 5.12 / 55.0 * absLat,
            62 + 19.44 / 55.0 * absLat,
        )
        Shafaq.ABYAD -> doubleArrayOf(
            75 + 25.6 / 55.0 * absLat,
            75 + 7.16 / 55.0 * absLat,
            75 + 36.84 / 55.0 * absLat,
            75 + 81.84 / 55.0 * absLat,
        )
        Shafaq.GENERAL -> doubleArrayOf(
            75 + 25.6 / 55.0 * absLat,
            75 + 2.05 / 55.0 * absLat,
            75 - 9.21 / 55.0 * absLat,
            75 + 6.14 / 55.0 * absLat,
        )
    }
}
