package io.github.bilalelsayed97.islamickit

import io.github.bilalelsayed97.islamickit.application.PrayerTimesService
import io.github.bilalelsayed97.islamickit.application.usecases.QiblaCalculator
import io.github.bilalelsayed97.islamickit.domain.valueobjects.Coordinates
import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Port of `test/qibla_test.dart`. */
class QiblaTest {
    @Test
    fun `Qibla from London matches reference (118_987 degrees)`() {
        val service = PrayerTimesService()
        val qibla = service.qibla(Coordinates(51.5073509, -0.1277583))
        assertTrue(abs(qibla.degrees - 118.98724271029) < 1e-6, "got ${qibla.degrees}")
    }

    @Test
    fun `Qibla is normalized to 0 to 360`() {
        val calculator = QiblaCalculator()
        val q = calculator.direction(Coordinates(-33.8688, 151.2093))
        assertTrue(q.degrees in 0.0..360.0)
    }

    @Test
    fun `toString formats two decimals with a ROOT locale`() {
        val q = QiblaCalculator().direction(Coordinates(51.5073509, -0.1277583))
        assertEquals("QiblaDirection(118.99° from 51.5073509, -0.1277583)", q.toString())
        assertEquals(Coordinates(21.422517, 39.826166), QiblaCalculator.KAABA)
    }
}
