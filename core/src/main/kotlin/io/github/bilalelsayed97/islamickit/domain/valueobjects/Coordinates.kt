package io.github.bilalelsayed97.islamickit.domain.valueobjects

import io.github.bilalelsayed97.islamickit.internal.DartMath

/** An immutable geographic coordinate in decimal degrees. */
data class Coordinates(
    /** Latitude in decimal degrees, positive north. */
    val latitude: Double,
    /** Longitude in decimal degrees, positive east. */
    val longitude: Double,
) {
    override fun toString(): String =
        "Coordinates(${DartMath.doubleToString(latitude)}, ${DartMath.doubleToString(longitude)})"
}
