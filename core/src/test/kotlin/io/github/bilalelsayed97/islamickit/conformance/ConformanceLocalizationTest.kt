package io.github.bilalelsayed97.islamickit.conformance

import io.github.bilalelsayed97.islamickit.domain.enums.AsrSchool
import io.github.bilalelsayed97.islamickit.domain.enums.CalculationMethod
import io.github.bilalelsayed97.islamickit.domain.enums.CalendarMethod
import io.github.bilalelsayed97.islamickit.domain.enums.HighLatitudeRule
import io.github.bilalelsayed97.islamickit.domain.enums.Language
import io.github.bilalelsayed97.islamickit.domain.enums.Localized
import io.github.bilalelsayed97.islamickit.domain.enums.MidnightMode
import io.github.bilalelsayed97.islamickit.domain.enums.Prayer
import io.github.bilalelsayed97.islamickit.domain.enums.Shafaq
import io.github.bilalelsayed97.islamickit.domain.enums.TimeFormat
import io.github.bilalelsayed97.islamickit.infrastructure.config.BundledMethodMap
import io.github.bilalelsayed97.islamickit.infrastructure.config.LocationDefaults
import io.github.bilalelsayed97.islamickit.infrastructure.localization.LocalizedName
import io.github.bilalelsayed97.islamickit.infrastructure.localization.Localizer
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import kotlin.test.assertEquals

/**
 * `localization.json`: every enum's ids, codes and EN/AR strings in
 * declaration order, the `Localizer` tables, lookup fallbacks,
 * `LocationDefaults` for every ISO code and the `BundledMethodMap`.
 */
class ConformanceLocalizationTest {
    private val fixture: JsonObject
        get() = Fixtures.load("localization.json").jsonObject

    /** Dart enum identifiers are camelCase; the Kotlin entries are UPPER_SNAKE. */
    private fun dartNameToKotlin(name: String): String =
        name.replace(Regex("([a-z0-9])([A-Z])"), "$1_$2").uppercase()

    private fun assertL10n(expected: JsonObject, value: Localized, kotlinName: String) {
        assertEquals(kotlinName, dartNameToKotlin(expected.str("name")), "enum entry name")
        assertEquals(expected.str("titleEn"), value.title(Language.EN), "titleEn")
        assertEquals(expected.str("titleAr"), value.title(Language.AR), "titleAr")
        assertEquals(expected.str("descriptionEn"), value.description(Language.EN), "descriptionEn")
        assertEquals(expected.str("descriptionAr"), value.description(Language.AR), "descriptionAr")
    }

    private fun <E> enumTests(
        key: String,
        entries: List<E>,
        extra: (JsonObject, E) -> Unit,
    ): List<DynamicNode> where E : Enum<E>, E : Localized {
        val expected = fixture.arr(key).map { it.jsonObject }
        assertEquals(expected.size, entries.size, "$key count")
        return expected.indices.map { i ->
            dynamicTest("$key[$i] ${expected[i].str("name")}") {
                assertL10n(expected[i], entries[i], entries[i].name)
                extra(expected[i], entries[i])
            }
        }
    }

    @TestFactory
    fun calculationMethods(): List<DynamicNode> = enumTests("calculationMethods", CalculationMethod.entries) { e, m ->
        assertEquals(e.int("id"), m.id, "id")
        assertEquals(e.str("code"), m.code, "code")
        assertEquals(e.str("methodName"), m.methodName, "methodName")
        assertEquals(e.bool("usesMoonsighting"), m.usesMoonsighting, "usesMoonsighting")
        assertEquals(e.bool("isAladhanMethod"), m.isAladhanMethod, "isAladhanMethod")
        JsonAssert.assertSameJson(e.getValue("params"), Fixtures.methodParamsJson(m.params), "params")
    }

    @TestFactory
    fun asrSchools(): List<DynamicNode> = enumTests("asrSchools", AsrSchool.entries) { e, v ->
        assertEquals(e.int("aladhanId"), v.aladhanId, "aladhanId")
        assertEquals(e.str("metaValue"), v.metaValue, "metaValue")
        assertEquals(e.int("shadowFactor"), v.shadowFactor, "shadowFactor")
    }

    @TestFactory
    fun midnightModes(): List<DynamicNode> = enumTests("midnightModes", MidnightMode.entries) { e, v ->
        assertEquals(e.int("aladhanId"), v.aladhanId, "aladhanId")
        assertEquals(e.str("metaValue"), v.metaValue, "metaValue")
    }

    @TestFactory
    fun highLatitudeRules(): List<DynamicNode> = enumTests("highLatitudeRules", HighLatitudeRule.entries) { e, v ->
        assertEquals(e.int("aladhanId"), v.aladhanId, "aladhanId")
        assertEquals(e.str("metaValue"), v.metaValue, "metaValue")
    }

    @TestFactory
    fun shafaqs(): List<DynamicNode> = enumTests("shafaqs", Shafaq.entries) { e, v ->
        assertEquals(e.str("code"), v.code, "code")
    }

    @TestFactory
    fun calendarMethods(): List<DynamicNode> = enumTests("calendarMethods", CalendarMethod.entries) { e, v ->
        assertEquals(e.str("code"), v.code, "code")
    }

    @TestFactory
    fun timeFormats(): List<DynamicNode> = enumTests("timeFormats", TimeFormat.entries) { e, v ->
        assertEquals(e.str("code"), v.code, "code")
    }

    @TestFactory
    fun prayers(): List<DynamicNode> = enumTests("prayers", Prayer.entries) { e, v ->
        assertEquals(e.str("key"), v.key, "key")
        assertEquals(e.str("nameEn"), v.nameEn, "nameEn")
        assertEquals(e.str("nameAr"), v.nameAr, "nameAr")
    }

    @TestFactory
    fun languages(): List<DynamicNode> = enumTests("languages", Language.entries) { _, _ -> }

    @TestFactory
    fun localizer(): List<DynamicNode> {
        val expected = fixture.obj("localizer")
        fun names(table: Map<Int, LocalizedName>): Map<String, Any?> {
            val out = LinkedHashMap<String, Any?>()
            for ((k, v) in table) out["$k"] = linkedMapOf("en" to v.en, "ar" to v.ar)
            return out
        }
        val tables = linkedMapOf(
            "islamicMonths" to Localizer.islamicMonths,
            "hijriWeekdays" to Localizer.hijriWeekdays,
            "gregorianMonths" to Localizer.gregorianMonths,
            "gregorianWeekdays" to Localizer.gregorianWeekdays,
        )
        val nodes = tables.map { (key, table) ->
            dynamicTest(key) { JsonAssert.assertSameJson(expected.getValue(key), names(table), key) }
        }
        return nodes + dynamicTest("monthAbbrEn") {
            JsonAssert.assertSameJson(expected.getValue("monthAbbrEn"), Localizer.monthAbbrEn, "monthAbbrEn")
        }
    }

    @Test
    fun fallbacks() {
        val actual = linkedMapOf(
            "calculationMethodFromId(-1)" to CalculationMethod.fromId(-1).code,
            "calculationMethodFromCode(NOPE)" to CalculationMethod.fromCode("NOPE").code,
            "asrSchoolFromAladhanId(9)" to AsrSchool.fromAladhanId(9).metaValue,
            "midnightModeFromAladhanId(9)" to MidnightMode.fromAladhanId(9).metaValue,
            "highLatitudeRuleFromAladhanId(9)" to HighLatitudeRule.fromAladhanId(9).metaValue,
            "shafaqFromCode(x)" to Shafaq.fromCode("x").code,
            "calendarMethodFromCode(x)" to CalendarMethod.fromCode("x").code,
        )
        JsonAssert.assertSameJson(fixture.getValue("fallbacks"), actual, "fallbacks")
    }

    @TestFactory
    fun locationDefaults(): List<DynamicNode> = fixture.obj("locationDefaults").map { (iso, expected) ->
        dynamicTest(iso) {
            val actual = linkedMapOf(
                "method" to LocationDefaults.methodForCountry(iso).code,
                "school" to LocationDefaults.schoolForCountry(iso).metaValue,
            )
            JsonAssert.assertSameJson(expected, actual, iso)
        }
    }

    @Test
    fun bundledMethodMap() {
        val expected = fixture.obj("bundledMethodMap")
        assertEquals(expected.keys.map { it.toInt() }.toSet(), BundledMethodMap.knownIds, "known ids")
        for ((id, code) in expected) {
            assertEquals(code.string, BundledMethodMap.methodForBundledId(id.toInt())?.code, "id $id")
        }
    }
}
