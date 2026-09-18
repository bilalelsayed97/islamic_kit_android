package io.github.bilalelsayed97.islamickit

import io.github.bilalelsayed97.islamickit.application.PrayerTimesService
import io.github.bilalelsayed97.islamickit.application.serialization.AladhanSerializer
import io.github.bilalelsayed97.islamickit.application.serialization.JsonWriter
import io.github.bilalelsayed97.islamickit.application.serialization.toAladhanJson
import io.github.bilalelsayed97.islamickit.application.usecases.QiblaCalculator
import io.github.bilalelsayed97.islamickit.domain.enums.CalculationMethod
import io.github.bilalelsayed97.islamickit.domain.enums.Prayer
import io.github.bilalelsayed97.islamickit.domain.enums.TimeFormat
import io.github.bilalelsayed97.islamickit.domain.valueobjects.CalculationParameters
import io.github.bilalelsayed97.islamickit.domain.valueobjects.Coordinates
import io.github.bilalelsayed97.islamickit.time.CivilDate
import io.github.bilalelsayed97.islamickit.time.UtcOffset
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** Port of `test/aladhan_serialization_test.dart` plus key-order checks. */
class AladhanSerializationTest {
    private val service = PrayerTimesService()
    private val london = Coordinates(51.508515, -0.1254872)
    private val params = CalculationParameters(
        method = CalculationMethod.ISNA,
        utcOffset = UtcOffset.ofHours(1),
        timezoneName = "Europe/London",
    )

    @Suppress("UNCHECKED_CAST")
    private fun Any?.asMap(): Map<String, Any?> = this as Map<String, Any?>

    @Test
    fun `single-date envelope matches aladhan shape`() {
        val json = service.timings(CivilDate(2014, 4, 24), london, params).toAladhanJson()

        assertEquals(200, json["code"])
        assertEquals("OK", json["status"])
        assertEquals(listOf("code", "status", "data"), json.keys.toList())

        val data = json["data"].asMap()
        assertEquals(listOf("timings", "date", "meta"), data.keys.toList())
        val timings = data["timings"].asMap()
        assertEquals("03:57", timings["Fajr"])
        assertEquals("13:00", timings["Dhuhr"])
        assertTrue(timings.containsKey("Firstthird"))
        assertEquals(Prayer.entries.map { it.key }, timings.keys.toList())

        val date = data["date"].asMap()
        assertEquals(listOf("readable", "timestamp", "hijri", "gregorian"), date.keys.toList())
        val gregorian = date["gregorian"].asMap()
        assertEquals("24-04-2014", gregorian["date"])
        assertEquals(4, gregorian["month"].asMap()["number"])
        assertIs<String>(date["timestamp"])

        val hijri = date["hijri"].asMap()
        assertTrue(hijri["weekday"].asMap().containsKey("ar"))
        assertIs<Int>(hijri["month"].asMap()["days"])

        val meta = data["meta"].asMap()
        assertEquals(2, meta["method"].asMap()["id"])
        assertEquals("STANDARD", meta["school"])
        assertEquals("MIDDLE_OF_THE_NIGHT", meta["latitudeAdjustmentMethod"])
        assertEquals("JAFARI", meta["midnightMode"])
        val offset = meta["offset"].asMap()
        assertEquals(9, offset.size)
        assertEquals(
            listOf("Imsak", "Fajr", "Sunrise", "Dhuhr", "Asr", "Sunset", "Maghrib", "Isha", "Midnight"),
            offset.keys.toList(),
        )
    }

    @Test
    fun `calendar timings carry the timezone suffix`() {
        val month = service.monthlyCalendar(2014, 4, london, params)
        val json = AladhanSerializer.calendarAladhanJson(month)
        val data = json["data"] as List<*>
        assertEquals(30, data.size)
        val firstTimings = data.first().asMap()["timings"].asMap()
        assertTrue((firstTimings["Fajr"] as String).endsWith("(Europe/London)"))
        val iso = AladhanSerializer.calendarAladhanJson(month, TimeFormat.ISO8601)
        val isoFajr = (iso["data"] as List<*>).first().asMap()["timings"].asMap()["Fajr"] as String
        val h24 = month.first().formatted(Prayer.FAJR)
        assertEquals("2014-04-01T$h24:00+01:00", isoFajr)
    }

    @Test
    fun `annual calendar is keyed by month string`() {
        val year = service.annualCalendar(2014, london, params)
        val json = AladhanSerializer.annualCalendarAladhanJson(year)
        val data = json["data"].asMap()
        assertTrue(data.keys.containsAll(listOf("1", "12")))
        assertEquals((1..12).map { "$it" }, data.keys.toList())
    }

    @Test
    fun `methods response exposes MWL with params`() {
        val json = AladhanSerializer.methodsAladhanJson()
        val data = json["data"].asMap()
        val mwl = data["MWL"].asMap()
        assertEquals(3, mwl["id"])
        assertEquals(18, mwl["params"].asMap()["Fajr"])
        assertEquals(17, mwl["params"].asMap()["Isha"])
        assertEquals(CalculationMethod.entries.map { it.code }, data.keys.toList())
        assertEquals(linkedMapOf("id" to 99, "name" to "Custom"), data["CUSTOM"])
    }

    @Test
    fun `makkah serializes Isha as 90 min`() {
        val json = AladhanSerializer.methodsAladhanJson()
        val data = json["data"].asMap()
        val makkah = data["MAKKAH"].asMap()
        assertEquals("90 min", makkah["params"].asMap()["Isha"])
        assertEquals(18.5, makkah["params"].asMap()["Fajr"])
    }

    @Test
    fun `tehran serializes its Maghrib angle and Jafari midnight`() {
        val tehran = AladhanSerializer.methodsAladhanJson()["data"].asMap()["TEHRAN"].asMap()["params"].asMap()
        assertEquals(listOf("Fajr", "Isha", "Maghrib", "Midnight"), tehran.keys.toList())
        assertEquals(17.7, tehran["Fajr"])
        assertEquals(14, tehran["Isha"])
        assertEquals(4.5, tehran["Maghrib"])
        assertEquals("JAFARI", tehran["Midnight"])
    }

    @Test
    fun `moonsighting serializes shafaq and NONE latitude rule`() {
        val r = service.timings(CivilDate(2014, 4, 24), london, params.copy(method = CalculationMethod.MOONSIGHTING))
        val meta = r.toAladhanJson()["data"].asMap()["meta"].asMap()
        assertEquals(linkedMapOf("shafaq" to "general"), meta["method"].asMap()["params"])
        assertEquals("NONE", meta["latitudeAdjustmentMethod"])
    }

    @Test
    fun `next prayer response keeps timings in first position with a single entry`() {
        val r = service.timings(CivilDate(2014, 4, 24), london, params)
        val json = AladhanSerializer.nextPrayerAladhanJson(r, Prayer.ASR)
        val data = json["data"].asMap()
        assertEquals(listOf("timings", "date", "meta"), data.keys.toList())
        assertEquals(linkedMapOf("Asr" to "16:56"), data["timings"])
    }

    @Test
    fun `qibla response`() {
        val q = QiblaCalculator().direction(Coordinates(51.5073509, -0.1277583))
        val json = AladhanSerializer.qiblaAladhanJson(q)
        val data = json["data"].asMap()
        assertEquals(51.5073509, data["latitude"])
        assertEquals(-0.1277583, data["longitude"])
        assertEquals(q.degrees, data["direction"])
    }

    @Test
    fun `encodes to the same text Dart jsonEncode produces`() {
        val r = service.timings(CivilDate(2014, 4, 24), london, params)
        val text = JsonWriter.encode(r.toAladhanJson())
        assertTrue(text.startsWith("{\"code\":200,\"status\":\"OK\",\"data\":{\"timings\":{\"Imsak\":\"03:47\",\"Fajr\":\"03:57\""))
        assertTrue(text.contains("\"latitude\":51.508515,\"longitude\":-0.1254872,\"timezone\":\"Europe/London\""))
        assertTrue(text.contains("\"location\":{\"latitude\":39.70421229999999,\"longitude\":-86.39943869999999}"))
        assertTrue(text.contains("\"weekday\":{\"en\":\"Thursday\",\"ar\":\"الخميس\"}"))
        assertTrue(text.contains("\"adjustedHolidays\":[],\"method\":\"HJCoSA\""))
        assertTrue(text.contains("\"lunarSighting\":false"))
    }
}
