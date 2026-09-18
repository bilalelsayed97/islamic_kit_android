package io.github.bilalelsayed97.islamickit.domain.valueobjects

import io.github.bilalelsayed97.islamickit.domain.enums.MidnightMode

/**
 * Strongly-typed twilight parameters for a calculation method.
 *
 * Isha and Maghrib can each be defined either as an angle **or** as a number
 * of minutes after the preceding event. [adjustments] carries the whole-minute
 * corrections the authority publishes on top of the astronomy.
 *
 * Being a `data class`, [copy] can also clear a nullable field (a superset of
 * the Dart `copyWith`, which only ever overrides).
 */
data class MethodParams @JvmOverloads constructor(
    /** Fajr twilight angle in degrees below the horizon. */
    val fajrAngle: Double = 0.0,
    /** Isha twilight angle in degrees, if defined by angle. */
    val ishaAngle: Double? = null,
    /** Minutes after Maghrib for Isha, if defined by a fixed interval. */
    val ishaMinutesAfterMaghrib: Int? = null,
    /**
     * Interval used instead of [ishaMinutesAfterMaghrib] during Ramadan.
     *
     * Umm al-Qura lengthens its 90-minute interval to 120 minutes for the
     * month; no other method varies by season.
     */
    val ramadanIshaMinutesAfterMaghrib: Int? = null,
    /** Maghrib angle in degrees, if defined by angle (rare; e.g. Jafari/Tehran). */
    val maghribAngle: Double? = null,
    /** Minutes after Sunset for Maghrib, if defined by a fixed interval. */
    val maghribMinutesAfterSunset: Int? = null,
    /** Whole-minute corrections the method applies to its own times. */
    val adjustments: MethodAdjustments = MethodAdjustments.NONE,
    /** Midnight mode implied by the method (e.g. Jafari for Shia methods). */
    val midnightMode: MidnightMode? = null,
    /** Reference location associated with the method (informational). */
    val location: Coordinates? = null,
) {
    /** Whether Isha is defined as a fixed number of minutes after Maghrib. */
    val ishaIsInterval: Boolean
        get() = ishaMinutesAfterMaghrib != null

    /** Whether Maghrib is defined as a fixed number of minutes after Sunset. */
    val maghribIsInterval: Boolean
        get() = maghribMinutesAfterSunset != null

    /** Whether the Isha interval changes during Ramadan. */
    val hasRamadanIshaInterval: Boolean
        get() = ramadanIshaMinutesAfterMaghrib != null

    /**
     * These params with the Ramadan Isha interval applied, when the method
     * defines one. Returns `this` unchanged otherwise.
     */
    fun forRamadan(): MethodParams {
        val ramadan = ramadanIshaMinutesAfterMaghrib ?: return this
        return copy(ishaMinutesAfterMaghrib = ramadan)
    }
}
