package io.github.bilalelsayed97.islamickit.application

import io.github.bilalelsayed97.islamickit.domain.models.CalculationMeta
import io.github.bilalelsayed97.islamickit.domain.models.DateInfo
import io.github.bilalelsayed97.islamickit.domain.models.GregorianDate
import io.github.bilalelsayed97.islamickit.domain.models.PrayerResult
import io.github.bilalelsayed97.islamickit.domain.models.PrayerTimings
import io.github.bilalelsayed97.islamickit.domain.ports.TwilightStrategy
import io.github.bilalelsayed97.islamickit.domain.valueobjects.CalculationParameters
import io.github.bilalelsayed97.islamickit.domain.valueobjects.Coordinates
import io.github.bilalelsayed97.islamickit.infrastructure.astronomy.AstronomicalCalculator
import io.github.bilalelsayed97.islamickit.infrastructure.calendar.HijriConverterFactory
import io.github.bilalelsayed97.islamickit.infrastructure.calendar.TableHijriConverter
import io.github.bilalelsayed97.islamickit.infrastructure.localization.Localizer
import io.github.bilalelsayed97.islamickit.infrastructure.twilight.MoonsightingTwilight
import io.github.bilalelsayed97.islamickit.internal.DartStrings
import io.github.bilalelsayed97.islamickit.time.CivilDate
import io.github.bilalelsayed97.islamickit.time.UtcOffset

/** Hijri month number of Ramadan. */
private const val RAMADAN_MONTH: Int = 9

/**
 * Builds a complete [PrayerResult] (timings + date block + meta) for one date
 * and location. Composes the astronomy engine, the Hijri converter and the
 * localizer — the single use case the facade builds every feature on.
 */
class PrayerCalculator @JvmOverloads constructor(
    val astronomy: AstronomicalCalculator = AstronomicalCalculator(),
    val hijriFactory: HijriConverterFactory = HijriConverterFactory(),
    val moonsighting: TwilightStrategy = MoonsightingTwilight(),
) {
    /**
     * Computes the result for [date] at [coordinates] with [params].
     *
     * @throws IllegalArgumentException when [date] is outside the range of the
     *   Hijri table selected by `params.calendarMethod`.
     */
    fun calculate(date: CivilDate, coordinates: Coordinates, params: CalculationParameters): PrayerResult {
        val twilight = if (params.method.usesMoonsighting) moonsighting else null
        val raw = astronomy.compute(
            date,
            coordinates,
            params,
            twilight = twilight,
            ramadan = isRamadan(date, params),
        )

        return PrayerResult(
            timings = PrayerTimings(raw = raw, date = date, utcOffset = params.utcOffset),
            date = dateInfo(date, params),
            meta = meta(coordinates, params),
        )
    }

    /**
     * Whether [date] falls in Ramadan, for methods whose Isha interval changes
     * during the month (only Umm al-Qura does).
     *
     * Always resolved against the Umm al-Qura table regardless of the caller's
     * `calendarMethod`, because the rule itself is Saudi. Dates outside the
     * table's range fall back to the method's ordinary interval.
     */
    private fun isRamadan(date: CivilDate, params: CalculationParameters): Boolean {
        if (!params.effectiveParams.hasRamadanIshaInterval) return false
        val converter = TableHijriConverter.ummAlQura()
        return try {
            converter.fromGregorian(date).month == RAMADAN_MONTH
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    private fun dateInfo(date: CivilDate, params: CalculationParameters): DateInfo {
        val gregorian = GregorianDate(
            day = date.day,
            month = date.month,
            year = date.year,
            weekdayEn = Localizer.gregorianWeekday(date.isoDayOfWeek).en,
            monthEn = Localizer.gregorianMonths.getValue(date.month).en,
        )
        val hijri = hijriFactory.create(params.calendarMethod).fromGregorian(date)
        val readable = "${DartStrings.two(date.day)} ${Localizer.monthAbbrEn[date.month - 1]} ${date.year}"
        val timestamp = date.unixMidnightSeconds() - params.utcOffset.totalSeconds

        return DateInfo(
            readable = readable,
            timestamp = timestamp,
            gregorian = gregorian,
            hijri = hijri,
        )
    }

    private fun meta(coordinates: Coordinates, params: CalculationParameters): CalculationMeta {
        return CalculationMeta(
            coordinates = coordinates,
            timezone = params.timezoneName ?: utcLabel(params.utcOffset),
            method = params.method,
            methodParams = params.effectiveParams,
            school = params.school,
            midnightMode = params.resolvedMidnightMode,
            latitudeAdjustmentMethod = params.resolvedHighLatitudeRule,
            shafaq = params.shafaq,
            offsets = params.tune.toMap(),
        )
    }

    private fun utcLabel(offset: UtcOffset): String {
        if (offset == UtcOffset.ZERO) return "UTC"
        return "UTC${offset.toIsoString()}"
    }
}
