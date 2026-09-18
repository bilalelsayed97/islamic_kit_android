package io.github.bilalelsayed97.islamickit.internal

/**
 * String helpers that reproduce Dart's Unicode-aware `trim()` and the
 * ECMAScript `\s` class behind `RegExp(r'\s+')` (Java's `\s` is ASCII-only).
 */
internal object DartStrings {
    /** Characters Dart's `String.trim()` strips. */
    private val TRIM_SET: Set<Char> = buildSet {
        for (code in 0x0009..0x000D) add(code.toChar())
        add(0x0020.toChar())
        add(0x0085.toChar())
        add(0x00A0.toChar())
        add(0x1680.toChar())
        for (code in 0x2000..0x200A) add(code.toChar())
        add(0x2028.toChar())
        add(0x2029.toChar())
        add(0x202F.toChar())
        add(0x205F.toChar())
        add(0x3000.toChar())
        add(0xFEFF.toChar())
    }

    /** The ECMAScript `\s` class, one or more times. */
    val WHITESPACE: Regex = Regex(
        "[\\t\\n\\u000B\\f\\r \\u00A0\\u1680\\u2000-\\u200A\\u2028\\u2029\\u202F\\u205F\\u3000\\uFEFF]+",
    )

    /** Dart `String.trim()`. */
    fun String.dartTrim(): String {
        var start = 0
        var end = length
        while (start < end && this[start] in TRIM_SET) start++
        while (end > start && this[end - 1] in TRIM_SET) end--
        return substring(start, end)
    }

    /** `n < 10 ? "0n" : "n"` — the two-digit padding used throughout. */
    fun two(n: Int): String = if (n < 10) "0$n" else "$n"

    /** Dart `year.toString().padLeft(4, '0')`. */
    fun padYear(year: Int): String = year.toString().padStart(4, '0')
}
