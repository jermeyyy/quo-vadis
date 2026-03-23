plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.android.lint)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.quoVadis)
    alias(libs.plugins.koin.compiler)
}

compose.resources {
    packageOfResClass = "navplayground.feature1.generated.resources"
}

// Quo Vadis KSP configuration (using local processor for development)
quoVadis {
    useLocalKsp = true
}

kotlin {

    androidLibrary {
        namespace = "com.jermey.feature1"
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

    val xcfName = "feature1Kit"

    @Suppress("DEPRECATION")
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
                implementation(projects.navigationApi)
                implementation(projects.feature1Api)
                implementation(projects.feature2Api)
                implementation(projects.quoVadisAnnotations)

                // Haze (glassmorphism blur effects, used by moved screens)
                implementation(libs.haze)
                implementation(libs.haze.materials)

                // Coil (image loading, used by ExploreDetailScreen)
                implementation(libs.coil.compose)

                // Koin
                implementation(libs.koin.core)
                api(libs.koin.annotations)
                implementation(libs.koin.compose)
                implementation(libs.koin.compose.viewmodel)
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
    }

}
// TEMPORARY WORKAROUND: Configure compose resources for new Android KMP library plugin
afterEvaluate {
    tasks.matching { it.name == "copyAndroidMainComposeResourcesToAndroidAssets" }.configureEach {
        val outputDirProperty = this::class.java.getDeclaredMethod("getOutputDirectory")
        val outputDir = outputDirProperty.invoke(this) as DirectoryProperty
        outputDir.set(layout.buildDirectory.dir("generated/compose/resourceGenerator/androidAssets/androidMain"))
    }

    tasks.matching { it.name == "processAndroidMainJavaRes" }.configureEach {
        dependsOn("copyAndroidMainComposeResourcesToAndroidAssets")

        val javaResTask = this as Sync
        javaResTask.from(layout.buildDirectory.dir("generated/compose/resourceGenerator/androidAssets/androidMain"))
    }
}
