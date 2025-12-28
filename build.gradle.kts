plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.android.lint) apply false
    alias(libs.plugins.detekt)
    alias(libs.plugins.dokka)
    alias(libs.plugins.maven.publish) apply false
}

// Apply Dokka to library subprojects for multi-module documentation
subprojects {
    if (project.name in listOf("quo-vadis-core", "quo-vadis-annotations", "quo-vadis-ksp")) {
        apply(plugin = "org.jetbrains.dokka")
    }
}

// Configure Dokka for multi-module aggregation
dokka {
    moduleName.set("Quo Vadis Navigation Library")
    
    dokkaPublications.html {
        outputDirectory.set(layout.buildDirectory.dir("dokka/html"))
    }
}

// Aggregate documentation from library subprojects
dependencies {
    dokka(projects.quoVadisCore)
    dokka(projects.quoVadisAnnotations)
    dokka(projects.quoVadisKsp)
}

allprojects {
    // Skip detekt for gradle plugin module and androidApp to avoid conflicts
    if (project.name !in listOf("quo-vadis-gradle-plugin", "androidApp")) {
        plugins.apply(rootProject.libs.plugins.detekt.get().pluginId)

        afterEvaluate {

            detekt {
                config.setFrom(rootProject.file("config/detekt/detekt.yml"))
                baseline = file("detekt-baseline.xml")
                autoCorrect = true
                parallel = true
                buildUponDefaultConfig = true
                source = fileTree("src").apply {
                    include("**/*.kt")
                    include("**/*.kts")
                    exclude("**/build/**")
                }
            }

            tasks.withType<dev.detekt.gradle.Detekt>().configureEach {
                reports {
                    html.required.set(true)
                    sarif.required.set(true)
                }
            }
        }
    }
}
