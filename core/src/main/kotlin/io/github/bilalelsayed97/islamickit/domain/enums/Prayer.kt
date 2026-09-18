package io.github.bilalelsayed97.islamickit.domain.enums

/**
 * The prayers and derived times the library computes.
 *
 * [key] is the canonical identifier used in the aladhan-compatible JSON model
 * (e.g. `"Fajr"`). Entries are declared in the Dart package's order, which is
 * the order every timings map iterates in.
 */
enum class Prayer(
    /** Canonical aladhan JSON key, e.g. `"Fajr"`. */
    val key: String,
    /** English display name. */
    val nameEn: String,
    /** Arabic display name. */
    val nameAr: String,
) : Localized {
    IMSAK("Imsak", "Imsak", "الإمساك"),
    FAJR("Fajr", "Fajr", "الفجر"),
    SUNRISE("Sunrise", "Sunrise", "الشروق"),
    DHUHR("Dhuhr", "Dhuhr", "الظهر"),
    ASR("Asr", "Asr", "العصر"),
    SUNSET("Sunset", "Sunset", "الغروب"),
    MAGHRIB("Maghrib", "Maghrib", "المغرب"),
    ISHA("Isha", "Isha", "العشاء"),
    MIDNIGHT("Midnight", "Midnight", "منتصف الليل"),
    FIRST_THIRD("Firstthird", "First Third", "الثلث الأول"),
    LAST_THIRD("Lastthird", "Last Third", "الثلث الأخير");

    /** Localized display name for [language]. */
    fun localizedName(language: Language): String = if (language == Language.AR) nameAr else nameEn

    override fun title(language: Language): String = EnumStrings.title(this, language)

    override fun description(language: Language): String = EnumStrings.description(this, language)

    companion object {
        /**
         * Looks a prayer up by its aladhan key.
         *
         * @throws IllegalArgumentException when [key] is not a known prayer key.
         */
        @JvmStatic
        fun fromKey(key: String): Prayer =
            entries.firstOrNull { it.key == key }
                ?: throw IllegalArgumentException("Unknown prayer key: $key")

        /**
         * The five obligatory daily prayers, in chronological order.
         *
         * Used to determine the next prayer.
         */
        @JvmField
        val DAILY_OBLIGATORY: List<Prayer> = listOf(FAJR, DHUHR, ASR, MAGHRIB, ISHA)
    }
}
