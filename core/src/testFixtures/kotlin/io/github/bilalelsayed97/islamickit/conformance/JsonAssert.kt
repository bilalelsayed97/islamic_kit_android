package io.github.bilalelsayed97.islamickit.conformance

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Structural comparison of a fixture [JsonElement] against a Kotlin value tree
 * (`Map<String, *>` / `Iterable` / `String` / `Int` / `Long` / `Double` /
 * `Boolean` / `null`), with two rules that a plain equality check would miss:
 *
 * - object **key order** must match (the aladhan JSON leaks it);
 * - a fixture number token without `.`, `e` or `E` was a Dart `int` and must
 *   be matched by an `Int`/`Long`; any other token was a `double` and must be
 *   matched exactly by a `Double`.
 *
 * Framework-agnostic: failures are plain [AssertionError]s so the same helper
 * serves JUnit 5 (`:core`) and JUnit 4 (`:geocoding`).
 */
object JsonAssert {
    /** Asserts [actual] is structurally identical to [expected]; [path] prefixes failure messages. */
    fun assertSameJson(expected: JsonElement, actual: Any?, path: String = "$") {
        when (expected) {
            is JsonNull -> if (actual != null) fail(path, "null", actual)
            is JsonObject -> assertObject(expected, actual, path)
            is JsonArray -> assertArray(expected, actual, path)
            is JsonPrimitive -> assertPrimitive(expected, actual, path)
        }
    }

    private fun assertObject(expected: JsonObject, actual: Any?, path: String) {
        if (actual !is Map<*, *>) fail(path, "object", actual)
        val expectedKeys = expected.keys.toList()
        val actualKeys = actual.keys.map { it.toString() }
        if (expectedKeys != actualKeys) {
            throw AssertionError("$path: key order differs\n  expected: $expectedKeys\n  actual:   $actualKeys")
        }
        for (key in expectedKeys) assertSameJson(expected.getValue(key), actual[key], "$path.$key")
    }

    private fun assertArray(expected: JsonArray, actual: Any?, path: String) {
        if (actual !is Iterable<*>) fail(path, "array", actual)
        val items = actual.toList()
        if (items.size != expected.size) {
            throw AssertionError("$path: array size differs, expected ${expected.size} but was ${items.size}")
        }
        for (i in expected.indices) assertSameJson(expected[i], items[i], "$path[$i]")
    }

    private fun assertPrimitive(expected: JsonPrimitive, actual: Any?, path: String) {
        val token = expected.content
        when {
            expected.isString -> if (actual != token) fail(path, "\"$token\"", actual)
            token == "true" || token == "false" -> if (actual != token.toBoolean()) fail(path, token, actual)
            token.any { it == '.' || it == 'e' || it == 'E' } -> {
                if (actual !is Double) fail(path, "double $token", actual)
                if (actual != token.toDouble()) fail(path, token, actual)
            }
            else -> {
                if (actual !is Int && actual !is Long) fail(path, "int $token", actual)
                if ((actual as Number).toLong() != token.toLong()) fail(path, token, actual)
            }
        }
    }

    private fun fail(path: String, expected: String, actual: Any?): Nothing {
        val shown = when (actual) {
            null -> "null"
            is String -> "\"$actual\""
            else -> "$actual (${actual::class.java.simpleName})"
        }
        throw AssertionError("$path: expected $expected but was $shown")
    }

    /**
     * Asserts two encoded JSON strings are identical, pointing at the first
     * differing character with a little context on failure.
     */
    fun assertSameEncoded(expected: String, actual: String, label: String) {
        if (expected == actual) return
        var i = 0
        while (i < expected.length && i < actual.length && expected[i] == actual[i]) i++
        val from = maxOf(0, i - 60)
        throw AssertionError(
            "$label: encoded JSON differs at index $i\n" +
                "  expected: …${expected.substring(from, minOf(expected.length, i + 60))}…\n" +
                "  actual:   …${actual.substring(from, minOf(actual.length, i + 60))}…",
        )
    }
}
