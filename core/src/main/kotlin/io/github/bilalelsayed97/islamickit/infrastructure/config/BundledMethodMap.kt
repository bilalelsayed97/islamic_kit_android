package io.github.bilalelsayed97.islamickit.infrastructure.config

import io.github.bilalelsayed97.islamickit.domain.enums.CalculationMethod
import io.github.bilalelsayed97.islamickit.domain.models.CityEntry
import io.github.bilalelsayed97.islamickit.domain.models.CountryInfo

/**
 * Translates the bundled database's `calc_method` column into a
 * [CalculationMethod].
 *
 * > **These ids are not aladhan ids.** The `prayer_times_country_lookups`
 * > table numbers its authorities independently, and the two schemes collide
 * > above 5 — database id 7 is Kuwait while aladhan id 7 is Tehran. Never
 * > pass a `calc_method` value to [CalculationMethod.fromId]; route it
 * > through [methodForBundledId] instead.
 */
object BundledMethodMap {
    private val byId: Map<Int, CalculationMethod> = linkedMapOf(
        1 to CalculationMethod.KARACHI,
        2 to CalculationMethod.ISNA,
        3 to CalculationMethod.MWL,
        4 to CalculationMethod.MAKKAH,
        5 to CalculationMethod.EGYPT,
        6 to CalculationMethod.DUBAI,
        7 to CalculationMethod.KUWAIT,
        8 to CalculationMethod.QATAR,
        9 to CalculationMethod.SINGAPORE,
        10 to CalculationMethod.ALGERIA,
        11 to CalculationMethod.FRANCE,
        12 to CalculationMethod.RUSSIA,
        13 to CalculationMethod.TUNISIA,
        14 to CalculationMethod.TURKEY,
        15 to CalculationMethod.MOROCCO,
        16 to CalculationMethod.JORDAN,
        17 to CalculationMethod.OMAN,
        18 to CalculationMethod.MUNICH,
        19 to CalculationMethod.MALDIVES,
        20 to CalculationMethod.CANADA,
        21 to CalculationMethod.TAJIKISTAN,
        22 to CalculationMethod.VIENNA,
        23 to CalculationMethod.BELGIUM,
        24 to CalculationMethod.SUDAN,
        25 to CalculationMethod.LIBYA,
        26 to CalculationMethod.IRAQ,
        27 to CalculationMethod.LUXEMBOURG,
        28 to CalculationMethod.TEHRAN,
        29 to CalculationMethod.MOONSIGHTING,
        30 to CalculationMethod.CUSTOM,
    )

    /**
     * The method for a database `calc_method` value.
     *
     * Returns `null` for an unknown id so callers can decide whether to fall
     * back; use [methodForBundledIdOrDefault] for the engine's own default.
     */
    @JvmStatic
    fun methodForBundledId(id: Int?): CalculationMethod? = if (id == null) null else byId[id]

    /**
     * As [methodForBundledId], falling back to [CalculationMethod.MWL] — the
     * bucket the database itself assigns to most of the world.
     */
    @JvmStatic
    fun methodForBundledIdOrDefault(id: Int?): CalculationMethod = methodForBundledId(id) ?: CalculationMethod.MWL

    /** Every database id the map understands. */
    @JvmStatic
    val knownIds: Set<Int>
        get() = byId.keys
}

/** The method the database recommends for this country, or `null` when it records none. */
val CountryInfo.calculationMethod: CalculationMethod?
    get() = BundledMethodMap.methodForBundledId(calculationMethodId)

/** The method the database recommends for this city's country, or `null` when it records none. */
val CityEntry.calculationMethod: CalculationMethod?
    get() = BundledMethodMap.methodForBundledId(calculationMethodId)
