package io.github.bilalelsayed97.islamickit.infrastructure.astronomy

/**
 * The sun's apparent equatorial coordinates for one Julian Day.
 *
 * All three values are needed to interpolate the sun's position across a day,
 * which is what gives the Meeus algorithm its accuracy over the simpler
 * single-sample formulas.
 */
class SolarCoordinates(julianDay: Double) {
    /** Apparent declination of the sun, in degrees. */
    val declination: Double

    /** Apparent right ascension of the sun, in degrees. */
    val rightAscension: Double

    /** Apparent sidereal time at Greenwich, in degrees. */
    val apparentSiderealTime: Double

    init {
        val t = Astronomical.julianCentury(julianDay)
        val meanSolarLongitude = Astronomical.meanSolarLongitude(t)
        val meanLunarLongitude = Astronomical.meanLunarLongitude(t)
        val ascendingNode = Astronomical.ascendingLunarNodeLongitude(t)
        val apparentLongitude = Astronomical.apparentSolarLongitude(t, meanSolarLongitude)

        val meanSiderealTime = Astronomical.meanSiderealTime(t)
        val nutationLongitude = Astronomical.nutationInLongitude(
            t,
            meanSolarLongitude,
            meanLunarLongitude,
            ascendingNode,
        )
        val nutationObliquity = Astronomical.nutationInObliquity(
            t,
            meanSolarLongitude,
            meanLunarLongitude,
            ascendingNode,
        )

        val meanObliquity = Astronomical.meanObliquityOfTheEcliptic(t)
        val apparentObliquity = Astronomical.apparentObliquityOfTheEcliptic(t, meanObliquity)

        // Meeus equations 25.6 / 25.7 — apparent declination and right ascension.
        declination = Astronomical.arcsin(
            Astronomical.sin(apparentObliquity) * Astronomical.sin(apparentLongitude),
        )
        rightAscension = Astronomical.unwindAngle(
            Astronomical.arctan2(
                Astronomical.cos(apparentObliquity) * Astronomical.sin(apparentLongitude),
                Astronomical.cos(apparentLongitude),
            ),
        )
        apparentSiderealTime = meanSiderealTime +
            (nutationLongitude * 3600 * Astronomical.cos(meanObliquity + nutationObliquity)) / 3600
    }
}
