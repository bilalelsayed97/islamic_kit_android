package io.github.bilalelsayed97.islamickit

import io.github.bilalelsayed97.islamickit.domain.enums.AsrSchool
import io.github.bilalelsayed97.islamickit.domain.enums.CalculationMethod
import io.github.bilalelsayed97.islamickit.domain.enums.CalendarMethod
import io.github.bilalelsayed97.islamickit.domain.enums.HighLatitudeRule
import io.github.bilalelsayed97.islamickit.domain.enums.Language
import io.github.bilalelsayed97.islamickit.domain.enums.MidnightMode
import io.github.bilalelsayed97.islamickit.domain.enums.Prayer
import io.github.bilalelsayed97.islamickit.domain.enums.Shafaq
import io.github.bilalelsayed97.islamickit.domain.enums.TimeFormat
import io.github.bilalelsayed97.islamickit.domain.valueobjects.CalculationParameters
import io.github.bilalelsayed97.islamickit.domain.valueobjects.MethodParams
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

/** Enum ids, codes, fallbacks and the resolved parameter getters. */
class EnumsTest {
    @Test
    fun `prayer keys and order`() {
        assertEquals(
            listOf("Imsak", "Fajr", "Sunrise", "Dhuhr", "Asr", "Sunset", "Maghrib", "Isha", "Midnight", "Firstthird", "Lastthird"),
            Prayer.entries.map { it.key },
        )
        assertEquals(Prayer.FIRST_THIRD, Prayer.fromKey("Firstthird"))
        assertThrows<IllegalArgumentException> { Prayer.fromKey("firstthird") }
        assertEquals(listOf(Prayer.FAJR, Prayer.DHUHR, Prayer.ASR, Prayer.MAGHRIB, Prayer.ISHA), Prayer.DAILY_OBLIGATORY)
        assertEquals("الفجر", Prayer.FAJR.localizedName(Language.AR))
        assertEquals("Fajr", Prayer.FAJR.localizedName(Language.EN))
        assertEquals("الثلث الأخير", Prayer.LAST_THIRD.title(Language.AR))
        assertEquals("The end of the first third of the night.", Prayer.FIRST_THIRD.description(Language.EN))
    }

    @Test
    fun `ids, codes and fallbacks`() {
        assertEquals(AsrSchool.STANDARD, AsrSchool.fromAladhanId(42))
        assertEquals(AsrSchool.HANAFI, AsrSchool.fromAladhanId(1))
        assertEquals(2, AsrSchool.HANAFI.shadowFactor)
        assertEquals(MidnightMode.STANDARD, MidnightMode.fromAladhanId(42))
        assertEquals(MidnightMode.JAFARI, MidnightMode.fromAladhanId(1))
        assertEquals(HighLatitudeRule.ANGLE_BASED, HighLatitudeRule.fromAladhanId(42))
        assertEquals(HighLatitudeRule.MIDDLE_OF_NIGHT, HighLatitudeRule.fromAladhanId(1))
        assertEquals("MIDDLE_OF_THE_NIGHT", HighLatitudeRule.MIDDLE_OF_NIGHT.metaValue)
        assertEquals(Shafaq.GENERAL, Shafaq.fromCode("nope"))
        assertEquals(Shafaq.ABYAD, Shafaq.fromCode("abyad"))
        assertEquals(CalendarMethod.HJCOSA, CalendarMethod.fromCode("nope"))
        assertEquals(CalendarMethod.DIYANET, CalendarMethod.fromCode("DIYANET"))
        assertEquals("HJCoSA", CalendarMethod.HJCOSA.code)
        assertEquals(listOf("24h", "12h", "12hNS", "Float", "iso8601"), TimeFormat.entries.map { it.code })
        assertEquals(CalculationMethod.MWL, CalculationMethod.fromId(0))
        assertEquals(CalculationMethod.KEMENAG, CalculationMethod.fromCode("KEMENAG"))
        assertEquals(34, CalculationMethod.entries.size)
        assertEquals(CalculationMethod.CUSTOM, CalculationMethod.entries.last())
    }

    @Test
    fun `localized labels`() {
        assertEquals("Hanafi", AsrSchool.HANAFI.title(Language.EN))
        assertEquals("الحنفي", AsrSchool.HANAFI.title(Language.AR))
        assertEquals("Umm al-Qura", CalculationMethod.MAKKAH.title(Language.EN))
        assertEquals("أم القرى", CalculationMethod.MAKKAH.title(Language.AR))
        assertEquals("Arabic", Language.AR.title(Language.EN))
        assertEquals("العربية", Language.AR.title(Language.AR))
        assertEquals("ISO 8601", TimeFormat.ISO8601.title(Language.EN))
        assertEquals("12-hour clock without an am/pm suffix, e.g. 3:57.", TimeFormat.H12_NO_SUFFIX.description(Language.EN))
    }

    @Test
    fun `CalculationParameters resolves custom, midnight and shadow`() {
        val defaults = CalculationParameters()
        assertEquals(CalculationMethod.MWL, defaults.method)
        assertEquals(MidnightMode.JAFARI, defaults.resolvedMidnightMode)
        assertEquals(1.0, defaults.resolvedShadowFactor)
        assertEquals(HighLatitudeRule.MIDDLE_OF_NIGHT, defaults.resolvedHighLatitudeRule)
        assertEquals(10, defaults.imsakMinutes)
        assertEquals(CalendarMethod.HJCOSA, defaults.calendarMethod)

        val tehran = CalculationParameters(method = CalculationMethod.TEHRAN)
        assertEquals(MidnightMode.JAFARI, tehran.resolvedMidnightMode)
        assertEquals(MidnightMode.STANDARD, tehran.copy(midnightMode = MidnightMode.STANDARD).resolvedMidnightMode)

        val custom = MethodParams(fajrAngle = 16.0, ishaAngle = 14.0)
        val withCustom = CalculationParameters(method = CalculationMethod.CUSTOM, customMethod = custom)
        assertEquals(custom, withCustom.effectiveParams)
        assertEquals(CalculationMethod.CUSTOM.params, CalculationParameters(method = CalculationMethod.CUSTOM).effectiveParams)
        // A custom params object is ignored unless the method is CUSTOM.
        assertEquals(CalculationMethod.MWL.params, CalculationParameters(customMethod = custom).effectiveParams)

        assertEquals(2.0, CalculationParameters(school = AsrSchool.HANAFI).resolvedShadowFactor)
        assertEquals(1.5, CalculationParameters(school = AsrSchool.HANAFI, asrShadowFactor = 1.5).resolvedShadowFactor)
    }
}
