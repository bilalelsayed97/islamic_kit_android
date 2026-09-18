package io.github.bilalelsayed97.islamickit.domain.ports

import io.github.bilalelsayed97.islamickit.domain.enums.CalendarMethod
import io.github.bilalelsayed97.islamickit.domain.models.HijriDate
import io.github.bilalelsayed97.islamickit.time.CivilDate

/** Converts between Gregorian and Hijri dates for a specific [method]. */
interface HijriConverter {
    val method: CalendarMethod

    /**
     * Converts a Gregorian [date] to a fully-populated [HijriDate]
     * (localized names + holidays included).
     *
     * [adjustment] shifts the result by whole days (only honored by the
     * Mathematical method; ignored by table-based methods).
     *
     * @throws IllegalArgumentException when [date] is outside a table method's range.
     */
    fun fromGregorian(date: CivilDate, adjustment: Int = 0): HijriDate

    /**
     * Converts a Hijri date to the Gregorian [CivilDate].
     *
     * [adjustment] shifts the result by whole days (only honored by the
     * Mathematical method; ignored by table-based methods).
     *
     * @throws IllegalArgumentException when the Hijri date is outside a table method's range.
     */
    fun toGregorian(year: Int, month: Int, day: Int, adjustment: Int = 0): CivilDate
}
