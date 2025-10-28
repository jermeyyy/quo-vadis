plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.androidLint)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.dokka)
    `maven-publish`
}

group = "com.jermey.quo.vadis"
version = "0.1.0-SNAPSHOT"

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

    // Web targets
    js(IR) {
        browser {
            commonWebpackConfig {
                outputFileName = "quo-vadis-core.js"
            }
        }
        binaries.executable()
    }

    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            commonWebpackConfig {
                outputFileName = "quo-vadis-core.wasm.js"
            }
        }
        binaries.executable()
    }

    jvm("desktop")

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)
                implementation(libs.kotlin.reflect)
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

                // Optional: annotations for users who want to use KSP
                api(projects.quoVadisAnnotations)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }

        androidMain {
            dependencies {
            }
        }

        getByName("androidDeviceTest") {
            dependencies {
                implementation(libs.androidx.runner)
                implementation(libs.androidx.core)
                implementation(libs.androidx.testExt.junit)
            }
        }

        iosMain {
            dependencies {
            }
        }

        jsMain {
            dependencies {
            }
        }

        wasmJsMain {
            dependencies {
            }
        }

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }
    }

}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

publishing {
    publications {
        withType<MavenPublication> {
            groupId = "com.jermey.quo.vadis"
            artifactId = "quo-vadis-core${if (name != "kotlinMultiplatform") "-$name" else ""}"
            version = project.version.toString()

            pom {
                name.set("Quo Vadis - Navigation Library")
                description.set(
                    "A comprehensive type-safe navigation library for Compose Multiplatform supporting " +
                    "Android and iOS with predictive back gestures, animations, and modular architecture."
                )
                url.set("https://github.com/jermeyyy/quo-vadis")

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }

                developers {
                    developer {
                        id.set("jermeyyy")
                        name.set("Jermey")
                        email.set("jermey@example.com")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/jermeyyy/quo-vadis.git")
                    developerConnection.set("scm:git:ssh://github.com/jermeyyy/quo-vadis.git")
                    url.set("https://github.com/jermeyyy/quo-vadis")
                }
            }
        }
    }

    repositories {
        mavenLocal()
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
