package io.github.bilalelsayed97.islamickit

import io.github.bilalelsayed97.islamickit.application.PrayerTimesService
import io.github.bilalelsayed97.islamickit.domain.enums.AsrSchool
import io.github.bilalelsayed97.islamickit.domain.enums.CalculationMethod
import io.github.bilalelsayed97.islamickit.domain.enums.Prayer
import io.github.bilalelsayed97.islamickit.domain.models.CityEntry
import io.github.bilalelsayed97.islamickit.domain.services.TimeFormatting.INVALID_TIME
import io.github.bilalelsayed97.islamickit.domain.valueobjects.Coordinates
import io.github.bilalelsayed97.islamickit.infrastructure.config.LocationDefaults
import io.github.bilalelsayed97.islamickit.time.CivilDate
import io.github.bilalelsayed97.islamickit.time.UtcOffset
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/** Port of `test/location_defaults_test.dart`. */
class LocationDefaultsTest {
    @Nested
    inner class Defaults {
        @Test
        fun `method by country`() {
            assertEquals(CalculationMethod.ISNA, LocationDefaults.methodForCountry("US"))
            assertEquals(CalculationMethod.MAKKAH, LocationDefaults.methodForCountry("SA"))
            assertEquals(CalculationMethod.KARACHI, LocationDefaults.methodForCountry("PK"))
            assertEquals(CalculationMethod.MWL, LocationDefaults.methodForCountry("gb"))
            assertEquals(CalculationMethod.MWL, LocationDefaults.methodForCountry("XX"))
        }

        @Test
        fun `school by country`() {
            assertEquals(AsrSchool.HANAFI, LocationDefaults.schoolForCountry("PK"))
            assertEquals(AsrSchool.STANDARD, LocationDefaults.schoolForCountry("GB"))
            assertEquals(AsrSchool.HANAFI, LocationDefaults.schoolForCountry("tr"))
        }
    }

    @Nested
    inner class AutomaticPerLocationSettings {
        private val service = PrayerTimesService() // curated geocoder (has Cairo, EG)

        @Test
        fun `recommendedParams picks method + school`() {
            val p = service.recommendedParams("EG", utcOffset = UtcOffset.ofHours(2))
            assertEquals(CalculationMethod.EGYPT, p.method)
            assertEquals(AsrSchool.STANDARD, p.school)
            assertEquals(UtcOffset.ofHours(2), p.utcOffset)
        }

        @Test
        fun `timingsByCityAuto infers method + offset with no presets`() {
            val result = service.timingsByCityAuto("Cairo", country = "EG", date = CivilDate(2024, 4, 24))
            assertEquals(CalculationMethod.EGYPT, result.meta.method)
            assertTrue(abs(result.meta.coordinates.latitude - 30.04) < 0.1)
            assertNotEquals(INVALID_TIME, result.formatted(Prayer.FAJR))
            assertEquals(UtcOffset.ofHours(2), result.timings.utcOffset)
        }

        @Test
        fun `timingsByCity applies the city offset only when the caller set none`() {
            val auto = service.timingsByCity("Cairo", date = CivilDate(2024, 4, 24), country = "EG")
            assertEquals(UtcOffset.ofHours(2), auto.timings.utcOffset)
            val explicit = service.timingsByCity(
                "Cairo",
                date = CivilDate(2024, 4, 24),
                country = "EG",
                params = service.recommendedParams("EG", utcOffset = UtcOffset.ofHours(3)),
            )
            assertEquals(UtcOffset.ofHours(3), explicit.timings.utcOffset)
        }

        @Test
        fun `an unknown location is a state error`() {
            val e = assertThrows<IllegalStateException> {
                service.timingsByAddress("Atlantis", date = CivilDate(2024, 4, 24))
            }
            assertEquals("Location not found: \"Atlantis\".", e.message)
        }

        @Test
        fun `coordinate-based automation requires a directory`() {
            assertThrows<IllegalStateException> { service.autoParamsForCoordinates(30.0, 31.0) }
        }

        @Test
        fun `autoParamsForCityEntry uses the database method or falls back`() {
            val cairo = CityEntry(
                id = 1,
                nameEn = "Cairo",
                nameAr = "القاهرة",
                countryId = 65,
                countryNameEn = "Egypt",
                countryNameAr = "مصر",
                isoCode = "EG",
                coordinates = Coordinates(30.06263, 31.24967),
                utcOffset = UtcOffset.ofHours(2),
                timeZoneId = "Africa/Cairo",
                calculationMethodId = 7, // database id 7 is Kuwait, not Tehran
            )
            val p = service.autoParamsForCityEntry(cairo)
            assertEquals(CalculationMethod.KUWAIT, p.method)
            assertEquals("Africa/Cairo", p.timezoneName)
            assertEquals(UtcOffset.ofHours(2), p.utcOffset)

            val noMethod = CityEntry(
                id = 2, nameEn = "Karachi", nameAr = "", countryId = 1, countryNameEn = "", countryNameAr = "",
                isoCode = "PK", coordinates = Coordinates(24.86, 67.01), utcOffset = UtcOffset.ofHours(5),
            )
            val q = service.autoParamsForCityEntry(noMethod)
            assertEquals(CalculationMethod.KARACHI, q.method)
            assertEquals(AsrSchool.HANAFI, q.school)
        }
    }
}
