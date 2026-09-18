package io.github.bilalelsayed97.islamickit.infrastructure.calendar

import io.github.bilalelsayed97.islamickit.domain.enums.CalendarMethod
import io.github.bilalelsayed97.islamickit.domain.models.HijriDate
import io.github.bilalelsayed97.islamickit.infrastructure.calendar.data.HijriHolidays
import io.github.bilalelsayed97.islamickit.infrastructure.localization.Localizer

/**
 * Assembles a fully-populated [HijriDate] from raw numeric parts, filling in
 * localized month/weekday names (from [Localizer]) and holidays. The weekday is
 * derived from the corresponding Gregorian date's ISO weekday.
 */
internal object HijriDateBuilder {
    fun build(
        day: Int,
        month: Int,
        year: Int,
        monthLength: Int,
        gregorianWeekday: Int,
        method: CalendarMethod,
    ): HijriDate {
        val m = Localizer.islamicMonths.getValue(month)
        val wd = Localizer.hijriWeekday(gregorianWeekday)
        val holidays = HijriHolidays.byMonth[month]?.get(day) ?: emptyList()
        return HijriDate(
            day = day,
            month = month,
            year = year,
            weekdayEn = wd.en,
            weekdayAr = wd.ar,
            monthEn = m.en,
            monthAr = m.ar,
            monthLength = monthLength,
            method = method,
            holidays = holidays,
        )
    }
}
