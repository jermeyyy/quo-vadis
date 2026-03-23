plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.android.lint)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.dokka)
    alias(libs.plugins.kover)
    alias(libs.plugins.maven.publish)
}

kotlin {
    androidLibrary {
        namespace = "com.jermey.quo.vadis.core"
        compileSdk = 36
        minSdk = 24

        withHostTestBuilder {
        }

        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
        lint {
            baseline = file("lint-baseline.xml")
        }
    }

    val xcfName = "quo-vadis-coreKit"

    iosX64 {
        binaries.framework {
            baseName = xcfName
        }
    }

    iosArm64 {
        binaries.framework {
            baseName = xcfName
        }
    }

    iosSimulatorArm64 {
        binaries.framework {
            baseName = xcfName
        }
    }

    jvm("desktop")

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)
                implementation(libs.kotlinx.serialization.json)
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)
                implementation(libs.compose.backhandler)
                implementation(libs.androidx.lifecycle.viewmodelCompose)
                implementation(libs.androidx.lifecycle.runtimeCompose)
                implementation(libs.navigationevent.compose)

                // Optional: annotations for users who want to use KSP
                api(projects.quoVadisAnnotations)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotest.framework.engine)
                implementation(libs.kotest.assertions.core)
            }
        }

        androidMain {
            dependencies {
                implementation(libs.androidx.material3.windowsizeclass)
            }
        }

        getByName("androidDeviceTest") {
            dependencies {
                implementation(libs.androidx.runner)
                implementation(libs.androidx.core)
                implementation(libs.androidx.testExt.junit)
            }
        }

        getByName("androidHostTest") {
            dependencies {
                implementation(libs.kotest.runner.junit5)
            }
        }

        iosMain {
            dependencies {
            }
        }

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }

        val desktopTest by getting {
            dependencies {
                implementation(libs.kotest.runner.junit5)
            }
        }
    }

}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.withType<org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest>().configureEach {
    failOnNoDiscoveredTests = false
}

kover {
    reports {
        filters {
            excludes {
                classes(
                    "*.generated.*",
                    "*.BuildConfig",
                    "*.Companion",
                    "*Test*",
                    "*Fake*",
                )
                annotatedBy("androidx.compose.runtime.Composable")
            }
        }
        total {
            xml {
                onCheck = false
                xmlFile = layout.buildDirectory.file("reports/kover/report.xml")
            }
            html {
                onCheck = false
                htmlDir = layout.buildDirectory.dir("reports/kover/html")
            }
        }
        verify {
            rule {
                minBound(70) // Minimum 70% line coverage for quo-vadis-core
            }
        }
    }
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()

    pom {
        name.set("Quo Vadis Core")
        description.set("Type-safe, reactive navigation library for Kotlin Multiplatform with Compose support")
        url.set("https://github.com/jermeyyy/quo-vadis")
        inceptionYear.set("2024")

        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
                distribution.set("repo")
            }
        }

        developers {
            developer {
                id.set("jermeyyy")
                name.set("Karol Celebi")
                url.set("https://github.com/jermeyyy")
            }
        }

        scm {
            url.set("https://github.com/jermeyyy/quo-vadis")
            connection.set("scm:git:git://github.com/jermeyyy/quo-vadis.git")
            developerConnection.set("scm:git:ssh://git@github.com/jermeyyy/quo-vadis.git")
        }
    }
}

dokka {
    moduleName.set("Core")
    moduleVersion.set(project.version.toString())

    dokkaPublications.html {
        outputDirectory.set(layout.buildDirectory.dir("dokka/html"))

        // Suppress obvious functions and inherited members at publication level
        suppressObviousFunctions.set(true)
        suppressInheritedMembers.set(false)
    }

    dokkaSourceSets.configureEach {
        // Source links to GitHub
        sourceLink {
            localDirectory.set(file("src/commonMain/kotlin"))
            remoteUrl("https://github.com/jermeyyy/quo-vadis/tree/main/quo-vadis-core/src/commonMain/kotlin")
            remoteLineSuffix.set("")
        }

        // External documentation links
        externalDocumentationLinks.create("android") {
            url("https://developer.android.com/reference/kotlin/")
            packageListUrl("https://developer.android.com/reference/kotlin/androidx/package-list")
        }

        externalDocumentationLinks.create("coroutines") {
            url("https://kotlinlang.org/api/kotlinx.coroutines/")
        }

        // Package options - suppress internal packages
        perPackageOption {
            matchingRegex.set(".*\\.internal.*")
            suppress.set(true)
        }

        // Reporting undocumented
        reportUndocumented.set(false)
        skipEmptyPackages.set(true)
    }
}
