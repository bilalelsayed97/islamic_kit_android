package io.github.bilalelsayed97.islamickit

import io.github.bilalelsayed97.islamickit.domain.enums.Prayer
import io.github.bilalelsayed97.islamickit.domain.enums.TimeFormat
import io.github.bilalelsayed97.islamickit.domain.models.PrayerTime
import io.github.bilalelsayed97.islamickit.domain.services.TimeFormatting
import io.github.bilalelsayed97.islamickit.domain.services.TimeFormatting.INVALID_TIME
import io.github.bilalelsayed97.islamickit.time.CivilDate
import io.github.bilalelsayed97.islamickit.time.UtcOffset
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TimeFormattingTest {
    private val date = CivilDate(2014, 4, 24)
    private val bst = UtcOffset.ofHours(1)

    private fun f(hours: Double?, format: TimeFormat, offset: UtcOffset = bst) =
        TimeFormatting.formatHours(hours, format, date, offset)

    @Test
    fun `invalid values`() {
        assertEquals("-----", INVALID_TIME)
        for (format in TimeFormat.entries) {
            assertEquals(INVALID_TIME, f(null, format))
            assertEquals(INVALID_TIME, f(Double.NaN, format))
        }
    }

    @Test
    fun `fixHour wraps into 0 to 24`() {
        assertEquals(1.0, TimeFormatting.fixHour(25.0))
        assertEquals(23.0, TimeFormatting.fixHour(-1.0))
        assertEquals(0.0, TimeFormatting.fixHour(24.0))
        assertEquals(0.0, TimeFormatting.fixHour(0.0))
    }

    @TestFactory
    fun `clock formats`(): List<DynamicTest> {
        // hours, 24h, 12h, 12hNS
        val cases = listOf(
            listOf(3.95, "03:57", "3:57 am", "3:57"),
            listOf(13.0, "13:00", "1:00 pm", "1:00"),
            listOf(0.0, "00:00", "12:00 am", "12:00"),
            listOf(12.0, "12:00", "12:00 pm", "12:00"),
            listOf(23.999, "00:00", "12:00 am", "12:00"), // +0.5 minute rounding wraps
            listOf(24.0833333, "00:05", "12:05 am", "12:05"), // rolled past midnight
            listOf(-0.5, "23:30", "11:30 pm", "11:30"), // previous day
            listOf(11.9917, "12:00", "12:00 pm", "12:00"), // 11:59:30 rounds up into pm
            listOf(16.9333, "16:56", "4:56 pm", "4:56"),
        )
        return cases.map { (h, h24, h12, ns) ->
            DynamicTest.dynamicTest("$h -> $h24 / $h12 / $ns") {
                assertEquals(h24, f(h as Double, TimeFormat.H24))
                assertEquals(h12, f(h, TimeFormat.H12))
                assertEquals(ns, f(h, TimeFormat.H12_NO_SUFFIX))
            }
        }
    }

    @TestFactory
    fun `iso8601 uses civil arithmetic and the offset suffix`(): List<DynamicTest> {
        val cases = listOf(
            Triple(3.95, bst, "2014-04-24T03:57:00+01:00"),
            Triple(25.6667, bst, "2014-04-25T01:40:00+01:00"),
            Triple(-1.6667, bst, "2014-04-23T22:20:00+01:00"),
            Triple(0.0, UtcOffset.ZERO, "2014-04-24T00:00:00+00:00"),
            Triple(-0.0001, UtcOffset.ofHours(-3, -30), "2014-04-24T00:00:00-03:30"), // -ceil path
            Triple(-0.02, UtcOffset.ofHours(5, 30), "2014-04-23T23:59:00+05:30"),
            Triple(23.9999, bst, "2014-04-25T00:00:00+01:00"),
            Triple(48.0, bst, "2014-04-26T00:00:00+01:00"),
        )
        return cases.map { (h, offset, expected) ->
            DynamicTest.dynamicTest("$h @ ${offset.toIsoString()} -> $expected") {
                assertEquals(expected, f(h, TimeFormat.ISO8601, offset))
            }
        }
    }

    @Test
    fun `float renders Dart double toString`() {
        assertEquals("3.95", f(3.95, TimeFormat.FLOAT))
        assertEquals("13.0", f(13.0, TimeFormat.FLOAT))
        assertEquals("-0.5", f(-0.5, TimeFormat.FLOAT))
        assertEquals("0.0001", f(0.0001, TimeFormat.FLOAT))
    }

    @Test
    fun `PrayerTime wraps formatting and validity`() {
        val fajr = PrayerTime(Prayer.FAJR, 3.95, date, bst)
        assertTrue(fajr.isValid)
        assertEquals("03:57", fajr.format())
        assertEquals("Fajr: 03:57", fajr.toString())
        assertEquals("الفجر", fajr.name(io.github.bilalelsayed97.islamickit.domain.enums.Language.AR))
        val invalid = PrayerTime(Prayer.FAJR, null, date, bst)
        assertFalse(invalid.isValid)
        assertNull(invalid.toEpochMillis())
        assertEquals(INVALID_TIME, invalid.format(TimeFormat.ISO8601))
        assertFalse(PrayerTime(Prayer.FAJR, Double.NaN, date, bst).isValid)
    }

    @Test
    fun `toEpochMillis rounds half away from zero on the millisecond`() {
        val midnightUtc = date.utcMidnightEpochMillis()
        assertEquals(midnightUtc - 3_600_000L, PrayerTime(Prayer.FAJR, 0.0, date, bst).toEpochMillis())
        // 0.5 h = 1_800_000 ms exactly; -0.5 h before midnight local.
        assertEquals(midnightUtc - 1_800_000L, PrayerTime(Prayer.FAJR, -0.5, date, UtcOffset.ZERO).toEpochMillis())
    }
}
