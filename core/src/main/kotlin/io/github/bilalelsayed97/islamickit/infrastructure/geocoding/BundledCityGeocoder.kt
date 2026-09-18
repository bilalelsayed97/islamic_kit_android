package io.github.bilalelsayed97.islamickit.infrastructure.geocoding

import io.github.bilalelsayed97.islamickit.domain.models.City
import io.github.bilalelsayed97.islamickit.domain.ports.Geocoder
import io.github.bilalelsayed97.islamickit.domain.valueobjects.Coordinates
import io.github.bilalelsayed97.islamickit.infrastructure.geocoding.data.CityDataset
import io.github.bilalelsayed97.islamickit.infrastructure.geocoding.data.CityRecord
import io.github.bilalelsayed97.islamickit.time.UtcOffset

/**
 * Offline [Geocoder] backed by the curated [CityDataset].
 *
 * Matching is name-based: exact, prefix, then substring, best-first. For
 * free-text addresses it also tries each comma-separated segment, so
 * `"Trafalgar Square, London, UK"` still resolves to London. Ties on score
 * keep dataset order (a stable sort; Dart's sort leaves tie order unspecified).
 */
class BundledCityGeocoder @JvmOverloads constructor(
    private val cities: List<CityRecord> = CityDataset.records,
) : Geocoder {
    override fun search(query: String, country: String?, state: String?): List<City> {
        val candidates = CityNameMatching.candidates(query)
        if (candidates.isEmpty()) return emptyList()

        val countryCode = CityNameMatching.resolveCountry(country)
        val normState = if (state == null) null else CityNameMatching.normalize(state)

        val scored = ArrayList<Pair<City, Int>>()
        for (record in cities) {
            if (countryCode != null && record.country.uppercase() != countryCode) continue
            if (normState != null &&
                (record.state == null || CityNameMatching.normalize(record.state) != normState)
            ) {
                continue
            }
            val score = CityNameMatching.score(CityNameMatching.normalize(record.name), candidates)
            if (score != null) scored.add(toCity(record) to score)
        }

        return scored.sortedBy { it.second }.map { it.first }
    }

    private fun toCity(r: CityRecord): City = City(
        name = r.name,
        country = r.country,
        state = r.state,
        coordinates = Coordinates(r.lat, r.lng),
        utcOffset = UtcOffset.ofMinutes(r.offsetMinutes),
    )
}
