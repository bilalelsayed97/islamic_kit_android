package io.github.bilalelsayed97.islamickit.conformance

import io.github.bilalelsayed97.islamickit.infrastructure.geocoding.BundledCityGeocoder
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import kotlin.test.assertEquals

/**
 * `geocoder_curated.json`: `BundledCityGeocoder.search` results as a multiset
 * of `(name, nameAr, country, state, lat, lng, offsetMinutes)`, plus the
 * first result whenever the fixture flags the best score as unique.
 */
class ConformanceCuratedGeocoderTest {
    private val geocoder = BundledCityGeocoder()

    @TestFactory
    fun searches(): List<DynamicNode> = Fixtures.cases("geocoder_curated.json").map { c ->
        val query = c.str("query")
        val country = c.strOrNull("country")
        val state = c.strOrNull("state")
        dynamicTest("\"$query\" country=$country state=$state") {
            val results = geocoder.search(query, country, state)
            val expected = c.arr("results").map { GeocoderRow.fromJson(it.jsonObject) }
            val actual = results.map(GeocoderRow::fromCity)
            assertEquals(expected.size, actual.size, "result count")
            assertEquals(expected.groupingBy { it }.eachCount(), actual.groupingBy { it }.eachCount(), "result set")
            if (c.bool("firstUnique")) assertEquals(expected.first(), actual.first(), "first result")
        }
    }
}
