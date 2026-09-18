package io.github.bilalelsayed97.islamickit.infrastructure.config

import io.github.bilalelsayed97.islamickit.domain.enums.AsrSchool
import io.github.bilalelsayed97.islamickit.domain.enums.CalculationMethod

/**
 * Recommended calculation defaults per country, keyed by ISO-3166 alpha-2
 * code. Countries not listed fall back to the Muslim World League method,
 * which is the common default across Europe and much of the world.
 *
 * This is the fallback for when the bundled city database is not open. When
 * it is, prefer the database's own `calc_method` column — see
 * [BundledMethodMap] and `PrayerTimesService.autoParamsForCoordinates`; it
 * covers all 251 countries rather than the subset listed here, and the two
 * agree wherever both have an opinion.
 */
object LocationDefaults {
    private val methodByCountry: Map<String, CalculationMethod> = mapOf(
        // North America
        "US" to CalculationMethod.ISNA,
        "CA" to CalculationMethod.CANADA,
        "MX" to CalculationMethod.ISNA,
        // Gulf / Arabian Peninsula
        "SA" to CalculationMethod.MAKKAH,
        "AE" to CalculationMethod.DUBAI,
        "KW" to CalculationMethod.KUWAIT,
        "QA" to CalculationMethod.QATAR,
        "BH" to CalculationMethod.MAKKAH,
        "OM" to CalculationMethod.OMAN,
        "YE" to CalculationMethod.MAKKAH,
        // Levant / North & East Africa
        "EG" to CalculationMethod.EGYPT,
        "NG" to CalculationMethod.EGYPT,
        "SD" to CalculationMethod.SUDAN,
        "SS" to CalculationMethod.SUDAN,
        "SY" to CalculationMethod.MAKKAH,
        "IQ" to CalculationMethod.IRAQ,
        "LY" to CalculationMethod.LIBYA,
        "JO" to CalculationMethod.JORDAN,
        "DZ" to CalculationMethod.ALGERIA,
        "MA" to CalculationMethod.MOROCCO,
        "EH" to CalculationMethod.MOROCCO,
        "TN" to CalculationMethod.TUNISIA,
        // Iran / Turkey / Russia / Central Asia
        "IR" to CalculationMethod.TEHRAN,
        "TR" to CalculationMethod.TURKEY,
        "RU" to CalculationMethod.RUSSIA,
        "TJ" to CalculationMethod.TAJIKISTAN,
        // South Asia
        "PK" to CalculationMethod.KARACHI,
        "IN" to CalculationMethod.KARACHI,
        "BD" to CalculationMethod.KARACHI,
        "AF" to CalculationMethod.KARACHI,
        "MV" to CalculationMethod.MALDIVES,
        // South-East Asia
        "ID" to CalculationMethod.SINGAPORE,
        "MY" to CalculationMethod.SINGAPORE,
        "SG" to CalculationMethod.SINGAPORE,
        "BN" to CalculationMethod.JAKIM,
        "VN" to CalculationMethod.MAKKAH,
        // Europe with dedicated authorities
        "FR" to CalculationMethod.FRANCE,
        "MF" to CalculationMethod.FRANCE,
        "PT" to CalculationMethod.PORTUGAL,
        "DE" to CalculationMethod.MUNICH,
        "AT" to CalculationMethod.VIENNA,
        "BE" to CalculationMethod.BELGIUM,
        "LU" to CalculationMethod.LUXEMBOURG,
    )

    /** Countries where the Hanafi Asr shadow (factor 2) is the common default. */
    private val hanafiDefault: Set<String> = setOf("PK", "IN", "BD", "AF", "TR")

    /** The recommended [CalculationMethod] for [countryCode] (falls back to MWL). */
    @JvmStatic
    fun methodForCountry(countryCode: String): CalculationMethod =
        methodByCountry[countryCode.uppercase()] ?: CalculationMethod.MWL

    /** The recommended [AsrSchool] for [countryCode] (falls back to Standard). */
    @JvmStatic
    fun schoolForCountry(countryCode: String): AsrSchool =
        if (hanafiDefault.contains(countryCode.uppercase())) AsrSchool.HANAFI else AsrSchool.STANDARD
}
