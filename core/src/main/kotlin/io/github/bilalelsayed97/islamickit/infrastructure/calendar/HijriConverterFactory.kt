package io.github.bilalelsayed97.islamickit.infrastructure.calendar

import io.github.bilalelsayed97.islamickit.domain.enums.CalendarMethod
import io.github.bilalelsayed97.islamickit.domain.ports.HijriConverter

/** Creates the [HijriConverter] for a given [CalendarMethod]. */
class HijriConverterFactory {
    fun create(method: CalendarMethod): HijriConverter = when (method) {
        CalendarMethod.UAQ -> TableHijriConverter.ummAlQura()
        CalendarMethod.DIYANET -> TableHijriConverter.diyanet()
        CalendarMethod.HJCOSA -> HjcosaConverter()
        CalendarMethod.MATHEMATICAL -> MathematicalConverter()
    }
}
