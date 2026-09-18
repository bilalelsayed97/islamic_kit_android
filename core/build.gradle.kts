import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// :core is a plain Kotlin/JVM library: no Android SDK, no SQLite. It runs its
// JUnit 5 suite in seconds and is consumable from any JVM.
plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
    `java-test-fixtures`
    `maven-publish`
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    withSourcesJar()
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    api(libs.androidx.annotation)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.kotlinx.serialization.json)
    testRuntimeOnly(libs.junit.platform.launcher)

    testFixturesImplementation(platform(libs.junit.bom))
    testFixturesImplementation(libs.junit.jupiter)
    testFixturesImplementation(libs.kotlin.test)
    // `api`: the fixture helpers expose kotlinx JsonElement in their signatures.
    testFixturesApi(libs.kotlinx.serialization.json)
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

// The test fixtures (conformance helpers shared with :geocoding's tests) are
// build-internal: keep their variants, and their JUnit dependencies, out of
// the published POM.
val javaComponent = components["java"] as AdhocComponentWithVariants
javaComponent.withVariantsFromConfiguration(configurations["testFixturesApiElements"]) { skip() }
javaComponent.withVariantsFromConfiguration(configurations["testFixturesRuntimeElements"]) { skip() }

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "core"
            pom {
                name.set("islamic_kit_android core")
                description.set(
                    "Offline prayer times (Meeus solar position), Hijri calendar, qibla, " +
                        "calendars and aladhan-compatible JSON for the JVM and Android.",
                )
                url.set(providers.gradleProperty("POM_URL"))
                licenses {
                    license {
                        name.set(providers.gradleProperty("POM_LICENSE_NAME"))
                        url.set(providers.gradleProperty("POM_LICENSE_URL"))
                    }
                }
                developers {
                    developer {
                        id.set("bilalelsayed97")
                        name.set("Bilal Elsayed")
                        email.set("bilalelsayed97@gmail.com")
                    }
                }
                scm {
                    url.set(providers.gradleProperty("POM_URL"))
                    connection.set(providers.gradleProperty("POM_SCM_CONNECTION"))
                    developerConnection.set(providers.gradleProperty("POM_SCM_DEV_CONNECTION"))
                }
            }
        }
    }
}
