package io.github.bilalelsayed97.islamickit.time

import io.github.bilalelsayed97.islamickit.internal.DartStrings
import java.util.Calendar
import java.util.TimeZone

/**
 * A proleptic-Gregorian calendar date with no time of day and no zone —
 * the only notion of "date" the engine works with.
 *
 * The primary constructor is strict ([month] `1..12`, [day] within the
 * month); [normalized] reproduces Dart's `DateTime(y, m, d)` overflow rules
 * (`DateTime(2014, 2, 30)` is 2014-03-02, day `0` is the last day of the
 * previous month) for the calendar arithmetic the facade needs.
 */
data class CivilDate(
    val year: Int,
    val month: Int,
    val day: Int,
) : Comparable<CivilDate> {
    init {
        require(month in 1..12) { "month must be in 1..12: $month" }
        require(day in 1..daysInMonth(year, month)) { "day $day is out of range for $year-$month" }
    }

    /** Chronological Julian Day Number of this date (JDN 2451545 = 2000-01-01). */
    val julianDayNumber: Int
        get() {
            val a = Math.floorDiv(14 - month, 12)
            val y = year + 4800 - a
            val m = month + 12 * a - 3
            return day + Math.floorDiv(153 * m + 2, 5) + 365 * y + Math.floorDiv(y, 4) -
                Math.floorDiv(y, 100) + Math.floorDiv(y, 400) - 32045
        }

    /** Days since 1970-01-01 (negative before the epoch). */
    val epochDay: Int
        get() = julianDayNumber - EPOCH_JDN

    /** ISO weekday: Monday = 1 … Sunday = 7. */
    val isoDayOfWeek: Int
        get() = Math.floorMod(julianDayNumber, 7) + 1

    /** Day of the year, 1 = January 1st. */
    val dayOfYear: Int
        get() = julianDayNumber - CivilDate(year, 1, 1).julianDayNumber + 1

    /** Whether [year] is a Gregorian leap year. */
    val isLeapYear: Boolean
        get() = isLeapYear(year)

    /** This date shifted by [days] (may be negative). */
    fun plusDays(days: Int): CivilDate = fromJulianDayNumber(julianDayNumber + days)

    /**
     * This date shifted by [months], with Dart `DateTime` overflow semantics:
     * the day is kept and rolls into the following month when the target
     * month is shorter (`2014-01-31 + 1 month` is 2014-03-03).
     */
    fun plusMonths(months: Int): CivilDate = normalized(year, month + months, day)

    /** Seconds since the Unix epoch at 00:00 UTC of this date. */
    fun unixMidnightSeconds(): Long = epochDay * 86_400L

    /** Milliseconds since the Unix epoch at 00:00 UTC of this date. */
    fun utcMidnightEpochMillis(): Long = epochDay * 86_400_000L

    override fun compareTo(other: CivilDate): Int = julianDayNumber.compareTo(other.julianDayNumber)

    /** ISO `yyyy-mm-dd`. */
    override fun toString(): String =
        "${DartStrings.padYear(year)}-${DartStrings.two(month)}-${DartStrings.two(day)}"

    companion object {
        /** Julian Day Number of 1970-01-01. */
        const val EPOCH_JDN: Int = 2440588

        /** Whether [year] is a Gregorian leap year. */
        @JvmStatic
        fun isLeapYear(year: Int): Boolean = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)

        /** Number of days in [month] of [year]. */
        @JvmStatic
        fun daysInMonth(year: Int, month: Int): Int = when (month) {
            1, 3, 5, 7, 8, 10, 12 -> 31
            4, 6, 9, 11 -> 30
            2 -> if (isLeapYear(year)) 29 else 28
            else -> throw IllegalArgumentException("month must be in 1..12: $month")
        }

        /**
         * The date Dart's `DateTime(year, month, day)` denotes: the month is
         * normalised first (13 rolls into the next year), then the day is
         * added to the first of that month, so `day` may be `0`, negative or
         * past the month's end.
         */
        @JvmStatic
        fun normalized(year: Int, month: Int, day: Int): CivilDate {
            val y = year + Math.floorDiv(month - 1, 12)
            val m = Math.floorMod(month - 1, 12) + 1
            return fromJulianDayNumber(CivilDate(y, m, 1).julianDayNumber + (day - 1))
        }

        /** The date with the given chronological Julian Day Number. */
        @JvmStatic
        fun fromJulianDayNumber(jdn: Int): CivilDate {
            val a = jdn + 32044
            val b = Math.floorDiv(4 * a + 3, 146097)
            val c = a - Math.floorDiv(146097 * b, 4)
            val d = Math.floorDiv(4 * c + 3, 1461)
            val e = c - Math.floorDiv(1461 * d, 4)
            val m = Math.floorDiv(5 * e + 2, 153)
            val day = e - Math.floorDiv(153 * m + 2, 5) + 1
            val month = m + 3 - 12 * Math.floorDiv(m, 10)
            val year = 100 * b + d - 4800 + Math.floorDiv(m, 10)
            return CivilDate(year, month, day)
        }

        /** The date [epochDay] days after 1970-01-01. */
        @JvmStatic
        fun fromEpochDay(epochDay: Int): CivilDate = fromJulianDayNumber(epochDay + EPOCH_JDN)

        /** The civil date of the instant [epochMillis] in a zone at [offset]. */
        @JvmStatic
        fun fromEpochMillis(epochMillis: Long, offset: UtcOffset): CivilDate =
            fromEpochDay(Math.floorDiv(epochMillis + offset.totalSeconds * 1000L, 86_400_000L).toInt())

        /** Today's date in [timeZone] (defaults to the device zone). */
        @JvmStatic
        @JvmOverloads
        fun today(timeZone: TimeZone = TimeZone.getDefault()): CivilDate {
            val calendar = Calendar.getInstance(timeZone)
            return CivilDate(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH),
            )
        }
    }
}
