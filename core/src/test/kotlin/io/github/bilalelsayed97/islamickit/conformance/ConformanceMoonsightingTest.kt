package io.github.bilalelsayed97.islamickit.conformance

import io.github.bilalelsayed97.islamickit.domain.enums.Shafaq
import io.github.bilalelsayed97.islamickit.infrastructure.twilight.MoonsightingTwilight
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import kotlin.test.assertEquals

/** `moonsighting.json`: Moonsighting Committee twilight seconds for a latitude × day-of-year grid. */
class ConformanceMoonsightingTest {
    private val twilight = MoonsightingTwilight()

    @TestFactory
    fun twilightSeconds(): List<DynamicNode> = Fixtures.cases("moonsighting.json").map { c ->
        val date = Fixtures.date(c.str("date"))
        val lat = c.double("lat")
        dynamicTest("$date lat $lat") {
            assertEquals(c.int("fajrSeconds"), twilight.fajrSecondsBeforeSunrise(date, lat), "fajr")
            val isha = c.obj("ishaSeconds")
            for (shafaq in Shafaq.entries) {
                assertEquals(isha.int(shafaq.code), twilight.ishaSecondsAfterSunset(date, lat, shafaq), "isha ${shafaq.code}")
            }
        }
    }
}
