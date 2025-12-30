import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm")
    alias(libs.plugins.dokka)
    alias(libs.plugins.maven.publish)
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

dependencies {
    implementation(libs.kotlin.reflect)
    implementation(projects.quoVadisAnnotations)
    implementation(projects.quoVadisCore)
    implementation(libs.symbol.processing.api)
    implementation(libs.kotlinpoet)
    implementation(libs.kotlinpoet.ksp)
    
    // Testing
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlin.testJunit)
    testImplementation(libs.junit)
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
    
    pom {
        name.set("Quo Vadis KSP")
        description.set("KSP code generator for Quo Vadis navigation library")
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
    moduleName.set("KSP Processor")
    moduleVersion.set(project.version.toString())
    
    dokkaPublications.html {
        outputDirectory.set(layout.buildDirectory.dir("dokka/html"))
        
        suppressObviousFunctions.set(true)
        suppressInheritedMembers.set(false)
    }
    
    dokkaSourceSets.configureEach {
        // Source links to GitHub
        sourceLink {
            localDirectory.set(file("src/main/kotlin"))
            remoteUrl("https://github.com/jermeyyy/quo-vadis/tree/main/quo-vadis-ksp/src/main/kotlin")
            remoteLineSuffix.set("")
        }
        
        // External documentation links
        externalDocumentationLinks.create("ksp") {
            url("https://kotlinlang.org/docs/ksp-reference.html")
        }
        
        // Reporting undocumented
        reportUndocumented.set(false)
        skipEmptyPackages.set(true)
    }
}
