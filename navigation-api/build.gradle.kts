plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.quoVadis)
}

// Quo Vadis KSP configuration (using local processor for development)
quoVadis {
    useLocalKsp = (project.findProperty("useLocalKsp") as? String)?.toBoolean() ?: true
}

kotlin {

    androidLibrary {
        namespace = "com.jermey.navplayground.navigation"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    js(IR) {
        browser()
    }

    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    jvm("desktop")

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)
                implementation(libs.kotlinx.coroutines.core)
                implementation(projects.quoVadisAnnotations)
                implementation(projects.quoVadisCore)
            }
        }
    }
}
