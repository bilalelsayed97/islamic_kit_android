package io.github.bilalelsayed97.islamickit.domain.models

import io.github.bilalelsayed97.islamickit.domain.enums.CalendarMethod
import io.github.bilalelsayed97.islamickit.internal.DartStrings

/** A Hijri (Islamic) calendar date with localized display fields and holidays. */
data class HijriDate @JvmOverloads constructor(
    val day: Int,
    /** Month number 1..12 (1 = Muharram). */
    val month: Int,
    val year: Int,
    /** English weekday name, e.g. `"Friday"`. */
    val weekdayEn: String,
    /** Arabic weekday name, e.g. `"الجمعة"`. */
    val weekdayAr: String,
    /** English (transliterated) month name, e.g. `"Rajab"`. */
    val monthEn: String,
    /** Arabic month name, e.g. `"رَجَب"`. */
    val monthAr: String,
    /** Number of days in this Hijri month (29 or 30). */
    val monthLength: Int,
    /** The calendar method used to compute this date. */
    val method: CalendarMethod,
    /** Islamic holidays/observances on this Hijri day (may be empty). */
    val holidays: List<String> = emptyList(),
) {
    /** `dd-mm-yyyy`, matching the aladhan `hijri.date` field. */
    val formatted: String
        get() = "${DartStrings.two(day)}-${DartStrings.two(month)}-${DartStrings.padYear(year)}"
}
