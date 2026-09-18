package io.github.bilalelsayed97.islamickit

import io.github.bilalelsayed97.islamickit.domain.valueobjects.Coordinates
import io.github.bilalelsayed97.islamickit.infrastructure.astronomy.Astronomical
import io.github.bilalelsayed97.islamickit.infrastructure.astronomy.SolarCoordinates
import io.github.bilalelsayed97.islamickit.infrastructure.astronomy.SolarTime
import io.github.bilalelsayed97.islamickit.internal.DartMath
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Port of `test/astronomy_test.dart`. */
class AstronomyTest {
    private fun assertCloseTo(expected: Double, actual: Double, delta: Double, message: String? = null) {
        assertTrue(abs(expected - actual) <= delta, "${message ?: ""} expected $expected ± $delta, got $actual")
    }

    @Nested
    inner class JulianDay {
        @Test
        fun `Meeus example 7a - 1957 October 4_81`() {
            assertCloseTo(2436116.31, Astronomical.julianDay(1957, 10, 4, 0.81 * 24), 0.00001)
        }

        @Test
        fun `the J2000 epoch`() {
            assertEquals(2451545.0, Astronomical.julianDay(2000, 1, 1, 12.0))
            assertEquals(0.0, Astronomical.julianCentury(2451545.0))
        }

        @Test
        fun `a Julian century is 36525 days`() {
            assertCloseTo(1.0, Astronomical.julianCentury(2451545.0 + 36525), 1e-12)
        }
    }

    // Meeus, Astronomical Algorithms, example 25.a: 1992 October 13 at 0h TD.
    @Nested
    inner class MeeusExample25a {
        private val jd = Astronomical.julianDay(1992, 10, 13)
        private val t = Astronomical.julianCentury(jd)

        @Test
        fun `Julian day and century`() {
            assertEquals(2448908.5, jd)
            assertCloseTo(-0.072183436, t, 1e-9)
        }

        @Test
        fun `geometric mean longitude`() {
            assertCloseTo(201.80720, Astronomical.meanSolarLongitude(t), 0.00001)
        }

        @Test
        fun `mean anomaly`() {
            assertCloseTo(278.99397, Astronomical.meanSolarAnomaly(t), 0.00001)
        }

        @Test
        fun `equation of the centre`() {
            val m = Astronomical.meanSolarAnomaly(t)
            assertCloseTo(-1.89732, Astronomical.solarEquationOfTheCenter(t, m), 0.00001)
        }

        @Test
        fun `apparent longitude`() {
            val l0 = Astronomical.meanSolarLongitude(t)
            assertCloseTo(199.90895, Astronomical.apparentSolarLongitude(t, l0), 0.00002)
        }

        @Test
        fun `mean obliquity of the ecliptic`() {
            assertCloseTo(23.44023, Astronomical.meanObliquityOfTheEcliptic(t), 0.00001)
        }

        @Test
        fun `apparent right ascension and declination`() {
            val solar = SolarCoordinates(jd)
            assertCloseTo(198.38083, solar.rightAscension, 0.00001)
            assertCloseTo(-7.78507, solar.declination, 0.00001)
        }
    }

    @Nested
    inner class AngleHelpers {
        @Test
        fun `unwindAngle wraps into 0 to 360`() {
            assertEquals(315.0, Astronomical.unwindAngle(-45.0))
            assertCloseTo(1.0, Astronomical.unwindAngle(361.0), 1e-12)
            assertEquals(0.0, Astronomical.unwindAngle(360.0))
        }

        @Test
        fun `closestAngle maps into -180 to 180`() {
            assertEquals(0.0, Astronomical.closestAngle(360.0))
            assertCloseTo(1.0, Astronomical.closestAngle(361.0), 1e-12)
            assertCloseTo(-10.0, Astronomical.closestAngle(-370.0), 1e-12)
            assertEquals(180.0, Astronomical.closestAngle(180.0))
        }

        @Test
        fun `javaRound breaks ties upward, unlike Dart round()`() {
            assertEquals(1, Astronomical.javaRound(0.5))
            assertEquals(0, Astronomical.javaRound(-0.5))
            assertEquals(-1L, DartMath.round(-0.5)) // the behaviour we must not use
            assertEquals(2, Astronomical.javaRound(2.4))
        }
    }

    @Nested
    inner class SolarTimeTests {
        @Test
        fun `sunrise and sunset bracket the transit`() {
            val solar = SolarTime(2015, 7, 12, Coordinates(35.7750, -78.6336))
            assertTrue(solar.sunrise < solar.transit)
            assertTrue(solar.transit < solar.sunset)
        }

        @Test
        fun `the Hanafi Asr falls later than the Shafi Asr`() {
            val solar = SolarTime(2015, 7, 12, Coordinates(35.7750, -78.6336))
            assertTrue(solar.afternoon(2.0) > solar.afternoon(1.0))
        }

        @Test
        fun `an unreachable angle yields NaN`() {
            // Stockholm at the solstice never reaches 18° below the horizon.
            val solar = SolarTime(2024, 6, 21, Coordinates(59.3293, 18.0686))
            assertFalse(solar.sunrise.isNaN())
            assertTrue(solar.hourAngle(-18.0, afterTransit = false).isNaN())
        }

        @Test
        fun `elevation brings sunrise earlier and sunset later`() {
            val coordinates = Coordinates(21.4225, 39.8262)
            val sea = SolarTime(2024, 6, 21, coordinates)
            val high = SolarTime(2024, 6, 21, coordinates, elevation = 1000.0)
            assertTrue(high.sunrise < sea.sunrise)
            assertTrue(high.sunset > sea.sunset)
            assertEquals(sea.transit, high.transit)
        }
    }
}
