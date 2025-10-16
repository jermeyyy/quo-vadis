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

    // Target declarations - add or remove as needed below. These define
    // which platforms this KMP module supports.
    // See: https://kotlinlang.org/docs/multiplatform-discover-project.html#targets
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

    // For iOS targets, this is also where you should
    // configure native binary output. For more information, see:
    // https://kotlinlang.org/docs/multiplatform-build-native-binaries.html#build-xcframeworks

    // A step-by-step guide on how to include this library in an XCode
    // project can be found here:
    // https://developer.android.com/kotlin/multiplatform/migrate
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

    // Desktop (JVM) target
    jvm("desktop")

    // Source set declarations.
    // Declaring a target automatically creates a source set with the same name. By default, the
    // Kotlin Gradle Plugin creates additional source sets that depend on each other, since it is
    // common to share sources between related targets.
    // See: https://kotlinlang.org/docs/multiplatform-hierarchy.html
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
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }

        androidMain {
            dependencies {
                // Add Android-specific dependencies here. Note that this source set depends on
                // commonMain by default and will correctly pull the Android artifacts of any KMP
                // dependencies declared in commonMain.
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
                // Add iOS-specific dependencies here. This a source set created by Kotlin Gradle
                // Plugin (KGP) that each specific iOS target (e.g., iosX64) depends on as
                // part of KMP's default source set hierarchy. Note that this source set depends
                // on common by default and will correctly pull the iOS artifacts of any
                // KMP dependencies declared in commonMain.
            }
        }

        jsMain {
            dependencies {
                // Add JS-specific dependencies here if needed
            }
        }

        wasmJsMain {
            dependencies {
                // Add Wasm-specific dependencies here if needed
            }
        }

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                // Desktop (JVM) dependencies automatically inherit from commonMain
            }
        }
    }

}

// Maven Publishing Configuration
publishing {
    publications {
        // Configure publications for all targets
        withType<MavenPublication> {
            groupId = "com.jermey.quo.vadis"
            artifactId = "quo-vadis-core${if (name != "kotlinMultiplatform") "-$name" else ""}"
            version = project.version.toString()

            pom {
                name.set("Quo Vadis - Navigation Library")
                description.set("A comprehensive type-safe navigation library for Compose Multiplatform supporting Android and iOS with predictive back gestures, animations, and modular architecture.")
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

// Dokka configuration for API documentation
dokka {
    moduleName.set("Quo Vadis Navigation Library")
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
