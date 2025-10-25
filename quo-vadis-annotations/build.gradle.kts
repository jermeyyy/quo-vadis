plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
}

kotlin {
    // All platforms that use the library
    jvm()
    
    androidTarget {
        publishLibraryVariants("release")
    }
    
    iosArm64()
    iosX64()
    iosSimulatorArm64()
    
    js(IR) {
        browser()
    }
    
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }
    
    sourceSets {
        commonMain.dependencies {
            // No dependencies - pure annotation module
        }
    }
}

android {
    namespace = "com.jermey.quo.vadis.annotations"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
