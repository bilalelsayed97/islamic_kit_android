package io.github.bilalelsayed97.islamickit.conformance

import io.github.bilalelsayed97.islamickit.application.PrayerTimesService
import io.github.bilalelsayed97.islamickit.application.serialization.JsonWriter
import io.github.bilalelsayed97.islamickit.application.serialization.toAladhanJson
import io.github.bilalelsayed97.islamickit.domain.enums.Prayer
import io.github.bilalelsayed97.islamickit.domain.enums.TimeFormat
import io.github.bilalelsayed97.islamickit.domain.models.PrayerResult
import io.github.bilalelsayed97.islamickit.domain.valueobjects.Coordinates
import io.github.bilalelsayed97.islamickit.infrastructure.astronomy.SolarTime
import io.github.bilalelsayed97.islamickit.time.CivilDate
import kotlinx.serialization.json.JsonObject
import org.junit.jupiter.api.DynamicContainer.dynamicContainer
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * `timings_matrix.json` (14 places × 8 dates × 34 methods → raw hours) and
 * `timings_detail.json` (hand-picked cases with every derived output).
 */
class ConformanceTimingsTest {
    private val service = PrayerTimesService()

    private fun compute(c: JsonObject): PrayerResult =
        service.timings(Fixtures.date(c.str("date")), Coordinates(c.double("lat"), c.double("lng")), Fixtures.params(c.obj("params")))

    @TestFactory
    fun `timings_matrix - raw hours are exact`(): List<DynamicNode> =
        Fixtures.cases("timings_matrix.json").map { c ->
            dynamicTest(c.str("id")) {
                JsonAssert.assertSameJson(c.obj("raw"), Fixtures.rawJson(compute(c).timings.raw), "raw")
            }
        }

    @TestFactory
    fun `timings_detail - every derived output`(): List<DynamicNode> =
        Fixtures.cases("timings_detail.json").map { c ->
            val id = c.str("id")
            // The generator replaces the case's `date` string with the date
            // block, so the civil date is recovered from its `dd-mm-yyyy` field.
            val date = c.obj("date").str("gregorian").split('-').map { it.toInt() }.let { (d, m, y) -> CivilDate(y, m, d) }
            val coordinates = Coordinates(c.double("lat"), c.double("lng"))
            val params = Fixtures.params(c.obj("params"))
            val result = service.timings(date, coordinates, params)
            val nodes = ArrayList<DynamicNode>()

            nodes += dynamicTest("raw") {
                JsonAssert.assertSameJson(c.obj("raw"), Fixtures.rawJson(result.timings.raw), "raw")
            }

            nodes += dynamicTest("solar (1e-9)") {
                val solar = SolarTime(date, coordinates, params.elevation)
                val actual = linkedMapOf(
                    "transit" to solar.transit,
                    "sunrise" to solar.sunrise,
                    "sunset" to solar.sunset,
                    "asrStandard" to solar.afternoon(1.0),
                    "asrHanafi" to solar.afternoon(2.0),
                )
                for ((key, value) in actual) {
                    val expected = c.obj("solar").doubleOrNull(key)
                    if (expected == null) {
                        assertTrue(value.isNaN(), "solar.$key expected NaN but was $value")
                    } else {
                        assertTrue(abs(value - expected) < 1e-9, "solar.$key expected $expected but was $value")
                    }
                }
            }

            for (format in TimeFormat.entries) {
                nodes += dynamicTest("formatted ${format.code}") {
                    val expected = c.obj("formatted").obj(format.code)
                    for (prayer in Prayer.entries) {
                        assertEquals(expected.str(prayer.key), result.formatted(prayer, format), "${format.code}.${prayer.key}")
                    }
                }
            }

            nodes += dynamicTest("epochMillis") {
                val expected = c.obj("epochMillis")
                for (prayer in Prayer.entries) {
                    val millis = expected[prayer.key]?.takeUnless { it.isNull }?.string?.toLong()
                    assertEquals(millis, result.time(prayer).toEpochMillis(), prayer.key)
                }
            }

            nodes += dynamicTest("date") {
                val d = result.date
                val actual = LinkedHashMap<String, Any?>()
                actual["readable"] = d.readable
                actual["timestamp"] = d.timestamp
                actual["gregorianWeekdayEn"] = d.gregorian.weekdayEn
                actual["gregorianMonthEn"] = d.gregorian.monthEn
                actual["gregorian"] = d.gregorian.formatted
                actual.putAll(Fixtures.hijriJson(d.hijri))
                JsonAssert.assertSameJson(c.obj("date"), actual, "date")
            }

            nodes += dynamicTest("meta") {
                val m = result.meta
                val offsets = LinkedHashMap<String, Any?>()
                for ((prayer, minutes) in m.offsets) offsets[prayer.key] = minutes
                val actual = linkedMapOf(
                    "timezone" to m.timezone,
                    "methodId" to m.method.id,
                    "school" to m.school.metaValue,
                    "midnightMode" to m.midnightMode.metaValue,
                    "latitudeAdjustmentMethod" to m.latitudeAdjustmentMethod.metaValue,
                    "shafaq" to m.shafaq.code,
                    "offsets" to offsets,
                )
                JsonAssert.assertSameJson(c.obj("meta"), actual, "meta")
            }

            nodes += dynamicTest("aladhanJson") {
                JsonAssert.assertSameEncoded(c.str("aladhanJson"), JsonWriter.encode(result.toAladhanJson()), id)
            }
            nodes += dynamicTest("aladhanJsonIso") {
                JsonAssert.assertSameEncoded(c.str("aladhanJsonIso"), JsonWriter.encode(result.toAladhanJson(TimeFormat.ISO8601)), id)
            }
            nodes += dynamicTest("aladhanJson12h") {
                JsonAssert.assertSameEncoded(c.str("aladhanJson12h"), JsonWriter.encode(result.toAladhanJson(TimeFormat.H12)), id)
            }

            dynamicContainer(id, nodes)
        }
}
