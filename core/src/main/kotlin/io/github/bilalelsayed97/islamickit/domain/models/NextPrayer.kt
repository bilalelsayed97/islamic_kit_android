package io.github.bilalelsayed97.islamickit.domain.models

import io.github.bilalelsayed97.islamickit.domain.enums.Prayer
import io.github.bilalelsayed97.islamickit.time.CivilDate

/** The next upcoming prayer relative to a given instant. */
data class NextPrayer(
    val prayer: Prayer,
    val time: PrayerTime,
    /** The civil date the next prayer falls on (may be the following day). */
    val onDate: CivilDate,
)
