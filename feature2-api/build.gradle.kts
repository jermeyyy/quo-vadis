plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.quoVadis)
}

quoVadis {
    useLocalKsp = true
    apiModule = true
}

kotlin {
    androidLibrary {
        namespace = "com.jermey.feature2.api"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    jvm("desktop")

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)
                implementation(compose.runtime)
                implementation(compose.animation)
                implementation(projects.quoVadisAnnotations)
                implementation(projects.quoVadisCore)
                implementation(projects.navigationApi)
            }
        }
    }
}
