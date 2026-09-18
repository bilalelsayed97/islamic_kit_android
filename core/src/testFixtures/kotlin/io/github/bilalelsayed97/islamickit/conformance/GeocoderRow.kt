package io.github.bilalelsayed97.islamickit.conformance

import io.github.bilalelsayed97.islamickit.domain.models.City
import kotlinx.serialization.json.JsonObject

/** The generator's `_cityJson` projection of a [City], comparable across ports. */
data class GeocoderRow(
    val name: String,
    val nameAr: String?,
    val country: String,
    val state: String?,
    val lat: Double,
    val lng: Double,
    val offsetMinutes: Int,
) {
    companion object {
        fun fromJson(j: JsonObject): GeocoderRow = GeocoderRow(
            name = j.str("name"),
            nameAr = j.strOrNull("nameAr"),
            country = j.str("country"),
            state = j.strOrNull("state"),
            lat = j.double("lat"),
            lng = j.double("lng"),
            offsetMinutes = j.int("offsetMinutes"),
        )

        fun fromCity(c: City): GeocoderRow = GeocoderRow(
            name = c.name,
            nameAr = c.nameAr,
            country = c.country,
            state = c.state,
            lat = c.coordinates.latitude,
            lng = c.coordinates.longitude,
            offsetMinutes = c.utcOffset.totalMinutes,
        )
    }
}
