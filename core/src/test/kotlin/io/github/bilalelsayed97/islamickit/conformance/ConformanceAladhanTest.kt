package io.github.bilalelsayed97.islamickit.conformance

import io.github.bilalelsayed97.islamickit.application.PrayerTimesService
import io.github.bilalelsayed97.islamickit.application.serialization.AladhanSerializer
import io.github.bilalelsayed97.islamickit.application.serialization.JsonWriter
import io.github.bilalelsayed97.islamickit.domain.enums.Prayer
import io.github.bilalelsayed97.islamickit.domain.enums.TimeFormat
import io.github.bilalelsayed97.islamickit.domain.valueobjects.Coordinates
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Test

/**
 * `methods.json` (`methodsAladhanJson()` verbatim) and `aladhan.json` (the
 * calendar / annual / next-prayer / qibla / methods envelopes), compared as
 * the encoded strings Dart's `jsonEncode` produced.
 */
class ConformanceAladhanTest {
    private val service = PrayerTimesService()

    @Test
    fun `methods_json - encoded string and structure`() {
        val fixture = Fixtures.load("methods.json").jsonObject
        val actual = AladhanSerializer.methodsAladhanJson()
        JsonAssert.assertSameEncoded(fixture.str("json"), JsonWriter.encode(actual), "methods")
        // `_numFmt` emits whole angles as ints and fractional ones as doubles;
        // the structural check pins that distinction independently of the text.
        JsonAssert.assertSameJson(fixture.getValue("data"), actual, "data")
    }

    @Test
    fun `aladhan_json - every envelope`() {
        val fixture = Fixtures.load("aladhan.json").jsonObject
        val input = fixture.obj("input")
        val coordinates = Coordinates(input.double("lat"), input.double("lng"))
        val params = Fixtures.params(input.obj("params"))
        val year = input.int("year")
        val month = input.int("month")

        val monthly = service.monthlyCalendar(year, month, coordinates, params)
        val annual = service.annualCalendar(year, coordinates, params)
        val day = service.timings(Fixtures.date(input.str("day")), coordinates, params)
        val qibla = service.qibla(Coordinates(input.obj("qibla").double("lat"), input.obj("qibla").double("lng")))
        val nextPrayer = Prayer.fromKey(input.str("nextPrayer"))

        JsonAssert.assertSameEncoded(
            fixture.str("calendar"),
            JsonWriter.encode(AladhanSerializer.calendarAladhanJson(monthly)),
            "calendar",
        )
        JsonAssert.assertSameEncoded(
            fixture.str("calendarIso"),
            JsonWriter.encode(AladhanSerializer.calendarAladhanJson(monthly, TimeFormat.ISO8601)),
            "calendarIso",
        )
        JsonAssert.assertSameEncoded(
            fixture.str("annual"),
            JsonWriter.encode(AladhanSerializer.annualCalendarAladhanJson(annual)),
            "annual",
        )
        JsonAssert.assertSameEncoded(
            fixture.str("nextPrayer"),
            JsonWriter.encode(AladhanSerializer.nextPrayerAladhanJson(day, nextPrayer)),
            "nextPrayer",
        )
        JsonAssert.assertSameEncoded(
            fixture.str("qibla"),
            JsonWriter.encode(AladhanSerializer.qiblaAladhanJson(qibla)),
            "qibla",
        )
        JsonAssert.assertSameEncoded(
            fixture.str("methods"),
            JsonWriter.encode(AladhanSerializer.methodsAladhanJson()),
            "methods",
        )
    }
}
