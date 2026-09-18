package io.github.bilalelsayed97.islamickit.conformance

import io.github.bilalelsayed97.islamickit.application.PrayerTimesService
import io.github.bilalelsayed97.islamickit.domain.valueobjects.Coordinates
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import kotlin.test.assertEquals

/** `next_prayer.json`: 90 `(from, coordinates, params)` cases including the after-Isha rollover. */
class ConformanceNextPrayerTest {
    private val service = PrayerTimesService()

    @TestFactory
    fun nextPrayer(): List<DynamicNode> = Fixtures.cases("next_prayer.json").map { c ->
        dynamicTest(c.str("id")) {
            val next = service.nextPrayer(
                Fixtures.dateTime(c.str("from")),
                Coordinates(c.double("lat"), c.double("lng")),
                Fixtures.params(c.obj("params")),
            )
            assertEquals(c.str("prayer"), next.prayer.key, "prayer")
            assertEquals(c.doubleOrNull("hours"), next.time.hours, "hours")
            assertEquals(c.str("formatted"), next.time.format(), "formatted")
            assertEquals(c.str("onDate"), next.onDate.toString(), "onDate")
        }
    }
}
