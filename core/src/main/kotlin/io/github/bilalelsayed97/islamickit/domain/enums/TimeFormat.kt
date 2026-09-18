package io.github.bilalelsayed97.islamickit.domain.enums

/** Output format for a computed time. */
enum class TimeFormat(
    /** The aladhan format identifier. */
    val code: String,
) : Localized {
    /** 24-hour clock, e.g. `03:57`. */
    H24("24h"),

    /** 12-hour clock with am/pm suffix, e.g. `3:57 am`. */
    H12("12h"),

    /** 12-hour clock without a suffix, e.g. `3:57`. */
    H12_NO_SUFFIX("12hNS"),

    /** Raw floating-point hours (0..24), e.g. `3.95`. */
    FLOAT("Float"),

    /** ISO-8601 with timezone offset, e.g. `2014-04-24T03:57:00+01:00`. */
    ISO8601("iso8601");

    override fun title(language: Language): String = EnumStrings.title(this, language)

    override fun description(language: Language): String = EnumStrings.description(this, language)
}
