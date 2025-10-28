import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm")
    alias(libs.plugins.dokka)
    `maven-publish`
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

dependencies {
    implementation(projects.quoVadisAnnotations)
    implementation(libs.symbol.processing.api)
    implementation(libs.kotlinpoet)
    implementation(libs.kotlinpoet.ksp)
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
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
