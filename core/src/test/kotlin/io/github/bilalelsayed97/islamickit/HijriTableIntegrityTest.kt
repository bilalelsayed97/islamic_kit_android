package io.github.bilalelsayed97.islamickit

import io.github.bilalelsayed97.islamickit.infrastructure.calendar.data.DiyanetTable
import io.github.bilalelsayed97.islamickit.infrastructure.calendar.data.HijriHolidays
import io.github.bilalelsayed97.islamickit.infrastructure.calendar.data.HijriSightings
import io.github.bilalelsayed97.islamickit.infrastructure.calendar.data.UmmAlQuraTable
import io.github.bilalelsayed97.islamickit.infrastructure.geocoding.data.CityDataset
import io.github.bilalelsayed97.islamickit.infrastructure.geocoding.data.CountryIsoMap
import io.github.bilalelsayed97.islamickit.infrastructure.localization.Localizer
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Pins the generated data tables so a regeneration cannot silently drift. */
class HijriTableIntegrityTest {
    @Test
    fun `Umm al-Qura table`() {
        val data = UmmAlQuraTable.data
        assertEquals(1741, data.size)
        assertEquals(28607, data.first())
        assertEquals(79990, data.last())
        assertMonotonic(data, minDelta = 28, maxDelta = 30)
    }

    @Test
    fun `Diyanet table`() {
        val data = DiyanetTable.data
        assertEquals(2197, data.size)
        assertEquals(15141, data.first())
        assertEquals(79990, data.last())
        assertMonotonic(data, minDelta = 29, maxDelta = 30)
    }

    private fun assertMonotonic(data: IntArray, minDelta: Int, maxDelta: Int) {
        for (i in 1 until data.size) {
            val delta = data[i] - data[i - 1]
            assertTrue(delta in minDelta..maxDelta, "delta at $i is $delta")
        }
    }

    @Test
    fun `sightings, holidays and localizer tables`() {
        assertEquals(22, HijriSightings.gregorianToHijri.size)
        assertEquals(22, HijriSightings.hijriToGregorian.size)
        assertEquals("01-09-1439", HijriSightings.gregorianToHijri["17-05-2018"])
        assertEquals("17-05-2018", HijriSightings.hijriToGregorian["01-09-1439"])
        assertEquals(12, HijriHolidays.byMonth.size)
        assertEquals(listOf("Eid-ul-Fitr"), HijriHolidays.byMonth[10]!![1])
        assertEquals(12, Localizer.islamicMonths.size)
        assertEquals(12, Localizer.gregorianMonths.size)
        assertEquals(7, Localizer.hijriWeekdays.size)
        assertEquals(7, Localizer.gregorianWeekdays.size)
        assertEquals("Apr", Localizer.monthAbbrEn[3])
        // The two Arabic weekday spellings differ deliberately (hamza vs plain alif).
        assertEquals("الاحد", Localizer.hijriWeekday(7).ar)
        assertEquals("الأحد", Localizer.gregorianWeekday(7).ar)
    }

    @Test
    fun `geocoding tables`() {
        assertEquals(103, CityDataset.records.size)
        assertEquals(23, CityDataset.countryAliases.size)
        assertEquals(251, CountryIsoMap.idToIso.size)
        assertEquals(251, CountryIsoMap.isoToId.size)
        assertEquals(18, CountryIsoMap.fractionalZoneOffsetMinutes.size)
        assertEquals("AD", CountryIsoMap.idToIso[1])
        assertEquals(1, CountryIsoMap.isoToId["AD"])
        assertEquals(210, CountryIsoMap.fractionalZoneOffsetMinutes["Asia/Tehran"])
        assertEquals(330, CountryIsoMap.fractionalZoneOffsetMinutes["Asia/Kolkata"])
    }
}
