package io.github.bilalelsayed97.islamickit.infrastructure.calendar

import io.github.bilalelsayed97.islamickit.domain.enums.CalendarMethod
import io.github.bilalelsayed97.islamickit.domain.models.HijriDate
import io.github.bilalelsayed97.islamickit.domain.ports.HijriConverter
import io.github.bilalelsayed97.islamickit.infrastructure.calendar.data.DiyanetTable
import io.github.bilalelsayed97.islamickit.infrastructure.calendar.data.UmmAlQuraTable
import io.github.bilalelsayed97.islamickit.time.CivilDate

/**
 * A Hijri `(year, month, day)` triple used only for table validity bounds
 * (a bound such as 1500-12-30 need not be a real date of any month table).
 */
data class HijriYmd(val year: Int, val month: Int, val day: Int) {
    override fun toString(): String = dartRecord(year, month, day)
}

/** Dart record formatting, `(1356, 1, 1)`, as the Dart error messages print bounds. */
private fun dartRecord(year: Int, month: Int, day: Int): String = "($year, $month, $day)"

private fun encode(year: Int, month: Int, day: Int): Int = year * 10000 + month * 100 + day

/**
 * Table-driven converter (Umm al-Qura, Diyanet). `gToH` is a lunation-table
 * lookup; `hToG` uses the arithmetic Hijri->JD path, exactly as the PHP
 * (the table is not consulted and `adjustment` is applied).
 */
class TableHijriConverter internal constructor(
    override val method: CalendarMethod,
    internal val data: IntArray,
    internal val lunations: Int,
    val gregorianFrom: CivilDate,
    val gregorianTo: CivilDate,
    val hijriFrom: HijriYmd,
    val hijriTo: HijriYmd,
) : HijriConverter {
    /**
     * @throws IllegalArgumentException when [date] is outside the table's Gregorian range.
     */
    fun verifyGregorian(date: CivilDate) {
        val v = encode(date.year, date.month, date.day)
        if (v < encode(gregorianFrom.year, gregorianFrom.month, gregorianFrom.day) ||
            v > encode(gregorianTo.year, gregorianTo.month, gregorianTo.day)
        ) {
            throw IllegalArgumentException(
                "Gregorian date out of range for ${method.code} " +
                    "(${dartRecord(gregorianFrom.year, gregorianFrom.month, gregorianFrom.day)} .. " +
                    "${dartRecord(gregorianTo.year, gregorianTo.month, gregorianTo.day)}).",
            )
        }
    }

    /**
     * @throws IllegalArgumentException when the Hijri date is outside the table's range.
     */
    fun verifyHijri(year: Int, month: Int, day: Int) {
        val v = encode(year, month, day)
        if (v < encode(hijriFrom.year, hijriFrom.month, hijriFrom.day) ||
            v > encode(hijriTo.year, hijriTo.month, hijriTo.day)
        ) {
            throw IllegalArgumentException(
                "Hijri date out of range for ${method.code} ($hijriFrom .. $hijriTo).",
            )
        }
    }

    override fun fromGregorian(date: CivilDate, adjustment: Int): HijriDate {
        verifyGregorian(date)
        val jd = JulianDayMath.gregorianToJd(date.year, date.month, date.day)
        val h = JulianDayMath.tableToHijri(data, lunations, jd)
        return HijriDateBuilder.build(
            day = h.day,
            month = h.month,
            year = h.year,
            monthLength = h.monthLength,
            gregorianWeekday = date.isoDayOfWeek,
            method = method,
        )
    }

    override fun toGregorian(year: Int, month: Int, day: Int, adjustment: Int): CivilDate {
        verifyHijri(year, month, day)
        val jd = JulianDayMath.hijriToJd(year, month, day, adjust = adjustment)
        return JulianDayMath.jdToGregorian(jd)
    }

    companion object {
        /** Umm al-Qura configuration (valid 1356–1500 AH). */
        @JvmStatic
        fun ummAlQura(): TableHijriConverter = ummAlQuraTable(CalendarMethod.UAQ)

        /** Diyanet configuration (valid 1318–1449 AH). */
        @JvmStatic
        fun diyanet(): TableHijriConverter = TableHijriConverter(
            method = CalendarMethod.DIYANET,
            data = DiyanetTable.data,
            lunations = 15804,
            gregorianFrom = CivilDate(1900, 5, 1),
            gregorianTo = CivilDate(2028, 1, 26),
            hijriFrom = HijriYmd(1318, 1, 1),
            hijriTo = HijriYmd(1449, 8, 29),
        )

        /** The Umm al-Qura table reporting [method] (HJCoSA overlays it under its own code). */
        internal fun ummAlQuraTable(method: CalendarMethod): TableHijriConverter = TableHijriConverter(
            method = method,
            data = UmmAlQuraTable.data,
            lunations = 16260,
            gregorianFrom = CivilDate(1937, 3, 14),
            gregorianTo = CivilDate(2077, 11, 16),
            hijriFrom = HijriYmd(1356, 1, 1),
            hijriTo = HijriYmd(1500, 12, 30),
        )
    }
}
