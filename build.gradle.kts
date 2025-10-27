plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.androidKotlinMultiplatformLibrary) apply false
    alias(libs.plugins.androidLint) apply false
    alias(libs.plugins.detekt)
    alias(libs.plugins.dokka)
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
    dokka(project(":quo-vadis-core"))
    dokka(project(":quo-vadis-annotations"))
    dokka(project(":quo-vadis-ksp"))
}

allprojects {
    // Skip detekt for gradle plugin module to avoid conflicts with kotlin-dsl
    if (project.name != "quo-vadis-gradle-plugin") {
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
                    xml.required.set(true)
                    html.required.set(true)
                    sarif.required.set(true)
                    md.required.set(true)
                }
            }
        }
    }
}
