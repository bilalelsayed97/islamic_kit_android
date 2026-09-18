package io.github.bilalelsayed97.islamickit

import io.github.bilalelsayed97.islamickit.domain.enums.Prayer
import io.github.bilalelsayed97.islamickit.domain.models.PrayerTime
import io.github.bilalelsayed97.islamickit.time.CivilDate
import io.github.bilalelsayed97.islamickit.time.CivilDateTime
import io.github.bilalelsayed97.islamickit.time.UtcOffset
import io.github.bilalelsayed97.islamickit.time.toCivilDate
import io.github.bilalelsayed97.islamickit.time.toCivilDateTime
import io.github.bilalelsayed97.islamickit.time.toInstant
import io.github.bilalelsayed97.islamickit.time.toLocalDate
import io.github.bilalelsayed97.islamickit.time.toLocalDateTime
import io.github.bilalelsayed97.islamickit.time.toUtcOffset
import io.github.bilalelsayed97.islamickit.time.toZoneOffset
import io.github.bilalelsayed97.islamickit.time.toZonedDateTime
import io.github.bilalelsayed97.islamickit.time.utcOffsetAt
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** The optional `java.time` bridges in `time/JavaTimeInterop.kt`. */
class JavaTimeInteropTest {
    @Test
    fun `dates and date-times round-trip`() {
        assertEquals(LocalDate.of(2014, 4, 24), CivilDate(2014, 4, 24).toLocalDate())
        assertEquals(CivilDate(2014, 4, 24), LocalDate.of(2014, 4, 24).toCivilDate())
        assertEquals(LocalDateTime.of(2014, 4, 24, 13, 30, 5), CivilDateTime(2014, 4, 24, 13, 30, 5).toLocalDateTime())
        assertEquals(CivilDateTime(2014, 4, 24, 13, 30, 5), LocalDateTime.of(2014, 4, 24, 13, 30, 5, 999).toCivilDateTime())
    }

    @Test
    fun `offsets convert both ways`() {
        assertEquals(ZoneOffset.ofHoursMinutes(5, 30), UtcOffset.ofHours(5, 30).toZoneOffset())
        assertEquals(ZoneOffset.ofHoursMinutes(-3, -30), UtcOffset.ofHours(-3, -30).toZoneOffset())
        assertEquals(UtcOffset.ofHours(-3, -30), ZoneOffset.ofHoursMinutes(-3, -30).toUtcOffset())
        assertEquals(UtcOffset.ZERO, ZoneOffset.UTC.toUtcOffset())
    }

    @Test
    fun `zone offset is taken at local noon of the date`() {
        val london = ZoneId.of("Europe/London")
        assertEquals(UtcOffset.ofHours(1), london.utcOffsetAt(CivilDate(2014, 4, 24)))
        assertEquals(UtcOffset.ZERO, london.utcOffsetAt(CivilDate(2014, 12, 21)))
        // Clocks go forward at 01:00 UTC on this date; noon is already in BST.
        assertEquals(UtcOffset.ofHours(1), london.utcOffsetAt(CivilDate(2014, 3, 30)))
        assertEquals(UtcOffset.ofHours(5, 30), ZoneId.of("Asia/Kolkata").utcOffsetAt(CivilDate(2014, 4, 24)))
    }

    @Test
    fun `prayer times become instants`() {
        val fajr = PrayerTime(Prayer.FAJR, 3.95, CivilDate(2014, 4, 24), UtcOffset.ofHours(1))
        assertEquals(Instant.parse("2014-04-24T02:57:00Z"), fajr.toInstant())
        val zoned = fajr.toZonedDateTime(ZoneId.of("Europe/London"))!!
        assertEquals(3, zoned.hour)
        assertEquals(57, zoned.minute)
        assertNull(PrayerTime(Prayer.FAJR, null, CivilDate(2014, 4, 24), UtcOffset.ZERO).toInstant())
        assertNull(PrayerTime(Prayer.FAJR, Double.NaN, CivilDate(2014, 4, 24), UtcOffset.ZERO).toZonedDateTime(ZoneOffset.UTC))
    }
}
