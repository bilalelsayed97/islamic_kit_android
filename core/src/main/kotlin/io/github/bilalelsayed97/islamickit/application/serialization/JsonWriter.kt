package io.github.bilalelsayed97.islamickit.application.serialization

import io.github.bilalelsayed97.islamickit.internal.DartMath

/**
 * A dependency-free JSON encoder for the `Map<String, Any?>` trees the aladhan
 * serializers build, matching Dart's `jsonEncode` byte for byte:
 *
 * - objects keep insertion order (build them with `linkedMapOf`);
 * - `Int`/`Long` encode without a fraction, `Double` via Dart's
 *   `double.toString()` (so `18.0` is `"18.0"` — callers that want `18` pass an
 *   `Int`, which is what the aladhan `_numFmt` rule does);
 * - `"`, `\` and control characters are escaped, `/` and non-ASCII text are
 *   left raw;
 * - `null`, `Boolean`, `String`, `Int`, `Long`, `Double`, `Map<String, *>`
 *   and `Iterable<*>` are the only supported value types.
 */
object JsonWriter {
    private val FORM_FEED: Char = 0x0C.toChar()

    /**
     * Encodes [value] as compact JSON, or pretty-printed with [indent] (e.g. `"  "`)
     * when given, mirroring `JsonEncoder.withIndent`.
     *
     * @throws IllegalArgumentException for NaN/infinite doubles or unsupported types.
     */
    @JvmStatic
    @JvmOverloads
    fun encode(value: Any?, indent: String? = null): String {
        val sb = StringBuilder()
        write(sb, value, indent, 0)
        return sb.toString()
    }

    private fun write(sb: StringBuilder, value: Any?, indent: String?, depth: Int) {
        when (value) {
            null -> sb.append("null")
            is Boolean -> sb.append(if (value) "true" else "false")
            is String -> writeString(sb, value)
            is Int -> sb.append(value)
            is Long -> sb.append(value)
            is Double -> writeDouble(sb, value)
            is Map<*, *> -> writeObject(sb, value, indent, depth)
            is Iterable<*> -> writeArray(sb, value, indent, depth)
            else -> throw IllegalArgumentException("Cannot encode ${value::class.java.name} as JSON")
        }
    }

    private fun writeDouble(sb: StringBuilder, value: Double) {
        require(!value.isNaN() && !value.isInfinite()) { "Cannot encode $value as JSON" }
        sb.append(DartMath.doubleToString(value))
    }

    private fun writeObject(sb: StringBuilder, map: Map<*, *>, indent: String?, depth: Int) {
        if (map.isEmpty()) {
            sb.append("{}")
            return
        }
        sb.append('{')
        var first = true
        for ((key, v) in map) {
            require(key is String) { "JSON object keys must be strings, got $key" }
            if (!first) sb.append(',')
            first = false
            newline(sb, indent, depth + 1)
            writeString(sb, key)
            sb.append(':')
            if (indent != null) sb.append(' ')
            write(sb, v, indent, depth + 1)
        }
        newline(sb, indent, depth)
        sb.append('}')
    }

    private fun writeArray(sb: StringBuilder, items: Iterable<*>, indent: String?, depth: Int) {
        if (!items.iterator().hasNext()) {
            sb.append("[]")
            return
        }
        sb.append('[')
        var first = true
        for (item in items) {
            if (!first) sb.append(',')
            first = false
            newline(sb, indent, depth + 1)
            write(sb, item, indent, depth + 1)
        }
        newline(sb, indent, depth)
        sb.append(']')
    }

    private fun newline(sb: StringBuilder, indent: String?, depth: Int) {
        if (indent == null) return
        sb.append('\n')
        repeat(depth) { sb.append(indent) }
    }

    private fun writeString(sb: StringBuilder, s: String) {
        sb.append('"')
        for (ch in s) {
            when (ch) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                '\b' -> sb.append("\\b")
                FORM_FEED -> sb.append("\\f")
                else -> if (ch.code < 0x20) {
                    sb.append("\\u")
                    val hex = ch.code.toString(16)
                    repeat(4 - hex.length) { sb.append('0') }
                    sb.append(hex)
                } else {
                    sb.append(ch)
                }
            }
        }
        sb.append('"')
    }
}
