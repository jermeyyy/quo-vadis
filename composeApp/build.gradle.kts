plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.quoVadis)
    alias(libs.plugins.koin.compiler)
}

// Quo Vadis KSP configuration (using local processor for development)
quoVadis {
    useLocalKsp = true
}

// Configure compose resources for the new Android KMP library plugin
compose.resources {
    packageOfResClass = "navplayground.composeapp.generated.resources"
}

kotlin {
    androidLibrary {
        namespace = "com.jermey.navplayground.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        withHostTestBuilder {
        }

        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
            implementation(compose.materialIconsExtended)
            implementation(libs.ktor.client.okhttp)
        }
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
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
            api(libs.koin.annotations)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            implementation(projects.feature1)
            implementation(projects.feature2)

            // Haze for glassmorphism
            implementation(libs.haze)
            implementation(libs.haze.materials)

            // Coil for image loading
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)
            implementation(libs.ktor.client.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.flowmvi.test)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        jsMain.dependencies {
            implementation(compose.html.core)
            implementation(compose.materialIconsExtended)
            implementation(libs.ktor.client.js)
        }
        wasmJsMain.dependencies {
            implementation(compose.materialIconsExtended)
            implementation(libs.ktor.client.js)
        }
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.ktor.client.okhttp)
                // Desktop JVM doesn't support materialIconsExtended in Compose 1.9.0
            }
        }
    }

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

// TEMPORARY WORKAROUND: Configure compose resources for new Android KMP library plugin
afterEvaluate {
    // Set output directory for the copy task
    tasks.matching { it.name == "copyAndroidMainComposeResourcesToAndroidAssets" }.configureEach {
        val outputDirProperty = this::class.java.getDeclaredMethod("getOutputDirectory")
        val outputDir = outputDirProperty.invoke(this) as DirectoryProperty
        outputDir.set(layout.buildDirectory.dir("generated/compose/resourceGenerator/androidAssets/androidMain"))
    }

    // Wire assets copy task to Android java resource processing
    tasks.matching { it.name == "processAndroidMainJavaRes" }.configureEach {
        dependsOn("copyAndroidMainComposeResourcesToAndroidAssets")

        // Add the compose resources to the java resources
        val javaResTask = this as Sync
        javaResTask.from(layout.buildDirectory.dir("generated/compose/resourceGenerator/androidAssets/androidMain"))
    }
}


