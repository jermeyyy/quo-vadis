import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm")
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

dependencies {
    implementation(project(":quo-vadis-annotations"))
    implementation("com.google.devtools.ksp:symbol-processing-api:2.2.20-2.0.4")
    implementation(libs.kotlinpoet)
    implementation(libs.kotlinpoet.ksp)
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}
