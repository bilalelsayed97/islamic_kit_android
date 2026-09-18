package io.github.bilalelsayed97.islamickit.domain.models

import io.github.bilalelsayed97.islamickit.internal.DartStrings

/** A Gregorian calendar date with localized display fields. */
data class GregorianDate(
    val day: Int,
    val month: Int,
    val year: Int,
    /** English weekday name, e.g. `"Wednesday"`. */
    val weekdayEn: String,
    /** English month name, e.g. `"January"`. */
    val monthEn: String,
) {
    /** `dd-mm-yyyy`, matching the aladhan `gregorian.date` field. */
    val formatted: String
        get() = "${DartStrings.two(day)}-${DartStrings.two(month)}-${DartStrings.padYear(year)}"
}
