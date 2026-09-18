package io.github.bilalelsayed97.islamickit.geocoding

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import io.github.bilalelsayed97.islamickit.domain.models.CityEntry
import io.github.bilalelsayed97.islamickit.domain.models.CountryInfo
import io.github.bilalelsayed97.islamickit.domain.models.TimeZoneInfo
import io.github.bilalelsayed97.islamickit.domain.ports.CityDirectory
import io.github.bilalelsayed97.islamickit.domain.valueobjects.Coordinates
import io.github.bilalelsayed97.islamickit.geocoding.internal.RowValues
import io.github.bilalelsayed97.islamickit.geocoding.internal.doubleOrNull
import io.github.bilalelsayed97.islamickit.geocoding.internal.intOrNull
import io.github.bilalelsayed97.islamickit.geocoding.internal.stringOrNull
import io.github.bilalelsayed97.islamickit.geocoding.internal.typedQuery
import io.github.bilalelsayed97.islamickit.infrastructure.geocoding.CityNameMatching
import io.github.bilalelsayed97.islamickit.time.UtcOffset
import java.io.Closeable

/**
 * [CityDirectory] backed by the bundled `prayer_times.db`.
 *
 * Open it with [BundledCityDatabase.openDirectory] (or wrap an
 * [SQLiteDatabase] you opened yourself), keep it for the process lifetime and
 * query it off the main thread. Queries run on the calling thread.
 */
class SqliteCityDirectory(private val db: SQLiteDatabase) : CityDirectory, Closeable {
    override fun countries(query: String?): List<CountryInfo> {
        val trimmed = RowValues.text(query)
        val filtered = trimmed.isNotEmpty()
        val out = ArrayList<CountryInfo>()
        db.typedQuery(
            "SELECT country_id, country_name_en, country_name_ar, calc_method " +
                "FROM prayer_times_country_lookups " +
                (if (filtered) "WHERE (country_name_en LIKE ? OR country_name_ar LIKE ?) " else "") +
                "ORDER BY country_name_en COLLATE NOCASE",
            if (filtered) listOf("%$trimmed%", "%$trimmed%") else emptyList(),
        ).use { cursor ->
            while (cursor.moveToNext()) out.add(toCountry(cursor))
        }
        return out
    }

    override fun country(countryId: Int): CountryInfo? =
        db.typedQuery(
            "SELECT country_id, country_name_en, country_name_ar, calc_method " +
                "FROM prayer_times_country_lookups WHERE country_id = ? LIMIT 1",
            listOf(countryId),
        ).use { cursor -> if (cursor.moveToFirst()) toCountry(cursor) else null }

    /**
     * Populated places inside [countryId], most prominent first.
     *
     * Prominence ordering matters here: a country's rows run to five figures and
     * are dominated by neighbourhoods, so an alphabetical list buries the
     * capital. [query] filters on either name; [limit] and [offset] page.
     */
    override fun citiesInCountry(countryId: Int, query: String?, limit: Int, offset: Int): List<CityEntry> =
        cities(countryId, query, limit, offset)

    override fun searchCities(query: String?, limit: Int, offset: Int): List<CityEntry> =
        cities(countryId = null, query = query, limit = limit, offset = offset)

    /**
     * The city [latitude]/[longitude] most plausibly names.
     *
     * Not simply the closest row: the database records neighbourhoods alongside
     * the cities that contain them, so a plain proximity sort answers "Az
     * Zamalek" where a person would answer "Cairo". Distance is therefore
     * weighted by settlement prominence, letting a nearby capital or
     * administrative seat outrank a marginally closer suburb, and the search
     * widens only if the initial box is empty.
     */
    override fun nearestCity(latitude: Double, longitude: Double): CityEntry? {
        for (delta in NEAREST_DELTAS) {
            val match = nearestWithin(latitude, longitude, delta)
            if (match != null) return match
        }
        return nearestWithin(latitude, longitude, null)
    }

    /**
     * The timezones [countryId] actually spans, ordered by IANA id.
     *
     * Border towns carry a neighbour's zone, which would otherwise present Egypt
     * as a four-zone country. A zone is included only when it covers at least
     * 0.5% of the country's populated places, which keeps every genuine zone of
     * even the most fragmented countries while dropping those strays.
     */
    override fun timeZonesForCountry(countryId: Int): List<TimeZoneInfo> {
        val out = ArrayList<TimeZoneInfo>()
        db.typedQuery(
            "WITH zone_counts AS (" +
                "  SELECT TRIM(time_zone_id) AS zone, COUNT(*) AS n" +
                "    FROM prayer_times_city_lookups" +
                "   WHERE country_id = ? AND city_level LIKE 'PPL%'" +
                "     AND TRIM(COALESCE(time_zone_id, '')) <> ''" +
                "   GROUP BY zone) " +
                "SELECT z.zone, t.zone_name_ar " +
                "  FROM zone_counts z " +
                "  LEFT JOIN prayer_times_time_zone_lookups t ON t.time_zone_id = z.zone " +
                " WHERE z.n * 200 >= (SELECT SUM(n) FROM zone_counts) " +
                " ORDER BY z.zone COLLATE NOCASE",
            listOf(countryId),
        ).use { cursor ->
            while (cursor.moveToNext()) {
                out.add(
                    TimeZoneInfo(
                        ianaId = cursor.stringOrNull("zone") ?: "",
                        countryId = countryId,
                        nameAr = RowValues.trimToNull(cursor.stringOrNull("zone_name_ar")),
                    ),
                )
            }
        }
        return out
    }

    /** Closes the underlying database. */
    override fun close() = db.close()

    // --- internals -----------------------------------------------------------

    /** Prominence-ordered page of places, scoped to [countryId] when given. */
    private fun cities(countryId: Int?, query: String?, limit: Int, offset: Int): List<CityEntry> {
        val trimmed = RowValues.text(query)
        val clauses = mutableListOf("c.city_level LIKE 'PPL%'")
        val bindings = ArrayList<Any?>()
        if (countryId != null) {
            clauses.add("c.country_id = ?")
            bindings.add(countryId)
        }
        if (trimmed.isNotEmpty()) {
            clauses.add("(c.city_name_en LIKE ? OR c.city_name_ar LIKE ?)")
            bindings.add("%$trimmed%")
            bindings.add("%$trimmed%")
        }
        bindings.add(limit)
        bindings.add(offset)

        val out = ArrayList<CityEntry>()
        db.typedQuery(
            "$CITY_SELECT WHERE ${clauses.joinToString(" AND ")} " +
                "ORDER BY $PROMINENCE, c.city_name_en COLLATE NOCASE " +
                "LIMIT ? OFFSET ?",
            bindings,
        ).use { cursor ->
            while (cursor.moveToNext()) out.add(toCity(cursor))
        }
        return out
    }

    private fun nearestWithin(latitude: Double, longitude: Double, delta: Double?): CityEntry? {
        val clauses = mutableListOf("c.city_level LIKE 'PPL%'")
        val bindings = ArrayList<Any?>()
        if (delta != null) {
            clauses.add("c.city_latitude BETWEEN ? AND ?")
            clauses.add("c.city_longitude BETWEEN ? AND ?")
            bindings.add(latitude - delta)
            bindings.add(latitude + delta)
            bindings.add(longitude - delta)
            bindings.add(longitude + delta)
        }
        bindings.add(latitude)
        bindings.add(latitude)
        bindings.add(longitude)
        bindings.add(longitude)

        return db.typedQuery(
            "$CITY_SELECT WHERE ${clauses.joinToString(" AND ")} " +
                "ORDER BY (((c.city_latitude - ?) * (c.city_latitude - ?)) " +
                "        + ((c.city_longitude - ?) * (c.city_longitude - ?))) " +
                "        * $PROMINENCE_WEIGHT " +
                "LIMIT 1",
            bindings,
        ).use { cursor -> if (cursor.moveToFirst()) toCity(cursor) else null }
    }

    private fun toCountry(cursor: Cursor): CountryInfo {
        val id = cursor.intOrNull("country_id") ?: 0
        return CountryInfo(
            id = id,
            nameEn = RowValues.text(cursor.stringOrNull("country_name_en")),
            nameAr = RowValues.text(cursor.stringOrNull("country_name_ar")),
            isoCode = RowValues.isoCode(id),
            calculationMethodId = cursor.intOrNull("calc_method"),
        )
    }

    private fun toCity(cursor: Cursor): CityEntry {
        val countryId = cursor.intOrNull("country_id") ?: 0
        val timeZoneId = RowValues.trimToNull(cursor.stringOrNull("time_zone_id"))
        return CityEntry(
            id = cursor.intOrNull("city_id") ?: 0,
            nameEn = RowValues.text(cursor.stringOrNull("city_name_en")),
            nameAr = RowValues.text(cursor.stringOrNull("city_name_ar")),
            countryId = countryId,
            countryNameEn = RowValues.text(cursor.stringOrNull("country_name_en")),
            countryNameAr = RowValues.text(cursor.stringOrNull("country_name_ar")),
            isoCode = RowValues.isoCode(countryId),
            coordinates = Coordinates(
                cursor.doubleOrNull("city_latitude") ?: 0.0,
                cursor.doubleOrNull("city_longitude") ?: 0.0,
            ),
            timeZoneId = timeZoneId,
            utcOffset = UtcOffset.ofMinutes(RowValues.offsetMinutes(timeZoneId, cursor.doubleOrNull("city_time_zone"))),
            calculationMethodId = cursor.intOrNull("calc_method"),
        )
    }

    private companion object {
        /** Search boxes (degrees) tried in order before the unbounded pass. */
        val NEAREST_DELTAS: List<Double> = listOf(0.5, 2.0)

        const val CITY_SELECT: String =
            "SELECT c.city_id, c.city_name_en, c.city_name_ar, c.country_id, " +
                "c.city_latitude, c.city_longitude, c.city_time_zone, c.time_zone_id, " +
                "c.city_level, co.country_name_en, co.country_name_ar, co.calc_method " +
                "FROM prayer_times_city_lookups c " +
                "LEFT JOIN prayer_times_country_lookups co " +
                "  ON co.country_id = c.country_id"

        /** Settlement prominence as a sort key, most prominent first. */
        const val PROMINENCE: String = "CASE c.city_level " +
            "WHEN 'PPLC' THEN 0 WHEN 'PPLA' THEN 1 WHEN 'PPLA2' THEN 2 " +
            "WHEN 'PPLA3' THEN 3 WHEN 'PPLA4' THEN 4 WHEN 'PPL' THEN 5 ELSE 6 END"

        /** Distance multiplier that lets a prominent place outrank a closer suburb. */
        const val PROMINENCE_WEIGHT: String = "CASE c.city_level " +
            "WHEN 'PPLC' THEN 1.0 WHEN 'PPLA' THEN 1.0 WHEN 'PPLA2' THEN 1.5 " +
            "WHEN 'PPLA3' THEN 2.0 WHEN 'PPLA4' THEN 2.5 ELSE 3.0 END"
    }
}
