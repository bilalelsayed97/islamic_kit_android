package io.github.bilalelsayed97.islamickit.geocoding

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import io.github.bilalelsayed97.islamickit.domain.models.City
import io.github.bilalelsayed97.islamickit.domain.ports.Geocoder
import io.github.bilalelsayed97.islamickit.domain.valueobjects.Coordinates
import io.github.bilalelsayed97.islamickit.geocoding.internal.RowValues
import io.github.bilalelsayed97.islamickit.geocoding.internal.doubleOrNull
import io.github.bilalelsayed97.islamickit.geocoding.internal.intOrNull
import io.github.bilalelsayed97.islamickit.geocoding.internal.stringOrNull
import io.github.bilalelsayed97.islamickit.geocoding.internal.typedQuery
import io.github.bilalelsayed97.islamickit.infrastructure.geocoding.CityNameMatching
import io.github.bilalelsayed97.islamickit.infrastructure.geocoding.data.CountryIsoMap
import io.github.bilalelsayed97.islamickit.time.UtcOffset
import java.io.Closeable

/**
 * [Geocoder] backed by the bundled `prayer_times.db` (≈131k populated places
 * with English + Arabic names).
 *
 * Open it with [BundledCityDatabase.openGeocoder] (or wrap an
 * [SQLiteDatabase] you opened yourself), keep it for the process lifetime and
 * query it off the main thread. Queries run on the calling thread.
 */
class SqliteCityGeocoder(private val db: SQLiteDatabase) : Geocoder, Closeable {
    private class Scored(val city: City, val score: Int, val rank: Int, val index: Int)

    /**
     * Returns matches for [query], best first: by name-match score, then by
     * settlement prominence (a capital wins over a same-named village), then
     * by database order for a deterministic result.
     *
     * [state] is accepted for interface compatibility but ignored: the bundled
     * database carries no administrative-region column, so [City.state] is
     * always `null` here.
     */
    override fun search(query: String, country: String?, state: String?): List<City> {
        val candidates = CityNameMatching.candidates(query)
        if (candidates.isEmpty()) return emptyList()

        val iso = CityNameMatching.resolveCountry(country)
        val where = StringBuilder(
            "city_level LIKE 'PPL%' AND (city_name_en LIKE ? OR city_name_ar LIKE ?)",
        )
        val args = mutableListOf<Any?>("%${candidates.first()}%", "%${candidates.first()}%")
        if (iso != null) {
            // An unknown country code can match nothing, mirroring the behaviour of
            // filtering on a code that is absent from the database.
            val countryId = CountryIsoMap.isoToId[iso] ?: return emptyList()
            where.append(" AND country_id = ?")
            args.add(countryId)
        }

        val scored = ArrayList<Scored>()
        db.typedQuery(
            "SELECT city_name_en, city_name_ar, city_latitude, city_longitude, " +
                "city_time_zone, time_zone_id, country_id, city_level " +
                "FROM prayer_times_city_lookups WHERE $where LIMIT 200",
            args,
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val en = RowValues.text(cursor.stringOrNull("city_name_en"))
                val ar = RowValues.trim(cursor.stringOrNull("city_name_ar"))
                val score = CityNameMatching.score(CityNameMatching.normalize(en), candidates)
                    ?: (if (ar != null) CityNameMatching.score(CityNameMatching.normalize(ar), candidates) else null)
                    ?: continue
                scored.add(
                    Scored(
                        city = toCity(cursor, en, ar),
                        score = score,
                        rank = CityNameMatching.placeRank(cursor.stringOrNull("city_level")),
                        index = scored.size,
                    ),
                )
            }
        }
        return scored
            .sortedWith(compareBy({ it.score }, { it.rank }, { it.index }))
            .map { it.city }
    }

    /** Closes the underlying database. */
    override fun close() = db.close()

    private fun toCity(cursor: Cursor, en: String, ar: String?): City = City(
        name = en,
        nameAr = if (ar.isNullOrEmpty()) null else ar,
        country = RowValues.isoCode(cursor.intOrNull("country_id")),
        state = null,
        coordinates = Coordinates(
            cursor.doubleOrNull("city_latitude") ?: 0.0,
            cursor.doubleOrNull("city_longitude") ?: 0.0,
        ),
        utcOffset = UtcOffset.ofMinutes(
            RowValues.offsetMinutes(cursor.stringOrNull("time_zone_id"), cursor.doubleOrNull("city_time_zone")),
        ),
    )
}
