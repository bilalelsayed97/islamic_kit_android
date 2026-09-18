package io.github.bilalelsayed97.islamickit.conformance

import io.github.bilalelsayed97.islamickit.application.PrayerTimesService
import io.github.bilalelsayed97.islamickit.domain.models.PrayerResult
import io.github.bilalelsayed97.islamickit.domain.valueobjects.Coordinates
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

/**
 * `calendars.json`: monthly, annual, range (and its `ArgumentError` cases),
 * Hijri-monthly for every calendar method, and Hijri-annual.
 */
class ConformanceCalendarsTest {
    private val service = PrayerTimesService()

    /** The generator's `day(r)` shape. */
    private fun day(r: PrayerResult): Map<String, Any?> = linkedMapOf(
        "date" to r.date.gregorian.formatted,
        "hijri" to r.date.hijri.formatted,
        "raw" to Fixtures.rawJson(r.timings.raw),
    )

    private fun assertDays(expected: JsonObject, days: List<PrayerResult>) {
        JsonAssert.assertSameJson(expected.getValue("days"), days.map(::day), "days")
    }

    private fun assertMonths(expected: JsonObject, months: Map<Int, List<PrayerResult>>) {
        val actual = LinkedHashMap<String, Any?>()
        for ((month, days) in months) {
            actual["$month"] = linkedMapOf("count" to days.size, "first" to day(days.first()), "last" to day(days.last()))
        }
        JsonAssert.assertSameJson(expected.getValue("months"), actual, "months")
    }

    @TestFactory
    fun calendars(): List<DynamicNode> = Fixtures.cases("calendars.json").map { c ->
        val kind = c.str("kind")
        val coordinates = Coordinates(c.double("lat"), c.double("lng"))
        val params = Fixtures.params(c.obj("params"))
        val name = when (kind) {
            "monthly" -> "monthly ${c.int("year")}-${c.int("month")}"
            "annual" -> "annual ${c.int("year")}"
            "range", "rangeError" -> "$kind ${c.str("start")}..${c.str("end")}"
            "hijriMonthly" -> "hijriMonthly ${c.int("hijriYear")}-${c.int("hijriMonth")} ${params.calendarMethod.code}"
            "hijriAnnual" -> "hijriAnnual ${c.int("hijriYear")} ${params.calendarMethod.code}"
            else -> throw IllegalStateException("Unknown calendar kind $kind")
        }
        dynamicTest(name) {
            when (kind) {
                "monthly" -> assertDays(c, service.monthlyCalendar(c.int("year"), c.int("month"), coordinates, params))
                "annual" -> assertMonths(c, service.annualCalendar(c.int("year"), coordinates, params))
                "range" -> {
                    val range = service.rangeCalendar(Fixtures.date(c.str("start")), Fixtures.date(c.str("end")), coordinates, params)
                    assertEquals(c.int("count"), range.size, "count")
                    JsonAssert.assertSameJson(c.getValue("first"), day(range.first()), "first")
                    JsonAssert.assertSameJson(c.getValue("last"), day(range.last()), "last")
                }
                "rangeError" -> {
                    assertEquals("ArgumentError", c.str("error"))
                    assertThrows<IllegalArgumentException> {
                        service.rangeCalendar(Fixtures.date(c.str("start")), Fixtures.date(c.str("end")), coordinates, params)
                    }
                }
                "hijriMonthly" -> assertDays(c, service.monthlyHijriCalendar(c.int("hijriYear"), c.int("hijriMonth"), coordinates, params))
                "hijriAnnual" -> assertMonths(c, service.annualHijriCalendar(c.int("hijriYear"), coordinates, params))
            }
        }
    }.also { check(it.size == Fixtures.load("calendars.json").jsonObject.arr("cases").size) }
}
