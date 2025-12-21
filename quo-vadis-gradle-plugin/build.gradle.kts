import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
    alias(libs.plugins.vanniktechMavenPublish)
}

group = "io.github.jermeyyy"
version = project.findProperty("VERSION_NAME") as String? ?: "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

// Helper to convert plugin notation to dependency
fun Provider<PluginDependency>.toDep() = map {
    "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}"
}

dependencies {
    compileOnly(libs.plugins.ksp.toDep())
    compileOnly(libs.plugins.kotlinMultiplatform.toDep())
}

gradlePlugin {
    plugins {
        create("quoVadis") {
            id = "io.github.jermeyyy.quo-vadis"
            implementationClass = "com.jermey.quo.vadis.gradle.QuoVadisPlugin"
            displayName = "Quo Vadis Navigation Plugin"
            description = "Gradle plugin for Quo Vadis navigation library KSP configuration"
        }
    }
}

// Generate BuildConfig with plugin version for synchronization
val generateBuildConfig by tasks.registering {
    val outputDir = layout.buildDirectory.dir("generated/source/buildConfig")
    outputs.dir(outputDir)
    
    val pluginVersion = project.version.toString()
    inputs.property("version", pluginVersion)
    
    doLast {
        val file = outputDir.get().file("com/jermey/quo/vadis/gradle/BuildConfig.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            """
            |package com.jermey.quo.vadis.gradle
            |
            |internal object BuildConfig {
            |    const val VERSION = "$pluginVersion"
            |}
            """.trimMargin()
        )
    }
}

sourceSets.main {
    kotlin.srcDir(generateBuildConfig)
}

tasks.named("compileKotlin") {
    dependsOn(generateBuildConfig)
}

@Suppress("UnstableApiUsage")
mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
    
    coordinates(
        groupId = "io.github.jermeyyy",
        artifactId = "quo-vadis-gradle-plugin",
        version = version.toString()
    )
    
    pom {
        name.set("Quo Vadis Gradle Plugin")
        description.set("Gradle plugin for Quo Vadis navigation library KSP configuration")
        inceptionYear.set("2025")
        url.set("https://github.com/jermeyyy/quo-vadis")
        
        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
            }
        }
        
        developers {
            developer {
                id.set("jermeyyy")
                name.set("Jermey")
            }
        }
        
        scm {
            url.set("https://github.com/jermeyyy/quo-vadis")
            connection.set("scm:git:git://github.com/jermeyyy/quo-vadis.git")
            developerConnection.set("scm:git:ssh://git@github.com/jermeyyy/quo-vadis.git")
        }
    }
}
