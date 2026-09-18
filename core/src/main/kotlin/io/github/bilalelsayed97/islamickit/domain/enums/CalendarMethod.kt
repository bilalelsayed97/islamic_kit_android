package io.github.bilalelsayed97.islamickit.domain.enums

/**
 * The Hijri calendar calculation method used for Gregorian <-> Hijri
 * conversion.
 */
enum class CalendarMethod(
    /** The aladhan `calendarMethod` identifier. */
    val code: String,
) : Localized {
    /**
     * High Judicial Council of Saudi Arabia (Umm al-Qura table + announced
     * lunar-sighting overrides). This is the aladhan.com default.
     */
    HJCOSA("HJCoSA"),

    /** Umm al-Qura (table lookup). */
    UAQ("UAQ"),

    /** Diyanet İşleri Başkanlığı (table lookup). */
    DIYANET("DIYANET"),

    /** Pure arithmetic (tabular) calendar; supports a day adjustment. */
    MATHEMATICAL("MATHEMATICAL");

    override fun title(language: Language): String = EnumStrings.title(this, language)

    override fun description(language: Language): String = EnumStrings.description(this, language)

    companion object {
        /** Looks a method up by code, falling back to [HJCOSA]. */
        @JvmStatic
        fun fromCode(code: String): CalendarMethod = entries.firstOrNull { it.code == code } ?: HJCOSA
    }
}
