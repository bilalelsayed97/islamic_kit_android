package io.github.bilalelsayed97.islamickit.conformance

import io.github.bilalelsayed97.islamickit.application.usecases.QiblaCalculator
import io.github.bilalelsayed97.islamickit.domain.valueobjects.Coordinates
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import kotlin.math.abs
import kotlin.test.assertTrue

/** `qibla.json`: bearings for 45 coordinates, within 1e-9 degrees. */
class ConformanceQiblaTest {
    private val qibla = QiblaCalculator()

    @TestFactory
    fun bearings(): List<DynamicNode> = Fixtures.cases("qibla.json").map { c ->
        val lat = c.double("lat")
        val lng = c.double("lng")
        dynamicTest("($lat, $lng)") {
            val expected = c.double("degrees")
            val actual = qibla.direction(Coordinates(lat, lng)).degrees
            assertTrue(abs(actual - expected) < 1e-9, "expected $expected but was $actual")
        }
    }
}
