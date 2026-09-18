package io.github.bilalelsayed97.islamickit

import io.github.bilalelsayed97.islamickit.domain.enums.CalculationMethod
import io.github.bilalelsayed97.islamickit.infrastructure.config.BundledMethodMap
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Port of `test/method_table_test.dart`: the published parameters for every
 * method, asserted as a table so a change to any angle or correction has to
 * be made deliberately.
 */
class MethodTableTest {
    @Nested
    inner class MethodParameters {
        // fajr angle, isha angle (null when interval), isha interval minutes.
        private val expected: Map<CalculationMethod, Triple<Double, Double?, Int?>> = linkedMapOf(
            CalculationMethod.KARACHI to Triple(18.0, 18.0, null),
            CalculationMethod.ISNA to Triple(15.0, 15.0, null),
            CalculationMethod.MWL to Triple(18.0, 17.0, null),
            CalculationMethod.MAKKAH to Triple(18.5, null, 90),
            CalculationMethod.EGYPT to Triple(19.5, 17.5, null),
            CalculationMethod.TEHRAN to Triple(17.7, 14.0, null),
            CalculationMethod.GULF to Triple(19.5, null, 90),
            CalculationMethod.KUWAIT to Triple(18.0, 17.5, null),
            CalculationMethod.QATAR to Triple(18.0, null, 90),
            CalculationMethod.SINGAPORE to Triple(20.0, 18.0, null),
            CalculationMethod.FRANCE to Triple(12.0, 12.0, null),
            CalculationMethod.TURKEY to Triple(18.0, 17.0, null),
            CalculationMethod.RUSSIA to Triple(16.0, 15.0, null),
            CalculationMethod.MOONSIGHTING to Triple(18.0, 18.0, null),
            CalculationMethod.DUBAI to Triple(18.2, 18.2, null),
            CalculationMethod.JAKIM to Triple(20.0, 18.0, null),
            CalculationMethod.TUNISIA to Triple(18.0, 18.0, null),
            CalculationMethod.ALGERIA to Triple(18.0, 17.0, null),
            CalculationMethod.KEMENAG to Triple(20.0, 18.0, null),
            CalculationMethod.MOROCCO to Triple(18.0, 17.0, null),
            CalculationMethod.PORTUGAL to Triple(18.0, null, 77),
            CalculationMethod.JORDAN to Triple(18.5, null, 90),
            CalculationMethod.OMAN to Triple(18.5, null, 90),
            CalculationMethod.MUNICH to Triple(18.0, 17.0, null),
            CalculationMethod.MALDIVES to Triple(18.0, 17.0, null),
            CalculationMethod.CANADA to Triple(15.0, 15.0, null),
            CalculationMethod.TAJIKISTAN to Triple(18.0, 17.0, null),
            CalculationMethod.VIENNA to Triple(18.0, 17.0, null),
            CalculationMethod.BELGIUM to Triple(18.0, 17.0, null),
            CalculationMethod.SUDAN to Triple(19.5, 17.5, null),
            CalculationMethod.LIBYA to Triple(19.5, 17.5, null),
            CalculationMethod.IRAQ to Triple(18.0, 17.0, null),
            CalculationMethod.LUXEMBOURG to Triple(18.0, 17.0, null),
            CalculationMethod.CUSTOM to Triple(15.0, 15.0, null),
        )

        @Test
        fun `covers every method`() {
            assertEquals(CalculationMethod.entries.toSet(), expected.keys)
        }

        @Test
        fun `declaration order matches the Dart package`() {
            assertEquals(expected.keys.toList(), CalculationMethod.entries.toList())
        }

        @TestFactory
        fun `each method carries its published angles`(): List<DynamicTest> = expected.map { (method, values) ->
            val (fajr, isha, interval) = values
            DynamicTest.dynamicTest("${method.code} is $fajr° / ${isha ?: "$interval min"}") {
                assertEquals(fajr, method.params.fajrAngle)
                assertEquals(isha, method.params.ishaAngle)
                assertEquals(interval, method.params.ishaMinutesAfterMaghrib)
            }
        }
    }

    @Nested
    inner class MethodCorrections {
        @Test
        fun `the authorities that publish Dhuhr a minute late`() {
            val plusOne = setOf(
                CalculationMethod.KARACHI,
                CalculationMethod.ISNA,
                CalculationMethod.MWL,
                CalculationMethod.EGYPT,
                CalculationMethod.SINGAPORE,
            )
            for (method in plusOne) {
                assertEquals(1, method.params.adjustments.dhuhr, method.code)
            }
        }

        @Test
        fun `Dubai shifts sunrise, Dhuhr, Asr and Maghrib`() {
            val a = CalculationMethod.DUBAI
            assertEquals(-3, a.params.adjustments.sunrise)
            assertEquals(3, a.params.adjustments.dhuhr)
            assertEquals(3, a.params.adjustments.asr)
            assertEquals(3, a.params.adjustments.maghrib)
        }

        @Test
        fun `Moonsighting shifts Dhuhr +5 and Maghrib +3`() {
            val a = CalculationMethod.MOONSIGHTING
            assertEquals(5, a.params.adjustments.dhuhr)
            assertEquals(3, a.params.adjustments.maghrib)
            assertTrue(a.usesMoonsighting)
        }

        @Test
        fun `Canada is not an alias of ISNA - it applies no corrections`() {
            assertTrue(CalculationMethod.CANADA.params.adjustments.isEmpty)
            assertFalse(CalculationMethod.ISNA.params.adjustments.isEmpty)
        }

        @Test
        fun `only Umm al-Qura varies its Isha interval in Ramadan`() {
            val varying = CalculationMethod.entries.filter { it.params.hasRamadanIshaInterval }
            assertEquals(listOf(CalculationMethod.MAKKAH), varying)
            assertEquals(120, CalculationMethod.MAKKAH.params.ramadanIshaMinutesAfterMaghrib)
            assertEquals(120, CalculationMethod.MAKKAH.params.forRamadan().ishaMinutesAfterMaghrib)
            assertEquals(CalculationMethod.MWL.params, CalculationMethod.MWL.params.forRamadan())
        }
    }

    @Nested
    inner class MethodIds {
        @Test
        fun `are unique`() {
            val ids = CalculationMethod.entries.map { it.id }
            assertEquals(ids.size, ids.toSet().size)
        }

        @Test
        fun `aladhan-numbered methods stay at their published id`() {
            assertEquals(CalculationMethod.KARACHI, CalculationMethod.fromId(1))
            assertEquals(CalculationMethod.MWL, CalculationMethod.fromId(3))
            assertEquals(CalculationMethod.MAKKAH, CalculationMethod.fromId(4))
            assertEquals(CalculationMethod.JORDAN, CalculationMethod.fromId(23))
            assertEquals(CalculationMethod.CUSTOM, CalculationMethod.fromId(99))
        }

        @Test
        fun `package-only methods are numbered from 101`() {
            val extra = CalculationMethod.entries.filter { !it.isAladhanMethod }
            assertEquals(11, extra.size)
            assertTrue(extra.all { it.id >= 101 })
        }

        @Test
        fun `an unknown id falls back to MWL`() {
            assertEquals(CalculationMethod.MWL, CalculationMethod.fromId(-1))
            assertEquals(CalculationMethod.MWL, CalculationMethod.fromCode("NOPE"))
        }
    }

    @Nested
    inner class BundledDatabaseMethodMap {
        @Test
        fun `database ids resolve to their own authority, not aladhan's`() {
            val cases = mapOf(
                1 to CalculationMethod.KARACHI,
                2 to CalculationMethod.ISNA,
                3 to CalculationMethod.MWL,
                4 to CalculationMethod.MAKKAH,
                5 to CalculationMethod.EGYPT,
                6 to CalculationMethod.DUBAI,
                7 to CalculationMethod.KUWAIT,
                8 to CalculationMethod.QATAR,
                9 to CalculationMethod.SINGAPORE,
                17 to CalculationMethod.OMAN,
                20 to CalculationMethod.CANADA,
                28 to CalculationMethod.TEHRAN,
            )
            for ((id, method) in cases) {
                assertEquals(method, BundledMethodMap.methodForBundledId(id), "$id")
            }
        }

        @Test
        fun `covers a contiguous 1 to 30 range`() {
            assertEquals((1..30).toSet(), BundledMethodMap.knownIds)
        }

        @Test
        fun `an unknown id has no method, but a defaulted one is MWL`() {
            assertNull(BundledMethodMap.methodForBundledId(999))
            assertNull(BundledMethodMap.methodForBundledId(null))
            assertEquals(CalculationMethod.MWL, BundledMethodMap.methodForBundledIdOrDefault(999))
        }
    }
}
