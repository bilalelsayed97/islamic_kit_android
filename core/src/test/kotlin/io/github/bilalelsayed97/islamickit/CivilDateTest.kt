package io.github.bilalelsayed97.islamickit

import io.github.bilalelsayed97.islamickit.time.CivilDate
import io.github.bilalelsayed97.islamickit.time.CivilDateTime
import io.github.bilalelsayed97.islamickit.time.UtcOffset
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertThrows
import java.util.TimeZone
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CivilDateTest {
    @Test
    fun `strict constructor rejects invalid dates`() {
        assertThrows<IllegalArgumentException> { CivilDate(2014, 13, 1) }
        assertThrows<IllegalArgumentException> { CivilDate(2014, 2, 29) }
        assertThrows<IllegalArgumentException> { CivilDate(2014, 4, 0) }
        CivilDate(2016, 2, 29)
    }

    @TestFactory
    fun `normalized reproduces Dart DateTime overflow`(): List<DynamicTest> {
        val cases = listOf(
            Triple(Triple(2014, 2, 30), CivilDate(2014, 3, 2), "day overflow"),
            Triple(Triple(2014, 13, 1), CivilDate(2015, 1, 1), "month overflow"),
            Triple(Triple(2014, 5, 0), CivilDate(2014, 4, 30), "day zero"),
            Triple(Triple(2014, 1, 0), CivilDate(2013, 12, 31), "day zero across a year"),
            Triple(Triple(2014, 3, -1), CivilDate(2014, 2, 27), "negative day"),
            Triple(Triple(2014, 0, 1), CivilDate(2013, 12, 1), "month zero"),
            Triple(Triple(2014, 4, 25), CivilDate(2014, 4, 25), "in range"),
            Triple(Triple(2014, 12, 32), CivilDate(2015, 1, 1), "year rollover"),
            Triple(Triple(2014, 1 + 11, 31), CivilDate(2014, 12, 31), "start + 11 months"),
            Triple(Triple(2014, 3 + 11, 31), CivilDate(2015, 3, 3), "start + 11 months overflowing February"),
        )
        return cases.map { (input, expected, label) ->
            DynamicTest.dynamicTest("$label: $input -> $expected") {
                assertEquals(expected, CivilDate.normalized(input.first, input.second, input.third))
            }
        }
    }

    @Test
    fun `julian day number and weekday`() {
        assertEquals(2451545, CivilDate(2000, 1, 1).julianDayNumber)
        assertEquals(6, CivilDate(2000, 1, 1).isoDayOfWeek) // Saturday
        assertEquals(4, CivilDate(2014, 4, 24).isoDayOfWeek) // Thursday
        assertEquals(1, CivilDate(2024, 12, 16).isoDayOfWeek) // Monday
        assertEquals(7, CivilDate(2024, 12, 15).isoDayOfWeek) // Sunday
        assertEquals(2440588, CivilDate(1970, 1, 1).julianDayNumber)
        assertEquals(0, CivilDate(1970, 1, 1).epochDay)
        assertEquals(0L, CivilDate(1970, 1, 1).unixMidnightSeconds())
        assertEquals(1398297600L, CivilDate(2014, 4, 24).unixMidnightSeconds())
        assertEquals(1398297600000L, CivilDate(2014, 4, 24).utcMidnightEpochMillis())
    }

    @Test
    fun `round trips through JDN for every day of several centuries`() {
        var d = CivilDate(1800, 1, 1)
        val end = CivilDate(2200, 12, 31)
        var jdn = d.julianDayNumber
        while (d <= end) {
            assertEquals(jdn, d.julianDayNumber, "$d")
            assertEquals(d, CivilDate.fromJulianDayNumber(jdn))
            d = d.plusDays(1)
            jdn++
        }
    }

    @Test
    fun `day arithmetic`() {
        assertEquals(CivilDate(2014, 4, 25), CivilDate(2014, 4, 24).plusDays(1))
        assertEquals(CivilDate(2014, 4, 23), CivilDate(2014, 4, 24).plusDays(-1))
        assertEquals(CivilDate(2015, 1, 1), CivilDate(2014, 12, 31).plusDays(1))
        assertEquals(CivilDate(2014, 3, 3), CivilDate(2014, 1, 31).plusMonths(1))
        assertEquals(CivilDate(2014, 12, 1), CivilDate(2014, 1, 1).plusMonths(11))
        assertEquals(115, CivilDate(2014, 4, 25).dayOfYear)
        assertEquals(366, CivilDate(2024, 12, 31).dayOfYear)
        assertEquals(29, CivilDate.daysInMonth(2024, 2))
        assertEquals(28, CivilDate.daysInMonth(1900, 2))
        assertEquals(29, CivilDate.daysInMonth(2000, 2))
        assertTrue(CivilDate(2014, 1, 1) < CivilDate(2014, 1, 2))
        assertEquals("2014-04-24", CivilDate(2014, 4, 24).toString())
        assertEquals("0099-01-05", CivilDate(99, 1, 5).toString())
    }

    @Test
    fun `epoch conversions honour the offset`() {
        val ms = 1398297600000L // 2014-04-24T00:00:00Z
        assertEquals(CivilDate(2014, 4, 24), CivilDate.fromEpochMillis(ms, UtcOffset.ZERO))
        assertEquals(CivilDate(2014, 4, 23), CivilDate.fromEpochMillis(ms, UtcOffset.ofHours(-1)))
        assertEquals(CivilDate(2014, 4, 24), CivilDate.fromEpochMillis(ms, UtcOffset.ofHours(1)))
        assertEquals(CivilDateTime(2014, 4, 23, 23, 59, 59), CivilDateTime.fromEpochMillis(ms - 1, UtcOffset.ZERO))
        assertEquals(CivilDateTime(2014, 4, 24, 5, 30, 0), CivilDateTime.fromEpochMillis(ms, UtcOffset.ofHours(5, 30)))
        assertEquals(13.5, CivilDateTime(2014, 4, 24, 13, 30).fractionalHours)
        assertEquals("2014-04-24T13:30:00", CivilDateTime(2014, 4, 24, 13, 30).toString())
    }

    @Test
    fun `today and now use the given zone`() {
        val utc = TimeZone.getTimeZone("UTC")
        val today = CivilDate.today(utc)
        val now = CivilDateTime.now(utc)
        assertTrue(now.date == today || now.date == today.plusDays(1))
    }

    @Test
    fun `UtcOffset arithmetic and formatting`() {
        assertEquals("+05:30", UtcOffset.ofHours(5, 30).toIsoString())
        assertEquals("-03:30", UtcOffset.ofHours(-3, -30).toIsoString())
        assertEquals("+00:00", UtcOffset.ZERO.toIsoString())
        assertEquals("-01:00", UtcOffset.ofMinutes(-60).toIsoString())
        assertEquals(-3, UtcOffset.ofHours(-3, -30).hours)
        assertEquals(30, UtcOffset.ofHours(-3, -30).minutesPart)
        assertEquals(-210, UtcOffset.ofHours(-3, -30).totalMinutes)
        assertTrue(UtcOffset.ofHours(-3, -30).isNegative)
        assertEquals(UtcOffset.ofHours(3, 30), UtcOffset.ofHours(-3, -30).abs())
        assertEquals(UtcOffset.ofHours(5, 30), UtcOffset.parse("+05:30"))
        assertEquals(UtcOffset.ofHours(-3, -30), UtcOffset.parse("-0330"))
        assertEquals(UtcOffset.ofHours(2), UtcOffset.parse("+02"))
        assertEquals(UtcOffset.ZERO, UtcOffset.parse("Z"))
        assertThrows<IllegalArgumentException> { UtcOffset.parse("05:30") }
        // Seconds-level offsets truncate the way Dart's Duration getters do.
        assertEquals("+01:00", UtcOffset(3630).toIsoString())
    }

    @Test
    fun `UtcOffset of TimeZone at a date picks up daylight saving`() {
        val london = TimeZone.getTimeZone("Europe/London")
        assertEquals(UtcOffset.ofHours(1), UtcOffset.of(london, CivilDate(2014, 4, 24)))
        assertEquals(UtcOffset.ZERO, UtcOffset.of(london, CivilDate(2014, 1, 24)))
        val tehran = TimeZone.getTimeZone("Asia/Tehran")
        assertEquals(UtcOffset.ofHours(3, 30), UtcOffset.of(tehran, CivilDate(2024, 1, 1)))
        val stJohns = TimeZone.getTimeZone("America/St_Johns")
        assertEquals(UtcOffset.ofHours(-3, -30), UtcOffset.of(stJohns, CivilDate(2024, 1, 1)))
    }
}
