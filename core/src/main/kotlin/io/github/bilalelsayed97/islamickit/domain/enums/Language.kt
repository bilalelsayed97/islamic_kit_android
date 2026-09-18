package io.github.bilalelsayed97.islamickit.domain.enums

/** Supported localization languages for prayer, month and weekday names. */
enum class Language : Localized {
    /** English. */
    EN,

    /** Arabic. */
    AR;

    override fun title(language: Language): String = EnumStrings.title(this, language)

    override fun description(language: Language): String = EnumStrings.description(this, language)
}
