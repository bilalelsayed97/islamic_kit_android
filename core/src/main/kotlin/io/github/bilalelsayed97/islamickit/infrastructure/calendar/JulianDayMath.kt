package io.github.bilalelsayed97.islamickit.infrastructure.calendar

import io.github.bilalelsayed97.islamickit.time.CivilDate
import kotlin.math.ceil
import kotlin.math.floor

/** Raw Hijri parts produced by the table and arithmetic lookups. */
internal data class HijriParts(val year: Int, val month: Int, val day: Int, val monthLength: Int)

/**
 * Low-level Julian-Day math for Hijri <-> Gregorian conversion.
 *
 * Julian-day conversions for the Gregorian and Hijri calendars,
 * `Date/Julian` and `Date/Hijri`. All values are chronological Julian Day
 * numbers (CJDN); integer/truncating arithmetic is used exactly as in the PHP.
 */
internal object JulianDayMath {
    /** Truncation with the PHP `intPart` epsilon (±1e-7). */
    fun intPart(x: Double): Double =
        if (x < -0.0000001) ceil(x - 0.0000001) else floor(x + 0.0000001)

    /**
     * Gregorian date -> CJDN.
     *
     * Note: the century offset uses the *original* year (matching the PHP, which
     * reads the century from the unadjusted date even for Jan/Feb).
     */
    fun gregorianToJd(year: Int, month: Int, day: Int): Int {
        var y = year
        var m = month
        val a = Math.floorDiv(year, 100)
        if (m < 3) {
            y -= 1
            m += 12
        }
        val jgc = a - Math.floorDiv(a, 4) - 2
        return floor(365.25 * (y + 4716)).toInt() +
            floor(30.6001 * (m + 1)).toInt() +
            day -
            jgc -
            1524
    }

    /** CJDN -> Gregorian date. */
    fun jdToGregorian(jd: Int): CivilDate {
        val a = floor((jd - 1867216.25) / 36524.25).toInt()
        val jgc = a - Math.floorDiv(a, 4) + 1
        val b = jd + jgc + 1524
        val c = floor((b - 122.1) / 365.25).toInt()
        val d = floor(365.25 * c).toInt()
        var month = floor((b - d) / 30.6001).toInt()
        val day = (b - d) - floor(30.6001 * month).toInt()
        var cc = c
        if (month > 13) {
            cc += 1
            month -= 12
        }
        month -= 1
        return CivilDate(cc - 4716, month, day)
    }

    /** Hijri date -> CJDN (pure arithmetic, used by every `hToG`). */
    fun hijriToJd(year: Int, month: Int, day: Int, adjust: Int = 0): Int {
        return ((11 * year + 3) / 30) +
            354 * year +
            30 * month -
            ((month - 1) / 2) +
            day +
            1948440 -
            385 +
            adjust
    }

    /** CJDN -> Hijri via a lunation-start table lookup (Umm al-Qura / Diyanet). */
    fun tableToHijri(data: IntArray, lunations: Int, jd: Int): HijriParts {
        val mcjdn = jd - 2400000
        var i = 0
        while (i < data.size) {
            if (data[i] > mcjdn) break
            i++
        }
        val iln = i + lunations
        val ii = Math.floorDiv(iln - 1, 12)
        return HijriParts(
            year = ii + 1,
            month = iln - 12 * ii,
            day = mcjdn - data[i - 1] + 1,
            monthLength = data[i] - data[i - 1],
        )
    }

    /** CJDN -> Hijri via the pure arithmetic (tabular) algorithm (month length not tracked). */
    fun mathematicalToHijri(jd: Int, adjustment: Int): HijriParts {
        var l = jd + adjustment - 1948440 + 10632.0
        val n = intPart((l - 1) / 10631)
        l = l - 10631 * n + 354
        val j = intPart((10985 - l) / 5316) * intPart((50 * l) / 17719) +
            intPart(l / 5670) * intPart((43 * l) / 15238)
        l = l -
            intPart((30 - j) / 15) * intPart((17719 * j) / 50) -
            intPart(j / 16) * intPart((15238 * j) / 43) +
            29
        val m = intPart((24 * l) / 709)
        val d = l - intPart((709 * m) / 24)
        val y = 30 * n + j - 30
        return HijriParts(year = y.toInt(), month = m.toInt(), day = d.toInt(), monthLength = 30)
    }
}
