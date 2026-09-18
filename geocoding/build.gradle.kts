import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// :geocoding is the Android library that ships the bundled 37 MB city
// database and the SQLite-backed Geocoder / CityDirectory implementations.
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    `maven-publish`
}

android {
    namespace = "io.github.bilalelsayed97.islamickit.geocoding"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    androidResources {
        // The database is opened in place by SQLite; it must not be deflated.
        noCompress += "db"
    }

    testOptions {
        // Robolectric needs the merged assets (the database) and the manifest.
        unitTests.isIncludeAndroidResources = true
        unitTests.all { test ->
            test.testLogging {
                events("failed")
                exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
            }
        }
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    api(project(":core"))

    testImplementation(libs.junit4)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    // Conformance fixture loader and JSON assertions shared with :core's tests.
    testImplementation(testFixtures(project(":core")))
}

publishing {
    publications {
        register<MavenPublication>("release") {
            artifactId = "geocoding"
            afterEvaluate {
                from(components["release"])
            }
            pom {
                name.set("islamic_kit_android geocoding")
                description.set(
                    "Bundled offline city database (SQLite) with a Geocoder and " +
                        "CityDirectory for islamic_kit_android.",
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
