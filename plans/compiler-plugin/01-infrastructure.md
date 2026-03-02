# Phase 1: Infrastructure & Gradle Plugin

**Status**: Not Started  
**Prerequisites**: None  
**Outcome**: Compiler plugin modules compile, load into Kotlin compiler, and receive configuration — no transformations yet

---

## Overview

Phase 1 establishes the foundational infrastructure for the K2 compiler plugin. This includes creating new modules, registering the plugin with the Kotlin compiler via SPI, and migrating the Gradle plugin from KSP configuration to `KotlinCompilerPluginSupportPlugin`.

At the end of this phase, the plugin will:
- Load successfully into the Kotlin compiler during build
- Receive the `modulePrefix` configuration from the Gradle plugin
- Compile for all KMP targets (JVM, JS, Wasm, Native)
- Do nothing (no-op) — no FIR or IR transformations yet

---

## Technical Approach

### Module Structure

Two new modules replace the KSP processor:

```
quo-vadis-compiler-plugin/          → JVM, JS, WASM targets
  build.gradle.kts
  src/main/kotlin/
    com/jermey/quo/vadis/compiler/
      QuoVadisCommandLineProcessor.kt
      QuoVadisCompilerPluginRegistrar.kt
      QuoVadisConfigurationKeys.kt
  src/main/resources/
    META-INF/services/
      org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
      org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar

quo-vadis-compiler-plugin-native/   → Kotlin/Native targets
  build.gradle.kts                  (mirrors main plugin, different compiler dep)
  src/                              (synced from main plugin module)
```

### Why Two Modules?

Kotlin/Native compiler plugins require `org.jetbrains.kotlin:kotlin-compiler` (unshadowed), while JVM/JS/Wasm targets use `org.jetbrains.kotlin:kotlin-compiler-embedded` (shadowed). Using the wrong dependency causes classpath conflicts. The native module's sources are automatically synced from the main plugin module during build.

### Gradle Plugin Migration

The existing `QuoVadisPlugin` (which configures KSP) will be extended to support both KSP and the compiler plugin. A `useCompilerPlugin` flag determines which mode is active. When compiler plugin mode is active, the plugin implements `KotlinCompilerPluginSupportPlugin` behavior.

---

## Tasks

### Sub-phase 1A: Compiler Plugin Module Setup

#### Task 1A.1: Create `quo-vadis-compiler-plugin` module

**Description**: Create the new Gradle module with proper build configuration.

**Files to create**:
- `quo-vadis-compiler-plugin/build.gradle.kts`

**Build configuration must**:
- Apply `kotlin("jvm")` plugin
- Depend on `org.jetbrains.kotlin:kotlin-compiler-embedded` (compileOnly, version matching project's Kotlin version)
- Apply `com.google.devtools.ksp` for `@AutoService` annotation processing
- Depend on `com.google.auto.service:auto-service-annotations` (compileOnly)
- Configure JVM target (11+)
- Set `group` and `version` consistent with other modules
- Configure artifact publishing (Maven coordinates: `io.github.jermeyyy:quo-vadis-compiler-plugin`)

**Acceptance Criteria**:
- [ ] Module compiles with `./gradlew :quo-vadis-compiler-plugin:build`
- [ ] Module is listed in `settings.gradle.kts`
- [ ] Kotlin compiler embedded dependency resolves correctly

---

#### Task 1A.2: Create `quo-vadis-compiler-plugin-native` module

**Description**: Create the Native-specific plugin module with automated source synchronization.

**Files to create**:
- `quo-vadis-compiler-plugin-native/build.gradle.kts`

**Build configuration must**:
- Apply `kotlin("jvm")` plugin
- Depend on `org.jetbrains.kotlin:kotlin-compiler` (compileOnly — NOT embedded)
- Include a Gradle `Sync` task that copies source files from `quo-vadis-compiler-plugin/src/main/kotlin` to this module's source directory
- All compilation tasks depend on the sync task
- Same `@AutoService` setup as main plugin module
- Configure artifact publishing (`io.github.jermeyyy:quo-vadis-compiler-plugin-native`)

**Acceptance Criteria**:
- [ ] Module compiles with `./gradlew :quo-vadis-compiler-plugin-native:build`
- [ ] Sources are synced from main plugin module during build
- [ ] Uses unshadowed `kotlin-compiler` dependency (not embedded)
- [ ] Module is listed in `settings.gradle.kts`

---

#### Task 1A.3: Register new modules in project

**Description**: Add both new modules to the root build configuration.

**Files to modify**:
- `settings.gradle.kts` — add `include(":quo-vadis-compiler-plugin")` and `include(":quo-vadis-compiler-plugin-native")`
- `gradle/libs.versions.toml` — add `kotlin-compiler-embedded` and `kotlin-compiler` version entries, `auto-service` dependency

**Acceptance Criteria**:
- [ ] `./gradlew projects` lists both new modules
- [ ] Version catalog entries resolve correctly

---

### Sub-phase 1B: SPI Registration

#### Task 1B.1: Create `QuoVadisCommandLineProcessor`

**Description**: Implement the `CommandLineProcessor` that declares the plugin's identity and accepts configuration options from Gradle.

**File to create**:
- `quo-vadis-compiler-plugin/src/main/kotlin/com/jermey/quo/vadis/compiler/QuoVadisCommandLineProcessor.kt`

**Implementation**:
```kotlin
@AutoService(CommandLineProcessor::class)
class QuoVadisCommandLineProcessor : CommandLineProcessor {
    companion object {
        const val PLUGIN_ID = "com.jermey.quo-vadis"
        
        val OPTION_MODULE_PREFIX = CliOption(
            optionName = "modulePrefix",
            valueDescription = "String",
            description = "Prefix for generated navigation config class names",
            required = false
        )
    }
    
    override val pluginId: String = PLUGIN_ID
    override val pluginOptions: Collection<CliOption> = listOf(OPTION_MODULE_PREFIX)
    
    override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) {
        when (option) {
            OPTION_MODULE_PREFIX -> configuration.put(QuoVadisConfigurationKeys.MODULE_PREFIX, value)
        }
    }
}
```

**Acceptance Criteria**:
- [ ] Plugin ID matches Gradle plugin expectations
- [ ] `modulePrefix` option is declared and processed
- [ ] Configuration key is stored in `CompilerConfiguration`

---

#### Task 1B.2: Create `QuoVadisConfigurationKeys`

**Description**: Define the configuration keys used to pass data from the `CommandLineProcessor` to FIR/IR extensions.

**File to create**:
- `quo-vadis-compiler-plugin/src/main/kotlin/com/jermey/quo/vadis/compiler/QuoVadisConfigurationKeys.kt`

**Implementation**:
```kotlin
object QuoVadisConfigurationKeys {
    val MODULE_PREFIX = CompilerConfigurationKey<String>("Quo Vadis module prefix")
}
```

**Acceptance Criteria**:
- [ ] Key is accessible from both FIR and IR extension contexts

---

#### Task 1B.3: Create `QuoVadisCompilerPluginRegistrar`

**Description**: Implement the `CompilerPluginRegistrar` that will eventually bind FIR and IR extensions. For Phase 1, it only reads configuration and logs.

**File to create**:
- `quo-vadis-compiler-plugin/src/main/kotlin/com/jermey/quo/vadis/compiler/QuoVadisCompilerPluginRegistrar.kt`

**Implementation**:
```kotlin
@AutoService(CompilerPluginRegistrar::class)
class QuoVadisCompilerPluginRegistrar : CompilerPluginRegistrar() {
    override val supportsK2: Boolean = true
    
    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        val modulePrefix = configuration.get(QuoVadisConfigurationKeys.MODULE_PREFIX) ?: return
        
        // Phase 2: FIR extensions will be registered here
        // Phase 3: IR extensions will be registered here
    }
}
```

**Acceptance Criteria**:
- [ ] `supportsK2` returns `true`
- [ ] `modulePrefix` is retrievable from configuration
- [ ] Plugin registrar is discovered by service loader

---

#### Task 1B.4: Create SPI service files

**Description**: Register the plugin entry points via META-INF service files.

**Files to create**:
- `quo-vadis-compiler-plugin/src/main/resources/META-INF/services/org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor`
  - Content: `com.jermey.quo.vadis.compiler.QuoVadisCommandLineProcessor`
- `quo-vadis-compiler-plugin/src/main/resources/META-INF/services/org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar`
  - Content: `com.jermey.quo.vadis.compiler.QuoVadisCompilerPluginRegistrar`

**Note**: If using `@AutoService`, these files are generated automatically. In that case, verify they exist in the build output.

**Acceptance Criteria**:
- [ ] Service files exist in compiled JAR's META-INF/services
- [ ] Kotlin compiler discovers the plugin during compilation

---

### Sub-phase 1C: Gradle Plugin Migration

#### Task 1C.1: Add `KotlinCompilerPluginSupportPlugin` behavior

**Description**: Extend the existing `QuoVadisPlugin` to support compiler plugin mode alongside KSP mode. The Gradle plugin must implement `KotlinCompilerPluginSupportPlugin` to inject the compiler plugin artifact into Kotlin compilation.

**Files to modify**:
- `quo-vadis-gradle-plugin/src/main/kotlin/com/jermey/quo/vadis/gradle/QuoVadisPlugin.kt`
- `quo-vadis-gradle-plugin/src/main/kotlin/com/jermey/quo/vadis/gradle/QuoVadisExtension.kt`
- `quo-vadis-gradle-plugin/build.gradle.kts` (add kotlin-gradle-plugin-api dependency)

**New extension property**:
```kotlin
abstract class QuoVadisExtension {
    abstract val modulePrefix: Property<String>
    abstract val useLocalKsp: Property<Boolean>
    abstract val useCompilerPlugin: Property<Boolean>  // NEW — default: false initially, later true
}
```

**Gradle plugin changes**:
When `useCompilerPlugin = true`, the plugin must:
1. NOT require KSP plugin
2. Implement `KotlinCompilerPluginSupportPlugin` interface
3. Return correct artifact from `getPluginArtifact()` (embedded) and `getPluginArtifactForNative()` (non-embedded)
4. Pass `modulePrefix` as `SubpluginOption`
5. NOT register KSP dependencies or generated source directories

When `useCompilerPlugin = false` (default initially), behavior is unchanged (current KSP configuration).

**Approach**: Since `KotlinCompilerPluginSupportPlugin` is an interface, create a separate implementation class `QuoVadisCompilerSubplugin` that implements it. The main `QuoVadisPlugin` conditionally registers it.

```kotlin
class QuoVadisCompilerSubplugin : KotlinCompilerPluginSupportPlugin {
    override fun getCompilerPluginId(): String = "com.jermey.quo-vadis"
    
    override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
        groupId = "io.github.jermeyyy",
        artifactId = "quo-vadis-compiler-plugin",
        version = BuildConfig.VERSION
    )
    
    override fun getPluginArtifactForNative(): SubpluginArtifact = SubpluginArtifact(
        groupId = "io.github.jermeyyy",
        artifactId = "quo-vadis-compiler-plugin-native",
        version = BuildConfig.VERSION
    )
    
    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true
    
    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project
        val extension = project.extensions.getByType<QuoVadisExtension>()
        return project.provider {
            listOf(SubpluginOption("modulePrefix", extension.modulePrefix.get()))
        }
    }
    
    override fun apply(target: Project) { /* no-op, handled by main plugin */ }
}
```

**Acceptance Criteria**:
- [ ] `quoVadis { useCompilerPlugin = true }` enables compiler plugin mode
- [ ] `quoVadis { useCompilerPlugin = false }` (default) uses KSP mode
- [ ] Compiler plugin artifact is injected into Kotlin compilation classpath
- [ ] Native compilation gets `quo-vadis-compiler-plugin-native` artifact
- [ ] `modulePrefix` is passed to compiler plugin via `SubpluginOption`
- [ ] KSP is NOT configured when compiler plugin mode is active

---

#### Task 1C.2: Auto-apply `quo-vadis-core` dependency

**Description**: When the Gradle plugin is applied, automatically add `quo-vadis-core` as a `commonMain` dependency. This removes one line of manual configuration for users.

**Files to modify**:
- `quo-vadis-gradle-plugin/src/main/kotlin/com/jermey/quo/vadis/gradle/QuoVadisPlugin.kt`

**Implementation**:
Add to the plugin's `apply` method:
```kotlin
project.extensions.configure<KotlinMultiplatformExtension> {
    sourceSets.getByName("commonMain") {
        dependencies {
            implementation("io.github.jermeyyy:quo-vadis-core:${BuildConfig.VERSION}")
        }
    }
}
```

**Acceptance Criteria**:
- [ ] `quo-vadis-core` is automatically available in `commonMain` without explicit dependency
- [ ] Version is synchronized with plugin version

---

#### Task 1C.3: Update Gradle plugin build configuration

**Description**: Add required dependencies for `KotlinCompilerPluginSupportPlugin` support.

**Files to modify**:
- `quo-vadis-gradle-plugin/build.gradle.kts`

**New dependencies**:
```kotlin
dependencies {
    compileOnly(libs.plugins.ksp.toDep())  // existing
    compileOnly(libs.plugins.kotlin.multiplatform.toDep())  // existing
    implementation(libs.kotlin.gradle.plugin.api)  // NEW — for KotlinCompilerPluginSupportPlugin
}
```

**Acceptance Criteria**:
- [ ] `KotlinCompilerPluginSupportPlugin` and related classes are available at compile time
- [ ] Gradle plugin builds without errors

---

### Sub-phase 1D: Integration Verification

#### Task 1D.1: Apply compiler plugin to demo app

**Description**: Configure `composeApp` to use compiler plugin mode for verification.

**Files to modify**:
- `composeApp/build.gradle.kts` — add/verify `quoVadis { useCompilerPlugin = true }` (can be behind a Gradle property flag for easy toggling)

**Acceptance Criteria**:
- [ ] `./gradlew :composeApp:compileKotlinDesktop` succeeds with compiler plugin mode
- [ ] Build log shows Quo Vadis compiler plugin being loaded
- [ ] No-op plugin does not break compilation
- [ ] Build log shows `modulePrefix` value being received

---

#### Task 1D.2: Verify multi-platform compilation

**Description**: Ensure the compiler plugin loads correctly for all KMP targets.

**Acceptance Criteria**:
- [ ] Desktop (JVM) compilation succeeds with plugin
- [ ] Android compilation succeeds with plugin
- [ ] iOS compilation succeeds with plugin (uses native artifact)
- [ ] JS compilation succeeds with plugin
- [ ] WasmJS compilation succeeds with plugin

---

#### Task 1D.3: Verify KSP fallback mode

**Description**: Ensure existing KSP mode still works when `useCompilerPlugin = false`.

**Acceptance Criteria**:
- [ ] `quoVadis { useCompilerPlugin = false }` maintains current KSP behavior
- [ ] All existing tests pass
- [ ] No regression in build times

---

## Files Created/Modified Summary

| Action | File | Phase |
|--------|------|-------|
| Create | `quo-vadis-compiler-plugin/build.gradle.kts` | 1A.1 |
| Create | `quo-vadis-compiler-plugin/src/main/kotlin/.../QuoVadisCommandLineProcessor.kt` | 1B.1 |
| Create | `quo-vadis-compiler-plugin/src/main/kotlin/.../QuoVadisConfigurationKeys.kt` | 1B.2 |
| Create | `quo-vadis-compiler-plugin/src/main/kotlin/.../QuoVadisCompilerPluginRegistrar.kt` | 1B.3 |
| Create | `quo-vadis-compiler-plugin/src/main/resources/META-INF/services/...` | 1B.4 |
| Create | `quo-vadis-compiler-plugin-native/build.gradle.kts` | 1A.2 |
| Modify | `settings.gradle.kts` | 1A.3 |
| Modify | `gradle/libs.versions.toml` | 1A.3 |
| Modify | `quo-vadis-gradle-plugin/build.gradle.kts` | 1C.3 |
| Modify | `quo-vadis-gradle-plugin/.../QuoVadisPlugin.kt` | 1C.1 |
| Modify | `quo-vadis-gradle-plugin/.../QuoVadisExtension.kt` | 1C.1 |
| Create | `quo-vadis-gradle-plugin/.../QuoVadisCompilerSubplugin.kt` | 1C.1 |
| Modify | `composeApp/build.gradle.kts` | 1D.1 |

## Task Dependency Graph

```
1A.1 (main module) ──→ 1B.1 (CommandLineProcessor)
         │              1B.2 (ConfigKeys)
         │              1B.3 (Registrar)
         │              1B.4 (SPI files)
         │                    │
1A.2 (native module) ────────┤
         │                    │
1A.3 (settings) ─────────────┤
                              ↓
                   1C.1 (Gradle plugin migration)
                   1C.2 (Auto-apply core dep)
                   1C.3 (Gradle build config)
                              │
                              ↓
                   1D.1 (Demo app verification)
                   1D.2 (Multi-platform verification)
                   1D.3 (KSP fallback verification)
```

**Parallelizable**: 1A.1, 1A.2, 1A.3 can be done in parallel. 1B.1–1B.4 can be done in parallel. 1C.1–1C.3 can be done in parallel. 1D.1–1D.3 require all prior tasks.
