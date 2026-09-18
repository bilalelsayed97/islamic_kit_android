package io.github.bilalelsayed97.islamickit.geocoding

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.bilalelsayed97.islamickit.domain.enums.CalculationMethod
import io.github.bilalelsayed97.islamickit.infrastructure.config.calculationMethod
import io.github.bilalelsayed97.islamickit.infrastructure.geocoding.data.CountryIsoMap
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith

/** Port of `test/city_directory_test.dart` against [SqliteCityDirectory]. */
@RunWith(AndroidJUnit4::class)
class CityDirectoryTest {
    companion object {
        private lateinit var directory: SqliteCityDirectory

        @JvmStatic
        @BeforeClass
        fun openDatabase() {
            directory = SqliteCityDirectory(TestDatabase.open())
        }

        @JvmStatic
        @AfterClass
        fun closeDatabase() {
            directory.close()
        }
    }

    private val egypt
        get() = directory.countries(query = "Egypt").single()

    @Test
    fun listsEveryCountryWithAnIsoCodeAndBothNames() {
        val countries = directory.countries()
        assertEquals(251, countries.size)
        assertTrue(countries.all { it.isoCode.length == 2 })
        assertTrue(countries.all { it.nameEn.isNotEmpty() })
        assertTrue(countries.all { it.nameAr.isNotEmpty() })
        // The Arabic names carry trailing tabs in the database; every read trims.
        assertTrue(countries.none { it.nameAr.endsWith("\t") })
    }

    @Test
    fun filtersCountriesByEitherLanguage() {
        assertEquals("EG", directory.countries(query = "Egypt").single().isoCode)
        assertEquals("EG", directory.countries(query = "مصر").single().isoCode)
    }

    @Test
    fun exposesTheRecommendedCalculationMethod() {
        assertEquals(5, egypt.calculationMethodId)
        assertEquals(CalculationMethod.EGYPT, egypt.calculationMethod)
    }

    @Test
    fun resolvesTheDatabaseMethodIdsThatCollideWithAladhanIds() {
        fun methodFor(iso: String): CalculationMethod? =
            directory.country(CountryIsoMap.isoToId.getValue(iso))!!.calculationMethod

        // The database numbers its authorities independently of aladhan: id 7 is
        // Kuwait here but Tehran there, id 9 is Singapore here but Kuwait there.
        assertEquals(CalculationMethod.KUWAIT, methodFor("KW"))
        assertEquals(CalculationMethod.QATAR, methodFor("QA"))
        assertEquals(CalculationMethod.SINGAPORE, methodFor("SG"))
        assertEquals(CalculationMethod.MAKKAH, methodFor("SA"))
        assertEquals(CalculationMethod.TURKEY, methodFor("TR"))
        assertEquals(CalculationMethod.CANADA, methodFor("CA"))
        assertEquals(CalculationMethod.TEHRAN, methodFor("IR"))
        assertEquals(CalculationMethod.OMAN, methodFor("OM"))
        assertEquals(CalculationMethod.MUNICH, methodFor("DE"))
        assertEquals(CalculationMethod.LUXEMBOURG, methodFor("LU"))
        // The default bucket the database assigns to most of the world.
        assertEquals(CalculationMethod.MWL, methodFor("GB"))
    }

    @Test
    fun everyCountryResolvesToAKnownMethod() {
        val unresolved = directory.countries().filter { it.calculationMethod == null }
        assertTrue(unresolved.toString(), unresolved.isEmpty())
    }

    @Test
    fun citiesCarryTheirCountrysRecommendedMethod() {
        assertEquals(CalculationMethod.MAKKAH, directory.nearestCity(21.4225, 39.8262)?.calculationMethod)
    }

    @Test
    fun ordersACountrysCitiesByProminenceCapitalFirst() {
        val cities = directory.citiesInCountry(egypt.id, limit = 1)
        assertEquals("Cairo", cities.single().nameEn)
        assertEquals("القاهرة", cities.single().nameAr)
        assertEquals("EG", cities.single().isoCode)
    }

    @Test
    fun pagesThroughACountrysCitiesWithoutRepeating() {
        val first = directory.citiesInCountry(egypt.id, limit = 10)
        val second = directory.citiesInCountry(egypt.id, limit = 10, offset = 10)
        assertEquals(10, first.size)
        assertEquals(10, second.size)
        assertTrue(first.toSet().intersect(second.toSet()).isEmpty())
    }

    @Test
    fun searchMatchesArabicAndEnglishNames() {
        assertEquals("Cairo", directory.searchCities(query = "القاهرة", limit = 1).single().nameEn)
        assertEquals("القاهرة", directory.searchCities(query = "Cairo", limit = 1).single().nameAr)
    }

    @Test
    fun reverseGeocodingAnswersTheCityNotTheNeighbourhood() {
        val city = directory.nearestCity(30.06263, 31.24967)
        assertEquals("Cairo", city?.nameEn)
        assertEquals("EG", city?.isoCode)
        assertEquals("Africa/Cairo", city?.timeZoneId)
    }

    @Test
    fun reverseGeocodingResolvesMecca() {
        val city = directory.nearestCity(21.4225, 39.8262)
        assertEquals("مكة المكرمة", city?.nameAr)
        assertEquals("SA", city?.isoCode)
    }

    @Test
    fun reverseGeocodingFallsBackWhenTheLocalBoxIsEmpty() {
        // Mid-Atlantic: no populated place for thousands of kilometres.
        assertNotNull(directory.nearestCity(0.0, -30.0))
    }

    @Test
    fun singleZoneCountriesOfferExactlyOneTimezone() {
        val zones = directory.timeZonesForCountry(egypt.id)
        assertEquals(1, zones.size)
        assertEquals("Africa/Cairo", zones.single().ianaId)
        assertTrue(!zones.single().nameAr.isNullOrEmpty())
    }

    @Test
    fun multiZoneCountriesOfferEveryRealZone() {
        val us = directory.countries(query = "United States").first { it.isoCode == "US" }
        val zones = directory.timeZonesForCountry(us.id)
        assertTrue(zones.size >= 8)
        val ids = zones.map { it.ianaId }
        assertTrue(ids.containsAll(listOf("America/New_York", "America/Los_Angeles")))
    }

    @Test
    fun everyOfferedTimezoneCarriesAnArabicLabel() {
        for (country in directory.countries()) {
            for (zone in directory.timeZonesForCountry(country.id)) {
                assertNotNull(zone.ianaId, zone.nameAr)
            }
        }
    }

    @Test
    fun isoMapRoundTripsInBothDirections() {
        assertEquals(251, CountryIsoMap.idToIso.size)
        assertNotNull(CountryIsoMap.isoToId["EG"])
        assertEquals("EG", CountryIsoMap.idToIso[CountryIsoMap.isoToId.getValue("EG")])
    }
}
