plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    // KSP disabled - recipes module is for documentation/patterns only
    // The quo-vadis-ksp processor has a bug that needs to be fixed in the KSP module
    // alias(libs.plugins.ksp)
}

kotlin {
    androidLibrary {
        namespace = "com.jermey.quo.vadis.recipes"
        compileSdk = 36
        minSdk = 24
    }

    val xcfName = "QuoVadisRecipes"

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

    js(IR) {
        browser()
    }

    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    jvm("desktop")

    sourceSets {
        commonMain.dependencies {
            // Quo Vadis navigation library
            implementation(project(":quo-vadis-core"))
            implementation(project(":quo-vadis-annotations"))

            // Compose dependencies
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

// KSP configuration disabled - recipes module is for documentation/patterns only
// The annotations are used to show correct patterns, but KSP processing is not needed
// Uncomment below when quo-vadis-ksp NavNodeBuilder bug is fixed
//
// dependencies {
//     add("kspCommonMainMetadata", project(":quo-vadis-ksp"))
// }
// 
// kotlin.sourceSets.commonMain {
//     kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
// }
// 
// afterEvaluate {
//     tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>().configureEach {
//         if (!name.startsWith("ksp") && !name.contains("Test", ignoreCase = true)) {
//             dependsOn("kspCommonMainKotlinMetadata")
//         }
//     }
// }
