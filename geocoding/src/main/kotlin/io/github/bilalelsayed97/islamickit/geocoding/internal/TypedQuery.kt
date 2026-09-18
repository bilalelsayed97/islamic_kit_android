package io.github.bilalelsayed97.islamickit.geocoding.internal

import android.database.Cursor
import android.database.sqlite.SQLiteCursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteQuery
import io.github.bilalelsayed97.islamickit.infrastructure.geocoding.CityNameMatching
import io.github.bilalelsayed97.islamickit.infrastructure.geocoding.data.CountryIsoMap
import kotlin.math.floor

/**
 * Runs [sql] with [args] bound by their Kotlin type — `Int`/`Long` as INTEGER,
 * `Double` as REAL, `String` as TEXT, `null` as NULL — the way Dart's `sqlite3`
 * binds them.
 *
 * The plain `rawQuery(sql, String[])` binds every argument as TEXT, which
 * makes `LIMIT ?`, `BETWEEN ?` and the distance arithmetic in `ORDER BY`
 * compare as strings; going through a [SQLiteDatabase.CursorFactory] is the
 * public way to reach the typed `bind*` calls on the underlying [SQLiteQuery].
 */
internal fun SQLiteDatabase.typedQuery(sql: String, args: List<Any?>): Cursor =
    rawQueryWithFactory(
        { _, driver, editTable, query ->
            args.forEachIndexed { i, arg -> bind(query, i + 1, arg) }
            SQLiteCursor(driver, editTable, query)
        },
        sql,
        null,
        // `editTable` is non-null in the SDK stubs; SQLiteCursor only stores it.
        "",
    )

private fun bind(query: SQLiteQuery, index: Int, arg: Any?) {
    when (arg) {
        null -> query.bindNull(index)
        is Int -> query.bindLong(index, arg.toLong())
        is Long -> query.bindLong(index, arg)
        is Double -> query.bindDouble(index, arg)
        is String -> query.bindString(index, arg)
        else -> throw IllegalArgumentException("Unsupported bind type ${arg::class.java.name} at index $index")
    }
}

/** The text at [column], or `null` when the cell is NULL. */
internal fun Cursor.stringOrNull(column: String): String? {
    val i = getColumnIndexOrThrow(column)
    return if (isNull(i)) null else getString(i)
}

/** The integer at [column], or `null` when the cell is NULL. */
internal fun Cursor.intOrNull(column: String): Int? {
    val i = getColumnIndexOrThrow(column)
    return if (isNull(i)) null else getInt(i)
}

/** The number at [column] as a double, or `null` when the cell is NULL. */
internal fun Cursor.doubleOrNull(column: String): Double? {
    val i = getColumnIndexOrThrow(column)
    return if (isNull(i)) null else getDouble(i)
}

/**
 * Row-value rules shared by the SQLite geocoder and directory, matching the
 * Dart implementations byte for byte.
 */
internal object RowValues {
    /** Dart `String.trim()` (strips the trailing tabs on the Arabic country names). */
    fun trim(value: String?): String? = value?.let(CityNameMatching::trim)

    /** Dart `(row[...] as String?)?.trim() ?? ''`. */
    fun text(value: String?): String = trim(value) ?: ""

    /** Dart `_trimToNull`: trimmed text, or `null` when absent or empty. */
    fun trimToNull(value: String?): String? = trim(value)?.takeIf { it.isNotEmpty() }

    /** The country's ISO 3166-1 alpha-2 code for a database id, or `""` when unmapped. */
    fun isoCode(countryId: Int?): String = CountryIsoMap.idToIso[countryId] ?: ""

    /**
     * Standard-time offset in minutes.
     *
     * `city_time_zone` holds whole hours truncated toward zero, so zones with a
     * half- or quarter-hour offset are resolved from their IANA id first and
     * only fall back to the hour column when the id is missing or unremarkable.
     * The fallback rounds like Dart's `round()` (ties away from zero).
     */
    fun offsetMinutes(timeZoneId: String?, hours: Double?): Int {
        if (!timeZoneId.isNullOrEmpty()) {
            val minutes = CountryIsoMap.fractionalZoneOffsetMinutes[timeZoneId]
            if (minutes != null) return minutes
        }
        return dartRound((hours ?: 0.0) * 60)
    }

    private fun dartRound(x: Double): Int =
        if (x < 0) -floor(-x + 0.5).toInt() else floor(x + 0.5).toInt()
}
