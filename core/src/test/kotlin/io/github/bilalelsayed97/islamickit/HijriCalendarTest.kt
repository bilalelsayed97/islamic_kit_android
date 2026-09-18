package io.github.bilalelsayed97.islamickit

import io.github.bilalelsayed97.islamickit.domain.enums.CalendarMethod
import io.github.bilalelsayed97.islamickit.infrastructure.calendar.HijriConverterFactory
import io.github.bilalelsayed97.islamickit.time.CivilDate
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

/** Port of `test/hijri_calendar_test.dart`. */
class HijriCalendarTest {
    private val factory = HijriConverterFactory()

    @Nested
    inner class GregorianToHijri20250214 {
        private val date = CivilDate(2025, 2, 14)

        @Test
        fun `Umm al-Qura - 15 Shaban 1446 (29-day month)`() {
            val h = factory.create(CalendarMethod.UAQ).fromGregorian(date)
            assertEquals(1446, h.year)
            assertEquals(8, h.month)
            assertEquals(15, h.day)
            assertEquals(29, h.monthLength)
        }

        @Test
        fun `HJCoSA matches Umm al-Qura when unadjusted`() {
            val h = factory.create(CalendarMethod.HJCOSA).fromGregorian(date)
            assertEquals(Triple(1446, 8, 15), Triple(h.year, h.month, h.day))
        }

        @Test
        fun `Diyanet - 16 Shaban 1446 (30-day month)`() {
            val h = factory.create(CalendarMethod.DIYANET).fromGregorian(date)
            assertEquals(Triple(1446, 8, 16), Triple(h.year, h.month, h.day))
            assertEquals(30, h.monthLength)
        }

        @Test
        fun `Mathematical - 15 Shaban 1446 (month length hardcoded 30)`() {
            val h = factory.create(CalendarMethod.MATHEMATICAL).fromGregorian(date)
            assertEquals(Triple(1446, 8, 15), Triple(h.year, h.month, h.day))
            assertEquals(30, h.monthLength)
        }

        @Test
        fun `Mathematical honours +1 adjustment`() {
            val h = factory.create(CalendarMethod.MATHEMATICAL).fromGregorian(date, adjustment = 1)
            assertEquals(16, h.day)
        }

        @Test
        fun `localized names, weekday and formatted date are populated`() {
            val h = factory.create(CalendarMethod.UAQ).fromGregorian(date)
            assertEquals("Sha'ban", h.monthEn)
            assertEquals("شَعْبان", h.monthAr)
            assertEquals("Friday", h.weekdayEn)
            assertEquals("الجمعة", h.weekdayAr)
            assertEquals("15-08-1446", h.formatted)
            assertEquals(CalendarMethod.UAQ, h.method)
        }

        @Test
        fun `holidays come from the Hijri holiday table`() {
            // 2025-03-30 is 1 Shawwal 1446 in the Umm al-Qura table.
            val eid = factory.create(CalendarMethod.UAQ).fromGregorian(CivilDate(2025, 3, 30))
            assertEquals(Triple(1446, 10, 1), Triple(eid.year, eid.month, eid.day))
            assertEquals(listOf("Eid-ul-Fitr"), eid.holidays)
        }
    }

    @Nested
    inner class HijriToGregorianRoundTrips {
        @Test
        fun `Umm al-Qura 15-08-1446 - 14-02-2025`() {
            val g = factory.create(CalendarMethod.UAQ).toGregorian(1446, 8, 15)
            assertEquals(CivilDate(2025, 2, 14), g)
        }

        @Test
        fun `Mathematical 15-08-1446 with -1 adjustment - 13-02-2025`() {
            val g = factory.create(CalendarMethod.MATHEMATICAL).toGregorian(1446, 8, 15, adjustment = -1)
            assertEquals(CivilDate(2025, 2, 13), g)
        }
    }

    @Nested
    inner class HjcosaLunarSightingOverrides {
        @Test
        fun `17-05-2018 - 1 Ramadan 1439`() {
            val h = factory.create(CalendarMethod.HJCOSA).fromGregorian(CivilDate(2018, 5, 17))
            assertEquals(Triple(1439, 9, 1), Triple(h.year, h.month, h.day))
        }

        @Test
        fun `1 Ramadan 1439 - 17-05-2018`() {
            val g = factory.create(CalendarMethod.HJCOSA).toGregorian(1439, 9, 1)
            assertEquals(CivilDate(2018, 5, 17), g)
        }
    }

    @Nested
    inner class ValidityRangesThrow {
        @Test
        fun `Umm al-Qura before range`() {
            val e = assertThrows<IllegalArgumentException> {
                factory.create(CalendarMethod.UAQ).toGregorian(1200, 8, 15)
            }
            assertEquals("Hijri date out of range for UAQ ((1356, 1, 1) .. (1500, 12, 30)).", e.message)
        }

        @Test
        fun `Umm al-Qura Gregorian before range`() {
            val e = assertThrows<IllegalArgumentException> {
                factory.create(CalendarMethod.UAQ).fromGregorian(CivilDate(1800, 1, 1))
            }
            assertEquals("Gregorian date out of range for UAQ ((1937, 3, 14) .. (2077, 11, 16)).", e.message)
        }

        @Test
        fun `HJCoSA reports its own code`() {
            val e = assertThrows<IllegalArgumentException> {
                factory.create(CalendarMethod.HJCOSA).fromGregorian(CivilDate(2078, 1, 1))
            }
            assertEquals("Gregorian date out of range for HJCoSA ((1937, 3, 14) .. (2077, 11, 16)).", e.message)
        }

        @Test
        fun `table edges are accepted`() {
            val uaq = factory.create(CalendarMethod.UAQ)
            assertEquals(Triple(1356, 1, 1), uaq.fromGregorian(CivilDate(1937, 3, 14)).let { Triple(it.year, it.month, it.day) })
            assertEquals(Triple(1500, 12, 30), uaq.fromGregorian(CivilDate(2077, 11, 16)).let { Triple(it.year, it.month, it.day) })
            val diyanet = factory.create(CalendarMethod.DIYANET)
            assertEquals(Triple(1318, 1, 1), diyanet.fromGregorian(CivilDate(1900, 5, 1)).let { Triple(it.year, it.month, it.day) })
            assertEquals(Triple(1449, 8, 29), diyanet.fromGregorian(CivilDate(2028, 1, 26)).let { Triple(it.year, it.month, it.day) })
        }
    }
}
