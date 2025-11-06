import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
}

// Force Compose Multiplatform version alignment
configurations.all {
    resolutionStrategy {
        force("org.jetbrains.compose.material3:material3:1.9.0")
        force("org.jetbrains.compose.material3:material3-desktop:1.9.0")
        force("org.jetbrains.compose.ui:ui:1.9.0")
        force("org.jetbrains.compose.ui:ui-desktop:1.9.0")
        force("org.jetbrains.compose.runtime:runtime:1.9.0")
        force("org.jetbrains.compose.runtime:runtime-desktop:1.9.0")
    }
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    // Web targets
    js(IR) {
        browser {
            commonWebpackConfig {
                outputFileName = "composeApp.js"
            }
        }
        binaries.executable()
    }

    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            commonWebpackConfig {
                outputFileName = "composeApp.wasm.js"
            }
        }
        binaries.executable()
    }

    // Desktop (JVM) target
    jvm("desktop")
    
    sourceSets {
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(compose.materialIconsExtended)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.compose.backhandler)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            implementation(projects.quoVadisCore)
            implementation(projects.quoVadisCoreFlowMvi)
            
            // Koin
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.flowmvi.test)
        }
        jsMain.dependencies {
            implementation(compose.html.core)
            implementation(compose.materialIconsExtended)
        }
        wasmJsMain.dependencies {
            implementation(compose.materialIconsExtended)
        }
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                // Desktop JVM doesn't support materialIconsExtended in Compose 1.9.0
            }
        }
    }
}

android {
    namespace = "com.jermey.navplayground"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.jermey.navplayground"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}

compose.desktop {
    application {
        mainClass = "com.jermey.navplayground.Main_desktopKt"
        
        // Configure JVM arguments if needed
        jvmArgs += listOf("-Xmx2G")
        
        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb
            )
            packageName = "NavPlayground"
            packageVersion = "1.0.0"
            description = "Quo Vadis Navigation Library Demo"
            copyright = "© 2025 Jermey. All rights reserved."
            vendor = "Jermey"
            
            macOS {
                bundleID = "com.jermey.navplayground"
                iconFile.set(project.file("src/desktopMain/resources/icon.icns"))
            }
            windows {
                iconFile.set(project.file("src/desktopMain/resources/icon.ico"))
                menuGroup = "NavPlayground"
            }
            linux {
                iconFile.set(project.file("src/desktopMain/resources/icon.png"))
            }
        }
    }
}

// KSP configuration for Kotlin Multiplatform
// According to https://kotlinlang.org/docs/ksp-multiplatform.html
// Use "kspCommonMainMetadata" configuration (not "kspCommonMainKotlinMetadata")
dependencies {
    add("kspCommonMainMetadata", project(":quo-vadis-ksp"))
}

// Fix KSP task dependencies for Kotlin Multiplatform
// KSP generated sources are registered automatically since KSP 1.8.0-1.0.9
// BUT for metadata target in KMP, we need to add the source directory manually
kotlin.sourceSets.commonMain {
    kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
}

afterEvaluate {
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>().configureEach {
        if (!name.startsWith("ksp") && !name.contains("Test", ignoreCase = true)) {
            dependsOn("kspCommonMainKotlinMetadata")
        }
    }
}
