package io.github.bilalelsayed97.islamickit.geocoding

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.bilalelsayed97.islamickit.time.UtcOffset
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

/** Port of the `SqliteCityGeocoder (bundled city database)` group of `test/geocoder_test.dart`. */
@RunWith(AndroidJUnit4::class)
class SqliteCityGeocoderTest {
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
    fun resolvesLondonGbWithArabicName() {
        val city = geocoder.resolve("London", country = "GB")
        assertNotNull(city)
        assertTrue(abs(city!!.coordinates.latitude - 51.50853) < 0.01)
        assertTrue(abs(city.coordinates.longitude - (-0.12574)) < 0.01)
        assertNotNull(city.nameAr)
        assertEquals("GB", city.country)
        assertNull(city.state)
    }

    @Test
    fun resolvesMecca() {
        val city = geocoder.resolve("Mecca")
        assertNotNull(city)
        assertTrue(abs(city!!.coordinates.latitude - 21.42) < 0.2)
    }

    @Test
    fun countryNameAliasResolvesToAnIsoCode() {
        assertEquals("EG", geocoder.resolve("Cairo", country = "Egypt")?.country)
    }

    @Test
    fun appliesHalfHourOffsetsThatTheHourColumnTruncates() {
        assertEquals(UtcOffset.ofHours(3, 30), geocoder.resolve("Tehran", country = "IR")?.utcOffset)
        assertEquals(UtcOffset.ofHours(5, 30), geocoder.resolve("Delhi", country = "IN")?.utcOffset)
    }

    @Test
    fun prefersTheCapitalOverSameNamedLesserSettlements() {
        val city = geocoder.resolve("Paris", country = "FR")
        assertNotNull(city)
        assertTrue(abs(city!!.coordinates.latitude - 48.85) < 0.2)
    }

    @Test
    fun unknownCountryCodeMatchesNothing() {
        assertTrue(geocoder.search("London", country = "ZZ").isEmpty())
    }
}
