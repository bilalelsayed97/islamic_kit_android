package io.github.bilalelsayed97.islamickit

import io.github.bilalelsayed97.islamickit.application.PrayerTimesService
import io.github.bilalelsayed97.islamickit.domain.enums.AsrSchool
import io.github.bilalelsayed97.islamickit.domain.enums.CalculationMethod
import io.github.bilalelsayed97.islamickit.domain.enums.HighLatitudeRule
import io.github.bilalelsayed97.islamickit.domain.enums.MidnightMode
import io.github.bilalelsayed97.islamickit.domain.enums.Prayer
import io.github.bilalelsayed97.islamickit.domain.enums.TimeFormat
import io.github.bilalelsayed97.islamickit.domain.models.PrayerResult
import io.github.bilalelsayed97.islamickit.domain.services.TimeFormatting.INVALID_TIME
import io.github.bilalelsayed97.islamickit.domain.valueobjects.CalculationParameters
import io.github.bilalelsayed97.islamickit.domain.valueobjects.Coordinates
import io.github.bilalelsayed97.islamickit.internal.DartMath
import io.github.bilalelsayed97.islamickit.time.CivilDate
import io.github.bilalelsayed97.islamickit.time.CivilDateTime
import io.github.bilalelsayed97.islamickit.time.UtcOffset
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/** Port of `test/prayer_times_test.dart`. */
class PrayerTimesTest {
    private val service = PrayerTimesService()

    private val london = Coordinates(51.508515, -0.1254872)
    private val londonParams = CalculationParameters(
        method = CalculationMethod.ISNA,
        utcOffset = UtcOffset.ofHours(1), // Europe/London, BST on 2014-04-24
    )

    // Reference vector published with the Adhan library. If the solar algorithm
    // is ported correctly, every one of these matches to the minute.
    @Nested
    inner class AdhanReferenceRaleigh {
        private val raleigh = Coordinates(35.7750, -78.6336)
        private val params = CalculationParameters(
            method = CalculationMethod.ISNA,
            school = AsrSchool.HANAFI,
            utcOffset = UtcOffset.ofHours(-4), // America/New_York, EDT
        )
        private val result: PrayerResult = service.timings(CivilDate(2015, 7, 12), raleigh, params)

        @TestFactory
        fun `every prayer matches the Adhan reference`(): List<DynamicTest> {
            val expected = linkedMapOf(
                Prayer.FAJR to "4:42 am",
                Prayer.SUNRISE to "6:08 am",
                Prayer.DHUHR to "1:21 pm",
                Prayer.ASR to "6:22 pm",
                Prayer.MAGHRIB to "8:32 pm",
                Prayer.ISHA to "9:57 pm",
            )
            return expected.map { (prayer, time) ->
                DynamicTest.dynamicTest("${prayer.key} == $time") {
                    assertEquals(time, result.formatted(prayer, TimeFormat.H12))
                }
            }
        }

        @Test
        fun `Shafi Asr differs from Hanafi`() {
            val shafi = service.timings(CivilDate(2015, 7, 12), raleigh, params.copy(school = AsrSchool.STANDARD))
            assertEquals("5:09 pm", shafi.formatted(Prayer.ASR, TimeFormat.H12))
        }
    }

    @Nested
    inner class IsnaLondon24h {
        private val result = service.timings(CivilDate(2014, 4, 24), london, londonParams)

        @TestFactory
        fun `every prayer matches`(): List<DynamicTest> {
            val expected = linkedMapOf(
                Prayer.FAJR to "03:57",
                Prayer.SUNRISE to "05:46",
                // ISNA publishes Dhuhr a minute past the zenith.
                Prayer.DHUHR to "13:00",
                Prayer.ASR to "16:56",
                Prayer.SUNSET to "20:12",
                Prayer.MAGHRIB to "20:12",
                Prayer.ISHA to "22:02",
                Prayer.IMSAK to "03:47",
                // Night measured sunset -> next Fajr (the engine's default basis).
                Prayer.MIDNIGHT to "00:05",
                Prayer.FIRST_THIRD to "22:47",
                Prayer.LAST_THIRD to "01:22",
            )
            return expected.map { (prayer, time) ->
                DynamicTest.dynamicTest("${prayer.key} == $time") {
                    assertEquals(time, result.formatted(prayer))
                }
            }
        }

        @Test
        fun `raw map iterates in Prayer order`() {
            assertEquals(Prayer.entries.toList(), result.timings.raw.keys.toList())
        }
    }

    @Nested
    inner class MidnightBasis {
        @Test
        fun `standard mode measures sunset to sunrise`() {
            val r = service.timings(
                CivilDate(2014, 4, 24),
                london,
                londonParams.copy(midnightMode = MidnightMode.STANDARD),
            )
            assertEquals("00:59", r.formatted(Prayer.MIDNIGHT))
        }

        @Test
        fun `default (jafari) measures sunset to Fajr`() {
            val r = service.timings(CivilDate(2014, 4, 24), london, londonParams)
            assertEquals("00:05", r.formatted(Prayer.MIDNIGHT))
        }
    }

    @Nested
    inner class Iso8601WithDayRollover {
        @Test
        fun `mid-latitude Fajr`() {
            val r = service.timings(CivilDate(2014, 4, 24), london, londonParams)
            assertEquals("2014-04-24T03:57:00+01:00", r.formatted(Prayer.FAJR, TimeFormat.ISO8601))
        }

        @Test
        fun `high latitude - Isha rolls to next day`() {
            val r = service.timings(CivilDate(2014, 4, 24), Coordinates(70.0, -10.0), londonParams)
            assertEquals("2014-04-25T01:40:00+01:00", r.formatted(Prayer.ISHA, TimeFormat.ISO8601))
        }

        @Test
        fun `high latitude - Fajr rolls to previous day`() {
            val r = service.timings(CivilDate(2014, 4, 24), Coordinates(70.0, 40.0), londonParams)
            assertEquals("2014-04-23T22:20:00+01:00", r.formatted(Prayer.FAJR, TimeFormat.ISO8601))
        }
    }

    @Nested
    inner class HighLatitude {
        @Test
        fun `safe bounds keep every time valid where the sun still rises`() {
            val r = service.timings(
                CivilDate(2018, 1, 19),
                Coordinates(67.104732, 67.104732),
                CalculationParameters(
                    method = CalculationMethod.KARACHI,
                    utcOffset = UtcOffset.ofHours(5), // Asia/Yekaterinburg
                ),
            )
            for (prayer in Prayer.entries) {
                assertNotEquals(INVALID_TIME, r.formatted(prayer), prayer.key)
            }
        }

        @Test
        fun `polar night invalidates the whole day`() {
            val r = service.timings(
                CivilDate(2024, 12, 15),
                Coordinates(78.2232, 15.6469), // Longyearbyen
                CalculationParameters(utcOffset = UtcOffset.ofHours(1)),
            )
            for (prayer in Prayer.entries) {
                assertEquals(INVALID_TIME, r.formatted(prayer), prayer.key)
            }
        }

        @Test
        fun `rule none leaves an unreachable angle invalid`() {
            // Stockholm at the solstice: the sun sets, but never falls 18° below
            // the horizon, so Fajr and Isha have no angle-based solution.
            val stockholm = Coordinates(59.3293, 18.0686)
            val params = CalculationParameters(utcOffset = UtcOffset.ofHours(2))

            val none = service.timings(
                CivilDate(2024, 6, 21),
                stockholm,
                params.copy(highLatitudeRule = HighLatitudeRule.NONE),
            )
            assertEquals("03:31", none.formatted(Prayer.SUNRISE))
            assertEquals(INVALID_TIME, none.formatted(Prayer.FAJR))
            assertEquals(INVALID_TIME, none.formatted(Prayer.ISHA))

            // The default rule bounds both at the middle of the night instead.
            val bounded = service.timings(CivilDate(2024, 6, 21), stockholm, params)
            assertEquals("00:50", bounded.formatted(Prayer.FAJR))
            assertEquals("00:50", bounded.formatted(Prayer.ISHA))
        }
    }

    @Nested
    inner class MoonsightingLondon {
        private val result = service.timings(
            CivilDate(2014, 4, 24),
            london,
            CalculationParameters(
                method = CalculationMethod.MOONSIGHTING,
                utcOffset = UtcOffset.ofHours(1),
            ),
        )

        @Test
        fun `Fajr == 04_04`() = assertEquals("04:04", result.formatted(Prayer.FAJR))

        @Test
        fun `Isha == 21_21`() = assertEquals("21:21", result.formatted(Prayer.ISHA))

        @Test
        fun `Imsak == 03_54`() = assertEquals("03:54", result.formatted(Prayer.IMSAK))

        @Test
        fun `Sunrise unchanged (05_46)`() = assertEquals("05:46", result.formatted(Prayer.SUNRISE))

        @Test
        fun `method adjustments move Dhuhr +5 and Maghrib +3`() {
            assertEquals("13:04", result.formatted(Prayer.DHUHR))
            assertEquals("20:15", result.formatted(Prayer.MAGHRIB))
        }
    }

    @Nested
    inner class UmmAlQuraRamadanIshaInterval {
        private val makkah = Coordinates(21.4225, 39.8262)
        private val params = CalculationParameters(
            method = CalculationMethod.MAKKAH,
            utcOffset = UtcOffset.ofHours(3),
        )

        private fun gapMinutes(r: PrayerResult): Long {
            val maghrib = r.timings.time(Prayer.MAGHRIB).hours!!
            val isha = r.timings.time(Prayer.ISHA).hours!!
            return DartMath.round((isha - maghrib) * 60)
        }

        @Test
        fun `inside Ramadan the interval is 120 minutes`() {
            // 1447 AH Ramadan runs from roughly 2026-02-18.
            val r = service.timings(CivilDate(2026, 2, 20), makkah, params)
            assertEquals(120L, gapMinutes(r))
        }

        @Test
        fun `outside Ramadan the interval is 90 minutes`() {
            val r = service.timings(CivilDate(2026, 4, 20), makkah, params)
            assertEquals(90L, gapMinutes(r))
        }

        @Test
        fun `no other method varies by month`() {
            val r = service.timings(CivilDate(2026, 2, 20), makkah, params.copy(method = CalculationMethod.QATAR))
            assertEquals(90L, gapMinutes(r))
        }
    }

    @Nested
    inner class NextPrayerTests {
        @Test
        fun `after Dhuhr returns Asr`() {
            val next = service.nextPrayer(CivilDateTime(2014, 4, 24, 13, 30), london, londonParams)
            assertEquals(Prayer.ASR, next.prayer)
            assertEquals("16:56", next.time.format())
        }

        @Test
        fun `after Isha rolls to next day Fajr`() {
            val next = service.nextPrayer(CivilDateTime(2014, 4, 24, 23, 30), london, londonParams)
            assertEquals(Prayer.FAJR, next.prayer)
            assertEquals(CivilDate(2014, 4, 25), next.onDate)
        }
    }

    @Nested
    inner class Calendars {
        @Test
        fun `monthly calendar has one entry per day`() {
            val month = service.monthlyCalendar(2014, 4, london, londonParams)
            assertEquals(30, month.size)
            assertTrue(month.first().formatted(Prayer.FAJR).isNotEmpty())
        }

        @Test
        fun `annual calendar keyed by 12 months`() {
            val year = service.annualCalendar(2014, london, londonParams)
            assertEquals((1..12).toList(), year.keys.toList())
            assertEquals(31, year.getValue(1).size)
        }

        @Test
        fun `range calendar rejects more than 11 months`() {
            assertThrows<IllegalArgumentException> {
                service.rangeCalendar(CivilDate(2014, 1, 1), CivilDate(2015, 1, 1), london, londonParams)
            }
        }

        @Test
        fun `range calendar rejects end before start`() {
            assertThrows<IllegalArgumentException> {
                service.rangeCalendar(CivilDate(2014, 1, 2), CivilDate(2014, 1, 1), london, londonParams)
            }
        }

        @Test
        fun `range calendar accepts exactly 11 months and counts every day`() {
            val days = service.rangeCalendar(CivilDate(2014, 1, 1), CivilDate(2014, 12, 1), london, londonParams)
            assertEquals(335, days.size)
            assertEquals(CivilDate(2014, 12, 1), days.last().timings.date)
        }

        @Test
        fun `Hijri monthly calendar covers the whole month`() {
            val month = service.monthlyHijriCalendar(1446, 8, london, londonParams)
            // HJCoSA (the default) reads Sha'ban 1446 from the Umm al-Qura table: 29 days.
            assertEquals(29, month.size)
            assertEquals(CivilDate(2025, 1, 31), month.first().timings.date)
        }
    }

    @Nested
    inner class DateBlock {
        private val result = service.timings(CivilDate(2014, 4, 24), london, londonParams)

        @Test
        fun `readable label and timestamp reflect the civil date at the offset`() {
            assertEquals("24 Apr 2014", result.date.readable)
            // 2014-04-24T00:00:00Z is 1398297600; minus the +01:00 offset.
            assertEquals(1398297600L - 3600L, result.date.timestamp)
            assertEquals("Thursday", result.date.gregorian.weekdayEn)
            assertEquals("April", result.date.gregorian.monthEn)
            assertEquals("24-04-2014", result.date.gregorian.formatted)
        }

        @Test
        fun `meta echoes the parameters`() {
            assertEquals("UTC+01:00", result.meta.timezone)
            assertEquals(MidnightMode.JAFARI, result.meta.midnightMode)
            assertEquals(HighLatitudeRule.MIDDLE_OF_NIGHT, result.meta.latitudeAdjustmentMethod)
            assertEquals(CalculationMethod.ISNA.params, result.meta.methodParams)
            val utc = service.timings(CivilDate(2014, 4, 24), london, CalculationParameters())
            assertEquals("UTC", utc.meta.timezone)
        }

        @Test
        fun `toEpochMillis converts through the offset`() {
            val fajr = result.time(Prayer.FAJR)
            // 03:57 local (+01:00) == 02:57Z on 2014-04-24.
            assertEquals(1398297600L * 1000 + (2 * 3600 + 57 * 60) * 1000L, fajr.toEpochMillis())
        }
    }
}
