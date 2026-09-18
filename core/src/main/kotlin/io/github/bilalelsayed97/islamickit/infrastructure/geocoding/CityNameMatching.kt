package io.github.bilalelsayed97.islamickit.infrastructure.geocoding

import io.github.bilalelsayed97.islamickit.infrastructure.geocoding.data.CityDataset
import io.github.bilalelsayed97.islamickit.internal.DartStrings
import io.github.bilalelsayed97.islamickit.internal.DartStrings.dartTrim

/**
 * The name-matching rules shared by every bundled geocoder (the curated
 * [BundledCityGeocoder] here and the SQLite geocoder in the `geocoding`
 * module): normalisation, query candidates, match scoring and country
 * resolution. Kept in core so both implementations rank identically.
 */
object CityNameMatching {
    /**
     * Dart `String.trim()`: strips the Unicode whitespace set Dart strips —
     * tabs included, which the bundled database's Arabic country names carry.
     * Exposed for the SQLite implementations in the `geocoding` module.
     */
    @JvmStatic
    fun trim(s: String): String = s.dartTrim()

    /**
     * Trims, lower-cases (locale-independent) and collapses runs of Unicode
     * whitespace to a single space.
     */
    @JvmStatic
    fun normalize(s: String): String = s.dartTrim().lowercase().replace(DartStrings.WHITESPACE, " ")

    /**
     * The normalised query followed by each distinct non-empty comma-separated
     * segment, so `"Trafalgar Square, London, UK"` also tries `"london"`.
     * Empty when the query is blank.
     */
    @JvmStatic
    fun candidates(query: String): List<String> {
        val full = normalize(query)
        if (full.isEmpty()) return emptyList()
        val parts = mutableListOf(full)
        for (seg in query.split(',')) {
            val n = normalize(seg)
            if (n.isNotEmpty() && !parts.contains(n)) parts.add(n)
        }
        return parts
    }

    /**
     * Best (lowest) match score of a normalised [name] against any candidate:
     * `0` exact, `1` prefix, `2` the candidate contains the name, `3` the name
     * contains the candidate; `null` when nothing matches.
     */
    @JvmStatic
    fun score(name: String, candidates: List<String>): Int? {
        var best: Int? = null
        for (c in candidates) {
            val s: Int? = when {
                name == c -> 0
                name.startsWith(c) -> 1
                c.contains(name) -> 2
                name.contains(c) -> 3
                else -> null
            }
            if (s != null && (best == null || s < best)) best = s
        }
        return best
    }

    /**
     * Resolves a caller-supplied country to an upper-case ISO-3166 alpha-2
     * code: two-character input is taken as a code as-is (so `"uk"` is **not**
     * aliased), longer input goes through the known country-name aliases and
     * otherwise is upper-cased unchanged. `null` stays `null`.
     */
    @JvmStatic
    fun resolveCountry(country: String?): String? {
        if (country == null) return null
        val normalized = normalize(country)
        if (normalized.length == 2) return normalized.uppercase()
        return CityDataset.countryAliases[normalized]?.uppercase() ?: normalized.uppercase()
    }

    /** Orders settlement feature codes by prominence, lowest first. */
    @JvmStatic
    fun placeRank(level: String?): Int = when (level) {
        "PPLC" -> 0 // national capital
        "PPLA" -> 1 // first-order administrative capital
        "PPLA2" -> 2
        "PPLA3" -> 3
        "PPLA4" -> 4
        "PPL" -> 5
        else -> 6
    }
}
