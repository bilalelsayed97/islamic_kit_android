package io.github.bilalelsayed97.islamickit.application

import io.github.bilalelsayed97.islamickit.application.usecases.QiblaCalculator
import io.github.bilalelsayed97.islamickit.domain.enums.CalculationMethod
import io.github.bilalelsayed97.islamickit.domain.enums.Prayer
import io.github.bilalelsayed97.islamickit.domain.models.City
import io.github.bilalelsayed97.islamickit.domain.models.CityEntry
import io.github.bilalelsayed97.islamickit.domain.models.NextPrayer
import io.github.bilalelsayed97.islamickit.domain.models.PrayerResult
import io.github.bilalelsayed97.islamickit.domain.models.QiblaDirection
import io.github.bilalelsayed97.islamickit.domain.ports.CityDirectory
import io.github.bilalelsayed97.islamickit.domain.ports.Geocoder
import io.github.bilalelsayed97.islamickit.domain.valueobjects.CalculationParameters
import io.github.bilalelsayed97.islamickit.domain.valueobjects.Coordinates
import io.github.bilalelsayed97.islamickit.infrastructure.calendar.HijriConverterFactory
import io.github.bilalelsayed97.islamickit.infrastructure.config.LocationDefaults
import io.github.bilalelsayed97.islamickit.infrastructure.config.calculationMethod
import io.github.bilalelsayed97.islamickit.infrastructure.geocoding.BundledCityGeocoder
import io.github.bilalelsayed97.islamickit.time.CivilDate
import io.github.bilalelsayed97.islamickit.time.CivilDateTime
import io.github.bilalelsayed97.islamickit.time.UtcOffset

/**
 * The library's top-level facade. Offers aladhan-equivalent operations —
 * timings, next prayer, calendars (Gregorian & Hijri, by coordinates / city /
 * address), qibla and the methods list — all computed offline.
 *
 * Every `timings*` and calendar operation may throw [IllegalArgumentException]
 * when the date lies outside the selected Hijri table's range, and the
 * `*ByCity` / `*ByAddress` operations throw [IllegalStateException] when the
 * [geocoder] finds no match.
 */
class PrayerTimesService @JvmOverloads constructor(
    geocoder: Geocoder? = null,
    /**
     * The bundled city database, used by the `*ByCoordinatesAuto` methods to
     * resolve a GPS fix to a city, its timezone and its country's recommended
     * calculation method.
     *
     * Optional: without it, coordinate-based automatic parameters fall back to
     * [LocationDefaults] via the geocoder, which cannot resolve a country from
     * coordinates — so pass one when you have the database open.
     */
    val directory: CityDirectory? = null,
    private val calculator: PrayerCalculator = PrayerCalculator(),
    private val qibla: QiblaCalculator = QiblaCalculator(),
    private val hijriFactory: HijriConverterFactory = HijriConverterFactory(),
) {
    /** The geocoder used by the `*ByCity` / `*ByAddress` methods. */
    val geocoder: Geocoder = geocoder ?: BundledCityGeocoder()

    // ---------------------------------------------------------------------------
    // Daily timings
    // ---------------------------------------------------------------------------

    /** Prayer times for [date] at [coordinates]. */
    @JvmOverloads
    fun timings(
        date: CivilDate,
        coordinates: Coordinates,
        params: CalculationParameters = DEFAULTS,
    ): PrayerResult = calculator.calculate(date, coordinates, params)

    /** Prayer times for [date] at a city, resolved via [geocoder]. */
    @JvmOverloads
    fun timingsByCity(
        city: String,
        date: CivilDate,
        country: String? = null,
        state: String? = null,
        params: CalculationParameters = DEFAULTS,
    ): PrayerResult {
        val resolved = requireCity(city, country = country, state = state)
        return calculator.calculate(date, resolved.coordinates, withCity(params, resolved))
    }

    /** Prayer times for [date] at a free-text address, resolved via [geocoder]. */
    @JvmOverloads
    fun timingsByAddress(
        address: String,
        date: CivilDate,
        params: CalculationParameters = DEFAULTS,
    ): PrayerResult {
        val resolved = requireCity(address)
        return calculator.calculate(date, resolved.coordinates, withCity(params, resolved))
    }

    // ---------------------------------------------------------------------------
    // Location-based defaults (automatic settings — no presets needed)
    // ---------------------------------------------------------------------------

    /**
     * Recommended [CalculationParameters] for a country, choosing the method and
     * Asr school by common regional convention (aladhan-style). Everything else
     * keeps its default; override any field with `copy`.
     *
     * ```kotlin
     * val p = service.recommendedParams("EG", utcOffset = UtcOffset.ofHours(2))
     * service.timings(date, coords, p) // Egyptian method, automatically
     * ```
     */
    @JvmOverloads
    fun recommendedParams(
        countryCode: String,
        utcOffset: UtcOffset = UtcOffset.ZERO,
        timezoneName: String? = null,
    ): CalculationParameters = CalculationParameters(
        method = LocationDefaults.methodForCountry(countryCode),
        school = LocationDefaults.schoolForCountry(countryCode),
        utcOffset = utcOffset,
        timezoneName = timezoneName,
    )

    /**
     * Fully automatic prayer times for a city: the method and Asr school are
     * chosen from the city's country, and the UTC offset comes from the city's
     * stored (standard-time) offset. No [CalculationParameters] required.
     *
     * ```kotlin
     * service.timingsByCityAuto("Cairo", date = CivilDate.today(), country = "EG")
     * ```
     */
    @JvmOverloads
    fun timingsByCityAuto(
        city: String,
        date: CivilDate,
        country: String? = null,
        state: String? = null,
    ): PrayerResult {
        val resolved = requireCity(city, country = country, state = state)
        return calculator.calculate(date, resolved.coordinates, autoParams(resolved))
    }

    /**
     * [CalculationParameters] resolved automatically for a geocoded [city]
     * (method + school by country, offset from the city).
     */
    fun autoParamsForCity(city: City): CalculationParameters = autoParams(city)

    private fun autoParams(city: City): CalculationParameters = CalculationParameters(
        method = LocationDefaults.methodForCountry(city.country),
        school = LocationDefaults.schoolForCountry(city.country),
        utcOffset = city.utcOffset,
    )

    /**
     * [CalculationParameters] resolved automatically from a GPS fix.
     *
     * Requires [directory]. Finds the nearest city, then takes the calculation
     * method from that city's country (as recorded in the bundled database),
     * the Asr school from regional convention, and the UTC offset from the
     * city's timezone.
     *
     * Returns `null` when no city can be resolved for the coordinates.
     *
     * @throws IllegalStateException when no [directory] was supplied.
     */
    fun autoParamsForCoordinates(latitude: Double, longitude: Double): CalculationParameters? {
        val city = requireDirectory().nearestCity(latitude, longitude)
        return if (city == null) null else autoParamsForCityEntry(city)
    }

    /**
     * [CalculationParameters] resolved automatically for a database [city] row.
     *
     * The method comes from the country's recorded preference, falling back to
     * [LocationDefaults] when the database records none.
     */
    fun autoParamsForCityEntry(city: CityEntry): CalculationParameters {
        val method = city.calculationMethod ?: LocationDefaults.methodForCountry(city.isoCode)
        return CalculationParameters(
            method = method,
            school = LocationDefaults.schoolForCountry(city.isoCode),
            utcOffset = city.utcOffset,
            timezoneName = city.timeZoneId,
        )
    }

    /**
     * Fully automatic prayer times for a GPS fix: nearest city, its country's
     * method, its timezone. No [CalculationParameters] required.
     *
     * Requires [directory]. Returns `null` when no city can be resolved.
     *
     * The stored offset is **standard time** — add an hour yourself where
     * daylight saving is in force on [date].
     *
     * @throws IllegalStateException when no [directory] was supplied.
     */
    fun timingsByCoordinatesAuto(latitude: Double, longitude: Double, date: CivilDate): PrayerResult? {
        val city = requireDirectory().nearestCity(latitude, longitude) ?: return null
        return calculator.calculate(date, city.coordinates, autoParamsForCityEntry(city))
    }

    private fun requireDirectory(): CityDirectory =
        directory ?: throw IllegalStateException(
            "This operation needs the bundled city database. Construct " +
                "PrayerTimesService with `directory` (see SqliteCityDirectory in the geocoding module).",
        )

    // ---------------------------------------------------------------------------
    // Next prayer
    // ---------------------------------------------------------------------------

    /**
     * The next obligatory prayer strictly after [from] (wall-clock local time,
     * interpreted with `params.utcOffset`). Rolls to the next day's Fajr if
     * [from] is after Isha.
     */
    @JvmOverloads
    fun nextPrayer(
        from: CivilDateTime,
        coordinates: Coordinates,
        params: CalculationParameters = DEFAULTS,
    ): NextPrayer {
        val localHour = from.fractionalHours
        val today = calculator.calculate(from.date, coordinates, params)
        for (prayer in Prayer.DAILY_OBLIGATORY) {
            val h = today.timings.raw[prayer]
            if (h != null && !h.isNaN() && h > localHour) {
                return NextPrayer(
                    prayer = prayer,
                    time = today.timings.time(prayer),
                    onDate = today.timings.date,
                )
            }
        }
        val tomorrow = from.date.plusDays(1)
        val next = calculator.calculate(tomorrow, coordinates, params)
        return NextPrayer(
            prayer = Prayer.FAJR,
            time = next.timings.time(Prayer.FAJR),
            onDate = tomorrow,
        )
    }

    /** [nextPrayer] resolved via [geocoder] for a free-text address. */
    @JvmOverloads
    fun nextPrayerByAddress(
        address: String,
        from: CivilDateTime,
        params: CalculationParameters = DEFAULTS,
    ): NextPrayer {
        val resolved = requireCity(address)
        return nextPrayer(from, resolved.coordinates, withCity(params, resolved))
    }

    // ---------------------------------------------------------------------------
    // Qibla & methods
    // ---------------------------------------------------------------------------

    /** The Qibla direction from [from] (degrees clockwise from true north). */
    fun qibla(from: Coordinates): QiblaDirection = qibla.direction(from)

    /** All available calculation methods, in declaration order. */
    fun methods(): List<CalculationMethod> = CalculationMethod.entries

    // ---------------------------------------------------------------------------
    // Calendars — Gregorian
    // ---------------------------------------------------------------------------

    /** Every day of a Gregorian month. */
    @JvmOverloads
    fun monthlyCalendar(
        year: Int,
        month: Int,
        coordinates: Coordinates,
        params: CalculationParameters = DEFAULTS,
    ): List<PrayerResult> {
        val days = CivilDate.daysInMonth(year, month)
        return (1..days).map { d -> calculator.calculate(CivilDate(year, month, d), coordinates, params) }
    }

    /** Every day of a Gregorian year, keyed by month 1..12. */
    @JvmOverloads
    fun annualCalendar(
        year: Int,
        coordinates: Coordinates,
        params: CalculationParameters = DEFAULTS,
    ): Map<Int, List<PrayerResult>> {
        val out = LinkedHashMap<Int, List<PrayerResult>>()
        for (m in 1..12) out[m] = monthlyCalendar(year, m, coordinates, params)
        return out
    }

    /**
     * Every day between [start] and [end] inclusive (max 11 months apart).
     *
     * @throws IllegalArgumentException when [end] precedes [start] or the range exceeds 11 months.
     */
    @JvmOverloads
    fun rangeCalendar(
        start: CivilDate,
        end: CivilDate,
        coordinates: Coordinates,
        params: CalculationParameters = DEFAULTS,
    ): List<PrayerResult> {
        require(end >= start) { "end must be on or after start." }
        val maxEnd = start.plusMonths(11)
        require(end <= maxEnd) { "Date range must be at most 11 months." }
        val out = ArrayList<PrayerResult>()
        var d = start
        while (d <= end) {
            out.add(calculator.calculate(d, coordinates, params))
            d = d.plusDays(1)
        }
        return out
    }

    /** [monthlyCalendar] for a city. */
    @JvmOverloads
    fun monthlyCalendarByCity(
        year: Int,
        month: Int,
        city: String,
        country: String? = null,
        state: String? = null,
        params: CalculationParameters = DEFAULTS,
    ): List<PrayerResult> {
        val resolved = requireCity(city, country = country, state = state)
        return monthlyCalendar(year, month, resolved.coordinates, withCity(params, resolved))
    }

    // ---------------------------------------------------------------------------
    // Calendars — Hijri
    // ---------------------------------------------------------------------------

    /** Every day of a Hijri month. */
    @JvmOverloads
    fun monthlyHijriCalendar(
        hijriYear: Int,
        hijriMonth: Int,
        coordinates: Coordinates,
        params: CalculationParameters = DEFAULTS,
    ): List<PrayerResult> {
        val converter = hijriFactory.create(params.calendarMethod)
        val start = converter.toGregorian(hijriYear, hijriMonth, 1)
        val length = converter.fromGregorian(start).monthLength
        return (0 until length).map { i -> calculator.calculate(start.plusDays(i), coordinates, params) }
    }

    /** Every day of a Hijri year, keyed by Hijri month 1..12. */
    @JvmOverloads
    fun annualHijriCalendar(
        hijriYear: Int,
        coordinates: Coordinates,
        params: CalculationParameters = DEFAULTS,
    ): Map<Int, List<PrayerResult>> {
        val out = LinkedHashMap<Int, List<PrayerResult>>()
        for (m in 1..12) out[m] = monthlyHijriCalendar(hijriYear, m, coordinates, params)
        return out
    }

    /** [monthlyHijriCalendar] for a city. */
    @JvmOverloads
    fun monthlyHijriCalendarByCity(
        hijriYear: Int,
        hijriMonth: Int,
        city: String,
        country: String? = null,
        state: String? = null,
        params: CalculationParameters = DEFAULTS,
    ): List<PrayerResult> {
        val resolved = requireCity(city, country = country, state = state)
        return monthlyHijriCalendar(hijriYear, hijriMonth, resolved.coordinates, withCity(params, resolved))
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private fun requireCity(query: String, country: String? = null, state: String? = null): City =
        geocoder.resolve(query, country = country, state = state)
            ?: throw IllegalStateException("Location not found: \"$query\".")

    /** Applies the city's standard UTC offset unless the caller set one already. */
    private fun withCity(params: CalculationParameters, city: City): CalculationParameters =
        if (params.utcOffset == UtcOffset.ZERO) params.copy(utcOffset = city.utcOffset) else params

    companion object {
        private val DEFAULTS: CalculationParameters = CalculationParameters()
    }
}
