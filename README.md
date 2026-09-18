<p align="center">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="doc/header-dark.svg">
    <img alt="islamic_kit_plus" src="doc/header.svg" width="720">
  </picture>
</p>

# islamic_kit_android

Native Kotlin port of [`islamic_kit_plus`](https://github.com/bilalelsayed97/islamic_kit_plus):
offline prayer times (Jean Meeus solar position), a four-method Hijri calendar,
qibla, calendars, aladhan-compatible JSON and English/Arabic labels.

Two Gradle modules:

- `:core` — pure Kotlin/JVM (no Android SDK). Everything except the city database.
- `:geocoding` — Android library bundling the 37 MB city database with a
  SQLite `Geocoder` and `CityDirectory`.

```kotlin
// settings.gradle.kts
dependencyResolutionManagement { repositories { maven("https://jitpack.io") } }

// build.gradle.kts
implementation("com.github.bilalelsayed97.islamic_kit_android:core:0.3.0")
implementation("com.github.bilalelsayed97.islamic_kit_android:geocoding:0.3.0")
```

```kotlin
val service = PrayerTimesService()
val result = service.timings(
    CivilDate(2014, 4, 24),
    Coordinates(51.508515, -0.1254872),
    CalculationParameters(method = CalculationMethod.ISNA, utcOffset = UtcOffset.ofHours(1)),
)
result.formatted(Prayer.FAJR) // "03:57"
```

Behavioural parity with the Dart package (same numbers, strings, JSON shape and
fallbacks) is verified by the ported test suite. Licensed under GPL-3.0.
