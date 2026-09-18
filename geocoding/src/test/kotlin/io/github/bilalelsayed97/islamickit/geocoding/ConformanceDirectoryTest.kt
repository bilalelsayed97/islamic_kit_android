package io.github.bilalelsayed97.islamickit.geocoding

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.bilalelsayed97.islamickit.application.PrayerTimesService
import io.github.bilalelsayed97.islamickit.conformance.Fixtures
import io.github.bilalelsayed97.islamickit.conformance.JsonAssert
import io.github.bilalelsayed97.islamickit.conformance.arr
import io.github.bilalelsayed97.islamickit.conformance.double
import io.github.bilalelsayed97.islamickit.conformance.int
import io.github.bilalelsayed97.islamickit.conformance.isNull
import io.github.bilalelsayed97.islamickit.conformance.str
import io.github.bilalelsayed97.islamickit.conformance.strOrNull
import io.github.bilalelsayed97.islamickit.domain.models.CityEntry
import io.github.bilalelsayed97.islamickit.domain.models.CountryInfo
import io.github.bilalelsayed97.islamickit.infrastructure.config.calculationMethod
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith

/**
 * `directory.json`: every `CityDirectory` query the generator recorded against
 * the bundled database, plus the facade's automatic parameters and timings.
 */
@RunWith(AndroidJUnit4::class)
class ConformanceDirectoryTest {
    companion object {
        private lateinit var directory: SqliteCityDirectory
        private lateinit var service: PrayerTimesService

        @JvmStatic
        @BeforeClass
        fun openDatabase() {
            directory = SqliteCityDirectory(TestDatabase.open())
            service = PrayerTimesService(directory = directory)
        }

        @JvmStatic
        @AfterClass
        fun closeDatabase() {
            directory.close()
        }
    }

    private val fixture: JsonObject
        get() = Fixtures.load("directory.json").jsonObject

    /** The generator's `_countryJson` shape. */
    private fun countryJson(c: CountryInfo): Map<String, Any?> = linkedMapOf(
        "id" to c.id,
        "nameEn" to c.nameEn,
        "nameAr" to c.nameAr,
        "isoCode" to c.isoCode,
        "calculationMethodId" to c.calculationMethodId,
        "calculationMethod" to c.calculationMethod?.code,
    )

    /** The generator's `_entryJson` shape. */
    private fun entryJson(e: CityEntry): Map<String, Any?> = linkedMapOf(
        "id" to e.id,
        "nameEn" to e.nameEn,
        "nameAr" to e.nameAr,
        "countryId" to e.countryId,
        "countryNameEn" to e.countryNameEn,
        "countryNameAr" to e.countryNameAr,
        "isoCode" to e.isoCode,
        "lat" to e.coordinates.latitude,
        "lng" to e.coordinates.longitude,
        "offsetMinutes" to e.utcOffset.totalMinutes,
        "timeZoneId" to e.timeZoneId,
        "calculationMethodId" to e.calculationMethodId,
        "calculationMethod" to e.calculationMethod?.code,
    )

    /** Ordered comparison of a city page: the bundled data has no equal-name ties inside any recorded page. */
    private fun assertEntries(label: String, expected: JsonElement, actual: List<CityEntry>) =
        JsonAssert.assertSameJson(expected, actual.map(::entryJson), label)

    @Test
    fun countries() {
        val countries = directory.countries()
        assertEquals(fixture.int("countriesCount"), countries.size)
        JsonAssert.assertSameJson(fixture.getValue("countries"), countries.map(::countryJson), "countries")
    }

    @Test
    fun countryQueries() {
        for (c in fixture.arr("countryQueries").map { it.jsonObject }) {
            val query = c.str("query")
            JsonAssert.assertSameJson(c.getValue("results"), directory.countries(query).map { it.id }, "countries(\"$query\")")
        }
    }

    @Test
    fun countryById() {
        for (c in fixture.arr("countryById").map { it.jsonObject }) {
            val id = c.int("id")
            JsonAssert.assertSameJson(c.getValue("result"), directory.country(id)?.let(::countryJson), "country($id)")
        }
    }

    @Test
    fun citiesInCountry() {
        for (c in fixture.arr("citiesInCountry").map { it.jsonObject }) {
            val countryId = c.int("countryId")
            val query = c.strOrNull("query")
            val limit = c.int("limit")
            val offset = c.int("offset")
            assertEntries(
                "citiesInCountry($countryId, \"$query\", $limit, $offset)",
                c.getValue("results"),
                directory.citiesInCountry(countryId, query, limit, offset),
            )
        }
    }

    @Test
    fun searchCities() {
        for (c in fixture.arr("searchCities").map { it.jsonObject }) {
            val query = c.strOrNull("query")
            val limit = c.int("limit")
            val offset = c.int("offset")
            assertEntries("searchCities(\"$query\", $limit, $offset)", c.getValue("results"), directory.searchCities(query, limit, offset))
        }
    }

    @Test
    fun nearestCity() {
        for (c in fixture.arr("nearestCity").map { it.jsonObject }) {
            val lat = c.double("lat")
            val lng = c.double("lng")
            JsonAssert.assertSameJson(c.getValue("result"), directory.nearestCity(lat, lng)?.let(::entryJson), "nearestCity($lat, $lng)")
        }
    }

    @Test
    fun timeZonesForEveryCountry() {
        val cases = fixture.arr("timeZonesForCountry").map { it.jsonObject }
        assertEquals(251, cases.size)
        for (c in cases) {
            val countryId = c.int("countryId")
            val zones = directory.timeZonesForCountry(countryId).map { z ->
                linkedMapOf("ianaId" to z.ianaId, "nameAr" to z.nameAr)
            }
            JsonAssert.assertSameJson(c.getValue("zones"), zones, "timeZonesForCountry($countryId)")
        }
    }

    @Test
    fun autoParams() {
        for (c in fixture.arr("autoParams").map { it.jsonObject }) {
            val lat = c.double("lat")
            val lng = c.double("lng")
            val params = service.autoParamsForCoordinates(lat, lng)
            JsonAssert.assertSameJson(c.getValue("params"), params?.let(Fixtures::paramsJson), "autoParams($lat, $lng)")
        }
    }

    @Test
    fun timingsByCoordinatesAuto() {
        for (c in fixture.arr("timingsByCoordinatesAuto").map { it.jsonObject }) {
            val lat = c.double("lat")
            val lng = c.double("lng")
            val result = service.timingsByCoordinatesAuto(lat, lng, Fixtures.date(c.str("date")))
            val expected = c.getValue("raw")
            if (expected.isNull) {
                assertEquals("timingsByCoordinatesAuto($lat, $lng)", null, result)
            } else {
                JsonAssert.assertSameJson(expected, Fixtures.rawJson(result!!.timings.raw), "timingsByCoordinatesAuto($lat, $lng)")
            }
        }
    }
}
