package io.github.bilalelsayed97.islamickit.domain.models

import io.github.bilalelsayed97.islamickit.domain.valueobjects.Coordinates
import io.github.bilalelsayed97.islamickit.time.UtcOffset

/**
 * A city row from the bundled city database, with its country resolved.
 *
 * This is the richer sibling of [City]: it keeps the database identifiers and
 * both localized name pairs, which a city-picker UI needs and a geocoding
 * result does not. Equality is by [id].
 */
class CityEntry @JvmOverloads constructor(
    /** Primary key in the bundled database (`prayer_times_city_lookups`). */
    val id: Int,
    /** English city name. */
    val nameEn: String,
    /** Arabic city name. */
    val nameAr: String,
    /** Owning country's primary key. */
    val countryId: Int,
    /** English country name. */
    val countryNameEn: String,
    /** Arabic country name. */
    val countryNameAr: String,
    /** ISO 3166-1 alpha-2 code of the owning country. Empty when unmapped. */
    val isoCode: String,
    val coordinates: Coordinates,
    /** Standard-time UTC offset. Does **not** account for daylight saving. */
    val utcOffset: UtcOffset,
    /** IANA timezone identifier, e.g. `"Africa/Cairo"`. `null` when unknown. */
    val timeZoneId: String? = null,
    /**
     * Raw `calc_method` value the database recommends for this city's country.
     *
     * The database's own numbering, **not** an aladhan method id. Resolve it
     * with `BundledMethodMap` (or the `calculationMethod` extension property).
     */
    val calculationMethodId: Int? = null,
) {
    override fun equals(other: Any?): Boolean = other is CityEntry && other.id == id

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = "CityEntry($nameEn, $isoCode)"
}
