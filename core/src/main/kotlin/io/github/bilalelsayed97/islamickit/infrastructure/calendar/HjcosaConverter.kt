package io.github.bilalelsayed97.islamickit.infrastructure.calendar

import io.github.bilalelsayed97.islamickit.domain.enums.CalendarMethod
import io.github.bilalelsayed97.islamickit.domain.models.HijriDate
import io.github.bilalelsayed97.islamickit.domain.ports.HijriConverter
import io.github.bilalelsayed97.islamickit.infrastructure.calendar.data.HijriSightings
import io.github.bilalelsayed97.islamickit.infrastructure.calendar.data.UmmAlQuraTable
import io.github.bilalelsayed97.islamickit.internal.DartStrings
import io.github.bilalelsayed97.islamickit.time.CivilDate

/**
 * High Judiciary Council of Saudi Arabia: the Umm al-Qura table overlaid with
 * announced lunar-sighting adjustments (both directions). Faithful port of the
 * PHP `HighJudiciaryCouncilOfSaudiArabia` class.
 */
class HjcosaConverter : HijriConverter {
    private val uaq: TableHijriConverter = TableHijriConverter.ummAlQuraTable(CalendarMethod.HJCOSA)

    override val method: CalendarMethod
        get() = CalendarMethod.HJCOSA

    override fun fromGregorian(date: CivilDate, adjustment: Int): HijriDate {
        uaq.verifyGregorian(date)
        val key = "${DartStrings.two(date.day)}-${DartStrings.two(date.month)}-${date.year}"
        val announced = HijriSightings.gregorianToHijri[key] ?: return uaq.fromGregorian(date)

        val parts = announced.split('-')
        val d = parts[0].toInt()
        val m = parts[1].toInt()
        val y = parts[2].toInt()

        // Month length from a reference calc on the 7th of the announced month,
        // since sightings adjust the *start* of a month.
        val refJd = JulianDayMath.hijriToJd(y, m, 7)
        val ref = JulianDayMath.tableToHijri(UmmAlQuraTable.data, 16260, refJd)

        return HijriDateBuilder.build(
            day = d,
            month = m,
            year = y,
            monthLength = ref.monthLength,
            gregorianWeekday = date.isoDayOfWeek,
            method = CalendarMethod.HJCOSA,
        )
    }

    override fun toGregorian(year: Int, month: Int, day: Int, adjustment: Int): CivilDate {
        uaq.verifyHijri(year, month, day)
        val key = "${DartStrings.two(day)}-${DartStrings.two(month)}-$year"
        val gregorian = HijriSightings.hijriToGregorian[key]
        if (gregorian != null) {
            val p = gregorian.split('-')
            return CivilDate(p[2].toInt(), p[1].toInt(), p[0].toInt())
        }
        // No announcement for this date: the Umm al-Qura table, the same one
        // fromGregorian falls back to.
        return uaq.toGregorian(year, month, day, adjustment)
    }
}
