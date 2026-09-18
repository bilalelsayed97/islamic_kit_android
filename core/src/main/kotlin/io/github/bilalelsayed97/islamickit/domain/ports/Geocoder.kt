package io.github.bilalelsayed97.islamickit.domain.ports

import io.github.bilalelsayed97.islamickit.domain.models.City

/**
 * Resolves free-text locations (addresses / city names) to coordinates.
 *
 * The default `BundledCityGeocoder` uses an offline dataset, but any
 * implementation can be injected (Dependency Inversion) — including one that
 * wraps an online service in an app that allows network access.
 */
interface Geocoder {
    /**
     * Returns all matches for [query], optionally filtered by [country]
     * (ISO-3166 alpha-2 or a known country name) and [state]. Ordered
     * best-match first.
     */
    fun search(query: String, country: String? = null, state: String? = null): List<City>

    /** The single best match for [query], or `null` if none. */
    fun resolve(query: String, country: String? = null, state: String? = null): City? =
        search(query, country, state).firstOrNull()
}
