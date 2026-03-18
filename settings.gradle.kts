rootProject.name = "NavPlayground"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    // Include the plugin module as a composite build for local development
    includeBuild("quo-vadis-gradle-plugin")
    
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

include(":composeApp")
include(":androidApp")
include(":navigation-api")
include(":quo-vadis-core")
include(":quo-vadis-core-flow-mvi")
include(":quo-vadis-annotations")
include(":quo-vadis-ksp")

include(":feature1")
include(":feature2")

include(":quo-vadis-compiler-plugin")
include(":quo-vadis-compiler-plugin-native")
