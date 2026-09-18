package io.github.bilalelsayed97.islamickit.domain.models

import io.github.bilalelsayed97.islamickit.domain.enums.AsrSchool
import io.github.bilalelsayed97.islamickit.domain.enums.CalculationMethod
import io.github.bilalelsayed97.islamickit.domain.enums.HighLatitudeRule
import io.github.bilalelsayed97.islamickit.domain.enums.MidnightMode
import io.github.bilalelsayed97.islamickit.domain.enums.Prayer
import io.github.bilalelsayed97.islamickit.domain.enums.Shafaq
import io.github.bilalelsayed97.islamickit.domain.valueobjects.Coordinates
import io.github.bilalelsayed97.islamickit.domain.valueobjects.MethodParams

/** Echo of the settings used for a calculation (mirrors aladhan `meta`). */
data class CalculationMeta(
    val coordinates: Coordinates,
    /** Timezone label (an IANA name if the caller supplied one, else `UTC±HH:MM`). */
    val timezone: String,
    val method: CalculationMethod,
    /**
     * The effective params (equals `method.params` unless a custom method or an
     * overriding shadow/interval was supplied).
     */
    val methodParams: MethodParams,
    val school: AsrSchool,
    val midnightMode: MidnightMode,
    val latitudeAdjustmentMethod: HighLatitudeRule,
    val shafaq: Shafaq,
    /** Per-prayer tuning offsets in minutes. */
    val offsets: Map<Prayer, Int>,
)
