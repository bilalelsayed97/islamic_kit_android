package io.github.bilalelsayed97.islamickit

import io.github.bilalelsayed97.islamickit.application.PrayerTimesService
import io.github.bilalelsayed97.islamickit.application.serialization.toAladhanJson
import io.github.bilalelsayed97.islamickit.domain.enums.CalculationMethod
import io.github.bilalelsayed97.islamickit.domain.enums.Prayer
import io.github.bilalelsayed97.islamickit.domain.valueobjects.CalculationParameters
import io.github.bilalelsayed97.islamickit.domain.valueobjects.Coordinates
import io.github.bilalelsayed97.islamickit.domain.valueobjects.Tune
import io.github.bilalelsayed97.islamickit.time.CivilDate
import io.github.bilalelsayed97.islamickit.time.UtcOffset
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/** Port of `test/tune_test.dart`. */
class TuneTest {
    private val london = Coordinates(51.508515, -0.1254872)
    private val service = PrayerTimesService()

    @Test
    fun `per-prayer tune shifts the computed time by the given minutes`() {
        val base = CalculationParameters(method = CalculationMethod.ISNA, utcOffset = UtcOffset.ofHours(1))
        val untuned = service.timings(CivilDate(2014, 4, 24), london, base)
        assertEquals("03:57", untuned.formatted(Prayer.FAJR))

        val tuned = service.timings(CivilDate(2014, 4, 24), london, base.copy(tune = Tune(fajr = 5))) // +5 minutes
        assertEquals("04:02", tuned.formatted(Prayer.FAJR))
    }

    @Test
    fun `Tune round-trips the aladhan CSV order`() {
        val tune = Tune.fromCsv("5,3,5,7,9,-1,0,8,-6")
        assertEquals(5, tune.imsak)
        assertEquals(3, tune.fajr)
        assertEquals(-1, tune.maghrib) // Maghrib precedes Sunset in the aladhan order
        assertEquals(0, tune.sunset)
        assertEquals(-6, tune.midnight)
        assertEquals("5,3,5,7,9,-1,0,8,-6", tune.toCsv())
    }

    @Test
    fun `fromCsv tolerates short, padded and garbage input`() {
        assertEquals(Tune(imsak = 5, fajr = 3), Tune.fromCsv("5, 3"))
        assertEquals(Tune(fajr = 2), Tune.fromCsv("x,2,,1.5"))
        assertEquals(Tune.NONE, Tune.fromCsv(""))
        assertTrue(Tune.fromCsv("").isEmpty)
    }

    @Test
    fun `equality is by CSV`() {
        assertEquals(Tune(fajr = 3), Tune.fromCsv("0,3"))
        assertEquals(Tune(fajr = 3).hashCode(), Tune.fromCsv("0,3").hashCode())
        assertNotEquals(Tune(fajr = 3), Tune(isha = 3))
    }

    @Test
    fun `toMap is in aladhan offset order (Sunset before Maghrib)`() {
        val map = Tune(maghrib = -1, sunset = 2).toMap()
        assertEquals(
            listOf(
                Prayer.IMSAK, Prayer.FAJR, Prayer.SUNRISE, Prayer.DHUHR, Prayer.ASR,
                Prayer.SUNSET, Prayer.MAGHRIB, Prayer.ISHA, Prayer.MIDNIGHT,
            ),
            map.keys.toList(),
        )
        assertEquals(2, map[Prayer.SUNSET])
        assertEquals(-1, map[Prayer.MAGHRIB])
    }

    @Test
    fun `offsets appear in the aladhan meta block`() {
        val json = service.timings(
            CivilDate(2014, 4, 24),
            london,
            CalculationParameters(
                method = CalculationMethod.ISNA,
                utcOffset = UtcOffset.ofHours(1),
                tune = Tune(fajr = 3, isha = 8),
            ),
        ).toAladhanJson()
        @Suppress("UNCHECKED_CAST")
        val offset = ((json["data"] as Map<String, Any?>)["meta"] as Map<String, Any?>)["offset"] as Map<String, Any?>
        assertEquals(3, offset["Fajr"])
        assertEquals(8, offset["Isha"])
        assertEquals(0, offset["Dhuhr"])
    }

    @Test
    fun `negative tune that crosses a minute boundary rounds with floor semantics`() {
        // Every tuned value is a whole number of seconds; a negative tune that
        // lands on an exact minute must not drift (floorMod, not `%`).
        val base = CalculationParameters(method = CalculationMethod.ISNA, utcOffset = UtcOffset.ofHours(1))
        val tuned = service.timings(CivilDate(2014, 4, 24), london, base.copy(tune = Tune(fajr = -240)))
        assertEquals("23:57", tuned.formatted(Prayer.FAJR))
        assertEquals("2014-04-23T23:57:00+01:00", tuned.formatted(Prayer.FAJR, io.github.bilalelsayed97.islamickit.domain.enums.TimeFormat.ISO8601))
    }
}
