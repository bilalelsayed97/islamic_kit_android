@file:JvmName("JavaTimeInterop")

package io.github.bilalelsayed97.islamickit.time

import androidx.annotation.RequiresApi
import io.github.bilalelsayed97.islamickit.domain.models.PrayerTime
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

// Optional `java.time` bridges. The library's own types keep the API usable on
// minSdk 24; these helpers are for callers on API 26+ (or any JVM), which is
// why every one of them is annotated rather than the types themselves.

/** This date as a [LocalDate]. */
@RequiresApi(26)
fun CivilDate.toLocalDate(): LocalDate = LocalDate.of(year, month, day)

/** This [LocalDate] as a [CivilDate]. */
@RequiresApi(26)
fun LocalDate.toCivilDate(): CivilDate = CivilDate(year, monthValue, dayOfMonth)

/** This wall-clock time as a [LocalDateTime]. */
@RequiresApi(26)
fun CivilDateTime.toLocalDateTime(): LocalDateTime =
    LocalDateTime.of(date.year, date.month, date.day, hour, minute, second)

/** This [LocalDateTime] as a [CivilDateTime] (nanoseconds are dropped). */
@RequiresApi(26)
fun LocalDateTime.toCivilDateTime(): CivilDateTime =
    CivilDateTime(toLocalDate().toCivilDate(), hour, minute, second)

/** This offset as a [ZoneOffset]. */
@RequiresApi(26)
fun UtcOffset.toZoneOffset(): ZoneOffset = ZoneOffset.ofTotalSeconds(totalSeconds)

/** This [ZoneOffset] as a [UtcOffset]. */
@RequiresApi(26)
fun ZoneOffset.toUtcOffset(): UtcOffset = UtcOffset(totalSeconds)

/**
 * The offset this zone applies at local noon of [date] — the value to pass as
 * `CalculationParameters.utcOffset` for that day (same rule as
 * [UtcOffset.of] with a `java.util.TimeZone`).
 */
@RequiresApi(26)
fun ZoneId.utcOffsetAt(date: CivilDate): UtcOffset =
    rules.getOffset(date.toLocalDate().atTime(12, 0)).toUtcOffset()

/** The absolute instant of this prayer time, or `null` if it is invalid. */
@RequiresApi(26)
fun PrayerTime.toInstant(): Instant? = toEpochMillis()?.let(Instant::ofEpochMilli)

/** This prayer time on the wall clock of [zone], or `null` if it is invalid. */
@RequiresApi(26)
fun PrayerTime.toZonedDateTime(zone: ZoneId): ZonedDateTime? = toInstant()?.atZone(zone)
