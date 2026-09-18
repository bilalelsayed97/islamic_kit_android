// Root build: plugins are declared here (not applied) so every module resolves
// the same AGP / Kotlin versions from gradle/libs.versions.toml.
plugins {
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.android) apply false
}

// Maven coordinates. JitPack exports the git tag as VERSION, which overrides
// the VERSION_NAME baked into gradle.properties.
allprojects {
    group = providers.gradleProperty("GROUP").get()
    version = System.getenv("VERSION") ?: providers.gradleProperty("VERSION_NAME").get()
}
