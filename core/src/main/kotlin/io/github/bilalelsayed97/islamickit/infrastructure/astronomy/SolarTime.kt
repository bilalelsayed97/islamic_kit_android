package io.github.bilalelsayed97.islamickit.infrastructure.astronomy

import io.github.bilalelsayed97.islamickit.domain.valueobjects.Coordinates
import io.github.bilalelsayed97.islamickit.time.CivilDate
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Solar events for one civil date at one location, in fractional **UTC**
 * hours measured from 00:00 UTC of that date.
 *
 * Coordinates are computed for the previous, current and next day so that
 * right ascension and declination can be interpolated to the moment of each
 * event — the step that separates this from single-sample approximations and
 * keeps results accurate to the second.
 *
 * Values may fall outside `[0, 24)` when an event lands on an adjacent UTC
 * day; the raw value is kept so day rollover survives. A `NaN` result means
 * the sun never reaches the requested altitude on that date.
 */
class SolarTime @JvmOverloads constructor(
    year: Int,
    month: Int,
    day: Int,
    val coordinates: Coordinates,
    /**
     * The observer's height above sea level in metres. It dips the apparent
     * horizon by `0.0347 · √metres` degrees, bringing sunrise earlier and
     * sunset later. This is a package extension the reference standard
     * algorithm does not model; at `0` the behaviour is identical.
     */
    elevation: Double = 0.0,
) {
    /** Computes the solar events for [date] at [coordinates]. */
    @JvmOverloads
    constructor(date: CivilDate, coordinates: Coordinates, elevation: Double = 0.0) :
        this(date.year, date.month, date.day, coordinates, elevation)

    private val solar: SolarCoordinates
    private val previous: SolarCoordinates
    private val next: SolarCoordinates

    /** Approximate transit as a fraction of the day (interpolation seed). */
    val approximateTransit: Double

    /** Solar transit — Dhuhr's astronomical basis. Fractional UTC hours. */
    val transit: Double

    /** Apparent sunrise, fractional UTC hours. */
    val sunrise: Double

    /** Apparent sunset, fractional UTC hours. */
    val sunset: Double

    init {
        val julianDay = Astronomical.julianDay(year, month, day)
        // JD is continuous, so ±1 is exactly the adjacent civil day at 0h UTC.
        solar = SolarCoordinates(julianDay)
        previous = SolarCoordinates(julianDay - 1)
        next = SolarCoordinates(julianDay + 1)

        approximateTransit = Astronomical.approximateTransit(
            coordinates.longitude,
            solar.apparentSiderealTime,
            solar.rightAscension,
        )

        transit = Astronomical.correctedTransit(
            approximateTransit,
            coordinates.longitude,
            solar.apparentSiderealTime,
            solar.rightAscension,
            previous.rightAscension,
            next.rightAscension,
        )

        val horizon = if (elevation > 0) {
            Astronomical.HORIZON_ALTITUDE - 0.0347 * sqrt(elevation)
        } else {
            Astronomical.HORIZON_ALTITUDE
        }

        sunrise = hourAngle(horizon, afterTransit = false)
        sunset = hourAngle(horizon, afterTransit = true)
    }

    /**
     * Time at which the sun's centre sits at [altitude] degrees (negative =
     * below the horizon), before or after transit. Fractional UTC hours.
     */
    fun hourAngle(altitude: Double, afterTransit: Boolean): Double {
        return Astronomical.correctedHourAngle(
            approximateTransit = approximateTransit,
            altitude = altitude,
            latitude = coordinates.latitude,
            longitude = coordinates.longitude,
            afterTransit = afterTransit,
            siderealTime = solar.apparentSiderealTime,
            rightAscension = solar.rightAscension,
            previousRightAscension = previous.rightAscension,
            nextRightAscension = next.rightAscension,
            declination = solar.declination,
            previousDeclination = previous.declination,
            nextDeclination = next.declination,
        )
    }

    /**
     * Time at which an object's shadow has grown by [shadowFactor] times its
     * own length beyond its shadow at transit — the Asr definition (1 for
     * Shafi'i/Maliki/Hanbali, 2 for Hanafi). Fractional UTC hours.
     */
    fun afternoon(shadowFactor: Double): Double {
        val tangent = abs(coordinates.latitude - solar.declination)
        val inverse = shadowFactor + Astronomical.tan(tangent)
        return hourAngle(Astronomical.arctan(1.0 / inverse), afterTransit = true)
    }
}
