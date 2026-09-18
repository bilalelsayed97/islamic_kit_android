package io.github.bilalelsayed97.islamickit.domain.valueobjects

import io.github.bilalelsayed97.islamickit.domain.enums.Prayer

/**
 * Per-prayer time adjustments, in minutes — the equivalent of the aladhan
 * `tune` parameter.
 *
 * Field order matches the aladhan contract exactly:
 * `Imsak, Fajr, Sunrise, Dhuhr, Asr, Maghrib, Sunset, Isha, Midnight`
 * (note Maghrib comes before Sunset). Each value defaults to `0`.
 * Equality is defined by the CSV form ([toCsv]).
 */
data class Tune @JvmOverloads constructor(
    val imsak: Int = 0,
    val fajr: Int = 0,
    val sunrise: Int = 0,
    val dhuhr: Int = 0,
    val asr: Int = 0,
    val maghrib: Int = 0,
    val sunset: Int = 0,
    val isha: Int = 0,
    val midnight: Int = 0,
) {
    /** Whether every adjustment is zero. */
    val isEmpty: Boolean
        get() = imsak == 0 && fajr == 0 && sunrise == 0 && dhuhr == 0 && asr == 0 &&
            maghrib == 0 && sunset == 0 && isha == 0 && midnight == 0

    /** The aladhan `tune` CSV, e.g. `"5,3,5,7,9,-1,0,8,-6"`. */
    fun toCsv(): String = "$imsak,$fajr,$sunrise,$dhuhr,$asr,$maghrib,$sunset,$isha,$midnight"

    /** A map keyed by [Prayer], in aladhan `offset` order. */
    fun toMap(): Map<Prayer, Int> = linkedMapOf(
        Prayer.IMSAK to imsak,
        Prayer.FAJR to fajr,
        Prayer.SUNRISE to sunrise,
        Prayer.DHUHR to dhuhr,
        Prayer.ASR to asr,
        Prayer.SUNSET to sunset,
        Prayer.MAGHRIB to maghrib,
        Prayer.ISHA to isha,
        Prayer.MIDNIGHT to midnight,
    )

    override fun equals(other: Any?): Boolean = other is Tune && other.toCsv() == toCsv()

    override fun hashCode(): Int = toCsv().hashCode()

    companion object {
        /** No adjustments. */
        @JvmField
        val NONE: Tune = Tune()

        /**
         * Parses an aladhan `tune` CSV string, e.g. `"5,3,5,7,9,-1,0,8,-6"`.
         * Missing trailing values default to `0`; unparsable values become `0`.
         */
        @JvmStatic
        fun fromCsv(csv: String): Tune {
            val parts = csv.split(',')
            fun at(i: Int): Int = if (i < parts.size) parts[i].trim().toIntOrNull() ?: 0 else 0
            return Tune(
                imsak = at(0),
                fajr = at(1),
                sunrise = at(2),
                dhuhr = at(3),
                asr = at(4),
                maghrib = at(5),
                sunset = at(6),
                isha = at(7),
                midnight = at(8),
            )
        }
    }
}
