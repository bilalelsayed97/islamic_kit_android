package io.github.bilalelsayed97.islamickit.domain.enums

/** End-user localization (title + description) shared by every enum. */
interface Localized {
    /** Short UI label for [language]. */
    fun title(language: Language): String

    /** One-sentence description for [language]. */
    fun description(language: Language): String
}
