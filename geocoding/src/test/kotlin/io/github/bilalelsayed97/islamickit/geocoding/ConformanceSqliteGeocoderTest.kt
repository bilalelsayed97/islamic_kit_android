package io.github.bilalelsayed97.islamickit.geocoding

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.bilalelsayed97.islamickit.conformance.Fixtures
import io.github.bilalelsayed97.islamickit.conformance.GeocoderRow
import io.github.bilalelsayed97.islamickit.conformance.arr
import io.github.bilalelsayed97.islamickit.conformance.bool
import io.github.bilalelsayed97.islamickit.conformance.str
import io.github.bilalelsayed97.islamickit.conformance.strOrNull
import kotlinx.serialization.json.jsonObject
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith

/**
 * `geocoder_sqlite.json`: `SqliteCityGeocoder.search` against the bundled
 * database — the same result multiset as Dart, and the same first result
 * whenever the best `(score, rank)` is unique.
 */
@RunWith(AndroidJUnit4::class)
class ConformanceSqliteGeocoderTest {
    companion object {
        private lateinit var geocoder: SqliteCityGeocoder

        @JvmStatic
        @BeforeClass
        fun openDatabase() {
            geocoder = SqliteCityGeocoder(TestDatabase.open())
        }

        @JvmStatic
        @AfterClass
        fun closeDatabase() {
            geocoder.close()
        }
    }

    @Test
    fun everySearchMatchesTheFixture() {
        val cases = Fixtures.cases("geocoder_sqlite.json")
        assertEquals(30, cases.size)
        for (c in cases) {
            val query = c.str("query")
            val country = c.strOrNull("country")
            val label = "\"$query\" country=$country"
            val expected = c.arr("results").map { GeocoderRow.fromJson(it.jsonObject) }
            val actual = geocoder.search(query, country).map(GeocoderRow::fromCity)
            assertEquals("$label result count", expected.size, actual.size)
            assertEquals("$label result set", expected.groupingBy { it }.eachCount(), actual.groupingBy { it }.eachCount())
            if (c.bool("firstUnique")) assertEquals("$label first result", expected.first(), actual.first())
        }
    }
}
