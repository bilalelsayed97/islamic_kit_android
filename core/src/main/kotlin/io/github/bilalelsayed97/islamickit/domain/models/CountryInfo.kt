package io.github.bilalelsayed97.islamickit.domain.models

/**
 * A country row from the bundled city database.
 *
 * Country names ship in both English and Arabic, so a caller can render the
 * active locale without a second lookup. Equality is by [id].
 */
class CountryInfo @JvmOverloads constructor(
    /** Primary key in the bundled database (`prayer_times_country_lookups`). */
    val id: Int,
    /** English country name, e.g. `"Egypt"`. */
    val nameEn: String,
    /** Arabic country name, e.g. `"مصر"`. */
    val nameAr: String,
    /** ISO 3166-1 alpha-2 code, e.g. `"EG"`. Empty when the id is unmapped. */
    val isoCode: String,
    /**
     * Raw `calc_method` value the database recommends for this country.
     *
     * This is the database's own numbering, **not** an aladhan method id — the
     * two collide above 5. Resolve it with `BundledMethodMap` (or the
     * `calculationMethod` extension property) rather than interpreting it.
     */
    val calculationMethodId: Int? = null,
) {
    override fun equals(other: Any?): Boolean = other is CountryInfo && other.id == id

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = "CountryInfo($nameEn, $isoCode)"
}
