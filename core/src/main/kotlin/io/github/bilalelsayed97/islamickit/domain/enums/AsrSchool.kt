package io.github.bilalelsayed97.islamickit.domain.enums

/** Juristic school that determines the shadow ratio used for Asr. */
enum class AsrSchool(
    /** Integer value accepted by the aladhan `school` query parameter. */
    val aladhanId: Int,
    /** String value echoed in the aladhan `meta.school` field. */
    val metaValue: String,
    /** The Asr shadow-length multiplier. */
    val shadowFactor: Int,
) : Localized {
    /** Shafi'i, Maliki, Hanbali — Asr begins at shadow factor 1. */
    STANDARD(0, "STANDARD", 1),

    /** Hanafi — Asr begins at shadow factor 2. */
    HANAFI(1, "HANAFI", 2);

    override fun title(language: Language): String = EnumStrings.title(this, language)

    override fun description(language: Language): String = EnumStrings.description(this, language)

    companion object {
        /** Looks a school up by aladhan id, falling back to [STANDARD]. */
        @JvmStatic
        fun fromAladhanId(id: Int): AsrSchool = entries.firstOrNull { it.aladhanId == id } ?: STANDARD
    }
}
