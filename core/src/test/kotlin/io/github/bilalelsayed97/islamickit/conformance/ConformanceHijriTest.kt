package io.github.bilalelsayed97.islamickit.conformance

import io.github.bilalelsayed97.islamickit.domain.enums.CalendarMethod
import io.github.bilalelsayed97.islamickit.domain.ports.HijriConverter
import io.github.bilalelsayed97.islamickit.infrastructure.calendar.HijriConverterFactory
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

/**
 * `hijri.json`: `fromGregorian` and `toGregorian` for every method across its
 * range (including out-of-range errors and mathematical adjustments), plus the
 * compact daily sweep of 2010–2030.
 */
class ConformanceHijriTest {
    private val factory = HijriConverterFactory()
    private val converters: Map<CalendarMethod, HijriConverter> =
        CalendarMethod.entries.associateWith { factory.create(it) }

    private fun converter(c: JsonObject): HijriConverter =
        converters.getValue(Fixtures.calendarMethodByCode(c.str("method")))

    private val fixture: JsonObject
        get() = Fixtures.load("hijri.json").jsonObject

    @TestFactory
    fun fromGregorian(): List<DynamicNode> =
        fixture.arr("fromGregorian").map { it.jsonObject }.map { c ->
            val adjustment = c.intOrNull("adjustment") ?: 0
            val name = "${c.str("method")} ${c.str("date")}" + (if (adjustment != 0) " adj $adjustment" else "")
            dynamicTest(name) {
                val date = Fixtures.date(c.str("date"))
                if (c.strOrNull("error") != null) {
                    assertThrows<IllegalArgumentException> { converter(c).fromGregorian(date, adjustment) }
                } else {
                    val actual = converter(c).fromGregorian(date, adjustment)
                    for ((key, value) in Fixtures.hijriJson(actual)) {
                        JsonAssert.assertSameJson(c.getValue(key), value, key)
                    }
                }
            }
        }

    @TestFactory
    fun toGregorian(): List<DynamicNode> =
        fixture.arr("toGregorian").map { it.jsonObject }.map { c ->
            val adjustment = c.intOrNull("adjustment") ?: 0
            val name = "${c.str("method")} ${c.str("hijri")}" + (if (adjustment != 0) " adj $adjustment" else "")
            dynamicTest(name) {
                val (y, m, d) = Fixtures.hijriParts(c.str("hijri"))
                if (c.strOrNull("error") != null) {
                    assertThrows<IllegalArgumentException> { converter(c).toGregorian(y, m, d, adjustment) }
                } else {
                    assertEquals(c.str("date"), converter(c).toGregorian(y, m, d, adjustment).toString())
                }
            }
        }

    /** The daily sweep is ~30k rows; grouped per method and Gregorian year to keep the report readable. */
    @TestFactory
    fun daily(): List<DynamicNode> {
        val daily = fixture.obj("daily")
        assertEquals(listOf("method", "date", "hijri", "monthLength"), daily.arr("columns").map { it.string })
        val rows = daily.arr("rows").map { it.jsonArray }
        return rows.groupBy { "${it[0].string} ${it[1].string.substring(0, 4)}" }.map { (group, groupRows) ->
            dynamicTest("$group (${groupRows.size} days)") {
                val converter = converters.getValue(Fixtures.calendarMethodByCode(groupRows.first()[0].string))
                for (row in groupRows) {
                    val date = row[1].string
                    val actual = converter.fromGregorian(Fixtures.date(date))
                    assertEquals(row[2].string, actual.formatted, "$group $date hijri")
                    assertEquals(row[3].string.toInt(), actual.monthLength, "$group $date monthLength")
                }
            }
        }
    }
}
