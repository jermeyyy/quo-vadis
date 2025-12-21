# Gradle Plugin for Quo Vadis KSP Configuration

## Problem Statement

Currently, every module that uses Quo Vadis navigation requires significant boilerplate in their `build.gradle.kts` files:

### Current Boilerplate (Repeated in Each Module)

```kotlin
import android.databinding.tool.ext.toCamelCase

plugins {
    // ... other plugins ...
    alias(libs.plugins.ksp)
}

kotlin {
    ksp {
        arg("quoVadis.modulePrefix", project.name.toCamelCase())
    }
}

// KSP configuration for Kotlin Multiplatform
dependencies {
    add("kspCommonMainMetadata", project(":quo-vadis-ksp"))
    // or for published version:
    // add("kspCommonMainMetadata", "io.github.jermeyyy:quo-vadis-ksp:x.x.x")
}

// Fix KSP task dependencies for Kotlin Multiplatform
kotlin.sourceSets.commonMain {
    kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
}

afterEvaluate {
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>().configureEach {
        if (!name.startsWith("ksp") && !name.contains("Test", ignoreCase = true)) {
            dependsOn("kspCommonMainKotlinMetadata")
        }
    }
}
```

### Issues

1. **~25 lines of boilerplate** per module
2. **Error-prone**: Easy to misconfigure or forget parts
3. **Maintenance burden**: Changes need to propagate to all modules
4. **Non-obvious**: The KSP KMP workarounds aren't intuitive
5. **Import dependency**: Requires `android.databinding.tool.ext.toCamelCase`

---

## Proposed Solution

Create a **Gradle Plugin** (`quo-vadis-gradle-plugin`) that encapsulates all KSP configuration.

### Target User Experience

```kotlin
// build.gradle.kts (per feature module)
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.quoVadis)  // ← Single line replaces all boilerplate
}

// Optional: Override module prefix (defaults to project.name in camelCase)
quoVadis {
    modulePrefix = "CustomPrefix"  // Optional
}
```

### What the Plugin Does Automatically

1. ✅ Applies the KSP plugin
2. ✅ Adds `kspCommonMainMetadata` dependency for quo-vadis-ksp
3. ✅ Configures KSP argument `quoVadis.modulePrefix` with sensible default
4. ✅ Registers generated source directory
5. ✅ Sets up proper task dependencies for KMP
6. ✅ Works with multimodule projects

---

## Architecture

### New Module: `quo-vadis-gradle-plugin`

```
quo-vadis-gradle-plugin/
├── build.gradle.kts
└── src/main/kotlin/com/jermey/quo/vadis/gradle/
    ├── QuoVadisPlugin.kt           # Main plugin entry point
    ├── QuoVadisExtension.kt        # DSL extension (quoVadis { ... })
    └── internal/
        └── StringUtils.kt          # toCamelCase implementation
```

### Plugin ID

- **ID**: `io.github.jermeyyy.quo-vadis`
- **Implementation**: `com.jermey.quo.vadis.gradle.QuoVadisPlugin`

### Extension DSL

```kotlin
// Extension class
abstract class QuoVadisExtension {
    /**
     * Module prefix for generated code.
     * Defaults to project.name converted to camelCase.
     */
    abstract val modulePrefix: Property<String>
    
    /**
     * KSP processor version to use.
     * Defaults to the same version as the plugin.
     */
    abstract val kspVersion: Property<String>
    
    /**
     * Whether to use local project dependency (for development).
     * Defaults to false.
     */
    abstract val useLocalKsp: Property<Boolean>
}
```

---

## Implementation Plan

### Phase 1: Module Setup

#### Task 1.1: Create Gradle Plugin Module Structure

**Files to create:**
- `quo-vadis-gradle-plugin/build.gradle.kts`
- `quo-vadis-gradle-plugin/src/main/kotlin/com/jermey/quo/vadis/gradle/QuoVadisPlugin.kt`
- `quo-vadis-gradle-plugin/src/main/kotlin/com/jermey/quo/vadis/gradle/QuoVadisExtension.kt`
- `quo-vadis-gradle-plugin/src/main/kotlin/com/jermey/quo/vadis/gradle/internal/StringUtils.kt`

**Update:**
- `settings.gradle.kts` - Include new module

#### Task 1.2: Implement Plugin Build Configuration

```kotlin
// quo-vadis-gradle-plugin/build.gradle.kts
plugins {
    `kotlin-dsl`
    alias(libs.plugins.vanniktechMavenPublish)
}

group = "io.github.jermeyyy"
version = project.findProperty("VERSION_NAME") as String? ?: "0.0.1-SNAPSHOT"

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

// Helper to convert plugin notation to dependency
fun Provider<PluginDependency>.toDep() = map {
    "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}"
}
```

### Phase 2: Core Plugin Implementation

#### Task 2.1: Implement Extension

```kotlin
// QuoVadisExtension.kt
package com.jermey.quo.vadis.gradle

import org.gradle.api.provider.Property

abstract class QuoVadisExtension {
    /**
     * Module prefix for generated code.
     * Used in KSP to distinguish generated code from different modules.
     * Defaults to project.name converted to camelCase.
     */
    abstract val modulePrefix: Property<String>
    
    /**
     * Whether to use local KSP processor (project dependency).
     * Useful during development. Defaults to false.
     * When false, uses Maven Central artifact.
     */
    abstract val useLocalKsp: Property<Boolean>
}
```

#### Task 2.2: Implement Main Plugin Logic

```kotlin
// QuoVadisPlugin.kt
package com.jermey.quo.vadis.gradle

import com.jermey.quo.vadis.gradle.internal.toCamelCase
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*

class QuoVadisPlugin : Plugin<Project> {
    
    override fun apply(project: Project) {
        // Create extension
        val extension = project.extensions.create<QuoVadisExtension>("quoVadis")
        
        // Set defaults
        extension.modulePrefix.convention(project.name.toCamelCase())
        extension.useLocalKsp.convention(false)
        
        // Apply KSP plugin
        project.pluginManager.apply("com.google.devtools.ksp")
        
        // Configure after KMP plugin is applied
        project.pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
            configureKsp(project, extension)
        }
    }
    
    private fun configureKsp(project: Project, extension: QuoVadisExtension) {
        project.afterEvaluate {
            // Add KSP dependency
            val kspDependency = if (extension.useLocalKsp.get()) {
                project.dependencies.project(":quo-vadis-ksp")
            } else {
                "io.github.jermeyyy:quo-vadis-ksp:${BuildConfig.VERSION}"
            }
            project.dependencies.add("kspCommonMainMetadata", kspDependency)
            
            // Configure KSP arguments
            val kmpExtension = project.extensions.getByType<KotlinMultiplatformExtension>()
            kmpExtension.sourceSets.getByName("commonMain") {
                kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
            }
            
            // Pass module prefix to KSP
            project.extensions.configure<KspExtension> {
                arg("quoVadis.modulePrefix", extension.modulePrefix.get())
            }
            
            // Fix task dependencies
            project.tasks.withType<KotlinCompilationTask<*>>().configureEach {
                if (!name.startsWith("ksp") && !name.contains("Test", ignoreCase = true)) {
                    dependsOn("kspCommonMainKotlinMetadata")
                }
            }
        }
    }
}
```

#### Task 2.3: Implement String Utilities

```kotlin
// internal/StringUtils.kt
package com.jermey.quo.vadis.gradle.internal

/**
 * Converts a kebab-case or snake_case string to camelCase.
 * Examples:
 *   - "feature-one" -> "featureOne"
 *   - "feature_one" -> "featureOne"
 *   - "featureOne" -> "featureOne"
 */
fun String.toCamelCase(): String {
    return split('-', '_', ' ')
        .mapIndexed { index, word ->
            if (index == 0) word.lowercase()
            else word.replaceFirstChar { it.uppercase() }
        }
        .joinToString("")
}
```

### Phase 3: Version Synchronization

#### Task 3.1: Generate BuildConfig for Plugin

The plugin needs to know its own version to reference the correct KSP processor version.

```kotlin
// Add to quo-vadis-gradle-plugin/build.gradle.kts
val generateBuildConfig by tasks.registering {
    val outputDir = layout.buildDirectory.dir("generated/source/buildConfig")
    outputs.dir(outputDir)
    
    val version = project.version.toString()
    
    doLast {
        val file = outputDir.get().file("com/jermey/quo/vadis/gradle/BuildConfig.kt").asFile
        file.parentFile.mkdirs()
        file.writeText("""
            package com.jermey.quo.vadis.gradle
            
            internal object BuildConfig {
                const val VERSION = "$version"
            }
        """.trimIndent())
    }
}

sourceSets.main {
    kotlin.srcDir(generateBuildConfig)
}

tasks.named("compileKotlin") {
    dependsOn(generateBuildConfig)
}
```

### Phase 4: Maven Central Publishing

#### Task 4.1: Configure Publishing

```kotlin
// Add to quo-vadis-gradle-plugin/build.gradle.kts
mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()
    
    coordinates(
        groupId = "io.github.jermeyyy",
        artifactId = "quo-vadis-gradle-plugin",
        version = version.toString()
    )
    
    pom {
        name.set("Quo Vadis Gradle Plugin")
        description.set("Gradle plugin for Quo Vadis navigation library")
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
```

### Phase 5: Version Catalog Integration

#### Task 5.1: Update Version Catalog

```toml
# gradle/libs.versions.toml

[plugins]
# ... existing plugins ...
quoVadis = { id = "io.github.jermeyyy.quo-vadis", version.ref = "quoVadis" }

[versions]
quoVadis = "1.0.0"  # Or whatever the current version is
```

### Phase 6: Migration & Testing

#### Task 6.1: Migrate composeApp Module

**Before:**
```kotlin
import android.databinding.tool.ext.toCamelCase

plugins {
    alias(libs.plugins.ksp)
    // ... other plugins
}

kotlin {
    ksp {
        arg("quoVadis.modulePrefix", project.name.toCamelCase())
    }
}

dependencies {
    add("kspCommonMainMetadata", project(":quo-vadis-ksp"))
}

kotlin.sourceSets.commonMain {
    kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
}

afterEvaluate {
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>().configureEach {
        if (!name.startsWith("ksp") && !name.contains("Test", ignoreCase = true)) {
            dependsOn("kspCommonMainKotlinMetadata")
        }
    }
}
```

**After:**
```kotlin
plugins {
    alias(libs.plugins.quoVadis)
    // ... other plugins (NO ksp needed)
}

// For development with local KSP:
quoVadis {
    useLocalKsp = true
}
```

#### Task 6.2: Migrate feature1 Module

Same transformation as composeApp.

#### Task 6.3: Migrate feature2 Module

Same transformation as composeApp.

#### Task 6.4: Verify Build

```bash
./gradlew clean assembleDebug
./gradlew :quo-vadis-core:desktopTest
```

### Phase 7: Documentation

#### Task 7.1: Update README

Add documentation for plugin usage:

```markdown
## Quick Start

### 1. Add Plugin to Version Catalog

```toml
# gradle/libs.versions.toml
[plugins]
quoVadis = { id = "io.github.jermeyyy.quo-vadis", version = "x.x.x" }
```

### 2. Apply Plugin

```kotlin
// build.gradle.kts
plugins {
    alias(libs.plugins.quoVadis)
}
```

### 3. (Optional) Configure

```kotlin
quoVadis {
    // Override module prefix if needed
    modulePrefix = "customPrefix"
}
```
```

#### Task 7.2: Update Documentation Website

Update docs/site with plugin usage instructions.

---

## File Changes Summary

### New Files

| File | Description |
|------|-------------|
| `quo-vadis-gradle-plugin/build.gradle.kts` | Plugin module build config |
| `quo-vadis-gradle-plugin/src/main/kotlin/.../QuoVadisPlugin.kt` | Main plugin |
| `quo-vadis-gradle-plugin/src/main/kotlin/.../QuoVadisExtension.kt` | DSL extension |
| `quo-vadis-gradle-plugin/src/main/kotlin/.../internal/StringUtils.kt` | Utilities |

### Modified Files

| File | Change |
|------|--------|
| `settings.gradle.kts` | Add `include(":quo-vadis-gradle-plugin")` |
| `gradle/libs.versions.toml` | Add plugin definition |
| `composeApp/build.gradle.kts` | Remove KSP boilerplate, add plugin |
| `feature1/build.gradle.kts` | Remove KSP boilerplate, add plugin |
| `feature2/build.gradle.kts` | Remove KSP boilerplate, add plugin |
| `build.gradle.kts` | Skip detekt for gradle plugin module |
| `README.md` | Add plugin documentation |

---

## Risk Analysis

| Risk | Mitigation |
|------|------------|
| Plugin conflicts with existing KSP config | Use `pluginManager.withPlugin` to conditionally apply |
| Version mismatch between plugin and KSP processor | Use BuildConfig to synchronize versions |
| Breaking change for existing users | Document migration path, keep manual config as fallback |
| Gradle version compatibility | Test with min supported Gradle version, document requirements |

---

## Success Criteria

1. ✅ Plugin applies successfully on all target modules
2. ✅ Generated code is identical to manual configuration
3. ✅ All existing tests pass
4. ✅ Plugin publishes to Maven Central
5. ✅ Documentation covers usage and migration

---

## Timeline Estimate

| Phase | Estimated Effort |
|-------|-----------------|
| Phase 1: Module Setup | 1-2 hours |
| Phase 2: Core Implementation | 2-3 hours |
| Phase 3: Version Sync | 30 min |
| Phase 4: Publishing Config | 1 hour |
| Phase 5: Version Catalog | 15 min |
| Phase 6: Migration & Testing | 1-2 hours |
| Phase 7: Documentation | 1 hour |
| **Total** | **7-10 hours** |

---

## Alternative Approaches Considered

### 1. Convention Plugin (buildSrc/build-logic)

**Pros:**
- Simpler for internal use
- No publishing needed

**Cons:**
- Not reusable across projects
- Users would need to copy the convention plugin

**Decision:** Rejected - quo-vadis targets external users

### 2. Pre-compiled Script Plugin

**Pros:**
- Slightly simpler implementation

**Cons:**
- Less powerful (no extension DSL)
- Harder to version and publish

**Decision:** Rejected - need DSL for configuration

### 3. Gradle Settings Plugin

**Pros:**
- Could configure all modules at once

**Cons:**
- Less granular control
- More invasive

**Decision:** Rejected - per-module plugin is more flexible

---

## Implementation Order

1. **Create module structure** (Task 1.1, 1.2)
2. **Implement core plugin** (Task 2.1, 2.2, 2.3)
3. **Add version sync** (Task 3.1)
4. **Test locally** (apply plugin to composeApp first)
5. **Configure publishing** (Task 4.1)
6. **Update version catalog** (Task 5.1)
7. **Migrate all modules** (Task 6.1, 6.2, 6.3)
8. **Run full verification** (Task 6.4)
9. **Update documentation** (Task 7.1, 7.2)
10. **Publish to Maven Central**
