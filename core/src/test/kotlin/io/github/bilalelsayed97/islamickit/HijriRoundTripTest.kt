package io.github.bilalelsayed97.islamickit

import io.github.bilalelsayed97.islamickit.domain.enums.CalendarMethod
import io.github.bilalelsayed97.islamickit.infrastructure.calendar.HijriConverterFactory
import io.github.bilalelsayed97.islamickit.infrastructure.calendar.TableHijriConverter
import io.github.bilalelsayed97.islamickit.infrastructure.calendar.data.HijriSightings
import io.github.bilalelsayed97.islamickit.time.CivilDate
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * The two directions of a converter must be inverses: a Hijri calendar screen
 * places a month with `toGregorian` and labels its days with `fromGregorian`,
 * so any disagreement shows up as a month that starts on "day 2".
 */
class HijriRoundTripTest {
    private val factory = HijriConverterFactory()

    private fun days(from: CivilDate, to: CivilDate): Sequence<CivilDate> =
        generateSequence(from) { it.plusDays(1) }.takeWhile { it <= to }

    private fun assertRoundTrips(method: CalendarMethod, dates: Sequence<CivilDate>) {
        val converter = factory.create(method)
        for (date in dates) {
            val hijri = converter.fromGregorian(date)
            assertEquals(
                date,
                converter.toGregorian(hijri.year, hijri.month, hijri.day),
                "${method.code}: $date is ${hijri.formatted}, which must convert back to it",
            )
        }
    }

    @Test
    fun `Umm al-Qura round-trips every day of its table`() {
        val table = TableHijriConverter.ummAlQura()
        assertRoundTrips(CalendarMethod.UAQ, days(table.gregorianFrom, table.gregorianTo))
    }

    @Test
    fun `Diyanet round-trips every day of its table`() {
        val table = TableHijriConverter.diyanet()
        assertRoundTrips(CalendarMethod.DIYANET, days(table.gregorianFrom, table.gregorianTo))
    }

    @Test
    fun `Mathematical round-trips`() {
        assertRoundTrips(CalendarMethod.MATHEMATICAL, days(CivilDate(1990, 1, 1), CivilDate(2040, 12, 31)))
    }

    @Test
    fun `HJCoSA round-trips every day after its last announcement`() {
        // Before it, an announcement moves single days of a month, which the
        // surrounding days (still read off the table) cannot mirror.
        val table = TableHijriConverter.ummAlQura()
        assertRoundTrips(CalendarMethod.HJCOSA, days(CivilDate(2021, 9, 9), table.gregorianTo))
    }

    @Test
    fun `HJCoSA round-trips every announced date`() {
        val converter = factory.create(CalendarMethod.HJCOSA)
        for (gregorian in HijriSightings.gregorianToHijri.keys) {
            val (d, m, y) = gregorian.split('-').map(String::toInt)
            val date = CivilDate(y, m, d)
            val hijri = converter.fromGregorian(date)
            assertEquals(date, converter.toGregorian(hijri.year, hijri.month, hijri.day), gregorian)
        }
    }

    @Test
    fun `the months that used to start a day off`() {
        val hjcosa = factory.create(CalendarMethod.HJCOSA)
        // The arithmetic calendar put these on the 24th, the 17th and the 17th.
        assertEquals(CivilDate(2025, 9, 23), hjcosa.toGregorian(1447, 4, 1))
        assertEquals(CivilDate(2026, 6, 16), hjcosa.toGregorian(1448, 1, 1))
        assertEquals(CivilDate(2026, 7, 15), hjcosa.toGregorian(1448, 2, 1))
    }

    @Test
    fun `a day past the month's end runs into the next month`() {
        val uaq = factory.create(CalendarMethod.UAQ)
        // Sha'ban 1446 has 29 days.
        assertEquals(uaq.toGregorian(1446, 9, 1), uaq.toGregorian(1446, 8, 30))
    }

    @Test
    fun `an adjustment shifts the Gregorian result by whole days`() {
        val uaq = factory.create(CalendarMethod.UAQ)
        assertEquals(CivilDate(2025, 2, 15), uaq.toGregorian(1446, 8, 15, adjustment = 1))
        assertEquals(CivilDate(2025, 2, 13), uaq.toGregorian(1446, 8, 15, adjustment = -1))
    }
}
