plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.dokka)
    alias(libs.plugins.vanniktechMavenPublish)
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

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
    
    pom {
        name.set("Quo Vadis Annotations")
        description.set("Annotation processor for Quo Vadis navigation library")
        url.set("https://github.com/jermeyyy/quo-vadis")
        inceptionYear.set("2025")
        
        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
                distribution.set("repo")
            }
        }
        
        developers {
            developer {
                id.set("jermeyyy")
                name.set("Karol Celebi")
                url.set("https://github.com/jermeyyy")
            }
        }
        
        scm {
            url.set("https://github.com/jermeyyy/quo-vadis")
            connection.set("scm:git:git://github.com/jermeyyy/quo-vadis.git")
            developerConnection.set("scm:git:ssh://git@github.com/jermeyyy/quo-vadis.git")
        }
    }
}

// Dokka configuration for API documentation
dokka {
    moduleName.set("Annotations")
    moduleVersion.set(project.version.toString())
    
    dokkaPublications.html {
        outputDirectory.set(layout.buildDirectory.dir("dokka/html"))
        
        suppressObviousFunctions.set(true)
        suppressInheritedMembers.set(false)
    }
    
    dokkaSourceSets.configureEach {
        // Source links to GitHub
        sourceLink {
            localDirectory.set(file("src/commonMain/kotlin"))
            remoteUrl("https://github.com/jermeyyy/quo-vadis/tree/main/quo-vadis-annotations/src/commonMain/kotlin")
            remoteLineSuffix.set("")
        }
        
        // Reporting undocumented
        reportUndocumented.set(false)
        skipEmptyPackages.set(true)
    }
}
