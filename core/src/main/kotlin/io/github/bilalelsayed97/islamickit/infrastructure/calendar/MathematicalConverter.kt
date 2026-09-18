package io.github.bilalelsayed97.islamickit.infrastructure.calendar

import io.github.bilalelsayed97.islamickit.domain.enums.CalendarMethod
import io.github.bilalelsayed97.islamickit.domain.models.HijriDate
import io.github.bilalelsayed97.islamickit.domain.ports.HijriConverter
import io.github.bilalelsayed97.islamickit.time.CivilDate

/**
 * Pure arithmetic (tabular) Hijri calendar. No validity restrictions; supports
 * a whole-day `adjustment` in both directions. Faithful port of the PHP
 * `Mathematical\Calculator`. Month length is always reported as 30 (the
 * algorithm does not track true month lengths).
 */
class MathematicalConverter : HijriConverter {
    override val method: CalendarMethod
        get() = CalendarMethod.MATHEMATICAL

    override fun fromGregorian(date: CivilDate, adjustment: Int): HijriDate {
        val jd = JulianDayMath.gregorianToJd(date.year, date.month, date.day)
        val h = JulianDayMath.mathematicalToHijri(jd, adjustment)
        return HijriDateBuilder.build(
            day = h.day,
            month = h.month,
            year = h.year,
            monthLength = 30,
            gregorianWeekday = date.isoDayOfWeek,
            method = CalendarMethod.MATHEMATICAL,
        )
    }

    override fun toGregorian(year: Int, month: Int, day: Int, adjustment: Int): CivilDate {
        val jd = JulianDayMath.hijriToJd(year, month, day, adjust = adjustment)
        return JulianDayMath.jdToGregorian(jd)
    }
}
