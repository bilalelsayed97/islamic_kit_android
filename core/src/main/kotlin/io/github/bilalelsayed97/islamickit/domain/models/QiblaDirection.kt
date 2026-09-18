package io.github.bilalelsayed97.islamickit.domain.models

import io.github.bilalelsayed97.islamickit.domain.valueobjects.Coordinates
import io.github.bilalelsayed97.islamickit.internal.DartMath
import java.util.Locale

/** The direction of the Qibla from an observer's location. */
data class QiblaDirection(
    /** Bearing to the Ka'aba measured clockwise from true north, in `[0, 360)`. */
    val degrees: Double,
    /** The observer's location. */
    val from: Coordinates,
) {
    override fun toString(): String =
        "QiblaDirection(${String.format(Locale.ROOT, "%.2f", degrees)}° from " +
            "${DartMath.doubleToString(from.latitude)}, ${DartMath.doubleToString(from.longitude)})"
}
