package io.github.bilalelsayed97.islamickit.domain.models

/** An IANA timezone offered for a country, with its Arabic label. Equality is by ([ianaId], [countryId]). */
class TimeZoneInfo @JvmOverloads constructor(
    /** IANA timezone identifier, e.g. `"America/Chicago"`. */
    val ianaId: String,
    /** Owning country's primary key in the bundled database. */
    val countryId: Int,
    /** Arabic zone label, e.g. `"التوقيت الرسمي المركزي"`. `null` when unknown. */
    val nameAr: String? = null,
) {
    override fun equals(other: Any?): Boolean =
        other is TimeZoneInfo && other.ianaId == ianaId && other.countryId == countryId

    override fun hashCode(): Int = 31 * ianaId.hashCode() + countryId

    override fun toString(): String = "TimeZoneInfo($ianaId)"
}
