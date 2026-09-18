package io.github.bilalelsayed97.islamickit

import io.github.bilalelsayed97.islamickit.infrastructure.geocoding.BundledCityGeocoder
import io.github.bilalelsayed97.islamickit.infrastructure.geocoding.CityNameMatching
import io.github.bilalelsayed97.islamickit.infrastructure.geocoding.data.CityDataset
import io.github.bilalelsayed97.islamickit.time.UtcOffset
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Port of the curated half of `test/geocoder_test.dart`. The SQLite group
 * lives in the `geocoding` module.
 */
class GeocoderTest {
    @Nested
    inner class BundledCityGeocoderCurated {
        private val geocoder = BundledCityGeocoder()

        @Test
        fun `resolves London, GB`() {
            val city = geocoder.resolve("London", country = "GB")
            assertNotNull(city)
            assertTrue(abs(city.coordinates.latitude - 51.5) < 0.1)
            assertEquals("GB", city.country)
            assertEquals("England", city.state)
            assertEquals(UtcOffset.ZERO, city.utcOffset)
        }

        @Test
        fun `resolves a free-text address by segment`() {
            val city = geocoder.resolve("Trafalgar Square, London, UK")
            assertEquals("London", city?.name)
        }

        @Test
        fun `country name alias works`() {
            val city = geocoder.resolve("Cairo", country = "Egypt")
            assertEquals("EG", city?.country)
            assertEquals(UtcOffset.ofHours(2), city?.utcOffset)
        }

        @Test
        fun `two-letter input is a code, not an alias`() {
            assertNull(geocoder.resolve("London", country = "uk"))
            assertNotNull(geocoder.resolve("London", country = "gb"))
        }

        @Test
        fun `state filter and unknown queries`() {
            assertEquals("London", geocoder.resolve("London", state = "England")?.name)
            assertNull(geocoder.resolve("London", state = "Ontario"))
            assertTrue(geocoder.search("").isEmpty())
            assertTrue(geocoder.search("   ").isEmpty())
            assertTrue(geocoder.search("Atlantis").isEmpty())
        }

        @Test
        fun `results are ordered best match first`() {
            val results = geocoder.search("Man")
            assertTrue(results.isNotEmpty())
            assertEquals("Manchester", results.first().name)
            assertEquals(103, CityDataset.records.size)
        }

        @Test
        fun `City toString mirrors Dart`() {
            assertEquals("City(London, England, GB)", geocoder.resolve("London")!!.toString())
            assertEquals("City(Dublin, IE)", geocoder.resolve("Dublin")!!.toString())
        }
    }

    @Nested
    inner class Matching {
        @Test
        fun `normalize trims, lower-cases and collapses whitespace`() {
            assertEquals("new york", CityNameMatching.normalize("  New" + 0xA0.toChar() + " York\t"))
            assertEquals("", CityNameMatching.normalize(" \n "))
        }

        @Test
        fun `candidates include the full query and each segment once`() {
            assertEquals(
                listOf("trafalgar square, london, uk", "trafalgar square", "london", "uk"),
                CityNameMatching.candidates("Trafalgar Square, London, UK"),
            )
            assertEquals(listOf("london, london", "london"), CityNameMatching.candidates("London, london"))
            assertEquals(listOf(","), CityNameMatching.candidates(","))
            assertTrue(CityNameMatching.candidates("").isEmpty())
            assertTrue(CityNameMatching.candidates(" \t ").isEmpty())
        }

        @Test
        fun `score ranks exact, prefix, contained-in, contains`() {
            assertEquals(0, CityNameMatching.score("london", listOf("london")))
            assertEquals(1, CityNameMatching.score("londonderry", listOf("london")))
            assertEquals(2, CityNameMatching.score("york", listOf("new york")))
            assertEquals(3, CityNameMatching.score("new york", listOf("york")))
            assertNull(CityNameMatching.score("paris", listOf("rome")))
            assertEquals(0, CityNameMatching.score("london", listOf("lon", "london")))
        }

        @Test
        fun `resolveCountry`() {
            assertNull(CityNameMatching.resolveCountry(null))
            assertEquals("GB", CityNameMatching.resolveCountry("gb"))
            assertEquals("UK", CityNameMatching.resolveCountry("uk"))
            assertEquals("GB", CityNameMatching.resolveCountry("United Kingdom"))
            assertEquals("EG", CityNameMatching.resolveCountry("egypt"))
            assertEquals("NARNIA", CityNameMatching.resolveCountry("Narnia"))
        }

        @Test
        fun `placeRank orders settlement codes`() {
            assertEquals(listOf(0, 1, 2, 3, 4, 5, 6, 6), listOf("PPLC", "PPLA", "PPLA2", "PPLA3", "PPLA4", "PPL", "PPLX", null).map { CityNameMatching.placeRank(it) })
        }
    }
}
