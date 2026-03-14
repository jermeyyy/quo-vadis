# Compiler Plugin / KSP Interchangeability Plan

## Overview

This document defines the work required to achieve **true zero-code-change backend switching** between the Quo Vadis K2 compiler plugin and KSP code generation backends.

The goal is full API parity: consumer code written against the public Quo Vadis API — including `@NavigationRoot`, `navigationConfig<T>()`, aggregated configs, and multi-module discovery — must work identically regardless of which backend is active. Switching backends should require only a Gradle property change (`quoVadis.backend=ksp` ↔ `quoVadis.backend=compiler`), with zero modifications to application or feature module source code.

### Design Principles

1. **Single consumer API surface** — `@NavigationRoot` and `navigationConfig<T>()` are shared features, not compiler-plugin-only.
2. **Backend-appropriate implementation** — The compiler plugin optimizes via compile-time call-site rewriting; KSP uses a runtime registry. Both arrive at the same observable behavior.
3. **Annotation-based discovery** — Both backends use `@GeneratedConfig` for multi-module discovery instead of a marker interface.
4. **Minimal core surface** — `quo-vadis-core` gains only what is strictly necessary (`@GeneratedConfig` annotation, `NavigationConfigRegistry` singleton, updated `navigationConfig<T>()` stub).

## Current State

### KSP responsibilities

- `quo-vadis-ksp` collects `@Stack`, `@Tabs`, `@Pane`, `@Destination`, `@Screen`, wrapper, and transition annotations through extractor classes in `QuoVadisSymbolProcessor`.
- It validates the collected graph through `ValidationEngine`.
- It generates module-prefixed source files in `com.jermey.quo.vadis.generated`.
- The main generated config is `${modulePrefix}NavigationConfig`, produced by `NavigationConfigGenerator`.
- The generated config implements `NavigationConfig` directly (not `GeneratedNavigationConfig`).
- The generated config delegates most non-composable registrations to a DSL-built `baseConfig`, then supplies custom `screenRegistry`, `containerRegistry`, `deepLinkRegistry`, `paneRoleRegistry`, `buildNavNode`, `plus`, and `roots` behavior.
- It also generates `${modulePrefix}DeepLinkHandler`, which implements `DeepLinkRegistry`.
- **KSP does not process `@NavigationRoot`** — it has no aggregation or auto-discovery.
- **KSP does not generate an aggregated app-level config** — consumers must manually compose module configs with `+`.
- **KSP does not register configs into any runtime registry** — `navigationConfig<T>()` throws when KSP is the active backend.

### Compiler plugin responsibilities

- `quo-vadis-compiler-plugin` synthesizes FIR declarations for:
  - `${modulePrefix}NavigationConfig` — implements `GeneratedNavigationConfig`
  - `${modulePrefix}DeepLinkHandler`
  - `${modulePrefix}ScreenRegistryImpl`
  - `${modulePrefix}__AggregatedConfig` — implements `NavigationConfig`, delegates to a composition of discovered module configs
- IR generators populate bodies for config, deep-link handler, screen registry, container registry, pane role registry, and aggregation.
- `NavigationConfigCallTransformer` rewrites `navigationConfig<T>()` call sites to reference the generated `${prefix}__AggregatedConfig` object.
- `MultiModuleDiscovery` scans the current module, dependency descriptors, and classpath roots for `GeneratedNavigationConfig` implementors (by checking super-type relationships).
- `NavigationRootUniquenessChecker` enforces that at most one `@NavigationRoot` exists per compilation unit.

### Core library surface

- `NavigationConfig` — the primary configuration interface in `quo-vadis-core/.../navigation/config/NavigationConfig.kt`.
- `GeneratedNavigationConfig` — a marker interface extending `NavigationConfig` in `quo-vadis-core/.../navigation/config/GeneratedNavigationConfig.kt`. Used exclusively by the compiler plugin for classpath discovery. KSP-generated configs do not implement it.
- `navigationConfig<T>()` — a stub function in `NavigationConfig.kt` that throws `IllegalStateException` ("requires the Quo Vadis compiler plugin"). The compiler plugin replaces call sites at IR level; KSP has no mechanism to make this function work.
- `@NavigationRoot` — annotation in `quo-vadis-annotations` processed only by the compiler plugin.

### Gradle/plugin wiring today

- `quo-vadis-gradle-plugin` exposes `QuoVadisExtension` with:
  - `modulePrefix` (defaults to `project.name.toCamelCase()`)
  - `backend` (`QuoVadisBackend` enum: `KSP` or `COMPILER`, defaults to `KSP`)
  - `useCompilerPlugin` (deprecated Boolean alias)
  - `useLocalKsp` (deprecated, for KSP dev mode)
- `QuoVadisPlugin.apply()` applies the compiler subplugin eagerly, then uses `afterEvaluate` to configure the selected backend.
- Backend resolution checks `backend` property first, falls back to deprecated `useCompilerPlugin`, defaults to `KSP`.
- Validation already warns about conflicting settings and errors on incompatible configurations (e.g., `useLocalKsp` in compiler mode).

### Consumer integration (demo app)

- `composeApp/.../DI.kt` defines `@NavigationRoot object AppNavigation` and calls `navigationConfig<AppNavigation>()`.
- This path is currently **compiler-plugin-only** — switching to KSP breaks the app because:
  1. KSP does not process `@NavigationRoot`
  2. `navigationConfig<T>()` throws at runtime without the compiler plugin's call-site rewrite
- The DI file contains commented-out KSP-era code showing manual `+` composition as a fallback.

### Test coverage today

- The compiler plugin has substantial compile-testing coverage for config generation, deep links, aggregation, multi-module behavior, and IR verification.
- KSP has extractor-level tests only and lacks comparable end-to-end contract tests for generated output.
- No shared contract test suite exists that validates both backends produce equivalent behavior.

## Target State

### Full API parity

Both backends support the complete consumer-facing API identically:

| Feature | Compiler Plugin | KSP |
|---------|----------------|-----|
| Module-level config generation | `${prefix}NavigationConfig` | `${prefix}NavigationConfig` |
| `@GeneratedConfig` annotation on generated configs | Yes | Yes |
| `@NavigationRoot` processing | Yes (FIR) | Yes (KSP round) |
| Aggregated config generation | `${prefix}__AggregatedConfig` | `${prefix}__AggregatedConfig` |
| Multi-module discovery | Classpath scan for `@GeneratedConfig` | Classpath scan for `@GeneratedConfig` |
| `navigationConfig<T>()` resolution | Compile-time call-site rewrite (IR) | Runtime registry lookup |
| Deep-link handler generation | Yes | Yes |
| `+` manual composition | Yes | Yes |

### Architecture overview

```
┌─────────────────────────────────────────────────────────────┐
│ quo-vadis-core                                              │
│                                                             │
│  NavigationConfig (interface)                               │
│  @GeneratedConfig (annotation)              ← NEW          │
│  NavigationConfigRegistry (internal object) ← NEW          │
│  navigationConfig<T>() → checks registry   ← UPDATED      │
│                                                             │
└─────────────────────────────────────────────────────────────┘
         ▲                            ▲
         │ implements                 │ implements
┌────────┴────────┐          ┌───────┴─────────┐
│ Compiler Plugin │          │ KSP             │
│                 │          │                 │
│ @GeneratedConfig│          │ @GeneratedConfig│
│ on module config│          │ on module config│
│                 │          │                 │
│ IR: rewrite     │          │ Generated init  │
│ navigationConfig│          │ block registers │
│ <T>() → object  │          │ into Registry   │
│                 │          │                 │
│ IR: aggregate   │          │ KSP: aggregate  │
│ via classpath   │          │ via classpath   │
│ @GeneratedConfig│          │ @GeneratedConfig│
│ scan            │          │ scan            │
└─────────────────┘          └─────────────────┘
```

### Consumer code (identical for both backends)

```kotlin
@NavigationRoot
object AppNavigation

// This call works with either backend:
val config: NavigationConfig = navigationConfig<AppNavigation>()
```

## Architecture Changes

### 1. Drop `GeneratedNavigationConfig` marker interface

**What changes:**
- Delete `quo-vadis-core/.../config/GeneratedNavigationConfig.kt`.
- Both backends generate configs that implement `NavigationConfig` directly.
- Discovery shifts from super-type checking to annotation scanning (see next section).

**Migration path:**
- Any existing references to `GeneratedNavigationConfig` in compiler plugin code (`MultiModuleDiscovery`, IR generators, tests) are updated to use `@GeneratedConfig` annotation checking.
- KSP-generated configs already implement `NavigationConfig` directly — no change needed on the KSP generation side.
- Documentation references (KDoc comments in `NavigationConfig.kt`, `NavigationHost.kt`, `Navigator.kt`, `TreeNavigator.kt`) are updated.

### 2. Add `@GeneratedConfig` annotation

**Location:** `quo-vadis-core/.../navigation/config/GeneratedConfig.kt`

```kotlin
package com.jermey.quo.vadis.core.navigation.config

/**
 * Marks a NavigationConfig implementation as generated by a Quo Vadis backend
 * (compiler plugin or KSP).
 *
 * Both backends annotate their generated config objects with this annotation.
 * Multi-module discovery scans for this annotation to find feature module configs
 * during aggregation.
 *
 * This annotation replaces the former [GeneratedNavigationConfig] marker interface
 * for discovery purposes.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class GeneratedConfig
```

**Why an annotation instead of an interface:**
- Decouples discovery from the type hierarchy.
- Both backends can annotate their generated configs without changing supertypes.
- Runtime retention enables KSP's classpath scanning to use reflection-based discovery.
- The annotation is lightweight and does not pollute the `NavigationConfig` type hierarchy.

### 3. Add `NavigationConfigRegistry`

**Location:** `quo-vadis-core/.../navigation/config/NavigationConfigRegistry.kt`

```kotlin
package com.jermey.quo.vadis.core.navigation.config

import kotlin.reflect.KClass

/**
 * Internal registry for NavigationConfig instances populated by generated code.
 *
 * KSP-generated aggregated configs register themselves here during class loading.
 * The [navigationConfig] function checks this registry at runtime when the compiler
 * plugin's compile-time rewrite is not active.
 *
 * The compiler plugin bypasses this registry entirely — it rewrites call sites to
 * reference the generated object directly. The registry exists solely to support
 * the KSP backend.
 */
@InternalQuoVadisApi
object NavigationConfigRegistry {
    private val configs = mutableMapOf<KClass<*>, NavigationConfig>()

    fun register(rootClass: KClass<*>, config: NavigationConfig) {
        configs[rootClass] = config
    }

    fun get(rootClass: KClass<*>): NavigationConfig? = configs[rootClass]
}
```

**Key properties:**
- Internal to `quo-vadis-core` — not part of the public API.
- Thread-safe registration is not required because registration happens during class initialization (single-threaded by JVM spec), and reads happen after initialization.
- The `KClass<*>` key corresponds to the `@NavigationRoot`-annotated class (e.g., `AppNavigation::class`).

### 4. Update `navigationConfig<T>()`

**Current** (in `NavigationConfig.kt`):
```kotlin
fun <T> navigationConfig(): NavigationConfig {
    error("navigationConfig<T>() requires the Quo Vadis compiler plugin. ...")
}
```

**Updated:**
```kotlin
inline fun <reified T> navigationConfig(): NavigationConfig {
    return NavigationConfigRegistry.get(T::class)
        ?: error(
            "navigationConfig<${T::class.simpleName}>() could not resolve a NavigationConfig. " +
            "Ensure the Quo Vadis Gradle plugin is applied and either the compiler plugin or KSP backend is active."
        )
}
```

**Behavior by backend:**
- **Compiler plugin:** The IR `NavigationConfigCallTransformer` replaces the call site before the function body ever executes. The registry lookup is dead code in compiler mode. No runtime overhead.
- **KSP:** The function executes normally. The KSP-generated aggregated config registers itself into `NavigationConfigRegistry` during its `init` block. The registry lookup succeeds at runtime.

**Note:** The function signature changes from `fun <T>` to `inline fun <reified T>` to enable `T::class` access. This is a source-compatible change for all call sites (callers already use `navigationConfig<AppNavigation>()` with a concrete type argument). The compiler plugin's `NavigationConfigCallTransformer` already matches on the FQN `com.jermey.quo.vadis.core.navigation.config.navigationConfig` and will continue to work because the call sites look the same.

### 5. Compiler plugin: switch from `GeneratedNavigationConfig` to `@GeneratedConfig`

**`MultiModuleDiscovery`:**
- Change `GENERATED_NAV_CONFIG_CLASS_ID` from `GeneratedNavigationConfig` class ID to `GeneratedConfig` annotation class ID.
- Replace super-type checking (`classSymbol.owner.superTypes.any { it.classOrNull == markerClass }`) with annotation checking (scan for classes annotated with `@GeneratedConfig`).
- For descriptor-based scanning: enumerate classes in `com.jermey.quo.vadis.generated` and check for `@GeneratedConfig` annotation.
- For classpath scanning: check `.class` files for the `GeneratedConfig` annotation in the bytecode (annotation with `RUNTIME` retention is present in class metadata).
- For current-module IR scanning: check `IrClass.annotations` for `@GeneratedConfig`.

**FIR declaration generation:**
- Generated `${modulePrefix}NavigationConfig` objects get `@GeneratedConfig` annotation instead of implementing `GeneratedNavigationConfig`.
- The generated object's supertype list contains only `NavigationConfig`, not `GeneratedNavigationConfig`.

**IR body synthesis:**
- No fundamental change — the aggregated config body synthesis continues to compose discovered configs. The discovery input changes (annotation-based instead of interface-based).

### 6. KSP: add `@NavigationRoot` processing

**`QuoVadisSymbolProcessor`:**
- Add a new processing step that looks for classes annotated with `@NavigationRoot`.
- Validate single-root constraint (same as compiler plugin's `NavigationRootUniquenessChecker`).
- When `@NavigationRoot` is found, trigger aggregated config generation.

**New KSP round behavior:**
1. In a standard processing round, KSP generates per-module configs as today (`${modulePrefix}NavigationConfig`).
2. If `@NavigationRoot` is present, KSP triggers an additional generation step that:
   a. Scans the classpath for classes annotated with `@GeneratedConfig` in the `com.jermey.quo.vadis.generated` package.
   b. Includes the current module's generated config.
   c. Generates `${modulePrefix}__AggregatedConfig` that composes all discovered configs using `+`.
   d. Generates an `init` block in the aggregated config that registers itself into `NavigationConfigRegistry`.

### 7. KSP: aggregated config generation

**New file:** `quo-vadis-ksp/.../generators/AggregatedConfigGenerator.kt`

The generated aggregated config object should:
- Be annotated with `@GeneratedConfig`.
- Implement `NavigationConfig` by delegating to a `+`-composed chain of all discovered module configs.
- Contain an `init` block that calls `NavigationConfigRegistry.register(RootClass::class, this)` where `RootClass` is the `@NavigationRoot`-annotated class.
- Order feature configs by FQN for deterministic composition (same as compiler plugin).

**Generated code example:**
```kotlin
@GeneratedConfig
object ComposeApp__AggregatedConfig : NavigationConfig by (
    ComposeAppNavigationConfig +
    Feature1NavigationConfig +
    Feature2NavigationConfig
) {
    init {
        NavigationConfigRegistry.register(AppNavigation::class, this)
    }
}
```

### 8. KSP: multi-module discovery via `@GeneratedConfig`

**Discovery strategy for KSP:**

KSP runs in the app module where `@NavigationRoot` is applied. At that point, dependency module JARs are on the classpath.

1. **Classpath scanning:** Use `Resolver`'s classpath or the KSP environment to find classes in the `com.jermey.quo.vadis.generated` package annotated with `@GeneratedConfig`.
2. **Filter:** Exclude aggregated configs (suffix `__AggregatedConfig`), `EmptyNavigationConfig`, `CompositeNavigationConfig`, and other internal types.
3. **Include current module:** Add the current module's generated config to the list.
4. **Sort by FQN** for deterministic output.

**Implementation note:** KSP's `Resolver.getDeclarationsFromPackage()` may not cover dependency JARs in all KSP versions. If direct resolution is insufficient, fall back to classpath scanning via `ServiceLoader`-style resource enumeration or raw bytecode scanning of JARs for the annotation. The `@GeneratedConfig` annotation has `RUNTIME` retention, making it detectable in compiled class files.

### 9. Gradle plugin: no consumer-facing changes

The existing `QuoVadisBackend` enum and `quoVadis.backend` property already support the switch. No new Gradle-side API is needed.

Internal changes:
- KSP configuration passes an additional KSP option (e.g., `quoVadis.enableNavigationRoot=true`) if the processor needs explicit opt-in for the heavier aggregation step. Alternatively, the processor can detect `@NavigationRoot` presence without any flag.
- Validation messaging is updated to remove references to `navigationConfig<T>()` being compiler-plugin-only.


## Gap Analysis

### Gap 1: `@GeneratedConfig` annotation does not exist

**Current:** Neither backend uses a `@GeneratedConfig` annotation. The compiler plugin relies on `GeneratedNavigationConfig` interface for discovery. KSP has no discovery mechanism.

**Impact:** Both backends need a unified discovery mechanism before multi-module aggregation can work consistently.

**Required work:**
- Add `@GeneratedConfig` annotation to `quo-vadis-core`
- Both backends annotate their generated configs with it
- Update compiler plugin's `MultiModuleDiscovery` to use annotation-based scanning

### Gap 2: `NavigationConfigRegistry` does not exist

**Current:** `navigationConfig<T>()` throws unconditionally. There is no runtime mechanism for KSP-generated code to make this function work.

**Impact:** KSP cannot support `navigationConfig<T>()` without a registry.

**Required work:**
- Add `NavigationConfigRegistry` internal singleton to `quo-vadis-core`
- Update `navigationConfig<T>()` to check the registry before throwing
- Ensure the registry is KMP-compatible (no JVM-only APIs)

### Gap 3: KSP does not process `@NavigationRoot`

**Current:** KSP ignores `@NavigationRoot` entirely.

**Impact:** KSP cannot generate an aggregated config, so `navigationConfig<T>()` has nothing to resolve to.

**Required work:**
- Add `@NavigationRoot` extraction to `QuoVadisSymbolProcessor`
- Validate single-root constraint
- Trigger aggregated config generation when annotation is present

### Gap 4: KSP has no multi-module discovery

**Current:** KSP generates per-module configs but cannot discover configs from dependency modules.

**Impact:** Even with `@NavigationRoot` processing, the aggregated config would only contain the current module's config.

**Required work:**
- Implement `ClasspathConfigDiscovery` in `quo-vadis-ksp`
- Scan compiled dependency classes for `@GeneratedConfig` annotation
- Generate `+` composition in the aggregated config from discovered modules

### Gap 5: `GeneratedNavigationConfig` marker interface still exists

**Current:** Compiler plugin uses `GeneratedNavigationConfig` interface. KSP does not.

**Impact:** Two different discovery mechanisms would be needed unless this is unified.

**Required work:**
- Remove `GeneratedNavigationConfig` interface from `quo-vadis-core`
- Update compiler plugin to use `@GeneratedConfig` annotation instead
- Update `MultiModuleDiscovery` to scan for annotation rather than interface

### Gap 6: Duplicate-generation guardrails

**Current:** The Gradle plugin validates that KSP processor dependencies are not present in compiler mode. Backend enum and validation are already in place.

**Remaining gap:** No validation that `clean` is required after switching backends.

**Required work:**
- Document `clean` requirement on backend flip
- Consider a Gradle task that detects stale generated sources from the other backend

### Gap 7: Test strategy is not symmetric

**Current:** Compiler plugin behavior is heavily tested. KSP lacks comparable contract tests.

**Impact:** No authoritative parity signal when switching backends.

**Required work:**
- Introduce shared backend contract tests covering all features including `navigationConfig<T>()` resolution

### Gap 8: Local compiler-plugin development is fragile

**Current:** Compiler subplugin resolves by Maven coordinates. Local source changes require `mavenLocal` republication.

**Impact:** Developers can unknowingly test stale plugin binaries.

**Required work:**
- Document `mavenLocal` publication as mandatory local-dev workflow
- Consider a dev-only resolution mode

## Configuration Model

### Backend selection (already implemented)

```kotlin
quoVadis {
    backend = QuoVadisBackend.KSP  // or QuoVadisBackend.COMPILER
}
```

Or via `gradle.properties`:

```properties
quoVadis.backend=ksp
# or
quoVadis.backend=compiler
```

### Backward compatibility (already implemented)

- `useCompilerPlugin` remains as a deprecated alias
- `backend` takes precedence when both are set

### Backend policy rules

- One backend for all Quo Vadis modules in a build
- The Gradle plugin fails fast on incompatible manual wiring
- `clean` is required when flipping backends (documented, not enforced)

## Task Breakdown

### Phase 1: Core infrastructure

Goal: Add the annotation, registry, and update the `navigationConfig<T>()` function.

**Tasks:**

1. **Add `@GeneratedConfig` annotation** to `quo-vadis-core`
   - Retention: `RUNTIME`
   - Target: `CLASS`

2. **Add `NavigationConfigRegistry`** to `quo-vadis-core`
   - Internal object with `register(rootClass: KClass<*>, config: NavigationConfig)` and `get(rootClass: KClass<*>): NavigationConfig?`
   - KMP-compatible (no JVM-only APIs)

3. **Update `navigationConfig<T>()`** in `quo-vadis-core`
   - Change signature to `inline fun <reified T> navigationConfig(): NavigationConfig`
   - Check `NavigationConfigRegistry.get(T::class)` before throwing
   - Update error message to be backend-neutral

4. **Remove `GeneratedNavigationConfig` interface**
   - Delete `GeneratedNavigationConfig.kt`
   - Update all references in core, compiler plugin, and docs

### Phase 2: Compiler plugin migration

Goal: Migrate the compiler plugin from `GeneratedNavigationConfig` interface to `@GeneratedConfig` annotation.

Dependencies: Phase 1

**Tasks:**

1. **Update FIR declaration generation**
   - Generated config supertype: `NavigationConfig` only (remove `GeneratedNavigationConfig`)
   - Add `@GeneratedConfig` annotation to the generated config class

2. **Update `MultiModuleDiscovery`**
   - Change discovery from interface-based to annotation-based
   - Scan for `@GeneratedConfig` annotation in dependency descriptors and classpath
   - Update exclusion rules to skip `*__AggregatedConfig` classes

3. **Update `NavigationConfigCallTransformer`**
   - Verify it works with updated `navigationConfig<T>()` signature (`inline fun <reified T>`)
   - No call-site matching changes needed if it matches on FQN

4. **Update compiler plugin tests**
   - Remove assertions against `GeneratedNavigationConfig` supertype
   - Add assertions for `@GeneratedConfig` annotation presence
   - Verify `navigationConfig<T>()` still resolves correctly via call-site rewriting

### Phase 3: KSP full API parity

Goal: Add `@NavigationRoot` processing, aggregated config generation, multi-module discovery, and registry integration to KSP.

Dependencies: Phases 1 and 2

**Tasks:**

1. **Add `@GeneratedConfig` annotation to KSP-generated configs**
   - Update `NavigationConfigGenerator` to annotate generated config objects with `@GeneratedConfig`

2. **Add `@NavigationRoot` processing to `QuoVadisSymbolProcessor`**
   - Extract `@NavigationRoot`-annotated symbols
   - Validate single-root constraint (error if multiple)
   - Store the root class reference for aggregated config generation

3. **Implement `ClasspathConfigDiscovery`**
   - Scan compiled classes on the KSP classpath for `@GeneratedConfig` annotation
   - Use `Resolver.getDeclarationsFromPackage()` as primary mechanism
   - Fall back to bytecode scanning of dependency JARs if needed
   - Exclude `*__AggregatedConfig` classes and known internal names
   - Sort discovered configs by FQN for deterministic output

4. **Implement `AggregatedConfigGenerator` for KSP**
   - Generate `{modulePrefix}__AggregatedConfig` object using KotlinPoet
   - Compose discovered configs via `+` operator delegation
   - Include `init` block that calls `NavigationConfigRegistry.register(rootClass::class, this)`
   - Annotate with `@GeneratedConfig`

5. **Wire aggregated config generation into the processor**
   - Only generate when `@NavigationRoot` is present
   - Ensure it runs after module-level config generation
   - Handle the case where no dependency configs are discovered (produce a config with only the current module)

6. **Remove `@Deprecated` from KSP processor**
   - KSP is no longer deprecated — it is a fully supported, permanent backend

### Phase 4: Shared contract tests and validation

Goal: Prove both backends produce equivalent behavior for all features.

Dependencies: Phases 2 and 3

**Tasks:**

1. **Create shared backend contract test suite**
   - Parameterized tests that run the same assertions against both KSP and compiler plugin output
   - Cover: stacks, tabs (flat and stack-backed), panes, screen bindings, transitions, deep links, `plus` composition, `roots`

2. **Add `navigationConfig<T>()` resolution tests for both backends**
   - KSP: verify registry-based resolution works end-to-end
   - Compiler: verify call-site rewriting still works
   - Both: verify the resolved config has correct registries and behavior

3. **Add multi-module discovery tests for KSP**
   - Compile dependency modules with KSP, then compile app module that uses `@NavigationRoot`
   - Verify all dependency configs are discovered and composed

4. **Add backend-flip tests**
   - Build with KSP then clean then build with compiler then verify identical behavior
   - Build with compiler then clean then build with KSP then verify identical behavior

5. **Add Gradle functional tests**
   - Verify backend selection and plugin wiring
   - Verify error messages for misconfiguration

### Phase 5: Documentation and cleanup

Dependencies: Phases 1 through 4

**Tasks:**

1. **Update migration docs**
   - Remove all references to `navigationConfig<T>()` being compiler-plugin-only
   - Document that all APIs work with both backends

2. **Update `@NavigationRoot` KDoc**
   - Remove compiler-plugin-specific language
   - Describe it as a backend-neutral feature

3. **Update README and demo references**
   - Show `navigationConfig<T>()` as the standard approach
   - Note that manual `+` composition is also supported
   - Remove any "compiler-plugin-only" warnings from these APIs

4. **Update copilot-instructions.md and plan documents**
   - Reflect the unified API surface

5. **Document backend switch workflow**
   - Property change + clean build
   - No source code changes required

## Recommended Sequencing

1. Add `@GeneratedConfig` annotation and `NavigationConfigRegistry` (Phase 1)
2. Update `navigationConfig<T>()` to use the registry (Phase 1)
3. Remove `GeneratedNavigationConfig` interface (Phase 1)
4. Migrate compiler plugin to annotation-based discovery (Phase 2)
5. Add `@GeneratedConfig` to KSP-generated configs (Phase 3.1)
6. Add `@NavigationRoot` processing and aggregated config to KSP (Phase 3.2-3.5)
7. Remove KSP `@Deprecated` annotation (Phase 3.6)
8. Build shared contract test suite (Phase 4)
9. Update documentation (Phase 5)

## Risks and Mitigations

### Risk: KSP classpath scanning may not find all dependency configs

Why it matters:

- KSP runs before compilation. Dependency modules must already be compiled for their classes to be discoverable on the classpath.
- In some Gradle configurations, KSP metadata compilation may not see all transitive dependency outputs.

Mitigation:

- Test multi-module discovery extensively with real Gradle builds (not just unit tests).
- Use `Resolver.getDeclarationsFromPackage()` as primary discovery, bytecode scanning as fallback.
- Provide an explicit config listing escape hatch if automatic discovery fails.

### Risk: `NavigationConfigRegistry.register()` timing in KSP mode

Why it matters:

- The `init` block in the generated aggregated config only runs when the class is loaded.
- If `navigationConfig<T>()` is called before the aggregated config class is loaded, the registry will be empty.

Mitigation:

- The KSP-generated aggregated config is referenced by the generated code that the app module imports. Class loading ensures the `init` block runs before `navigationConfig<T>()` is called.
- Add a generated top-level property or function that forces class loading if needed.
- In practice, DI setup references the generated package, ensuring class loading.

### Risk: duplicate generation or duplicate classes

Why it matters:

- Both backends generate the same package and similarly named artifacts.

Mitigation:

- Gradle plugin validates exclusive backend selection (already implemented).
- Require `clean` on backend flips in docs and CI.
- Add Gradle functional tests that exercise backend transitions.

### Risk: mixed backend mode across modules

Why it matters:

- Aggregation and naming assumptions become ambiguous if modules use different backends.

Mitigation:

- Require one backend for all Quo Vadis modules in a build.
- Validate centrally from the Gradle plugin.

### Risk: binary/source compatibility drift between generated artifacts

Why it matters:

- Even small differences in supertypes, member signatures, or behavior can break consumer code when switching backends.

Mitigation:

- Shared contract test suite validates behavioral equivalence.
- Both backends generate from the same annotations, targeting the same `NavigationConfig` interface.

### Risk: stale compiler plugin artifact in local development

Why it matters:

- Developers can unknowingly test an old compiler plugin binary.

Mitigation:

- Document `mavenLocal` publication as mandatory for local compiler-plugin development.
- Add warnings when local source is present but the plugin resolves from published coordinates.

### Risk: Kotlin compiler API churn

Why it matters:

- The compiler plugin relies on experimental FIR/IR APIs.

Mitigation:

- KSP backend as a verified, permanent fallback allows quick recovery if a Kotlin update breaks the compiler plugin.
- Retain both backends indefinitely as a resilience measure.

## Validation Strategy

### Build and functional validation

Run a backend matrix against representative modules:

- `composeApp` in KSP mode — verify `navigationConfig<AppNavigation>()` resolves via registry
- `composeApp` in compiler mode — verify `navigationConfig<AppNavigation>()` resolves via call-site rewrite
- `feature1` and `feature2` in both modes with multi-module aggregation
- Clean builds after flipping the backend property

### Contract validation

For both backends, assert:

- Generated config object exists in `com.jermey.quo.vadis.generated` with `@GeneratedConfig` annotation
- Generated deep-link handler exists with the expected module-prefixed name
- `navigationConfig<T>()` resolves to a valid `NavigationConfig` instance
- `NavigationConfig` behavior is equivalent for `buildNavNode`, registries, `plus`, and `roots`
- Deep-link resolution and URI creation are equivalent
- Multi-module aggregation discovers all dependency configs

### Workflow validation

Validate:

- Fresh clone experience with each backend
- Clean backend flip (both directions)
- Local compiler-plugin change plus republish flow
- Failure messaging when incompatible backend wiring is present

## Open Questions

1. Should `NavigationConfigRegistry` use `AtomicReference` or a concurrent map for thread safety on JVM? (Recommendation: simple `mutableMapOf` — registration happens during class loading which is inherently sequential.)
2. Is a dev-only `project` resolution mode for the compiler subplugin required now, or is a documented `mavenLocal` workflow acceptable? (Recommendation: `mavenLocal` is sufficient.)
3. Should the Gradle plugin tolerate the bare KSP Gradle plugin being applied in compiler mode to preserve a property-only switch? (Recommendation: yes, tolerate the bare plugin; only fail on Quo Vadis KSP processor dependencies.)
4. Should KSP's `ClasspathConfigDiscovery` use bytecode scanning (ASM-based) or `Resolver.getDeclarationsFromPackage()`? (Recommendation: try `Resolver` first; fall back to bytecode scanning if needed.)

## Recommendation

Proceed with **full API parity** between the two backends. The `navigationConfig<T>()` function and `@NavigationRoot` annotation must work identically with both KSP and the compiler plugin.

The architecture uses a **dual-path resolution strategy**: the compiler plugin rewrites `navigationConfig<T>()` calls at compile time for zero overhead, while KSP uses a lightweight runtime registry that the generated aggregated config populates during class loading. Both paths are transparent to consumer code.

This approach enables a true property-only backend switch, simplifies documentation (one API surface, not two), and positions KSP as a permanent fallback rather than a deprecated path.
