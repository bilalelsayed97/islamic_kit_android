package io.github.bilalelsayed97.islamickit.infrastructure.astronomy

import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.truncate

/**
 * Degree-based trigonometry and the solar-position formulas of Jean Meeus'
 * *Astronomical Algorithms* (2nd ed.).
 *
 * Every angle is in degrees unless the name says otherwise; times are
 * fractional hours. The constants, the operation order and the
 * integer-truncation points are all deliberate — they keep results agreeing
 * with the published reference to the second.
 */
object Astronomical {
    private const val DEG_TO_RAD: Double = PI / 180.0
    private const val RAD_TO_DEG: Double = 180.0 / PI

    /**
     * Altitude of the sun's centre at apparent sunrise/sunset: −0.833°, which
     * folds together atmospheric refraction and the solar semi-diameter.
     */
    const val HORIZON_ALTITUDE: Double = -0.833333333333333

    // ---------------------------------------------------------------------------
    // Degree trigonometry
    // ---------------------------------------------------------------------------

    @JvmStatic
    fun sin(degrees: Double): Double = kotlin.math.sin(degrees * DEG_TO_RAD)

    @JvmStatic
    fun cos(degrees: Double): Double = kotlin.math.cos(degrees * DEG_TO_RAD)

    @JvmStatic
    fun tan(degrees: Double): Double = kotlin.math.tan(degrees * DEG_TO_RAD)

    @JvmStatic
    fun arcsin(value: Double): Double = kotlin.math.asin(value) * RAD_TO_DEG

    /** Inverse cosine in degrees; NaN when [value] is outside `[-1, 1]` (no clamping). */
    @JvmStatic
    fun arccos(value: Double): Double = kotlin.math.acos(value) * RAD_TO_DEG

    @JvmStatic
    fun arctan(value: Double): Double = kotlin.math.atan(value) * RAD_TO_DEG

    @JvmStatic
    fun arctan2(y: Double, x: Double): Double = kotlin.math.atan2(y, x) * RAD_TO_DEG

    // ---------------------------------------------------------------------------
    // Numeric helpers
    // ---------------------------------------------------------------------------

    /**
     * Rounds half **up**, i.e. `floor(x + 0.5)`.
     *
     * Dart's `round()` rounds half *away from zero*, which disagrees on exact
     * negative halves (`-0.5` → `0` here, `-1` there). The published tables
     * assume half-up, so every rounding step goes through this.
     */
    @JvmStatic
    fun javaRound(value: Double): Int = floor(value + 0.5).toInt()

    /** Wraps [value] into `[0, max)`. */
    @JvmStatic
    fun normalizeWithBound(value: Double, max: Double): Double = value - max * floor(value / max)

    /** Wraps an angle into `[0, 360)`. */
    @JvmStatic
    fun unwindAngle(value: Double): Double = normalizeWithBound(value, 360.0)

    /** Maps an angle into `[-180, 180]`. */
    @JvmStatic
    fun closestAngle(angle: Double): Double {
        if (angle >= -180.0 && angle <= 180.0) return angle
        return angle - 360.0 * javaRound(angle / 360.0)
    }

    // ---------------------------------------------------------------------------
    // Julian day
    // ---------------------------------------------------------------------------

    /**
     * Julian Day for a Gregorian calendar date at [hours] UTC.
     *
     * Meeus, *Astronomical Algorithms*, chapter 7. The integer truncations are
     * intentional — the formula is defined in terms of them.
     */
    @JvmStatic
    @JvmOverloads
    fun julianDay(year: Int, month: Int, day: Int, hours: Double = 0.0): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = y / 100
        val b = (2 - a) + (a / 4)

        return truncate((y + 4716) * 365.25) +
            truncate((m + 1) * 30.6001) +
            (day + hours / 24.0) +
            b -
            1524.5
    }

    /** Julian centuries since the J2000.0 epoch. */
    @JvmStatic
    fun julianCentury(julianDay: Double): Double = (julianDay - 2451545.0) / 36525.0

    // ---------------------------------------------------------------------------
    // Solar position (Meeus)
    // ---------------------------------------------------------------------------

    /** Geometric mean longitude of the sun, in degrees. */
    @JvmStatic
    fun meanSolarLongitude(t: Double): Double =
        unwindAngle(280.4664567 + 36000.76983 * t + 0.0003032 * t * t)

    /** Geometric mean longitude of the moon, in degrees. */
    @JvmStatic
    fun meanLunarLongitude(t: Double): Double = unwindAngle(218.3165 + 481267.8813 * t)

    /** Mean anomaly of the sun, in degrees. */
    @JvmStatic
    fun meanSolarAnomaly(t: Double): Double =
        unwindAngle(357.52911 + 35999.05029 * t - 0.0001537 * t * t)

    /** Longitude of the ascending node of the lunar orbit, in degrees. */
    @JvmStatic
    fun ascendingLunarNodeLongitude(t: Double): Double = unwindAngle(
        125.04452 -
            1934.136261 * t +
            0.0020708 * t * t +
            (t * t * t) / 450000.0,
    )

    /** The sun's equation of the centre, in degrees, for mean anomaly [m]. */
    @JvmStatic
    fun solarEquationOfTheCenter(t: Double, m: Double): Double {
        val mRad = m * DEG_TO_RAD
        return (1.914602 - 0.004817 * t - 0.000014 * t * t) * kotlin.math.sin(mRad) +
            (0.019993 - 0.000101 * t) * kotlin.math.sin(2 * mRad) +
            0.000289 * kotlin.math.sin(3 * mRad)
    }

    /** Apparent longitude of the sun (nutation and aberration applied). */
    @JvmStatic
    fun apparentSolarLongitude(t: Double, meanLongitude: Double): Double {
        val longitude = meanLongitude +
            solarEquationOfTheCenter(t, meanSolarAnomaly(t)) -
            0.00569 -
            0.00478 * sin(125.04 - 1934.136 * t)
        return unwindAngle(longitude)
    }

    /** Mean obliquity of the ecliptic, in degrees. */
    @JvmStatic
    fun meanObliquityOfTheEcliptic(t: Double): Double =
        23.439291 -
            0.013004167 * t -
            0.0000001639 * t * t +
            0.0000005036 * t * t * t

    /** Apparent obliquity of the ecliptic, in degrees. */
    @JvmStatic
    fun apparentObliquityOfTheEcliptic(t: Double, meanObliquity: Double): Double =
        meanObliquity + 0.00256 * cos(125.04 - 1934.136 * t)

    /** Mean sidereal time at Greenwich, in degrees. */
    @JvmStatic
    fun meanSiderealTime(t: Double): Double {
        val jd = t * 36525.0 + 2451545.0
        val theta = 280.46061837 +
            360.98564736629 * (jd - 2451545.0) +
            0.000387933 * t * t -
            (t * t * t) / 38710000.0
        return unwindAngle(theta)
    }

    /** Nutation in longitude, in degrees. */
    @JvmStatic
    fun nutationInLongitude(
        t: Double,
        solarLongitude: Double,
        lunarLongitude: Double,
        ascendingNode: Double,
    ): Double {
        return (-17.2 / 3600) * sin(ascendingNode) -
            (1.32 / 3600) * sin(2 * solarLongitude) -
            (0.23 / 3600) * sin(2 * lunarLongitude) +
            (0.21 / 3600) * sin(2 * ascendingNode)
    }

    /** Nutation in obliquity, in degrees. */
    @JvmStatic
    fun nutationInObliquity(
        t: Double,
        solarLongitude: Double,
        lunarLongitude: Double,
        ascendingNode: Double,
    ): Double {
        return (9.2 / 3600) * cos(ascendingNode) +
            (0.57 / 3600) * cos(2 * solarLongitude) +
            (0.10 / 3600) * cos(2 * lunarLongitude) -
            (0.09 / 3600) * cos(2 * ascendingNode)
    }

    /** Altitude of a celestial body above the horizon, in degrees. */
    @JvmStatic
    fun altitudeOfCelestialBody(
        observerLatitude: Double,
        declination: Double,
        localHourAngle: Double,
    ): Double {
        return arcsin(
            sin(observerLatitude) * sin(declination) +
                cos(observerLatitude) * cos(declination) * cos(localHourAngle),
        )
    }

    // ---------------------------------------------------------------------------
    // Transit and hour angles
    // ---------------------------------------------------------------------------

    /** Approximate transit as a fraction of the day. */
    @JvmStatic
    fun approximateTransit(
        longitude: Double,
        siderealTime: Double,
        rightAscension: Double,
    ): Double {
        val lw = longitude * -1
        return normalizeWithBound((rightAscension + lw - siderealTime) / 360, 1.0)
    }

    /** Transit time (fractional hours) corrected by interpolation. */
    @JvmStatic
    fun correctedTransit(
        approximateTransit: Double,
        longitude: Double,
        siderealTime: Double,
        rightAscension: Double,
        previousRightAscension: Double,
        nextRightAscension: Double,
    ): Double {
        val lw = longitude * -1
        val theta = unwindAngle(siderealTime + 360.985647 * approximateTransit)
        val alpha = unwindAngle(
            interpolateAngles(
                rightAscension,
                previousRightAscension,
                nextRightAscension,
                approximateTransit,
            ),
        )
        val h = closestAngle(theta - lw - alpha)
        val deltaM = h / -360
        return (approximateTransit + deltaM) * 24
    }

    /**
     * Time (fractional hours) at which the sun reaches altitude [altitude].
     *
     * Returns `NaN` when the sun never reaches that altitude on the day.
     */
    @JvmStatic
    fun correctedHourAngle(
        approximateTransit: Double,
        altitude: Double,
        latitude: Double,
        longitude: Double,
        afterTransit: Boolean,
        siderealTime: Double,
        rightAscension: Double,
        previousRightAscension: Double,
        nextRightAscension: Double,
        declination: Double,
        previousDeclination: Double,
        nextDeclination: Double,
    ): Double {
        val lw = longitude * -1

        val term = (sin(altitude) - sin(latitude) * sin(declination)) /
            (cos(latitude) * cos(declination))
        // acos() outside [-1, 1] yields NaN, which propagates: the caller treats a
        // NaN result as "the sun never reaches this angle today".
        val h0 = arccos(term) / 360

        val m = if (afterTransit) approximateTransit + h0 else approximateTransit - h0
        val theta = unwindAngle(siderealTime + 360.985647 * m)
        val alpha = unwindAngle(
            interpolateAngles(
                rightAscension,
                previousRightAscension,
                nextRightAscension,
                m,
            ),
        )
        val delta = interpolate(
            declination,
            previousDeclination,
            nextDeclination,
            m,
        )
        val h = (theta - lw) - alpha
        val altitudeOfSun = altitudeOfCelestialBody(latitude, delta, h)
        val deltaM = (altitudeOfSun - altitude) /
            (360 * cos(delta) * cos(latitude) * sin(h))
        return (m + deltaM) * 24
    }

    /** Three-point interpolation of a value. Meeus chapter 3. */
    @JvmStatic
    fun interpolate(
        value: Double,
        previousValue: Double,
        nextValue: Double,
        factor: Double,
    ): Double {
        val a = value - previousValue
        val b = nextValue - value
        val c = b - a
        return value + ((factor / 2) * (a + b + factor * c))
    }

    /** Three-point interpolation of an angle (each difference unwound first). */
    @JvmStatic
    fun interpolateAngles(
        value: Double,
        previousValue: Double,
        nextValue: Double,
        factor: Double,
    ): Double {
        val a = unwindAngle(value - previousValue)
        val b = unwindAngle(nextValue - value)
        val c = b - a
        return value + ((factor / 2) * (a + b + factor * c))
    }
}
