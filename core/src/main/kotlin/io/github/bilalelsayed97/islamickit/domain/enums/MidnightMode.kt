package io.github.bilalelsayed97.islamickit.domain.enums

/** How the Midnight (and the night thirds) are anchored. */
enum class MidnightMode(
    /** Integer value accepted by the aladhan `midnightMode` query parameter. */
    val aladhanId: Int,
    /** String value echoed in the aladhan `meta.midnightMode` field. */
    val metaValue: String,
) : Localized {
    /** Midpoint of Sunset to Sunrise. */
    STANDARD(0, "STANDARD"),

    /** Midpoint of Sunset to Fajr (Shia / Jafari). */
    JAFARI(1, "JAFARI");

    override fun title(language: Language): String = EnumStrings.title(this, language)

    override fun description(language: Language): String = EnumStrings.description(this, language)

    companion object {
        /** Looks a mode up by aladhan id, falling back to [STANDARD]. */
        @JvmStatic
        fun fromAladhanId(id: Int): MidnightMode = entries.firstOrNull { it.aladhanId == id } ?: STANDARD
    }
}
