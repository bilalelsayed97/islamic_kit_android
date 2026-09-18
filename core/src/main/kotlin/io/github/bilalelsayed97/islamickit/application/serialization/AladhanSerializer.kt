package io.github.bilalelsayed97.islamickit.application.serialization

import io.github.bilalelsayed97.islamickit.domain.enums.CalculationMethod
import io.github.bilalelsayed97.islamickit.domain.enums.MidnightMode
import io.github.bilalelsayed97.islamickit.domain.enums.Prayer
import io.github.bilalelsayed97.islamickit.domain.enums.Shafaq
import io.github.bilalelsayed97.islamickit.domain.enums.TimeFormat
import io.github.bilalelsayed97.islamickit.domain.models.CalculationMeta
import io.github.bilalelsayed97.islamickit.domain.models.DateInfo
import io.github.bilalelsayed97.islamickit.domain.models.GregorianDate
import io.github.bilalelsayed97.islamickit.domain.models.HijriDate
import io.github.bilalelsayed97.islamickit.domain.models.PrayerResult
import io.github.bilalelsayed97.islamickit.domain.models.QiblaDirection
import io.github.bilalelsayed97.islamickit.domain.valueobjects.MethodParams
import io.github.bilalelsayed97.islamickit.internal.DartStrings

/**
 * aladhan-compatible JSON model builders. Every function returns an
 * insertion-ordered `Map<String, Any?>` tree that [JsonWriter.encode] turns
 * into the same text Dart's `jsonEncode` produces.
 */
object AladhanSerializer {
    /** Order of the `offset` block in aladhan `meta`. */
    private val OFFSET_ORDER: List<Prayer> = listOf(
        Prayer.IMSAK,
        Prayer.FAJR,
        Prayer.SUNRISE,
        Prayer.DHUHR,
        Prayer.ASR,
        Prayer.SUNSET,
        Prayer.MAGHRIB,
        Prayer.ISHA,
        Prayer.MIDNIGHT,
    )

    /** Emits whole doubles as ints (18.0 -> 18) and keeps fractional ones (18.5). */
    private fun numFmt(v: Double): Number {
        val i = v.toInt()
        return if (v == i.toDouble()) i else v
    }

    private fun paramsJson(p: MethodParams, moonsighting: Boolean, shafaq: Shafaq): Map<String, Any?> {
        if (moonsighting) return linkedMapOf("shafaq" to shafaq.code)
        val out = linkedMapOf<String, Any?>("Fajr" to numFmt(p.fajrAngle))
        if (p.ishaMinutesAfterMaghrib != null) {
            out["Isha"] = "${p.ishaMinutesAfterMaghrib} min"
        } else if (p.ishaAngle != null) {
            out["Isha"] = numFmt(p.ishaAngle)
        }
        if (p.maghribAngle != null) {
            out["Maghrib"] = numFmt(p.maghribAngle)
        } else if (p.maghribMinutesAfterSunset != null) {
            out["Maghrib"] = "${p.maghribMinutesAfterSunset} min"
        }
        if (p.midnightMode == MidnightMode.JAFARI) out["Midnight"] = "JAFARI"
        return out
    }

    private fun methodJson(m: CalculationMeta): Map<String, Any?> {
        val method = m.method
        val json = linkedMapOf<String, Any?>(
            "id" to method.id,
            "name" to method.methodName,
            "params" to paramsJson(m.methodParams, moonsighting = method.usesMoonsighting, shafaq = m.shafaq),
        )
        val loc = m.methodParams.location
        if (loc != null) {
            json["location"] = linkedMapOf<String, Any?>(
                "latitude" to loc.latitude,
                "longitude" to loc.longitude,
            )
        }
        return json
    }

    private fun metaJson(m: CalculationMeta): Map<String, Any?> {
        val offset = LinkedHashMap<String, Any?>()
        for (p in OFFSET_ORDER) offset[p.key] = m.offsets[p] ?: 0
        return linkedMapOf(
            "latitude" to m.coordinates.latitude,
            "longitude" to m.coordinates.longitude,
            "timezone" to m.timezone,
            "method" to methodJson(m),
            "latitudeAdjustmentMethod" to
                if (m.method.usesMoonsighting) "NONE" else m.latitudeAdjustmentMethod.metaValue,
            "midnightMode" to m.midnightMode.metaValue,
            "school" to m.school.metaValue,
            "offset" to offset,
        )
    }

    private fun gregorianJson(g: GregorianDate): Map<String, Any?> = linkedMapOf(
        "date" to g.formatted,
        "format" to "DD-MM-YYYY",
        "day" to DartStrings.two(g.day),
        "weekday" to linkedMapOf<String, Any?>("en" to g.weekdayEn),
        "month" to linkedMapOf<String, Any?>("number" to g.month, "en" to g.monthEn),
        "year" to "${g.year}",
        "designation" to linkedMapOf<String, Any?>(
            "abbreviated" to "AD",
            "expanded" to "Anno Domini",
        ),
        "lunarSighting" to false,
    )

    private fun hijriJson(h: HijriDate): Map<String, Any?> = linkedMapOf(
        "date" to h.formatted,
        "format" to "DD-MM-YYYY",
        "day" to "${h.day}",
        "weekday" to linkedMapOf<String, Any?>("en" to h.weekdayEn, "ar" to h.weekdayAr),
        "month" to linkedMapOf<String, Any?>(
            "number" to h.month,
            "en" to h.monthEn,
            "ar" to h.monthAr,
            "days" to h.monthLength,
        ),
        "year" to "${h.year}",
        "designation" to linkedMapOf<String, Any?>(
            "abbreviated" to "AH",
            "expanded" to "Anno Hegirae",
        ),
        "holidays" to h.holidays,
        "adjustedHolidays" to emptyList<String>(),
        "method" to h.method.code,
    )

    private fun dateJson(d: DateInfo): Map<String, Any?> = linkedMapOf(
        "readable" to d.readable,
        "timestamp" to "${d.timestamp}",
        "hijri" to hijriJson(d.hijri),
        "gregorian" to gregorianJson(d.gregorian),
    )

    private fun timingsJson(r: PrayerResult, format: TimeFormat, calendar: Boolean): Map<String, Any?> {
        val suffix = if (calendar && format != TimeFormat.ISO8601 && format != TimeFormat.FLOAT) {
            " (${r.meta.timezone})"
        } else {
            ""
        }
        val out = LinkedHashMap<String, Any?>()
        for (prayer in r.timings.raw.keys) {
            out[prayer.key] = "${r.timings.formatted(prayer, format)}$suffix"
        }
        return out
    }

    /**
     * The `data` object: `{timings, date, meta}`. When [calendar] is true,
     * clock-formatted timings carry the ` (timezone)` suffix aladhan uses.
     */
    @JvmStatic
    @JvmOverloads
    fun toAladhanData(
        result: PrayerResult,
        format: TimeFormat = TimeFormat.H24,
        calendar: Boolean = false,
    ): Map<String, Any?> = linkedMapOf(
        "timings" to timingsJson(result, format, calendar),
        "date" to dateJson(result.date),
        "meta" to metaJson(result.meta),
    )

    /** The full envelope: `{code, status, data}`. */
    @JvmStatic
    @JvmOverloads
    fun toAladhanJson(result: PrayerResult, format: TimeFormat = TimeFormat.H24): Map<String, Any?> = linkedMapOf(
        "code" to 200,
        "status" to "OK",
        "data" to toAladhanData(result, format = format),
    )

    /** aladhan-compatible `/methods` response, built from [CalculationMethod] in declaration order. */
    @JvmStatic
    fun methodsAladhanJson(): Map<String, Any?> {
        val data = LinkedHashMap<String, Any?>()
        for (m in CalculationMethod.entries) {
            if (m == CalculationMethod.CUSTOM) {
                data[m.code] = linkedMapOf<String, Any?>("id" to m.id, "name" to m.methodName)
                continue
            }
            val entry = linkedMapOf<String, Any?>(
                "id" to m.id,
                "name" to m.methodName,
                "params" to paramsJson(m.params, moonsighting = m.usesMoonsighting, shafaq = Shafaq.GENERAL),
            )
            val loc = m.params.location
            if (loc != null) {
                entry["location"] = linkedMapOf<String, Any?>(
                    "latitude" to loc.latitude,
                    "longitude" to loc.longitude,
                )
            }
            data[m.code] = entry
        }
        return linkedMapOf("code" to 200, "status" to "OK", "data" to data)
    }

    /** aladhan-compatible calendar response: `data` is an array of day objects. */
    @JvmStatic
    @JvmOverloads
    fun calendarAladhanJson(days: List<PrayerResult>, format: TimeFormat = TimeFormat.H24): Map<String, Any?> =
        linkedMapOf(
            "code" to 200,
            "status" to "OK",
            "data" to days.map { d -> toAladhanData(d, format = format, calendar = true) },
        )

    /** aladhan-compatible annual calendar: `data` is a map keyed by month "1".."12". */
    @JvmStatic
    @JvmOverloads
    fun annualCalendarAladhanJson(
        months: Map<Int, List<PrayerResult>>,
        format: TimeFormat = TimeFormat.H24,
    ): Map<String, Any?> {
        val data = LinkedHashMap<String, Any?>()
        for ((month, days) in months) {
            data["$month"] = days.map { d -> toAladhanData(d, format = format, calendar = true) }
        }
        return linkedMapOf("code" to 200, "status" to "OK", "data" to data)
    }

    /**
     * aladhan-compatible next-prayer response: a `data` object whose `timings`
     * holds a single entry (the key keeps its first position).
     */
    @JvmStatic
    @JvmOverloads
    fun nextPrayerAladhanJson(
        dayResult: PrayerResult,
        prayer: Prayer,
        format: TimeFormat = TimeFormat.H24,
    ): Map<String, Any?> {
        val data = LinkedHashMap(toAladhanData(dayResult, format = format))
        data["timings"] = linkedMapOf<String, Any?>(prayer.key to dayResult.timings.formatted(prayer, format))
        return linkedMapOf("code" to 200, "status" to "OK", "data" to data)
    }

    /** aladhan-compatible qibla response. */
    @JvmStatic
    fun qiblaAladhanJson(q: QiblaDirection): Map<String, Any?> = linkedMapOf(
        "code" to 200,
        "status" to "OK",
        "data" to linkedMapOf<String, Any?>(
            "latitude" to q.from.latitude,
            "longitude" to q.from.longitude,
            "direction" to q.degrees,
        ),
    )
}

/** The `data` object: `{timings, date, meta}`. See [AladhanSerializer.toAladhanData]. */
fun PrayerResult.toAladhanData(format: TimeFormat = TimeFormat.H24, calendar: Boolean = false): Map<String, Any?> =
    AladhanSerializer.toAladhanData(this, format, calendar)

/** The full envelope: `{code, status, data}`. See [AladhanSerializer.toAladhanJson]. */
fun PrayerResult.toAladhanJson(format: TimeFormat = TimeFormat.H24): Map<String, Any?> =
    AladhanSerializer.toAladhanJson(this, format)
