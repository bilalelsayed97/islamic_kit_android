package io.github.bilalelsayed97.islamickit.domain.models

import io.github.bilalelsayed97.islamickit.domain.valueobjects.Coordinates
import io.github.bilalelsayed97.islamickit.time.UtcOffset

/** A geocoded city from the bundled dataset (or a custom `Geocoder`). */
data class City @JvmOverloads constructor(
    val name: String,
    /** ISO 3166-1 alpha-2 country code, e.g. `"GB"`. */
    val country: String,
    val coordinates: Coordinates,
    /**
     * Standard-time UTC offset for the city (does **not** account for DST;
     * override per-date if you need DST-correct results).
     */
    val utcOffset: UtcOffset,
    /** Arabic city name, if known (from the bundled database). */
    val nameAr: String? = null,
    /** Administrative region / state, if known. */
    val state: String? = null,
) {
    override fun toString(): String = "City($name${if (state != null) ", $state" else ""}, $country)"
}
