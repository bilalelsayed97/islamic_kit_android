package io.github.bilalelsayed97.islamickit.time

import io.github.bilalelsayed97.islamickit.internal.DartStrings
import java.util.Calendar
import java.util.TimeZone

/**
 * A wall-clock date and time with no zone: the `from` instant of
 * `PrayerTimesService.nextPrayer`, interpreted with the caller's UTC offset.
 */
data class CivilDateTime @JvmOverloads constructor(
    val date: CivilDate,
    val hour: Int = 0,
    val minute: Int = 0,
    val second: Int = 0,
) : Comparable<CivilDateTime> {
    init {
        require(hour in 0..23) { "hour must be in 0..23: $hour" }
        require(minute in 0..59) { "minute must be in 0..59: $minute" }
        require(second in 0..59) { "second must be in 0..59: $second" }
    }

    @JvmOverloads
    constructor(year: Int, month: Int, day: Int, hour: Int = 0, minute: Int = 0, second: Int = 0) :
        this(CivilDate(year, month, day), hour, minute, second)

    /** Seconds since midnight. */
    val secondOfDay: Int
        get() = hour * 3600 + minute * 60 + second

    /** The time of day as fractional hours, e.g. `13.5` for 13:30:00. */
    val fractionalHours: Double
        get() = hour + minute / 60.0 + second / 3600.0

    override fun compareTo(other: CivilDateTime): Int {
        val byDate = date.compareTo(other.date)
        return if (byDate != 0) byDate else secondOfDay.compareTo(other.secondOfDay)
    }

    /** ISO `yyyy-mm-ddThh:mm:ss`. */
    override fun toString(): String =
        "${date}T${DartStrings.two(hour)}:${DartStrings.two(minute)}:${DartStrings.two(second)}"

    companion object {
        /** The current wall-clock time in [timeZone] (defaults to the device zone). */
        @JvmStatic
        @JvmOverloads
        fun now(timeZone: TimeZone = TimeZone.getDefault()): CivilDateTime {
            val calendar = Calendar.getInstance(timeZone)
            return CivilDateTime(
                CivilDate(
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH) + 1,
                    calendar.get(Calendar.DAY_OF_MONTH),
                ),
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                calendar.get(Calendar.SECOND),
            )
        }

        /** The wall-clock time of the instant [epochMillis] in a zone at [offset]. */
        @JvmStatic
        fun fromEpochMillis(epochMillis: Long, offset: UtcOffset): CivilDateTime {
            val local = epochMillis + offset.totalSeconds * 1000L
            val epochDay = Math.floorDiv(local, 86_400_000L)
            val secondOfDay = (Math.floorMod(local, 86_400_000L) / 1000L).toInt()
            return CivilDateTime(
                CivilDate.fromEpochDay(epochDay.toInt()),
                secondOfDay / 3600,
                (secondOfDay / 60) % 60,
                secondOfDay % 60,
            )
        }
    }
}
