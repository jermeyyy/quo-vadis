import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm")
    alias(libs.plugins.maven.publish)
}

group = "io.github.jermeyyy"
version = project.findProperty("VERSION_NAME") as String? ?: "0.0.1-SNAPSHOT"

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
        freeCompilerArgs.add("-Xcontext-parameters")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

// Sync sources from main compiler plugin module
val syncSources by tasks.registering(Sync::class) {
    from(project(":quo-vadis-compiler-plugin").file("src/main/kotlin"))
    into(layout.buildDirectory.dir("synced-sources/main/kotlin"))
}

val syncResources by tasks.registering(Sync::class) {
    from(project(":quo-vadis-compiler-plugin").file("src/main/resources"))
    into(layout.buildDirectory.dir("synced-sources/main/resources"))
}

sourceSets.main {
    kotlin.srcDir(layout.buildDirectory.dir("synced-sources/main/kotlin"))
    resources.srcDir(layout.buildDirectory.dir("synced-sources/main/resources"))
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    dependsOn(syncSources)
}

tasks.named("processResources") {
    dependsOn(syncResources)
}

tasks.matching { it.name == "sourcesJar" }.configureEach {
    dependsOn(syncSources, syncResources)
}

dependencies {
    compileOnly(libs.kotlin.compiler)
    compileOnly(libs.auto.service.annotations)
}

mavenPublishing {
    publishToMavenCentral()

    coordinates(
        groupId = "io.github.jermeyyy",
        artifactId = "quo-vadis-compiler-plugin-native",
        version = version.toString()
    )

    pom {
        name.set("Quo Vadis Compiler Plugin (Native)")
        description.set("K2 compiler plugin for Quo Vadis navigation library (Kotlin/Native targets)")
        inceptionYear.set("2025")
        url.set("https://github.com/jermeyyy/quo-vadis")

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
