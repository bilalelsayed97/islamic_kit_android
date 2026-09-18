package io.github.bilalelsayed97.islamickit

import io.github.bilalelsayed97.islamickit.application.serialization.JsonWriter
import io.github.bilalelsayed97.islamickit.internal.DartMath
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class JsonWriterTest {
    @Test
    fun `keeps insertion order and encodes primitives like Dart`() {
        val value = linkedMapOf<String, Any?>(
            "z" to 1,
            "a" to "text",
            "n" to null,
            "b" to true,
            "d" to 18.5,
            "w" to 18.0,
            "l" to 1398297600L,
            "list" to listOf(1, "two", 3.0, emptyList<Int>(), emptyMap<String, Int>()),
            "nested" to linkedMapOf("k" to linkedMapOf("deep" to "v")),
        )
        assertEquals(
            "{\"z\":1,\"a\":\"text\",\"n\":null,\"b\":true,\"d\":18.5,\"w\":18.0,\"l\":1398297600," +
                "\"list\":[1,\"two\",3.0,[],{}],\"nested\":{\"k\":{\"deep\":\"v\"}}}",
            JsonWriter.encode(value),
        )
    }

    @Test
    fun `escapes like Dart jsonEncode`() {
        assertEquals("\"a\\\"b\\\\c/d\"", JsonWriter.encode("a\"b\\c/d"))
        assertEquals("\"\\n\\r\\t\\b\\f\\u0001\\u001f\"", JsonWriter.encode("\n\r\t\b" + 0x0C.toChar() + 0x01.toChar() + 0x1F.toChar()))
        assertEquals("\"الخميس — é\"", JsonWriter.encode("الخميس — é"))
    }

    @Test
    fun `pretty printing mirrors JsonEncoder withIndent`() {
        val value = linkedMapOf<String, Any?>("a" to 1, "b" to listOf(1, 2), "c" to emptyMap<String, Any?>())
        assertEquals(
            "{\n  \"a\": 1,\n  \"b\": [\n    1,\n    2\n  ],\n  \"c\": {}\n}",
            JsonWriter.encode(value, indent = "  "),
        )
    }

    @Test
    fun `rejects NaN and unsupported types`() {
        assertThrows<IllegalArgumentException> { JsonWriter.encode(Double.NaN) }
        assertThrows<IllegalArgumentException> { JsonWriter.encode(Double.POSITIVE_INFINITY) }
        assertThrows<IllegalArgumentException> { JsonWriter.encode(Any()) }
        assertThrows<IllegalArgumentException> { JsonWriter.encode(mapOf(1 to 2)) }
    }

    @Test
    fun `output is valid JSON for kotlinx parsing`() {
        val text = JsonWriter.encode(linkedMapOf("code" to 200, "data" to linkedMapOf("x" to 1.5, "s" to "q\"")))
        val parsed: JsonObject = Json.parseToJsonElement(text).jsonObject
        assertEquals("200", parsed["code"]!!.jsonPrimitive.content)
        assertEquals("q\"", parsed["data"]!!.jsonObject["s"]!!.jsonPrimitive.content)
        assertEquals(listOf("code", "data"), parsed.keys.toList())
    }

    @TestFactory
    fun `Dart double toString`(): List<DynamicTest> {
        val cases = linkedMapOf(
            3.95 to "3.95",
            13.0 to "13.0",
            -0.5 to "-0.5",
            0.1 to "0.1",
            1e-5 to "0.00001",
            1e-6 to "0.000001",
            1e-7 to "1e-7",
            1.5e-7 to "1.5e-7",
            1e16 to "10000000000000000.0",
            1e20 to "100000000000000000000.0",
            1e21 to "1e+21",
            1.25e22 to "1.25e+22",
            51.508515 to "51.508515",
            39.70421229999999 to "39.70421229999999",
            -86.39943869999999 to "-86.39943869999999",
            123456789.0 to "123456789.0",
            0.30000000000000004 to "0.30000000000000004",
            100.0 to "100.0",
            0.0 to "0.0",
            -0.0 to "-0.0",
            2.5e-3 to "0.0025",
            1234567.0 to "1234567.0",
            12345678.9 to "12345678.9",
        )
        return cases.map { (value, expected) ->
            DynamicTest.dynamicTest("$value -> $expected") {
                assertEquals(expected, DartMath.doubleToString(value))
            }
        }
    }

    @Test
    fun `Dart round is half away from zero`() {
        assertEquals(1L, DartMath.round(0.5))
        assertEquals(-1L, DartMath.round(-0.5))
        assertEquals(2L, DartMath.round(1.5))
        assertEquals(-2L, DartMath.round(-1.5))
        assertEquals(0L, DartMath.round(0.49999999999999994))
        assertEquals(2L, DartMath.round(2.4999999999999996))
        assertEquals(3L, DartMath.round(2.5))
        assertEquals(14220000L, DartMath.round(3.95 * 3_600_000.0))
        assertThrows<IllegalArgumentException> { DartMath.round(Double.NaN) }
    }
}
