package io.github.bilalelsayed97.islamickit.domain.enums

/**
 * Adjustment applied to Fajr/Isha/Imsak/Maghrib at high latitudes where the
 * sun may not reach the required twilight angle.
 */
enum class HighLatitudeRule(
    /**
     * Integer value accepted by the aladhan `latitudeAdjustmentMethod` parameter.
     * ([NONE] has no aladhan integer; it is represented as `0` here.)
     */
    val aladhanId: Int,
    /** String value echoed in the aladhan `meta.latitudeAdjustmentMethod` field. */
    val metaValue: String,
) : Localized {
    /** No adjustment. */
    NONE(0, "NONE"),

    /** The night is split in half (middle of the night). */
    MIDDLE_OF_NIGHT(1, "MIDDLE_OF_THE_NIGHT"),

    /** One-seventh of the night. */
    ONE_SEVENTH(2, "ONE_SEVENTH"),

    /** A portion of the night proportional to the twilight angle (angle / 60). */
    ANGLE_BASED(3, "ANGLE_BASED");

    override fun title(language: Language): String = EnumStrings.title(this, language)

    override fun description(language: Language): String = EnumStrings.description(this, language)

    companion object {
        /** Looks a rule up by aladhan id, falling back to [ANGLE_BASED]. */
        @JvmStatic
        fun fromAladhanId(id: Int): HighLatitudeRule =
            entries.firstOrNull { it.aladhanId == id } ?: ANGLE_BASED
    }
}
