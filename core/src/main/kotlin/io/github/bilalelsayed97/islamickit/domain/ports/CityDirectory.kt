package io.github.bilalelsayed97.islamickit.domain.ports

import io.github.bilalelsayed97.islamickit.domain.models.CityEntry
import io.github.bilalelsayed97.islamickit.domain.models.CountryInfo
import io.github.bilalelsayed97.islamickit.domain.models.TimeZoneInfo

/**
 * Browsable, localized view over the bundled city database.
 *
 * A [Geocoder] answers "which city is this text?"; a [CityDirectory] answers
 * the questions a location-picker asks instead — list the countries, page
 * through a country's cities, resolve GPS coordinates to a city, and offer the
 * timezones a country actually spans. Every method reads English and Arabic
 * names together so the caller can render either locale.
 *
 * Core defines only the port; the `geocoding` module ships the SQLite-backed
 * `SqliteCityDirectory`.
 */
interface CityDirectory {
    /**
     * Every country that has at least one populated place, ordered by English
     * name. [query] filters on either name, case-insensitively.
     */
    fun countries(query: String? = null): List<CountryInfo>

    /** The country with [countryId], or `null` when no such row exists. */
    fun country(countryId: Int): CountryInfo?

    /**
     * Populated places inside [countryId], most prominent first.
     *
     * [query] filters on either name; [limit] and [offset] page.
     */
    fun citiesInCountry(countryId: Int, query: String? = null, limit: Int = 50, offset: Int = 0): List<CityEntry>

    /** Populated places anywhere, most prominent first. See [citiesInCountry]. */
    fun searchCities(query: String? = null, limit: Int = 50, offset: Int = 0): List<CityEntry>

    /** The city [latitude]/[longitude] most plausibly names, or `null` when the database is empty. */
    fun nearestCity(latitude: Double, longitude: Double): CityEntry?

    /** The timezones [countryId] actually spans, ordered by IANA id. */
    fun timeZonesForCountry(countryId: Int): List<TimeZoneInfo>
}
