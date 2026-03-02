# Phase 4: Multi-Module Auto-Discovery

**Status**: Planning  
**Priority**: CRITICAL  
**Dependencies**: Phase 3 (IR Backend) fully complete  
**Created**: 2 March 2026

---

## 1. Overview

### What Phase 4 Accomplishes

Phase 4 eliminates the manual `+` operator chaining required to compose `NavigationConfig` instances across feature modules. Instead, a single `@NavigationRoot` annotation on the application module triggers automatic classpath-driven discovery of all `NavigationConfig` implementors from dependency modules, producing a single aggregated config object.

### Why It Matters

In a modular KMP application, every feature module generates its own `{Prefix}NavigationConfig`. Today, the application module must manually combine them:

```kotlin
// DI.kt in composeApp — current boilerplate
val appConfig = ComposeAppNavigationConfig +
    Feature1NavigationConfig +
    Feature2NavigationConfig
```

Adding or removing a feature module requires updating this chain. In large projects with 10-20+ feature modules, this creates:
- **Error-prone boilerplate** — forgetting a module silently breaks navigation  
- **Tight coupling** — the app module must import every feature's generated config  
- **Poor scalability** — each new module adds a line of wiring code  

### Target Experience

```kotlin
// Application module — annotate any class/object
@NavigationRoot
object MyApp

// Generated: MyAppNavigationConfig automatically includes ALL feature configs
val navigator = rememberQuoVadisNavigator(MainTabs::class, MyAppNavigationConfig)
```

Zero imports from feature modules. Zero manual chaining. Adding a feature module = adding the Gradle dependency.

### Prerequisites

| Prerequisite | Phase | Status |
|---|---|---|
| Compiler plugin infrastructure (SPI, Gradle plugin) | Phase 1 | Must be complete |
| FIR synthetic declaration generation | Phase 2 | Must be complete |
| IR implementation body generation | Phase 3 | Must be complete |
| Feature modules generate valid `{Prefix}NavigationConfig` objects | Phase 2+3 | Must be complete |
| Annotations have BINARY retention | Phase 4A (this phase) | First task |

---

## 2. Current vs Target Architecture

### Before (KSP + Manual Composition)

```
┌─────────────────────────────────────────────────────────────┐
│ composeApp (application module)                             │
│                                                             │
│  DI.kt:                                                    │
│    import Feature1NavigationConfig                          │
│    import Feature2NavigationConfig                          │
│    import ComposeAppNavigationConfig                        │
│                                                             │
│    val config = ComposeAppNavigationConfig                  │
│                 + Feature1NavigationConfig                  │
│                 + Feature2NavigationConfig                  │
│                                                             │
│  Each KSP-generated config:                                 │
│    object Feature1NavigationConfig : NavigationConfig       │
│    object Feature2NavigationConfig : NavigationConfig       │
│    object ComposeAppNavigationConfig : NavigationConfig     │
└─────────────────────────────────────────────────────────────┘
         ▲                    ▲                    ▲
         │ import             │ import             │ import
    ┌────┴────┐         ┌────┴────┐         ┌────┴─────────┐
    │ feature1│         │ feature2│         │ composeApp    │
    │ KSP gen │         │ KSP gen │         │ own KSP gen  │
    └─────────┘         └─────────┘         └──────────────┘
```

**Pain points**: Explicit imports, manual `+` chain, compile error if any import is missing.

### After (Compiler Plugin + Auto-Discovery)

```
┌─────────────────────────────────────────────────────────────┐
│ composeApp (application module)                             │
│                                                             │
│  @NavigationRoot                                            │
│  object MyApp                                               │
│                                                             │
│  Compiler plugin generates:                                 │
│    MyAppNavigationConfig (aggregated)                       │
│      ├── ComposeAppNavigationConfig (own module's config)   │
│      ├── Feature1NavigationConfig   (discovered)            │
│      └── Feature2NavigationConfig   (discovered)            │
│                                                             │
│  Usage:                                                     │
│    rememberQuoVadisNavigator(MainTabs::class,               │
│                              MyAppNavigationConfig)         │
└─────────────────────────────────────────────────────────────┘
         ▲ symbol resolution       ▲ symbol resolution
    ┌────┴────────┐          ┌─────┴───────┐
    │ feature1    │          │ feature2     │
    │ .klib/.jar  │          │ .klib/.jar   │
    │ contains:   │          │ contains:    │
    │ Feature1Nav │          │ Feature2Nav  │
    │ Config obj  │          │ Config obj   │
    │ (BINARY     │          │ (BINARY      │
    │  retention  │          │  retention   │
    │  metadata)  │          │  metadata)   │
    └─────────────┘          └──────────────┘
```

**Gains**: Zero imports, zero manual wiring, adding a module = adding Gradle dependency.

---

## 3. Technical Approach

### 3.1 The Cross-Module Visibility Problem

Kotlin compiler plugins operate within a single `IrModuleFragment` — they cannot access `IrModuleFragment` instances of dependency modules directly. However, there are two mechanisms for cross-module information exchange:

1. **Annotation metadata in .klib/.jar** — Annotations with `BINARY` or `RUNTIME` retention are preserved in compiled artifacts and can be read by the compiler plugin processing a dependent module.

2. **Symbol resolution via `IrPluginContext`** — The `IrPluginContext` provides access to `referenceClass()`, `referenceFunctions()`, etc. for symbols from dependency modules. The plugin can resolve class descriptors, supertypes, and member signatures of classes compiled in other modules.

### 3.2 Discovery Strategy

The discovery works in **two compiler passes** within the application module:

#### Pass 1: FIR Phase (Declaration Synthesis)

When the Quo-Vadis FIR extension encounters `@NavigationRoot` on a class/object in the current module:

1. It synthesizes a top-level declaration `{Prefix}NavigationConfig` (similar to how Phase 2 synthesizes per-module configs).
2. The synthesized declaration has supertype `NavigationConfig`.
3. At FIR level, the implementation body is deferred — only the declaration shape is needed for IDE autocomplete.

#### Pass 2: IR Phase (Classpath Scanning + Body Generation)

When the IR backend extension processes the module containing `@NavigationRoot`:

1. **Scan classpath**: Use `IrPluginContext.referenceClass(ClassId("com.jermey.quo.vadis.core.navigation.config", "NavigationConfig"))` to get the `NavigationConfig` interface symbol.
2. **Find implementors**: Iterate known class symbols to find all objects/classes whose supertypes include `NavigationConfig`. Since feature modules' generated configs are `object : NavigationConfig`, they are discoverable via their supertype.
3. **Filter**: Exclude the root config itself and `CompositeNavigationConfig` / `EmptyNavigationConfig` from the runtime library.
4. **Build aggregation**: Generate IR that chains discovered configs via the `plus` operator in deterministic order.

### 3.3 Alternative: Marker Interface Approach

Instead of scanning all `NavigationConfig` implementors (which may include user-defined manual configs), each compiler-plugin-generated config could implement a marker interface:

```kotlin
// In quo-vadis-core (new interface)
interface GeneratedNavigationConfig : NavigationConfig
```

The IR scanner then specifically looks for `GeneratedNavigationConfig` implementors, avoiding false positives from user-created manual configs.

**Recommendation**: Use the marker interface approach. It provides precise targeting and avoids accidentally including user-defined configs that should remain manual.

### 3.4 Ordering Determinism

Classpath order is not guaranteed to be stable across builds. To ensure deterministic aggregation:

- Sort discovered configs by their fully-qualified class name (lexicographic).
- The application module's own config (if it has `@Stack`/`@Tabs`/`@Pane` annotations) always comes **first** (highest base priority).
- Feature module configs are chained in sorted order.
- The `CompositeNavigationConfig` right-hand-wins priority rule means later configs override earlier ones for duplicates.

This produces a deterministic, reproducible config regardless of classpath ordering.

### 3.5 Analogy: How Koin Does It

Koin's compiler plugin migration follows an identical pattern:
- Each module's plugin generates a class implementing `KoinModule`.
- The application module's plugin scans the classpath for all `KoinModule` implementors.
- Reduced ~25 lines of manual `modules()` DSL to 1 declarative annotation.

Quo-Vadis mirrors this approach with `GeneratedNavigationConfig` replacing `KoinModule`.

---

## 4. Tasks

### Sub-phase 4A: Annotation & Retention Changes

#### Task 4A.1: Introduce `@NavigationRoot` Annotation

**Description**: Create a new annotation in the `quo-vadis-annotations` module that marks the application module's entry point for auto-discovery.

**Files to Create**:
- `quo-vadis-annotations/src/commonMain/kotlin/com/jermey/quo/vadis/annotations/NavigationRoot.kt`

**Definition**:
```kotlin
/**
 * Marks this module as the navigation root that auto-discovers
 * all NavigationConfig implementations from dependency modules.
 *
 * The compiler plugin will generate a `{Prefix}NavigationConfig`
 * that aggregates configs from all modules in the dependency graph.
 *
 * Only one @NavigationRoot is allowed per compilation unit.
 * Multiple roots will produce a compilation error.
 *
 * @property prefix Optional prefix for the generated config class name.
 *   Defaults to the class/object name. The generated config will be
 *   named `{prefix}NavigationConfig`.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class NavigationRoot(
    val prefix: String = ""
)
```

**Acceptance Criteria**:
- [ ] Annotation compiles on all KMP targets
- [ ] Retention is `BINARY` (must survive into .klib/.jar)
- [ ] `prefix` parameter defaults to empty string (derived from class name at generation time)
- [ ] KDoc explains purpose and single-root constraint
- [ ] Added to annotation table in `plans/ksp-analysis-report.md`

---

#### Task 4A.2: Change Annotation Retentions from SOURCE to BINARY

**Description**: Update the retention of navigation annotations that must be visible across module boundaries. Without `BINARY` retention, annotations are stripped during compilation and invisible to the compiler plugin processing dependent modules.

**Files to Modify**:

| File | Annotation | Current | Target |
|------|-----------|---------|--------|
| `quo-vadis-annotations/.../Stack.kt` | `@Stack` | `SOURCE` | `BINARY` |
| `quo-vadis-annotations/.../Destination.kt` | `@Destination` | `SOURCE` | `BINARY` |
| `quo-vadis-annotations/.../Screen.kt` | `@Screen` | `SOURCE` | `BINARY` |
| `quo-vadis-annotations/.../TabAnnotations.kt` | `@Tabs` | `SOURCE` | `BINARY` |
| `quo-vadis-annotations/.../TabAnnotations.kt` | `@TabItem` | `SOURCE` | `BINARY` |
| `quo-vadis-annotations/.../PaneAnnotations.kt` | `@Pane` | `SOURCE` | `BINARY` |
| `quo-vadis-annotations/.../PaneAnnotations.kt` | `@PaneItem` | `SOURCE` | `BINARY` |

**Annotations that stay unchanged**:

| Annotation | Current Retention | Reason |
|------------|------------------|--------|
| `@Argument` | `SOURCE` | Only needed within same-module processing (parameter-level) |
| `@TabsContainer` | `RUNTIME` | Already sufficient (RUNTIME ⊃ BINARY) |
| `@PaneContainer` | `RUNTIME` | Already sufficient |
| `@Transition` | `RUNTIME` | Already sufficient |

**Acceptance Criteria**:
- [ ] All 7 annotations updated to `@Retention(AnnotationRetention.BINARY)`
- [ ] Existing KSP processor still functions (KSP can read BINARY annotations)
- [ ] `.klib` output for feature modules contains annotation metadata
- [ ] No behavioral change for single-module projects

---

#### Task 4A.3: Verify Annotations Survive in .klib/.jar Metadata

**Description**: Write verification tests that confirm BINARY-retained annotations are present and readable from compiled dependency artifacts.

**Approach**:
1. Compile `feature1` module to .klib
2. From a dependent module, use compiler plugin test infrastructure to resolve `feature1`'s `@Stack`-annotated classes
3. Verify annotation arguments (`name`, `startDestination`) are accessible

**Files to Create**:
- `quo-vadis-compiler-plugin/src/test/kotlin/com/jermey/quo/vadis/compiler/multimodule/AnnotationRetentionTest.kt`

**Acceptance Criteria**:
- [ ] Test confirms `@Stack` annotation on `Feature1Destination` is readable from dependent module
- [ ] Test confirms annotation arguments (name, startDestination, route) are preserved
- [ ] Test runs on JVM, JS, and Native targets (at minimum JVM)
- [ ] Test fails if retention is changed back to SOURCE

---

### Sub-phase 4B: Feature Module Metadata Embedding

#### Task 4B.1: Ensure Generated Configs Have Discoverable Supertypes

**Description**: The FIR declarations synthesized in Phase 2 for feature module configs must include `NavigationConfig` (or `GeneratedNavigationConfig` marker) as a supertype so they are discoverable via supertype scanning.

**Technical Detail**: In FIR, the synthetic class declaration for `Feature1NavigationConfig` must have:
```
superTypes = [NavigationConfig]  // or GeneratedNavigationConfig
```

This should already be the case from Phase 2/3, but must be explicitly verified for the marker interface variant.

**Files to Modify**:
- `quo-vadis-compiler-plugin/src/main/kotlin/.../fir/QuoVadisDeclarationGenerator.kt` — ensure supertype includes `GeneratedNavigationConfig`

**Decision Required**: Whether to use `NavigationConfig` directly or introduce `GeneratedNavigationConfig` marker interface (see Section 3.3).

**Acceptance Criteria**:
- [ ] Generated config objects have the correct supertype in FIR metadata
- [ ] Supertype is visible when resolving the config class from a dependent module via `IrPluginContext`
- [ ] Decision on marker interface vs direct supertype documented

---

#### Task 4B.2: Introduce `GeneratedNavigationConfig` Marker Interface

**Description**: Create a marker interface that extends `NavigationConfig`, used exclusively by compiler-plugin-generated configs. This enables precise classpath scanning without false positives from user-defined manual configs.

**Files to Create**:
- `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/config/GeneratedNavigationConfig.kt`

**Definition**:
```kotlin
/**
 * Marker interface for compiler-plugin-generated navigation configs.
 *
 * Used by @NavigationRoot auto-discovery to find configs from
 * dependency modules. User-created manual configs should implement
 * [NavigationConfig] directly, not this interface.
 *
 * @see NavigationConfig
 */
interface GeneratedNavigationConfig : NavigationConfig
```

**Acceptance Criteria**:
- [ ] Interface is in `quo-vadis-core` public API
- [ ] Extends `NavigationConfig` without adding any members
- [ ] All compiler-plugin-generated configs implement this interface (update Phase 2/3 FIR generation)
- [ ] KSP-generated configs do NOT implement this (backward compat — KSP uses `NavigationConfig` directly)
- [ ] User manual configs continue using `NavigationConfig` and are NOT discovered by auto-discovery

---

#### Task 4B.3: Verify Generated Configs are Visible from Dependent Modules

**Description**: Integration test confirming that `IrPluginContext` in the application module can resolve config objects from feature module dependencies.

**Approach**:
1. Set up a multi-module test project: `test-feature` → `test-app`
2. `test-feature` has `@Stack` + `@Destination` → plugin generates `TestFeatureNavigationConfig`
3. In `test-app`, the plugin should be able to call `IrPluginContext.referenceClass(ClassId("com.jermey.quo.vadis.generated", "TestFeatureNavigationConfig"))` and get a valid symbol

**Files to Create**:
- `quo-vadis-compiler-plugin/src/test/kotlin/com/jermey/quo/vadis/compiler/multimodule/CrossModuleVisibilityTest.kt`

**Acceptance Criteria**:
- [ ] Test resolves `TestFeatureNavigationConfig` from `test-app` context
- [ ] Test confirms the resolved class has `GeneratedNavigationConfig` supertype
- [ ] Test confirms `roots` property is accessible
- [ ] Test works for .klib (KMP) artifact format

---

### Sub-phase 4C: Application Module Discovery

#### Task 4C.1: FIR Detection of `@NavigationRoot`

**Description**: Extend the FIR checker/generator to detect `@NavigationRoot` on classes/objects in the current module.

**Technical Detail**: Add a `FirDeclarationChecker` (or extend the existing one from Phase 2) that:
1. Matches classes annotated with `@NavigationRoot`
2. Validates at most one `@NavigationRoot` exists in the module
3. Extracts the `prefix` parameter (or derives from class name)
4. Registers the root for subsequent declaration generation

**Files to Modify**:
- `quo-vadis-compiler-plugin/src/main/kotlin/.../fir/QuoVadisDeclarationGenerator.kt`
- `quo-vadis-compiler-plugin/src/main/kotlin/.../fir/QuoVadisDeclarationChecker.kt` (or new checker)

**Acceptance Criteria**:
- [ ] `@NavigationRoot` on a class triggers root config generation flow
- [ ] `@NavigationRoot` on an object also works
- [ ] Prefix defaults to class name when `prefix = ""` (e.g., `MyApp` → `MyAppNavigationConfig`)
- [ ] Explicit prefix works (e.g., `@NavigationRoot(prefix = "App")` → `AppNavigationConfig`)

---

#### Task 4C.2: Synthesize Root `{Prefix}NavigationConfig` in FIR

**Description**: When `@NavigationRoot` is detected, generate a synthetic top-level `{Prefix}NavigationConfig` object declaration in FIR. This gives IDE visibility (autocomplete, go-to-definition) for the aggregated config.

**Technical Detail**:
- The FIR synthetic declaration should be an `object` with supertype `NavigationConfig`
- It should expose all `NavigationConfig` interface members as synthetic declarations
- The actual implementation bodies are deferred to IR (Task 4C.4)
- If the module ALSO has its own `@Stack`/`@Tabs` annotations, a separate per-module config is generated (as in Phase 2), and the root config wraps it alongside discovered configs

**Files to Modify**:
- `quo-vadis-compiler-plugin/src/main/kotlin/.../fir/QuoVadisDeclarationGenerator.kt`

**Acceptance Criteria**:
- [ ] `{Prefix}NavigationConfig` appears in IDE autocomplete within the application module
- [ ] The synthetic object implements `NavigationConfig`
- [ ] If the same module also has `@Stack` annotations, both the per-module config and the root config are generated (distinct names)
- [ ] Root config is generated in the `com.jermey.quo.vadis.generated` package

---

#### Task 4C.3: IR Classpath Scanning for `NavigationConfig` Implementors

**Description**: In the IR backend pass, scan `IrPluginContext` to find all `GeneratedNavigationConfig` implementors from the full classpath.

**Technical Approach**:

The compiler plugin cannot enumerate "all classes in classpath" directly. Instead, use one of these strategies:

**Strategy A: Metadata-Driven Discovery (Recommended)**
Each feature module's compiler plugin run embeds a list of generated config FQNs into a resource or companion property. The root module's plugin reads these resources.

**Strategy B: Known-Package Scanning**
Since all generated configs live in `com.jermey.quo.vadis.generated`, the plugin can attempt to resolve expected class names: `{modulePrefix}NavigationConfig` for each known module. Module names come from Gradle dependency metadata.

**Strategy C: Supertype-Based Resolution**
Use `IrPluginContext.referenceClass(GeneratedNavigationConfig.classId)` to get the marker interface, then find all classes in the current module's dependencies that implement it. This requires iterating the module's `IrModuleFragment.dependencies` (list of `IrModuleDescriptor`).

> **Note**: Strategy C is the most robust but requires investigation into whether `IrPluginContext` supports iterating dependency class descriptors. Strategy A provides the most control and is the recommended starting approach. See Open Questions.

**Files to Create/Modify**:
- `quo-vadis-compiler-plugin/src/main/kotlin/.../ir/MultiModuleDiscovery.kt` (new)
- `quo-vadis-compiler-plugin/src/main/kotlin/.../ir/QuoVadisIrGeneration.kt`

**Acceptance Criteria**:
- [ ] All `GeneratedNavigationConfig` implementors from dependency modules are found
- [ ] The application module's own per-module config (if any) is also found
- [ ] `NavigationConfig.Empty`, `CompositeNavigationConfig`, and `EmptyNavigationConfig` are excluded
- [ ] Manual user configs (implementing `NavigationConfig` directly) are NOT discovered
- [ ] Discovery works for .klib (Kotlin/Native), .jar (JVM), and JS artifacts

---

#### Task 4C.4: Generate Aggregated Config Using CompositeNavigationConfig

**Description**: Once all configs are discovered, generate IR code that chains them via the `plus` operator to produce the root config's implementation.

**Generated IR equivalent**:
```kotlin
object MyAppNavigationConfig : NavigationConfig {
    private val delegate: NavigationConfig by lazy {
        // Sorted lexicographically by FQN for determinism
        ComposeAppNavigationConfig +     // own module (if present)
            Feature1NavigationConfig +   // "com.jermey.quo.vadis.generated.Feature1NavigationConfig"
            Feature2NavigationConfig     // "com.jermey.quo.vadis.generated.Feature2NavigationConfig"
    }

    override val screenRegistry get() = delegate.screenRegistry
    override val scopeRegistry get() = delegate.scopeRegistry
    override val transitionRegistry get() = delegate.transitionRegistry
    override val containerRegistry get() = delegate.containerRegistry
    override val deepLinkRegistry get() = delegate.deepLinkRegistry
    override val paneRoleRegistry get() = delegate.paneRoleRegistry

    override fun buildNavNode(
        destinationClass: KClass<out NavDestination>,
        key: String?,
        parentKey: String?
    ): NavNode? = delegate.buildNavNode(destinationClass, key, parentKey)

    override operator fun plus(other: NavigationConfig): NavigationConfig =
        CompositeNavigationConfig(this, other)
}
```

**Key decisions**:
- `lazy` initialization prevents resolution-order issues
- The delegate pattern avoids duplicating compositing logic
- `plus` is still available for users who want to add manual configs on top

**Files to Modify**:
- `quo-vadis-compiler-plugin/src/main/kotlin/.../ir/QuoVadisIrGeneration.kt`
- `quo-vadis-compiler-plugin/src/main/kotlin/.../ir/MultiModuleDiscovery.kt`

**Acceptance Criteria**:
- [ ] Root config delegates all registry calls to the composite chain
- [ ] `lazy` initialization is used to avoid circular init issues
- [ ] Application module's own config is included (first in chain, base priority)
- [ ] Feature configs are chained in deterministic order (lexicographic by FQN)
- [ ] Right-hand-wins priority rule applies (last config has highest priority for duplicates)
- [ ] Generated code compiles and runs on all KMP targets

---

#### Task 4C.5: Handle Priority and Ordering of Discovered Modules

**Description**: Define and implement clear priority rules for how configs are ordered in the composite chain.

**Priority Rules**:

| Position | Config | Priority |
|----------|--------|----------|
| 1 (leftmost / lowest priority) | Application module's own config | Base — overridden by features |
| 2..N | Feature module configs (sorted by FQN) | Higher position = higher priority |

> **Rationale**: Application module provides defaults (tab structure, top-level navigation). Feature modules provide specific screens, scopes, and transitions that should take priority. Within feature modules, lexicographic ordering provides determinism.

**Optional**: Support a `@NavigationRoot(priority = ...)` or module-level priority annotation if users need explicit ordering control. **Deferred** — only implement if user demand warrants it.

**Files to Modify**:
- `quo-vadis-compiler-plugin/src/main/kotlin/.../ir/MultiModuleDiscovery.kt`

**Acceptance Criteria**:
- [ ] Application module's own config is always first (lowest priority)
- [ ] Feature configs sorted lexicographically by FQN
- [ ] Same ordering produced regardless of classpath order
- [ ] Ordering behavior documented in KDoc on `@NavigationRoot`

---

### Sub-phase 4D: Backward Compatibility & Fallback

#### Task 4D.1: Preserve Manual `+` Operator for Explicit Composition

**Description**: The auto-generated root config must still support the `plus` operator, allowing users to layer additional manual configs on top.

**Usage pattern**:
```kotlin
// Auto-discovered configs + a manually-created config
val fullConfig = MyAppNavigationConfig + ManualOverrideConfig
```

**Technical Detail**: The generated root config's `plus` method simply creates a new `CompositeNavigationConfig(this, other)`, identical to how individual configs do it today. No special handling needed — this is already how `NavigationConfig.plus` works.

**Files to Modify**: None (inherent in the generated code from Task 4C.4).

**Acceptance Criteria**:
- [ ] `MyAppNavigationConfig + SomeManualConfig` compiles and works correctly
- [ ] Manual config's registrations take priority (right-hand wins)
- [ ] This pattern is documented in migration guide

---

#### Task 4D.2: Handle Mixed Mode (Compiler Plugin + KSP Modules)

**Description**: During migration, some modules may still use KSP while others use the compiler plugin. The root module must discover configs from both.

**Approach**: KSP-generated configs implement `NavigationConfig` directly (not `GeneratedNavigationConfig`). Two options:

**Option A**: Auto-discovery only finds `GeneratedNavigationConfig` implementors. KSP modules must still be wired manually via `+`.  
**Option B**: The Gradle plugin provides a configuration DSL to list additional KSP-module configs by name:

```kotlin
quoVadis {
    additionalConfigs = listOf(
        "com.jermey.quo.vadis.generated.Feature1NavigationConfig"  // KSP-generated
    )
}
```

**Recommendation**: Option A for initial implementation. The manual `+` fallback from Task 4D.1 covers mixed mode. Option B can be added later if demand warrants.

**Files to Modify**:
- Documentation only for initial release
- Optionally: `quo-vadis-gradle-plugin/.../QuoVadisExtension.kt` (if implementing Option B)

**Acceptance Criteria**:
- [ ] KSP-generated configs can be manually `+`'d to the root config
- [ ] No compilation error when mixing compiler-plugin and KSP modules
- [ ] Migration guide documents the mixed-mode pattern
- [ ] No silent loss of KSP-module routes (either discovered or compile warning)

---

#### Task 4D.3: Gradle Plugin Option to Disable Auto-Discovery

**Description**: Add a configuration flag to opt out of auto-discovery, reverting to manual composition behavior.

**Gradle DSL**:
```kotlin
quoVadis {
    autoDiscovery = false  // default: true when @NavigationRoot is present
}
```

When `autoDiscovery = false`, `@NavigationRoot` is ignored and no root config is generated. The user must compose configs manually.

**Files to Modify**:
- `quo-vadis-gradle-plugin/src/main/kotlin/.../QuoVadisExtension.kt` — add `autoDiscovery` property
- `quo-vadis-gradle-plugin/src/main/kotlin/.../QuoVadisPlugin.kt` — pass option to compiler plugin
- `quo-vadis-compiler-plugin/src/main/kotlin/.../QuoVadisCommandLineProcessor.kt` — accept flag
- `quo-vadis-compiler-plugin/src/main/kotlin/.../QuoVadisComponentRegistrar.kt` — pass to extensions

**Acceptance Criteria**:
- [ ] `autoDiscovery = false` prevents root config generation even with `@NavigationRoot`
- [ ] `autoDiscovery = true` (default) enables normal discovery
- [ ] Gradle property is documented in plugin README
- [ ] No behavioral change when property is not set (defaults to true)

---

#### Task 4D.4: Diagnostic Warning for Orphan Configs

**Description**: Emit a compiler warning when a module generates a `NavigationConfig` but is not discovered by any `@NavigationRoot` module. This helps catch missing dependencies.

**Technical Challenge**: This is inherently difficult because a feature module's compiler plugin run doesn't know if a root module will discover it. The diagnostic must be emitted at the **root module's** compilation:

- After discovery, compare the set of found configs against the full set of module dependencies.
- If a dependency module applies the Quo-Vadis plugin but its config wasn't discovered, emit a warning.

**Alternative**: Skip this for initial release. The manual `+` fallback covers the gap. Implement if users report confusion about missing routes.

**Files to Modify**:
- `quo-vadis-compiler-plugin/src/main/kotlin/.../fir/QuoVadisDiagnostics.kt` — new diagnostic
- `quo-vadis-compiler-plugin/src/main/kotlin/.../ir/MultiModuleDiscovery.kt` — detection logic

**Acceptance Criteria**:
- [ ] Warning emitted when a dependency has Quo-Vadis plugin applied but its config wasn't found
- [ ] Warning includes the module name and expected config class name
- [ ] Warning is suppressible via `@Suppress("ORPHAN_NAVIGATION_CONFIG")`
- [ ] No false positives for modules that intentionally don't generate configs (e.g., annotation-only modules)

---

### Sub-phase 4E: Edge Cases & Advanced Patterns

#### Task 4E.1: Multiple `@NavigationRoot` Modules — Error Diagnostic

**Description**: Emit a compilation error if more than one `@NavigationRoot` is found in the same module. Multiple roots across different modules are inherently separate compilations, so only same-module duplicates need checking.

**Diagnostic message**:
```
error: Multiple @NavigationRoot annotations found in this module.
  Only one @NavigationRoot is allowed per compilation unit.
  Found: MyApp (file.kt:10), AnotherRoot (file2.kt:5)
```

**Files to Modify**:
- `quo-vadis-compiler-plugin/src/main/kotlin/.../fir/QuoVadisDeclarationChecker.kt`
- `quo-vadis-compiler-plugin/src/main/kotlin/.../fir/QuoVadisDiagnostics.kt`

**Acceptance Criteria**:
- [ ] Compilation error if two `@NavigationRoot` annotations exist in the same module
- [ ] Error message lists both locations
- [ ] Single `@NavigationRoot` compiles without error
- [ ] Zero `@NavigationRoot` annotations is not an error (module just generates its own config)

---

#### Task 4E.2: Transitive Dependency Discovery

**Description**: If module A depends on module B, and module B depends on module C, and all three generate `NavigationConfig`, the root module (A) must discover configs from both B and C.

**Example**:
```
composeApp (@NavigationRoot)
  └── feature1 (generates Feature1NavigationConfig)
       └── shared-navigation (generates SharedNavigationConfig)
```

`MyAppNavigationConfig` must include `Feature1NavigationConfig` AND `SharedNavigationConfig`.

**Technical Detail**: `IrPluginContext` resolves symbols from the **full transitive classpath**, not just direct dependencies. So if `SharedNavigationConfig` is on the classpath (which it is, transitively through `feature1`), it will be found during scanning. No special handling needed.

**Files to Modify**: None expected (should work by default with Strategy A or C from Task 4C.3).

**Acceptance Criteria**:
- [ ] Transitively reachable configs are discovered
- [ ] Test with 3-level dependency chain: app → feature → shared
- [ ] All three configs are in the aggregated root
- [ ] No duplicate inclusion (each config appears exactly once)

---

#### Task 4E.3: Platform-Specific Configs (expect/actual + Conditional Discovery)

**Description**: Handle `expect`/`actual` declarations for platform-specific navigation configs. A module may have different destinations on different platforms.

**Scenarios**:
1. **Common config with platform-specific screens**: The `@Stack` is in `commonMain`, `@Screen` bindings are in platform source sets. The generated config is per-platform — this already works from Phase 2/3.
2. **Platform-only destinations**: A `@Stack` exists only in `androidMain`. The generated config exists only for Android. Auto-discovery should find it only when compiling for Android.

**Technical Detail**: Compiler plugin runs per-target. When compiling `composeApp` for Android, `IrPluginContext` sees Android-specific configs. When compiling for iOS, it sees iOS-specific configs. Each target's compilation produces its own aggregated root config, which is correct behavior.

**Files to Modify**: None expected for basic support. Platform-conditional logic is inherent in KMP compilation.

**Acceptance Criteria**:
- [ ] Android-only configs are included only in Android compilation
- [ ] iOS-only configs are included only in iOS compilation
- [ ] Common configs are included in all target compilations
- [ ] No phantom references to platform-specific classes on wrong target

---

#### Task 4E.4: Feature Module That Depends on Another Feature Module's Destinations

**Description**: A feature module may need to navigate to destinations defined in another feature module (cross-feature navigation). This already works with manual `+` chaining because both configs are merged. With auto-discovery, the same applies — the root config aggregates all, so cross-feature `navigate()` calls resolve correctly at runtime.

**Edge case**: A feature module's `@Screen` references a destination class from another feature. This requires the destination class to be in a shared dependency. The compiler plugin doesn't change this fundamental requirement.

**Acceptance Criteria**:
- [ ] Cross-feature navigation works when both modules' configs are auto-discovered
- [ ] `navigate(FeatureBDestination.SomeScreen)` from Feature A resolves correctly
- [ ] Compile-time error if Feature A references a destination not on its classpath (standard Kotlin behavior)

---

#### Task 4E.5: Configuration Ordering Determinism

**Description**: Verify that the aggregated config produces the same `NavigationConfig` regardless of:
- File system ordering
- Classpath ordering (which varies by build system, OS, parallel builds)
- Gradle dependency declaration order

**Approach**: The determinism guarantee comes from Task 4C.5's lexicographic FQN sorting. This task adds explicit tests.

**Files to Create**:
- `quo-vadis-compiler-plugin/src/test/kotlin/com/jermey/quo/vadis/compiler/multimodule/OrderingDeterminismTest.kt`

**Test scenarios**:
1. Compile with dependencies declared in order A, B, C → verify config order
2. Compile with dependencies declared in order C, A, B → verify same config order
3. Verify `buildNavNode` produces identical tree in both cases

**Acceptance Criteria**:
- [ ] Same aggregated config regardless of dependency declaration order
- [ ] Same output for repeated builds (no non-determinism)
- [ ] Sorted by FQN as specified in Task 4C.5

---

## 5. Acceptance Criteria Summary

### Phase 4 Complete When:

- [ ] `@NavigationRoot` annotation exists with BINARY retention
- [ ] Feature module annotations have BINARY retention and survive in .klib/.jar
- [ ] `GeneratedNavigationConfig` marker interface exists in quo-vadis-core
- [ ] FIR phase detects `@NavigationRoot` and synthesizes root config declaration
- [ ] IR phase scans classpath and finds all `GeneratedNavigationConfig` implementors
- [ ] Root config aggregates all discovered configs via `CompositeNavigationConfig` chain
- [ ] Demo app (`composeApp`) works with `@NavigationRoot` instead of manual `+` chaining  
  Current `DI.kt`:
  ```kotlin
  ComposeAppNavigationConfig + Feature1NavigationConfig + Feature2NavigationConfig
  ```
  Target `DI.kt`:
  ```kotlin
  MyAppNavigationConfig  // auto-includes all three
  ```
- [ ] Manual `+` operator still works as fallback
- [ ] Multiple `@NavigationRoot` in same module produces compile error
- [ ] Transitive dependencies are discovered
- [ ] Ordering is deterministic
- [ ] `autoDiscovery = false` Gradle flag disables the feature
- [ ] Integration tests pass on JVM, JS, and Native targets

---

## 6. Risks

| Risk | Impact | Probability | Mitigation |
|------|--------|------------|------------|
| **IrPluginContext cannot enumerate dependency classes by supertype** | High | Medium | Fall back to Strategy A (metadata resources) or Strategy B (known-package scanning). Research during Task 4C.3 implementation. |
| **BINARY retention increases .klib/.jar size** | Low | High (guaranteed) | Measured increase should be minimal (annotation metadata is small). Benchmark and document. |
| **KSP backward compat break from retention change** | Medium | Low | KSP reads BINARY annotations fine. Test explicitly in Task 4A.2. |
| **Non-deterministic classpath ordering causes flaky builds** | Medium | Medium | Lexicographic FQN sorting (Task 4C.5). Tested in Task 4E.5. |
| **Mixed KSP + compiler-plugin projects have confusing behavior** | Medium | Medium | Clear migration guide. Manual `+` fallback. Diagnostic warnings (Task 4D.4). |
| **Compose compiler interaction with lazy delegate in root config** | Medium | Low | The root config only delegates — no `@Composable` code in the root itself. Composable dispatch lives in individual module configs. |
| **CI build parallelism affects discovery** | Low | Low | Each target compilation is isolated. No shared mutable state. |

---

## 7. Open Questions

### OQ-4.1: IrPluginContext Supertype Enumeration Capability

**Question**: Can `IrPluginContext` iterate all classes that implement a given interface across the full transitive classpath?

**Context**: Strategy C (Section 3.3) depends on this. The `IrPluginContext` API provides `referenceClass()` for resolving known FQNs, but it's unclear if it supports "find all implementors of interface X".

**Research needed**: Inspect `IrPluginContext` API surface, review Kotlin compiler source, and check how Koin's compiler plugin handles this. If not possible, fall back to Strategy A (resource embedding).

**Impact**: Determines implementation approach for Task 4C.3. Must be resolved before starting Sub-phase 4C.

---

### OQ-4.2: Resource Embedding as Metadata Channel

**Question**: Can a compiler plugin embed a text resource (e.g., `META-INF/quo-vadis/configs.txt`) into the .klib/.jar during IR phase? Can a dependent module's plugin read these resources?

**Context**: Strategy A requires each feature module to write its generated config FQN into a resource file. The root module's plugin reads all such resources from the classpath.

**Research needed**: Investigate `IrPluginContext` file generation capabilities and KMP resource bundling.

**Impact**: Fallback mechanism if Strategy C is not viable.

---

### OQ-4.3: Kotlin/Native .klib Structure for Cross-Module Resolution

**Question**: Are .klib files structured differently from .jar files in terms of metadata accessibility? Does `IrPluginContext` behave identically for both?

**Context**: KMP targets produce .klib (K/N, K/JS) and .jar (K/JVM). The discovery mechanism must work for all artifact types.

**Research needed**: Test cross-module resolution on Native target specifically.

**Impact**: Tasks 4B.3, 4C.3.

---

### OQ-4.4: Should `@NavigationRoot` Support Explicit Include/Exclude Lists?

**Question**: Should `@NavigationRoot` have optional parameters for explicitly including or excluding specific module configs?

```kotlin
@NavigationRoot(
    exclude = [DebugFeatureNavigationConfig::class]
)
object MyApp
```

**Current stance**: Defer. Auto-discovery should be all-or-nothing for simplicity. Exclude via `autoDiscovery = false` + manual `+`.

**Impact**: API design for `@NavigationRoot` annotation (Task 4A.1).

---

### OQ-4.5: Interaction with Kotlin Incremental Compilation

**Question**: When a feature module is recompiled (e.g., a screen is added), does the application module's root config get re-generated?

**Context**: Incremental compilation may cache the root module's compilation output. If a dependency changes, the root module must be recompiled to discover the updated config.

**Research needed**: Investigate Kotlin incremental compilation behavior for compiler plugin outputs when dependencies change.

**Impact**: Possible stale config if incremental compilation doesn't invalidate correctly. May need Gradle task dependency configuration.

---

### OQ-4.6: ServiceLoader as Alternative Discovery Mechanism

**Question**: Could `java.util.ServiceLoader` (JVM) or equivalent KMP mechanism be used at **runtime** instead of compile-time classpath scanning?

**Pro**: ServiceLoader is battle-tested for service discovery on JVM.  
**Con**: No KMP equivalent for K/Native and K/JS. Would require platform-specific implementations. Compile-time is preferable for type safety and error reporting.

**Current stance**: Compile-time discovery is preferred. ServiceLoader is not viable for KMP.

**Impact**: Architecture decision validation.

---

## 8. Sequencing

```
4A.1 (@NavigationRoot annotation)  ──┐
4A.2 (Retention changes)            ├──→ 4A.3 (Retention verification)
                                     │
4B.2 (GeneratedNavigationConfig)  ──┤
                                     │
                                     ├──→ 4B.1 (FIR supertype) ──→ 4B.3 (Cross-module visibility)
                                     │                                      │
                                     │         ┌───────────────────────────┘
                                     │         ▼
                                     ├──→ 4C.1 (FIR @NavigationRoot) ──→ 4C.2 (FIR root synthesis)
                                     │                                            │
                                     │                                            ▼
                                     └──→ 4C.3 (IR classpath scanning) ──→ 4C.4 (Aggregation)
                                                                                  │
                                                                           4C.5 (Ordering)
                                                                                  │
                                                                                  ▼
4D.1 (Manual + preserved)  ────────────────────────────────────────────► Integration
4D.2 (Mixed KSP mode)      ────────────────────────────────────────────► Integration
4D.3 (autoDiscovery flag)   ────────────────────────────────────────────► Integration
4D.4 (Orphan warnings)     ────────────────────────────────────────────► Integration
                                                                                  │
                                                                                  ▼
                                                                        4E.1-4E.5 (Edge cases)
```

**Critical path**: 4A.1 → 4A.2 → 4B.2 → 4B.1 → 4C.1 → 4C.2 → 4C.3 → 4C.4 → Integration testing

---

## 9. File Inventory

### New Files

| File | Purpose |
|------|---------|
| `quo-vadis-annotations/.../NavigationRoot.kt` | `@NavigationRoot` annotation |
| `quo-vadis-core/.../GeneratedNavigationConfig.kt` | Marker interface for auto-discovery |
| `quo-vadis-compiler-plugin/.../ir/MultiModuleDiscovery.kt` | Classpath scanning logic |
| `quo-vadis-compiler-plugin/.../multimodule/AnnotationRetentionTest.kt` | Retention verification test |
| `quo-vadis-compiler-plugin/.../multimodule/CrossModuleVisibilityTest.kt` | Cross-module symbol resolution test |
| `quo-vadis-compiler-plugin/.../multimodule/OrderingDeterminismTest.kt` | Deterministic ordering test |

### Modified Files

| File | Change |
|------|--------|
| `quo-vadis-annotations/.../Stack.kt` | Retention: SOURCE → BINARY |
| `quo-vadis-annotations/.../Destination.kt` | Retention: SOURCE → BINARY |
| `quo-vadis-annotations/.../Screen.kt` | Retention: SOURCE → BINARY |
| `quo-vadis-annotations/.../TabAnnotations.kt` | Retention: SOURCE → BINARY (Tabs, TabItem) |
| `quo-vadis-annotations/.../PaneAnnotations.kt` | Retention: SOURCE → BINARY (Pane, PaneItem) |
| `quo-vadis-compiler-plugin/.../fir/QuoVadisDeclarationGenerator.kt` | @NavigationRoot detection + root config synthesis |
| `quo-vadis-compiler-plugin/.../fir/QuoVadisDeclarationChecker.kt` | Multiple @NavigationRoot diagnostic |
| `quo-vadis-compiler-plugin/.../fir/QuoVadisDiagnostics.kt` | New diagnostics |
| `quo-vadis-compiler-plugin/.../ir/QuoVadisIrGeneration.kt` | Root config body generation |
| `quo-vadis-gradle-plugin/.../QuoVadisExtension.kt` | `autoDiscovery` property |
| `quo-vadis-gradle-plugin/.../QuoVadisPlugin.kt` | Pass autoDiscovery flag |
| `quo-vadis-compiler-plugin/.../QuoVadisCommandLineProcessor.kt` | Accept autoDiscovery flag |
| `composeApp/.../DI.kt` | Replace manual `+` chain with `MyAppNavigationConfig` |
