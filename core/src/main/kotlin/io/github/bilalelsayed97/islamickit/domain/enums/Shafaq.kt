package io.github.bilalelsayed97.islamickit.domain.enums

/**
 * Twilight (shafaq) definition used by the Moonsighting Committee method for
 * computing Isha.
 */
enum class Shafaq(
    /** The aladhan `shafaq` identifier. */
    val code: String,
) : Localized {
    /** General twilight. */
    GENERAL("general"),

    /** Red twilight (shafaq al-ahmar). */
    AHMER("ahmer"),

    /** White twilight (shafaq al-abyad). */
    ABYAD("abyad");

    override fun title(language: Language): String = EnumStrings.title(this, language)

    override fun description(language: Language): String = EnumStrings.description(this, language)

    companion object {
        /** Looks a shafaq up by code, falling back to [GENERAL]. */
        @JvmStatic
        fun fromCode(code: String): Shafaq = entries.firstOrNull { it.code == code } ?: GENERAL
    }
}
