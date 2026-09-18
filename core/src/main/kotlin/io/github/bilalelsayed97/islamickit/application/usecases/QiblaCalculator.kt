package io.github.bilalelsayed97.islamickit.application.usecases

import io.github.bilalelsayed97.islamickit.domain.models.QiblaDirection
import io.github.bilalelsayed97.islamickit.domain.valueobjects.Coordinates
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/** Computes the Qibla direction (great-circle bearing to the Ka'aba). */
class QiblaCalculator {
    /** The bearing from [from] to the Ka'aba, clockwise from true north in `[0, 360)`. */
    fun direction(from: Coordinates): QiblaDirection {
        val a = dtr(KAABA.longitude - from.longitude)
        val b = dtr(90 - from.latitude)
        val c = dtr(90 - KAABA.latitude)
        var degrees = rtd(
            atan2(
                sin(a),
                sin(b) * cot(c) - cos(b) * cos(a),
            ),
        )
        if (degrees < 0) degrees += 360
        return QiblaDirection(degrees = degrees, from = from)
    }

    companion object {
        /** Geographic coordinates of the Ka'aba, in degrees. */
        @JvmField
        val KAABA: Coordinates = Coordinates(21.422517, 39.826166)

        private fun dtr(d: Double): Double = d * PI / 180.0
        private fun rtd(r: Double): Double = r * 180.0 / PI
        private fun cot(x: Double): Double = tan(PI / 2 - x)
    }
}
