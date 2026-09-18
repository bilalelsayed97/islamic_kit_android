package io.github.bilalelsayed97.islamickit.domain.valueobjects

/**
 * Whole-minute corrections a calculation method applies to its own output.
 *
 * These are part of the *method's definition* — several authorities publish
 * times that sit a minute or two off the pure astronomical value (most add a
 * minute to Dhuhr so the printed time is safely past the zenith). They are
 * applied on top of the astronomy and before rounding, and are independent of
 * the user's own `tune` offsets, which apply afterwards.
 */
data class MethodAdjustments @JvmOverloads constructor(
    val fajr: Int = 0,
    val sunrise: Int = 0,
    val dhuhr: Int = 0,
    val asr: Int = 0,
    val maghrib: Int = 0,
    val isha: Int = 0,
) {
    /** Whether every correction is zero. */
    val isEmpty: Boolean
        get() = fajr == 0 && sunrise == 0 && dhuhr == 0 && asr == 0 && maghrib == 0 && isha == 0

    override fun toString(): String =
        "MethodAdjustments(fajr: $fajr, sunrise: $sunrise, dhuhr: $dhuhr, asr: $asr, maghrib: $maghrib, isha: $isha)"

    companion object {
        /** No corrections — the astronomical values stand as computed. */
        @JvmField
        val NONE: MethodAdjustments = MethodAdjustments()
    }
}
