# Migration Companion: KSP ↔ Compiler Backend

This guide is the focused cutover companion for existing KSP users who want to switch Quo Vadis to the compiler backend, or vice versa.
Both backends support the full consumer API: module-prefixed `NavigationConfig` objects, `@NavigationRoot` aggregation, `navigationConfig<T>()` lookup, deep-link handlers, and `+` composition.

If you are still deciding which backend to adopt, start with [COMPILER-PLUGIN.md](COMPILER-PLUGIN.md) for backend selection guidance, installation options, limitations, and rollback context.

## Why Switch Backends?

- **Faster builds**: No separate KSP processing pass — code generation happens during normal compilation
- **Better IDE support**: FIR synthetic declarations provide instant autocomplete without requiring a build
- **Shared generated contract**: Both backends generate module-level config objects that can be composed explicitly
- **Full feature parity**: `@NavigationRoot`, `navigationConfig<T>()`, and aggregated configs work with both backends

> **Current rollout posture**: KSP is the default backend. Opt into the compiler backend with
> `quoVadis.backend=compiler`. Switching requires only a Gradle property change + clean build — no source code changes.

> **Warning**: Do not wire Quo Vadis KSP processing and compiler-backend processing for the same module at the same time.

## Prerequisites

- Kotlin 2.1.0 or later (K2 compiler required)
- Quo Vadis 0.x.x or later (version with compiler plugin support)

## Step 1: Choose a Backend

### Single Module

**Default (KSP):**
```kotlin
// build.gradle.kts
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.ksp)
    alias(libs.plugins.quoVadis)
}

quoVadis {
    modulePrefix = "MyApp"
}
```

**Compiler backend:**
```kotlin
// gradle.properties
quoVadis.backend=compiler
```

```kotlin
// build.gradle.kts
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.ksp) // optional during the experiment to preserve a property-only switch
    alias(libs.plugins.quoVadis)
}

quoVadis {
    modulePrefix = "MyApp"
}
```

The deprecated `useCompilerPlugin` Boolean alias still works for one transition window, but `backend`
or the root `quoVadis.backend` property is the primary API now.

### Multi-Module Project

**Shared contract in both backends — app module manually combines configs:**
```kotlin
val appConfig = Feature1NavigationConfig + Feature2NavigationConfig + AppNavigationConfig
val navigator = rememberQuoVadisNavigator(MainTabs::class, appConfig)
```

**Auto-discovery with `@NavigationRoot` (both backends):**
```kotlin
// In your app module, add @NavigationRoot to any class:
@NavigationRoot
object MyApp

// Use the generated aggregated config:
val navigator = rememberQuoVadisNavigator(MainTabs::class, MyAppNavigationConfig)
```

Both backends automatically discover generated module configs and merge them when `@NavigationRoot` is present.

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

## Step 2: Remove KSP-Specific Wiring Only When You Standardize on Compiler Mode

1. Remove any manual `ksp(...)` dependency declarations that point at `quo-vadis-ksp`
2. Remove `quoVadis { useLocalKsp = true }` if present
3. Remove manual KSP-generated source directory configurations (for example `kotlin.srcDir("build/generated/ksp/...")`)
4. Optionally remove `alias(libs.plugins.ksp)` once you no longer need property-only backend flips
5. Clean build after every backend flip: `./gradlew clean`

## Step 3: Verify Build

```bash
# Build all modules
./gradlew build

# Or use the E2E verification script
./scripts/e2e-compiler-plugin.sh all
```

## Annotation Changes

### No Code Changes Required

All navigation annotations (`@Stack`, `@Destination`, `@Screen`, `@Tabs`, `@TabItem`, `@Pane`, `@PaneItem`, `@Transition`, `@Argument`) remain exactly the same. No source code changes are needed when switching backends.

### Tab Annotation Pattern Change

> **Note:** This is independent of the KSP↔compiler backend migration. If you were previously using the old `@Tabs(name, initialTab, items=[...])` + marker `@TabItem` pattern, you need to migrate to the new child-to-parent pattern regardless of backend choice.

**Before:**
```kotlin
@Tabs(name = "mainTabs", initialTab = HomeTab::class, items = [HomeTab::class, ProfileTab::class])
sealed class MainTabs : NavDestination {
    @TabItem
    @Destination(route = "main/home")
    data object HomeTab : MainTabs()

    @TabItem
    @Destination(route = "main/profile")
    data object ProfileTab : MainTabs()
}
```

**After:**
```kotlin
@Tabs(name = "mainTabs")
class MainTabs : NavDestination {
    companion object : NavDestination
}

@TabItem(parent = MainTabs::class, ordinal = 0)
@Destination(route = "main/home")
data object HomeTab : NavDestination

@TabItem(parent = MainTabs::class, ordinal = 1)
@Destination(route = "main/profile")
data object ProfileTab : NavDestination
```

Key changes:
- `@Tabs` only takes `name` — no `initialTab` or `items` parameters
- `@TabItem` takes `parent` (KClass reference) and `ordinal` (Int position, 0-based)
- `ordinal = 0` defines the initial tab
- Ordinals must be contiguous starting from 0 (validated at compile time)
- Tab items no longer need to be nested inside the `@Tabs` sealed class — cross-module tabs are supported naturally

See [ANNOTATIONS.md](ANNOTATIONS.md#tabs-and-tabitem-annotations) for full details.

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

Deep-link parity is covered by compiler-plugin regression tests for stack destinations, flat tab items, pane items, standalone destinations, and query-plus-path argument resolution.

### Differences

| Aspect | KSP | Compiler Plugin |
|--------|-----|-----------------|
| Generated source files | Visible in `build/generated/ksp/` | No generated source files (IR-level generation) |
| IDE autocomplete | Requires build to see generated classes | Instant via FIR synthetic declarations |
| Build speed | Separate KSP pass | Integrated into compilation |
| Debugging generated code | Inspect generated `.kt` files | Use `-PquoVadis.dumpIr=true` for IR dump |
| Multi-module | Manual `+` or `@NavigationRoot` | Manual `+` or `@NavigationRoot` |

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
Yes. Route patterns, argument extraction, and URI creation work identically, including flat tab items, pane items, standalone destinations, and query-backed `@Argument` values. When query and path parameters share a key, the path value wins.

### What Kotlin version is required?
Kotlin 2.1.0 or later. The compiler plugin uses K2 FIR and IR APIs.

### Can I leave the KSP Gradle plugin applied in compiler mode?
Yes. During the experiment, the bare KSP Gradle plugin can stay applied to preserve a property-only backend switch.
What is not allowed in compiler mode is Quo Vadis-specific KSP wiring such as `quo-vadis-ksp` dependencies or
`useLocalKsp = true`.

### Can different modules use different backends?
That is currently unsupported for the rollout. Set the backend once at the root build level and keep all Quo Vadis
modules on the same backend.

### Should I migrate every project right now?
No. KSP remains the default backend. Both backends support the same consumer API, so switch when the compiler backend's trade-offs (faster builds, better IDE support) suit your project.

### My IDE doesn't show generated classes — how do I fix it?
Ensure K2 mode is enabled in IDE settings. Invalidate caches if needed. The compiler plugin uses FIR synthetic declarations which require K2 support.

### How do I debug what the compiler plugin generates?
Use `-PquoVadis.dumpIr=true` Gradle property to dump IR output. For detailed logging, use `./gradlew build --info`.

### I'm getting a "duplicate class" error — what happened?
You likely flipped to compiler mode while still wiring the Quo Vadis KSP processor. Remove any
`quo-vadis-ksp` KSP dependencies, clear `useLocalKsp`, and run a clean build.

### Is the compiler plugin stable?
The compiler plugin uses Kotlin's `ExperimentalCompilerApi`, and Kotlin compiler API changes
may require plugin updates when upgrading Kotlin versions.

## Rollback

If you encounter issues and need to revert to KSP:

1. Set `quoVadis.backend=ksp` at the root build level
2. Re-add manual Quo Vadis KSP wiring only if you had removed it entirely
3. Clean and rebuild: `./gradlew clean build`

The KSP backend remains the stable default and produces the same module-level generated contract used for explicit
config composition.
