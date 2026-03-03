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

dependencies {
    compileOnly(libs.kotlin.compiler.embeddable)
    compileOnly(libs.auto.service.annotations)

    testImplementation(libs.kotlin.compiler.embeddable)
    testImplementation(libs.kotlin.testJunit)
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.reflect)
    testImplementation(libs.kotlin.compile.testing)
    testImplementation(projects.quoVadisAnnotations)
    testImplementation(projects.quoVadisCore)
}

mavenPublishing {
    publishToMavenCentral()

    coordinates(
        groupId = "io.github.jermeyyy",
        artifactId = "quo-vadis-compiler-plugin",
        version = version.toString()
    )

    pom {
        name.set("Quo Vadis Compiler Plugin")
        description.set("K2 compiler plugin for Quo Vadis navigation library")
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
