# Migration Guide: KSP → Compiler Plugin

This guide covers migrating from the KSP-based code generation to the K2 compiler plugin for Quo Vadis navigation.

## Why Migrate?

- **Faster builds**: No separate KSP processing pass — code generation happens during normal compilation
- **Better IDE support**: FIR synthetic declarations provide instant autocomplete without requiring a build
- **Multi-module auto-discovery**: `@NavigationRoot` automatically aggregates all feature module configs
- **Unified toolchain**: Single compiler plugin replaces KSP processor + generated source management
- **Future-proof**: KSP module is deprecated and will be removed in a future version

## Prerequisites

- Kotlin 2.1.0 or later (K2 compiler required)
- Quo Vadis 0.x.x or later (version with compiler plugin support)

## Step 1: Update Build Configuration

### Single Module

**Before (KSP):**
```kotlin
// build.gradle.kts
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.ksp)           // ← Remove this
    alias(libs.plugins.quoVadis)
}

quoVadis {
    modulePrefix = "MyApp"
    useCompilerPlugin = false          // ← Was KSP mode
    useLocalKsp = true                 // ← Remove this
}
```

**After (Compiler Plugin):**
```kotlin
// build.gradle.kts
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    // No KSP plugin needed
    alias(libs.plugins.quoVadis)
}

quoVadis {
    modulePrefix = "MyApp"
    // useCompilerPlugin defaults to true — no configuration needed
}
```

### Multi-Module Project

**Before (KSP) — App module manually combines configs:**
```kotlin
val appConfig = Feature1NavigationConfig + Feature2NavigationConfig + AppNavigationConfig
val navigator = rememberQuoVadisNavigator(MainTabs::class, appConfig)
```

**After (Compiler Plugin) — Auto-discovery with @NavigationRoot:**
```kotlin
// In your app module, add @NavigationRoot to any class:
@NavigationRoot
object MyApp

// Use the generated aggregated config:
val navigator = rememberQuoVadisNavigator(MainTabs::class, MyAppNavigationConfig)
```

The compiler plugin automatically scans the classpath for all `GeneratedNavigationConfig` implementations and merges them.

> **Note**: Manual `+` operator composition still works as a fallback.

### Feature Modules

Feature modules require no changes to their navigation annotations. Just update the build configuration:

```kotlin
// feature/build.gradle.kts
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.quoVadis)
}

quoVadis {
    modulePrefix = "Feature1"
}
```

## Step 2: Remove KSP Configuration

1. Remove `alias(libs.plugins.ksp)` from all module `plugins {}` blocks
2. Remove any `ksp(...)` dependency declarations
3. Remove `quoVadis { useLocalKsp = true }` if present
4. Remove KSP-generated source directory configurations (e.g., `kotlin.srcDir("build/generated/ksp/...")`)
5. Clean build: `./gradlew clean`

## Step 3: Verify Build

```bash
# Build all modules
./gradlew build

# Or use the E2E verification script
./scripts/e2e-compiler-plugin.sh all
```

## Annotation Changes

### No Code Changes Required

All navigation annotations (`@Stack`, `@Destination`, `@Screen`, `@Tabs`, `@TabItem`, `@Pane`, `@PaneItem`, `@Transition`, `@Argument`) remain exactly the same. No source code changes are needed.

### Retention Level Changes

Some annotation retention levels changed from `SOURCE` to `BINARY`:

| Annotation | Before | After | Impact |
|-----------|--------|-------|--------|
| `@Stack` | SOURCE | BINARY | None — annotation now visible in compiled output |
| `@Destination` | SOURCE | BINARY | None |
| `@Screen` | SOURCE | BINARY | None |
| `@Tabs` | SOURCE | BINARY | None |
| `@TabItem` | SOURCE | BINARY | None |
| `@Pane` | SOURCE | BINARY | None |
| `@PaneItem` | SOURCE | BINARY | None |
| `@NavigationRoot` | N/A | BINARY | New annotation for multi-module auto-discovery |
| `@Argument` | SOURCE | SOURCE | Unchanged |
| `@TabsContainer` | RUNTIME | RUNTIME | Unchanged |
| `@PaneContainer` | RUNTIME | RUNTIME | Unchanged |
| `@Transition` | RUNTIME | RUNTIME | Unchanged |

**Why?** `BINARY` retention allows the compiler plugin to read annotations from compiled dependencies, enabling cross-module navigation discovery.

**User impact**: None. Annotations work identically in source code. The only difference is that they're now preserved in `.class`/`.klib` files.

## New: @NavigationRoot Annotation

The `@NavigationRoot` annotation enables automatic multi-module config aggregation:

```kotlin
@NavigationRoot
object MyApp
```

**Rules:**
- Apply to any class or object in your **app module** (the module that assembles the final application)
- Only one `@NavigationRoot` per compilation unit
- The generated config name is derived from the annotated class: `MyApp` → `MyAppNavigationConfig`
- You can specify a custom prefix: `@NavigationRoot(prefix = "CustomPrefix")`

## Behavioral Differences

### Expected: Identical Behavior

The compiler plugin generates functionally identical output to KSP. All navigation patterns work the same:
- Stack push/pop navigation
- Tab-based navigation with independent backstacks
- Pane-based adaptive layouts
- Deep link resolution with type-safe arguments
- Transition animations
- Scope-aware navigation
- MVI container lifecycle management

### Differences

| Aspect | KSP | Compiler Plugin |
|--------|-----|-----------------|
| Generated source files | Visible in `build/generated/ksp/` | No generated source files (IR-level generation) |
| IDE autocomplete | Requires build to see generated classes | Instant via FIR synthetic declarations |
| Build speed | Separate KSP pass | Integrated into compilation |
| Debugging generated code | Inspect generated `.kt` files | Use `-PquoVadis.dumpIr=true` for IR dump |
| Multi-module | Manual `+` operator | Automatic via `@NavigationRoot` |

## Debugging

### No Generated Source Files

Unlike KSP, the compiler plugin doesn't produce `.kt` files you can inspect. Instead:

1. **IR Dump**: Add `-PquoVadis.dumpIr=true` to your Gradle command to dump the generated IR
2. **Compiler verbose mode**: Use `--info` or `--debug` Gradle flags for detailed compiler output
3. **Test with kotlin-compile-testing**: Use the test infrastructure from `quo-vadis-compiler-plugin` module

### IDE Not Showing Generated Classes

If the IDE doesn't autocomplete generated navigation configs:

1. Ensure K2 mode is enabled in your IDE (IntelliJ IDEA / Android Studio)
2. Invalidate caches and restart: `File → Invalidate Caches... → Invalidate and Restart`
3. Verify the plugin is applied: check for `quoVadis` extension in Gradle sync output

## FAQ

### Do I need to change my annotations?
No. All annotations (`@Stack`, `@Destination`, `@Screen`, etc.) are identical. Only the code generation backend changed.

### Will my deep links still work?
Yes. Route patterns, argument extraction, and URI creation work identically.

### What Kotlin version is required?
Kotlin 2.1.0 or later. The compiler plugin uses K2 FIR and IR APIs.

### Can I use KSP and compiler plugin in the same module?
No. Choose one per module. The Gradle plugin will error if both are active.

### Can different modules use different backends?
Technically possible but not recommended. All modules should migrate together to avoid version conflicts.

### My IDE doesn't show generated classes — how do I fix it?
Ensure K2 mode is enabled in IDE settings. Invalidate caches if needed. The compiler plugin uses FIR synthetic declarations which require K2 support.

### How do I debug what the compiler plugin generates?
Use `-PquoVadis.dumpIr=true` Gradle property to dump IR output. For detailed logging, use `./gradlew build --info`.

### I'm getting a "duplicate class" error — what happened?
You likely have both KSP and compiler plugin active. Remove the KSP plugin from your `plugins {}` block.

### What's the deprecation timeline for KSP?

| Version | KSP Status |
|---------|------------|
| Current | Deprecated with WARNING — both backends supported |
| Current + 2 minor | Deprecated with ERROR — KSP fails to compile |
| Current + 3 minor | KSP module removed |

### Is the compiler plugin stable?
The compiler plugin uses Kotlin's `ExperimentalCompilerApi`. While the plugin itself is thoroughly tested, Kotlin compiler API changes may require plugin updates when upgrading Kotlin versions.

## Rollback

If you encounter issues and need to revert to KSP:

1. Re-add `alias(libs.plugins.ksp)` to your `plugins {}` block
2. Set `quoVadis { useCompilerPlugin = false }` in each module
3. Clean and rebuild: `./gradlew clean build`

The KSP processor is still functional (just deprecated) and produces identical navigation output.
