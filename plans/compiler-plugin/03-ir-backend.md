# Phase 3: IR Backend Implementation

**Status**: Planning  
**Prerequisites**: Phase 1 (Infrastructure), Phase 2 (FIR Frontend)  
**Module**: `quo-vadis-compiler-plugin` (+ `quo-vadis-compiler-plugin-native` source sync)

---

## 1. Overview

Phase 3 implements the IR (Intermediate Representation) backend that materializes the synthetic declarations created by Phase 2's FIR frontend into executable code. While FIR declares the *shape* of `{Prefix}NavigationConfig` and `{Prefix}DeepLinkHandler` (making them visible in the IDE), the IR backend weaves the actual *implementation logic* into the compiled binaries.

### What This Phase Accomplishes

- Transforms empty FIR stubs into fully-functional IR class bodies
- Generates `navigationConfig { }` DSL calls for non-composable registries (scope, transition, container building)
- Generates `when`-dispatch IR for composable registries (screen, wrapper)
- Synthesizes the complete `DeepLinkHandler` with regex-based route matching and type-safe argument extraction
- Wires all sub-registries into the `NavigationConfig` implementation
- Produces platform-agnostic IR compatible with JVM, JS, Wasm, and Native backends

### Prerequisites from Phase 2

| Requirement | Description |
|-------------|-------------|
| Synthetic `{Prefix}NavigationConfig` class | FIR object declaration with `NavigationConfig` supertype, all property/method signatures declared |
| Synthetic `{Prefix}DeepLinkHandler` class | FIR object declaration with `DeepLinkRegistry` supertype |
| Annotation metadata collected | `FirPredicateBasedProvider` has aggregated all `@Stack`, `@Destination`, `@Screen`, `@Tabs`, `@Pane`, `@TabItem`, `@PaneItem`, `@TabsContainer`, `@PaneContainer`, `@Transition`, `@Argument` metadata |
| `GeneratedDeclarationKey` linkage | Synthetic declarations tagged with plugin-specific key for IR lookup |
| Diagnostics validated | Route collisions, argument mismatches, and structural errors already reported by FIR checkers |

---

## 2. Technical Approach

### 2.1 Two-Pass IR Strategy

The IR backend uses a two-pass approach to avoid circular references and ensure all symbols resolve correctly:

```
IrModuleFragment
    │
    ▼
┌─────────────────────────────────────────┐
│ Pass 1: Stub Materialization            │
│  - Locate FIR synthetic declarations    │
│  - Create IR class/object shells        │
│  - Bind properties and method stubs     │
│  - Register in symbol table             │
└─────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────┐
│ Pass 2: Body Synthesis                  │
│  - Inject IrBlockBody into methods      │
│  - Generate DSL lambda calls            │
│  - Generate when-dispatch expressions   │
│  - Wire registry properties             │
│  - Construct DeepLinkHandler bodies     │
└─────────────────────────────────────────┘
    │
    ▼
  Lowered IR → Platform backends (JVM/JS/Wasm/Native)
```

**Why two passes**: Pass 1 ensures that when Pass 2 generates a reference to `{Prefix}DeepLinkHandler` from within `{Prefix}NavigationConfig` (for the `deepLinkRegistry` property), the target symbol already exists in the IR module. Single-pass generation would require careful ordering and risk unresolved forward references.

### 2.2 IrPluginContext Usage

The `IrPluginContext` provides:

| Capability | Usage |
|-----------|-------|
| `referenceClass(ClassId)` | Resolve core library types: `NavigationConfig`, `ScreenRegistry`, `NavDestination`, etc. |
| `referenceFunctions(CallableId)` | Resolve DSL builder functions: `navigationConfig()`, extension lambdas |
| `referenceProperties(CallableId)` | Resolve companion properties like `NavigationConfig.Empty` |
| `irBuiltIns` | Access primitive types, `Unit`, `Boolean`, `String`, `Nothing` |
| `irFactory` | Create `IrClass`, `IrFunction`, `IrProperty`, `IrField`, `IrBlockBody`, etc. |
| `symbolTable` | Look up previously-created IR symbols |
| `moduleFragment` | The module being compiled — where synthetic IR is inserted |

### 2.3 Symbol Resolution Strategy

A dedicated `SymbolResolver` utility will cache resolved symbols for all referenced types. This avoids repeated `ClassId` construction and `referenceClass` calls throughout the transformer:

```
SymbolResolver
├── Core types: NavigationConfig, NavDestination, NavNode, KClass
├── Registry interfaces: ScreenRegistry, ScopeRegistry, TransitionRegistry, ContainerRegistry, DeepLinkRegistry, PaneRoleRegistry
├── Registry support types: ContainerInfo, ScopeKey, PaneRole, PaneBackBehavior, AdaptStrategy
├── DSL types: NavigationConfigBuilder, StackBuilder, TabsBuilder, PanesBuilder, ScopeBuilder
├── DSL functions: navigationConfig(), stack(), tabs(), panes(), scope(), transition()
├── Deep link types: DeepLink, DeepLinkResult, Navigator
├── Transition types: NavTransition (SlideHorizontal, SlideVertical, Fade, None)
├── Compose types: Composable, SharedTransitionScope, AnimatedVisibilityScope
├── Container scopes: TabsContainerScope, PaneContainerScope
├── Internal: CompositeNavigationConfig
└── Kotlin stdlib: Lazy, lazy(), Regex, Map, Set, List, KClass
```

### 2.4 FIR-to-IR Bridge

Synthetic declarations from Phase 2 are located in the IR tree via their `GeneratedDeclarationKey`. The bridge:

1. Walks `IrModuleFragment.files` looking for `IrClass` declarations
2. Checks each class's `origin` against the plugin's `IrDeclarationOrigin`
3. Matches by `ClassId` (`{package}.{Prefix}NavigationConfig`, `{package}.{Prefix}DeepLinkHandler`)
4. Extracts the collected annotation metadata from a shared `PluginMetadataStore` (populated by FIR, read by IR)

### 2.5 Hybrid Generation Approach

| Component | Approach | Rationale |
|-----------|----------|-----------|
| `baseConfig` (containers, scopes, transitions) | Generate IR that calls `navigationConfig { }` DSL | DSL already handles complex NavNode tree construction correctly; reuse reduces IR complexity |
| `screenRegistry.Content()` | Direct `when`-dispatch IR | Composable code cannot be reliably generated through DSL lambdas due to Compose compiler constraints |
| `screenRegistry.hasContent()` | Direct `when`-dispatch IR | Simple boolean dispatch, no DSL equivalent |
| `containerRegistry.TabsContainer()` | Direct `when`-dispatch IR | Composable wrapper dispatch |
| `containerRegistry.PaneContainer()` | Direct `when`-dispatch IR | Composable wrapper dispatch |
| `containerRegistry.getContainerInfo()` | Delegation to `baseConfig` | Already built by DSL |
| `paneRoleRegistry` | Direct `when`-dispatch IR | Simple mapping, no DSL equivalent |
| `deepLinkHandler` | Full IR synthesis | Self-contained object with regex logic, no DSL equivalent |

---

## 3. Tasks

### Sub-phase 3A: IR Foundation

#### Task 3A.1: IrGenerationExtension Registration

Register the IR generation extension in `CompilerPluginRegistrar` so the IR transformer is invoked after FIR completes.

**Files to create/modify:**
- `quo-vadis-compiler-plugin/src/main/kotlin/com/jermey/quo/vadis/compiler/QuoVadisCompilerPluginRegistrar.kt` — add `IrGenerationExtension.registerExtension()`
- `quo-vadis-compiler-plugin/src/main/kotlin/com/jermey/quo/vadis/compiler/ir/QuoVadisIrGenerationExtension.kt` — **new** — implements `IrGenerationExtension`

**Description:**
- Create `QuoVadisIrGenerationExtension` implementing `IrGenerationExtension`
- Override `generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext)`
- Register in `CompilerPluginRegistrar.ExtensionStorage` alongside FIR extensions
- Read `modulePrefix` from `CompilerConfiguration`
- Entry point orchestrates Pass 1 and Pass 2 transformers sequentially

**Acceptance Criteria:**
- [ ] `QuoVadisIrGenerationExtension` compiles and is registered via SPI
- [ ] `generate()` is called during compilation with valid `IrPluginContext`
- [ ] `modulePrefix` is accessible from the configuration
- [ ] No-op implementation passes `./gradlew build` without errors

---

#### Task 3A.2: Two-Pass Transformer Infrastructure

Create the two-pass transformer framework: stub materialization and body synthesis.

**Files to create:**
- `quo-vadis-compiler-plugin/src/main/kotlin/com/jermey/quo/vadis/compiler/ir/StubMaterializationTransformer.kt` — **new** — Pass 1
- `quo-vadis-compiler-plugin/src/main/kotlin/com/jermey/quo/vadis/compiler/ir/BodySynthesisTransformer.kt` — **new** — Pass 2

**Description:**

**Pass 1 (`StubMaterializationTransformer`):**
- Extends `IrElementTransformerVoid`
- Locates synthetic `IrClass` declarations by matching plugin-origin `IrDeclarationOrigin`
- Validates structural integrity (correct supertypes, expected member signatures)
- Creates any additional IR stubs needed beyond what FIR lowered (e.g., private helper classes like `RoutePattern`)
- Registers stubs in a `SynthesizedDeclarations` data class passed to Pass 2

**Pass 2 (`BodySynthesisTransformer`):**
- Extends `IrElementTransformerVoid`
- Receives `SynthesizedDeclarations` from Pass 1
- Iterates through stubs and injects `IrBlockBody` / `IrExpressionBody` for each member
- Delegates to specialized body generators (per sub-phase 3B–3F)

**Acceptance Criteria:**
- [ ] Pass 1 locates synthetic declarations from the IR module
- [ ] Pass 2 can inject bodies into located stubs
- [ ] Both passes are invoked sequentially from `QuoVadisIrGenerationExtension.generate()`
- [ ] `SynthesizedDeclarations` correctly bridges Pass 1 → Pass 2
- [ ] Error handling: graceful failure if synthetic declarations are missing (with diagnostic)

---

#### Task 3A.3: Symbol Resolution Utilities

Build a `SymbolResolver` that pre-resolves and caches all core library type references needed across IR generation.

**Files to create:**
- `quo-vadis-compiler-plugin/src/main/kotlin/com/jermey/quo/vadis/compiler/ir/SymbolResolver.kt` — **new**

**Description:**
- Accepts `IrPluginContext` in constructor
- Lazily resolves and caches `IrClassSymbol` for every type listed in Section 2.3
- Provides helper methods:
  - `resolveClass(packageFqn: String, name: String): IrClassSymbol`
  - `resolveFunction(packageFqn: String, name: String): IrSimpleFunctionSymbol`
  - `resolveProperty(packageFqn: String, name: String): IrPropertySymbol`
  - Convenience accessors: `navigationConfigClass`, `screenRegistryClass`, `navDestinationClass`, etc.
- Validates that all required symbols exist at initialization (fail-fast with clear error messages)
- Handles the Compose annotation resolution (`@Composable` class symbol)

**Key symbol groups to resolve:**

| Group | Package | Symbols |
|-------|---------|---------|
| Config | `com.jermey.quo.vadis.core.navigation.config` | `NavigationConfig` |
| Registries | `com.jermey.quo.vadis.core.registry` | `ScreenRegistry`, `ScopeRegistry`, `TransitionRegistry`, `ContainerRegistry`, `DeepLinkRegistry`, `PaneRoleRegistry`, `ContainerInfo` |
| Navigation | `com.jermey.quo.vadis.core.navigation` | `NavNode`, `NavDestination` |
| DSL | `com.jermey.quo.vadis.core.dsl` | `navigationConfig()` function, `NavigationConfigBuilder`, `StackBuilder`, `TabsBuilder`, `PanesBuilder`, `ScopeBuilder` |
| Pane | `com.jermey.quo.vadis.core.navigation.pane` (or actual package) | `PaneRole`, `PaneBackBehavior`, `AdaptStrategy` |
| Transition | `com.jermey.quo.vadis.core.compose.transition` | `NavTransition` |
| Deep Link | `com.jermey.quo.vadis.core.navigation.destination` | `DeepLink`, `DeepLinkResult` |
| Internal | `com.jermey.quo.vadis.core.navigation.internal.config` | `CompositeNavigationConfig` |
| Compose | `androidx.compose.runtime` | `Composable` |
| Compose Anim | `androidx.compose.animation` | `SharedTransitionScope`, `AnimatedVisibilityScope` |
| Container Scopes | (actual packages) | `TabsContainerScope`, `PaneContainerScope` |

**Acceptance Criteria:**
- [ ] All core library symbols resolve without error when the plugin runs
- [ ] Missing symbols produce clear error messages naming the unresolved type
- [ ] Resolution is lazy (no eager computation at registrar time)
- [ ] Cache avoids redundant `referenceClass()` calls
- [ ] Correctly handles nested classes (e.g., `ContainerInfo.TabContainer`, `DeepLinkResult.Matched`)

---

#### Task 3A.4: FIR-to-IR Bridge — Metadata Transfer

Establish the mechanism for transferring annotation metadata collected during FIR to the IR phase.

**Files to create/modify:**
- `quo-vadis-compiler-plugin/src/main/kotlin/com/jermey/quo/vadis/compiler/common/PluginMetadataStore.kt` — **new**
- `quo-vadis-compiler-plugin/src/main/kotlin/com/jermey/quo/vadis/compiler/common/NavigationMetadata.kt` — **new**
- Modify FIR extensions (from Phase 2) to populate the store

**Description:**

The FIR phase collects structured metadata about all annotated declarations. The IR phase needs this data to generate bodies. The `PluginMetadataStore` bridges the two:

**`NavigationMetadata`** — Immutable data model holding:
```
NavigationMetadata
├── modulePrefix: String
├── stacks: List<StackMetadata>
│   ├── name: String
│   ├── startDestination: ClassId
│   ├── sealedClassId: ClassId
│   └── destinations: List<DestinationMetadata>
│       ├── classId: ClassId
│       ├── route: String?
│       ├── arguments: List<ArgumentMetadata>
│       │   ├── name: String, key: String, type: TypeRef, optional: Boolean
│       └── transitionType: TransitionType?
├── tabs: List<TabsMetadata>
│   ├── name: String, classId: ClassId, initialTab: ClassId?
│   └── items: List<TabItemMetadata> { classId, type: NESTED_STACK | FLAT_SCREEN }
├── panes: List<PaneMetadata>
│   ├── name: String, classId: ClassId, backBehavior: PaneBackBehavior
│   └── items: List<PaneItemMetadata> { classId, role: PaneRole, adaptStrategy }
├── screens: List<ScreenMetadata>
│   ├── functionFqn: FqName
│   ├── destinationClassId: ClassId
│   ├── hasDestinationParam: Boolean
│   ├── hasSharedTransitionScope: Boolean
│   └── hasAnimatedVisibilityScope: Boolean
├── tabsContainers: List<TabsContainerMetadata>
│   ├── functionFqn: FqName, tabClassId: ClassId
├── paneContainers: List<PaneContainerMetadata>
│   ├── functionFqn: FqName, paneClassId: ClassId
└── transitions: List<TransitionMetadata>
    ├── destinationClassId: ClassId, type: TransitionType, customClass: ClassId?
```

**`PluginMetadataStore`:**
- Thread-safe singleton or `CompilerConfiguration` extension property
- FIR writes via `store(metadata: NavigationMetadata)`
- IR reads via `retrieve(): NavigationMetadata`
- Scoped per compilation module

**Acceptance Criteria:**
- [ ] FIR extensions populate `PluginMetadataStore` with all annotation data
- [ ] IR extension retrieves the same data without loss
- [ ] Metadata model covers all 11 annotations and their parameters
- [ ] ClassId references are used (not string-based) for type safety
- [ ] Store handles the case where FIR found no annotations (empty metadata)

---

### Sub-phase 3B: NavigationConfig Object Synthesis

#### Task 3B.1: Object Singleton IR Structure

Generate the IR structure for `{Prefix}NavigationConfig` as an object singleton implementing `NavigationConfig`.

**Files to create:**
- `quo-vadis-compiler-plugin/src/main/kotlin/com/jermey/quo/vadis/compiler/ir/generators/NavigationConfigIrGenerator.kt` — **new**

**Description:**
- In Pass 1: Verify the FIR-lowered `IrClass` has:
  - `kind = ClassKind.OBJECT`
  - Supertype `NavigationConfig`
  - All expected member declarations (properties + methods from the interface)
- If FIR lowering didn't produce complete IR stubs, create missing member declarations
- Establish the `INSTANCE` field pattern for object singletons
- In Pass 2: Delegate to sub-generators (Tasks 3B.2–3B.8, 3C, 3D, 3F) to fill bodies

**Acceptance Criteria:**
- [ ] IR object has correct `ClassKind.OBJECT`
- [ ] Supertype `NavigationConfig` is correctly set
- [ ] All `NavigationConfig` interface members present as `IrDeclaration` stubs
- [ ] Object `INSTANCE` field is properly initialized
- [ ] Compiles to valid object singleton on all platforms

---

#### Task 3B.2: `baseConfig` Lazy Property — DSL Lambda Generation

Generate the `private val baseConfig by lazy { navigationConfig { ... } }` property using IR that calls the existing DSL.

**Files to create:**
- `quo-vadis-compiler-plugin/src/main/kotlin/com/jermey/quo/vadis/compiler/ir/generators/BaseConfigIrGenerator.kt` — **new**

**Description:**
- Generate a private `IrField` backed by `Lazy<NavigationConfig>`
- Generate the `lazy { }` call wrapping a `navigationConfig { }` call
- The DSL lambda body is an `IrBlockBody` containing:
  - Container registrations (Tasks 3B.3–3B.5)
  - Scope registrations (Task 3B.6)
  - Transition registrations (Task 3B.7)
- The `navigationConfig` function reference is resolved via `SymbolResolver`
- The lambda is passed as a function reference to `NavigationConfigBuilder.() -> Unit`

**IR structure to generate:**
```
IrField (baseConfig)
  └── IrCall (lazy)
      └── IrFunctionExpression (lambda)
          └── IrCall (navigationConfig)
              └── IrFunctionExpression (builder lambda: NavigationConfigBuilder.() -> Unit)
                  └── IrBlockBody
                      ├── IrCall (stack<D>(...) { ... })    // Task 3B.3
                      ├── IrCall (tabs<D>(...) { ... })     // Task 3B.4
                      ├── IrCall (panes<D>(...) { ... })    // Task 3B.5
                      ├── IrCall (scope(...) { ... })       // Task 3B.6
                      └── IrCall (transition<D>(...))       // Task 3B.7
```

**Acceptance Criteria:**
- [ ] `baseConfig` is a `private val` with `Lazy<NavigationConfig>` type
- [ ] `lazy { }` wrapper generates correct IR
- [ ] `navigationConfig { }` call resolves to the core DSL function
- [ ] Builder lambda has correct receiver type (`NavigationConfigBuilder`)
- [ ] Lazy delegate accessor (`.value`) generates correct property getter IR

---

#### Task 3B.3: Stack DSL Blocks

Generate IR for `stack<D>(scopeKey, startDestination) { destination<D>() ... }` calls within the DSL lambda.

**Files to create/modify:**
- `quo-vadis-compiler-plugin/src/main/kotlin/com/jermey/quo/vadis/compiler/ir/generators/dsl/StackDslIrGenerator.kt` — **new**

**Description:**
For each `@Stack` in the metadata, generate an `IrCall` to the `NavigationConfigBuilder.stack<D>()` inline function:
- Type argument: the sealed class (`D : NavDestination`)
- `scopeKey` parameter: string from `@Stack.name`
- Builder lambda body: calls to `destination<SubClass>()` for each `@Destination` subclass

The builder lambda (`StackBuilder.() -> Unit`):
- `startDestination` is set via `@Stack.startDestination` reified class reference
- Each destination subclass is registered via the `StackBuilder` API

**Metadata used:** `NavigationMetadata.stacks`

**Acceptance Criteria:**
- [ ] One `stack<D>()` call generated per `@Stack`-annotated sealed class
- [ ] Scope key matches `@Stack.name`
- [ ] Start destination resolves to correct `KClass` reference
- [ ] All `@Destination` subclasses registered within the builder lambda
- [ ] Reified type arguments emit correct IR (inline function type argument handling)

---

#### Task 3B.4: Tabs DSL Blocks

Generate IR for `tabs<D>(scopeKey) { tab(...) ... }` calls.

**Files to create:**
- `quo-vadis-compiler-plugin/src/main/kotlin/com/jermey/quo/vadis/compiler/ir/generators/dsl/TabsDslIrGenerator.kt` — **new**

**Description:**
For each `@Tabs` in the metadata, generate:
- `IrCall` to `NavigationConfigBuilder.tabs<D>()` with type argument = tabs class
- `scopeKey` = `@Tabs.name`
- Builder lambda body (`TabsBuilder.() -> Unit`):
  - `initialTab` assignment from `@Tabs.initialTab` (`initialTabIndex` derived from items ordering)
  - For each `@TabItem` in `@Tabs.items`:
    - If the tab item is also a `@Stack` → `NESTED_STACK` (tab with its own back stack)
    - If it's a simple screen → `FLAT_SCREEN`
    - Generate appropriate `tab(...)` calls with the tab class reference

**Metadata used:** `NavigationMetadata.tabs`, cross-referenced with `NavigationMetadata.stacks` for nested stack detection

**Acceptance Criteria:**
- [ ] One `tabs<D>()` call generated per `@Tabs` annotation
- [ ] `initialTab` correctly computed from `@Tabs.initialTab` or defaults to first
- [ ] Tab items correctly classified as `NESTED_STACK` vs `FLAT_SCREEN`
- [ ] Tab ordering matches `@Tabs.items` array order
- [ ] Type arguments correct for each tab entry

---

#### Task 3B.5: Pane DSL Blocks

Generate IR for `panes<D>(scopeKey, backBehavior) { pane(role, ...) ... }` calls.

**Files to create:**
- `quo-vadis-compiler-plugin/src/main/kotlin/com/jermey/quo/vadis/compiler/ir/generators/dsl/PanesDslIrGenerator.kt` — **new**

**Description:**
For each `@Pane` in the metadata, generate:
- `IrCall` to `NavigationConfigBuilder.panes<D>()` with type argument = pane sealed class
- `scopeKey` = `@Pane.name`
- Builder lambda body (`PanesBuilder.() -> Unit`):
  - `backBehavior` assignment from `@Pane.backBehavior` enum value
  - For each `@PaneItem` subclass:
    - `role` from `@PaneItem.role` (`PaneRole.PRIMARY`, `SECONDARY`, `EXTRA`)
    - `adaptStrategy` from `@PaneItem.adaptStrategy`
    - Generate pane registration call with role, strategy, and nested destinations

**Metadata used:** `NavigationMetadata.panes`

**Acceptance Criteria:**
- [ ] One `panes<D>()` call generated per `@Pane` annotation
- [ ] `backBehavior` enum value correctly resolved as IR enum entry reference
- [ ] Each `@PaneItem` produces a pane registration with correct `PaneRole`
- [ ] `AdaptStrategy` enum values correctly mapped
- [ ] Pane item ordering preserved

---

#### Task 3B.6: Scope Registration Blocks

Generate IR for `scope(key) { +DestClass::class ... }` calls.

**Files to create:**
- `quo-vadis-compiler-plugin/src/main/kotlin/com/jermey/quo/vadis/compiler/ir/generators/dsl/ScopeDslIrGenerator.kt` — **new**

**Description:**
Scope registration maps scope keys to sets of destination KClasses. The KSP processor collects this from:
- Stack destinations → scope key from `@Stack.name`
- Tab items → scope key from `@Tabs.name`
- Pane items → scope key from `@Pane.name`

For each scope entry, generate:
- `IrCall` to `NavigationConfigBuilder.scope(scopeKey) { ... }`
- Builder lambda body: `+DestinationClass::class` for each member destination

**Metadata used:** Derived from `NavigationMetadata.stacks`, `.tabs`, `.panes` — all destinations grouped by their containing scope key

**Acceptance Criteria:**
- [ ] One `scope()` call per unique scope key
- [ ] All destination classes within each scope are registered
- [ ] `KClass` references (`::class`) generate correct `IrClassReference` IR
- [ ] Scope keys match those used in container registrations
- [ ] No duplicate scope entries

---

#### Task 3B.7: Transition Registration Blocks

Generate IR for `transition<D>(NavTransition.X)` calls.

**Files to create:**
- `quo-vadis-compiler-plugin/src/main/kotlin/com/jermey/quo/vadis/compiler/ir/generators/dsl/TransitionDslIrGenerator.kt` — **new**

**Description:**
For each `@Transition`-annotated destination class, generate:
- `IrCall` to `NavigationConfigBuilder.transition<D>(transition)` with:
  - Type argument = destination class
  - `transition` argument = `NavTransition.SlideHorizontal` / `.SlideVertical` / `.Fade` / `.None` (enum-like references)
  - For `TransitionType.Custom`: reference the `customTransition` KClass to build a `NavTransition.Custom(instance)` expression

**Metadata used:** `NavigationMetadata.transitions`

**Acceptance Criteria:**
- [ ] One `transition<D>()` call per `@Transition` annotation
- [ ] Standard transition types correctly map to `NavTransition` companion entries
- [ ] `Custom` transition type creates instance from provided `KClass`
- [ ] Type argument resolves to the correct destination class
- [ ] No transitions emitted for destinations without `@Transition`

---

#### Task 3B.8: Deep Link Registration in DSL

**Note:** The current KSP implementation handles deep links entirely through the generated `DeepLinkHandler` object (not through DSL calls in `baseConfig`). The `DeepLinkBlockGenerator` in KSP is a placeholder that generates only a comment.

**Decision:** Deep links will continue to be handled entirely by the `{Prefix}DeepLinkHandler` object (Sub-phase 3E). No deep link DSL calls are needed in `baseConfig`. This task is a **no-op** — documented here for completeness and to record the decision.

If future DSL-based deep link configuration is added to `NavigationConfigBuilder`, this task can be revisited.

**Acceptance Criteria:**
- [ ] Confirmed: no deep link DSL calls generated in `baseConfig`
- [ ] `deepLinkRegistry` property in `NavigationConfig` references the generated `DeepLinkHandler` object directly

---

### Sub-phase 3C: Screen Registry IR Generation

#### Task 3C.1: Anonymous ScreenRegistry Object

Create an anonymous `object : ScreenRegistry { ... }` IR expression for the `screenRegistry` property.

**Files to create:**
- `quo-vadis-compiler-plugin/src/main/kotlin/com/jermey/quo/vadis/compiler/ir/generators/ScreenRegistryIrGenerator.kt` — **new**

**Description:**
- Generate an anonymous `IrClass` with `ClassKind.OBJECT` implementing `ScreenRegistry`
- Assign as the initializer of the `screenRegistry` property in `{Prefix}NavigationConfig`
- The anonymous object overrides two methods:
  - `Content(destination, sharedTransitionScope?, animatedVisibilityScope?)` — composable, Task 3C.2
  - `hasContent(destination)` — Task 3C.3

**Acceptance Criteria:**
- [ ] Anonymous object declaration compiles as valid IR
- [ ] Supertype `ScreenRegistry` correctly set
- [ ] Both `Content` and `hasContent` override declarations present
- [ ] Property initializer correctly assigns the anonymous object

---

#### Task 3C.2: `Content()` Method — Composable When-Dispatch

Generate the `@Composable Content()` method body with `when`-dispatch to user-defined `@Screen` functions.

**Files to modify:**
- `quo-vadis-compiler-plugin/src/main/kotlin/com/jermey/quo/vadis/compiler/ir/generators/ScreenRegistryIrGenerator.kt`

**Description:**
Generate an `IrWhen` expression inside the `Content` method:

```
when (destination) {
    is HomeDestination.Feed -> FeedScreen()
    is HomeDestination.Detail -> DetailScreen(
        destination = destination,  // smart-cast
        sharedTransitionScope = sharedTransitionScope!!,
        animatedVisibilityScope = animatedVisibilityScope!!
    )
    else -> error("No screen registered for destination: $destination")
}
```

**IR construction details:**
- Each branch is an `IrBranch` with:
  - Condition: `IrTypeOperatorCall` (`INSTANCEOF`) for `destination is DestType`
  - Result: `IrCall` to the `@Screen`-annotated function
- Smart cast: the `destination` parameter after `is` check is used as the specific type
- Parameter detection per `ScreenMetadata`:
  - `hasDestinationParam=true` → pass `destination` (smart-casted) as first arg
  - `hasSharedTransitionScope=true` → pass `sharedTransitionScope!!` (not-null assertion)
  - `hasAnimatedVisibilityScope=true` → pass `animatedVisibilityScope!!`
- Note: `Navigator` is **NOT** passed as parameter (provided via CompositionLocal at runtime)
- `else` branch: `IrCall` to `kotlin.error()` with diagnostic string

**Metadata used:** `NavigationMetadata.screens`

**Acceptance Criteria:**
- [ ] `when` dispatch covers all `@Screen`-registered destinations
- [ ] Smart casts generate correct `IrTypeOperatorCall` + implicit cast IR
- [ ] `@Screen` function calls resolve to the correct user-defined `FqName`
- [ ] Parameter passing matches each screen function's detected params
- [ ] `SharedTransitionScope` and `AnimatedVisibilityScope` passed with `!!` when needed
- [ ] `else` branch produces runtime error with descriptive message
- [ ] Method has `@Composable` annotation in IR

---

#### Task 3C.3: `hasContent()` Method

Generate the `hasContent(destination: NavDestination): Boolean` method body.

**Files to modify:**
- `quo-vadis-compiler-plugin/src/main/kotlin/com/jermey/quo/vadis/compiler/ir/generators/ScreenRegistryIrGenerator.kt`

**Description:**
Simple `when` expression returning `true` for registered destinations, `false` otherwise:

```
return when (destination) {
    is HomeDestination.Feed, is HomeDestination.Detail -> true
    else -> false
}
```

**IR construction:**
- Single `IrBranch` with multiple `is` conditions OR-ed
- Returns `IrConst<Boolean>(true)` or `IrConst<Boolean>(false)`

**Acceptance Criteria:**
- [ ] Returns `true` for all destinations that have a `@Screen` mapping
- [ ] Returns `false` for unknown destinations
- [ ] Branch conditions match exactly those in `Content()`

---

#### Task 3C.4: Compose Compiler Interop

Ensure all composable IR declarations are compatible with the Compose compiler plugin.

**Files to modify:**
- `quo-vadis-compiler-plugin/src/main/kotlin/com/jermey/quo/vadis/compiler/ir/generators/ScreenRegistryIrGenerator.kt`
- (any other file generating `@Composable` methods)

**Description:**
The Compose compiler plugin transforms `@Composable` functions by adding `$composer` and `$changed` parameters. Generated IR must:

1. **Apply `@Composable` annotation** to all composable method overrides (`Content`, `TabsContainer`, `PaneContainer`)
2. **Not interfere with Compose lowering** — the Compose plugin runs as a separate IR transformation pass. Our generated `IrCall` to user `@Composable` functions must be simple function calls; the Compose compiler will add the `$composer` threading automatically
3. **Ordering concern**: Verify that the Quo-Vadis IR extension runs *before* the Compose compiler's IR pass so that composable stubs have bodies by the time Compose processes them. If ordering is not guaranteed, investigate `CompilerPluginRegistrar` registration order or priority mechanisms

**Investigation required:**
- Determine the plugin execution order (Compose IR vs Quo-Vadis IR)
- Test whether the Compose plugin correctly transforms our synthetic composable methods
- If ordering issues arise, consider generating composable call-through wrappers

**Acceptance Criteria:**
- [ ] `@Composable` annotation present on all composable override methods in IR
- [ ] Compose compiler successfully processes synthetic composable methods
- [ ] No `$composer` or `$changed` parameter mismatches at runtime
- [ ] Screen dispatch works correctly on all platforms (JVM, iOS, JS, Wasm)
- [ ] Shared transition and animated visibility scopes pass through correctly

---

### Sub-phase 3D: Container Registry IR Generation

#### Task 3D.1: Merged ContainerRegistry Anonymous Object

Create the anonymous `object : ContainerRegistry { ... }` that merges DSL-based building with composable wrapper dispatch.

**Files to create:**
- `quo-vadis-compiler-plugin/src/main/kotlin/com/jermey/quo/vadis/compiler/ir/generators/ContainerRegistryIrGenerator.kt` — **new**

**Description:**
Generate an anonymous `IrClass` implementing `ContainerRegistry` with:
- Private `val tabsContainerKeys = setOf("key1", "key2", ...)` — set of tab scope keys that have `@TabsContainer` wrappers
- Private `val paneContainerKeys = setOf("key1", ...)` — set of pane scope keys that have `@PaneContainer` wrappers
- Five method overrides (Tasks 3D.2–3D.5)

**Metadata used:** `NavigationMetadata.tabs`, `.panes`, `.tabsContainers`, `.paneContainers`

**Acceptance Criteria:**
- [ ] Anonymous object implements `ContainerRegistry`
- [ ] `tabsContainerKeys` and `paneContainerKeys` sets correctly populated
- [ ] All five `ContainerRegistry` methods declared as overrides
- [ ] Assigned to `containerRegistry` property of `NavigationConfig`

---

#### Task 3D.2: `getContainerInfo()` Delegation

Generate `getContainerInfo(destination)` that delegates to `baseConfig.containerRegistry.getContainerInfo(destination)`.

**Files to modify:**
- `quo-vadis-compiler-plugin/src/main/kotlin/com/jermey/quo/vadis/compiler/ir/generators/ContainerRegistryIrGenerator.kt`

**Description:**
Simple delegation IR:
```
override fun getContainerInfo(destination: NavDestination): ContainerInfo? =
    baseConfig.containerRegistry.getContainerInfo(destination)
```

**IR construction:**
- `IrCall` to `baseConfig` field getter → `.containerRegistry` property getter → `.getContainerInfo(destination)` method call
- Chain of property access + method call

**Acceptance Criteria:**
- [ ] Delegates to `baseConfig.containerRegistry.getContainerInfo()`
- [ ] Return type is `ContainerInfo?` (nullable)
- [ ] `baseConfig` lazy access generates correct `.value` IR

---

#### Task 3D.3: `TabsContainer()` Composable When-Dispatch

Generate `@Composable TabsContainer(tabNodeKey, scope, content)` with when-dispatch to `@TabsContainer` functions.

**Files to modify:**
- `quo-vadis-compiler-plugin/src/main/kotlin/com/jermey/quo/vadis/compiler/ir/generators/ContainerRegistryIrGenerator.kt`

**Description:**
```
@Composable
override fun TabsContainer(tabNodeKey: String, scope: TabsContainerScope, content: @Composable () -> Unit) {
    when (tabNodeKey) {
        "mainTabs" -> MainTabsWrapper(scope = scope, content = content)
        else -> content()
    }
}
```

**IR construction:**
- `IrWhen` on `tabNodeKey` string
- Each branch: `IrBranch` with string equality check → `IrCall` to `@TabsContainer`-annotated function
- Wrapper function receives `scope` and `content` parameters
- `else` branch: direct `content()` invocation (default passthrough)
- Method annotated with `@Composable`

**Metadata used:** `NavigationMetadata.tabsContainers` — maps tab class → wrapper function FqName, derive scope key from corresponding `@Tabs.name`

**Acceptance Criteria:**
- [ ] One `when` branch per `@TabsContainer` function
- [ ] String equality checks on scope keys
- [ ] Wrapper function called with correct `scope` and `content` parameters
- [ ] `else` branch calls `content()` directly
- [ ] `@Composable` annotation present

---

#### Task 3D.4: `PaneContainer()` Composable When-Dispatch

Generate `@Composable PaneContainer(paneNodeKey, scope, content)` with when-dispatch to `@PaneContainer` functions.

**Files to modify:**
- `quo-vadis-compiler-plugin/src/main/kotlin/com/jermey/quo/vadis/compiler/ir/generators/ContainerRegistryIrGenerator.kt`

**Description:**
Mirrors Task 3D.3 pattern but for pane containers:
```
@Composable
override fun PaneContainer(paneNodeKey: String, scope: PaneContainerScope, content: @Composable () -> Unit) {
    when (paneNodeKey) {
        "catalog" -> CatalogPaneContainer(scope = scope, content = content)
        else -> content()
    }
}
```

**Metadata used:** `NavigationMetadata.paneContainers`

**Acceptance Criteria:**
- [ ] One `when` branch per `@PaneContainer` function
- [ ] String equality checks on pane scope keys
- [ ] Wrapper function called with correct `PaneContainerScope` and `content`
- [ ] `else` branch calls `content()` directly
- [ ] `@Composable` annotation present

---

#### Task 3D.5: `hasTabsContainer()` / `hasPaneContainer()` Set-Based Checks

Generate the boolean check methods using pre-built sets.

**Files to modify:**
- `quo-vadis-compiler-plugin/src/main/kotlin/com/jermey/quo/vadis/compiler/ir/generators/ContainerRegistryIrGenerator.kt`

**Description:**
```
override fun hasTabsContainer(tabNodeKey: String): Boolean = tabNodeKey in tabsContainerKeys
override fun hasPaneContainer(paneNodeKey: String): Boolean = paneNodeKey in paneContainerKeys
```

**IR construction:**
- `IrCall` to `Set.contains()` (or `in` operator which lowers to `contains`)
- Uses the private `tabsContainerKeys` / `paneContainerKeys` sets defined in Task 3D.1

**Acceptance Criteria:**
- [ ] Returns `true` for keys with registered wrappers
- [ ] Returns `false` for unknown keys
- [ ] Uses set membership (`in`) — O(1) lookup
- [ ] Sets populated from `@TabsContainer`/`@PaneContainer` metadata

---

### Sub-phase 3E: Deep Link Handler IR Generation

#### Task 3E.1: RoutePattern Helper Class

Generate the private `{Prefix}RoutePattern` data class used internally by the deep link handler.

**Files to create:**
- `quo-vadis-compiler-plugin/src/main/kotlin/com/jermey/quo/vadis/compiler/ir/generators/DeepLinkHandlerIrGenerator.kt` — **new**

**Description:**
Generate a private `IrClass` (data class) within the same file/package:

```kotlin
private data class {Prefix}RoutePattern(
    val pattern: String,
    val paramNames: List<String>,
    val createDestination: (Map<String, String>) -> NavDestination
) {
    private val regex: Regex = buildRegex()

    private fun buildRegex(): Regex {
        var regexPattern = pattern
            .replace("{", "(?<")
            .replace("}", ">[^/]+)")
        regexPattern = "^$regexPattern$"
        return Regex(regexPattern)
    }

    fun match(path: String): Map<String, String>? {
        val result = regex.matchEntire(path) ?: return null
        return paramNames.associateWith { name ->
            result.groups[name]?.value ?: ""
        }
    }
}
```

**Note:** Named regex groups may not work consistently across all Kotlin platforms (JS/Wasm). The actual KSP implementation handles this — replicate the same regex-building approach.

**IR construction:**
- `IrClass` with `data` flag, private visibility
- Properties: `pattern`, `paramNames`, `createDestination` (function type)
- `regex` private property with `buildRegex()` initializer
- `buildRegex()` and `match()` method bodies

**Acceptance Criteria:**
- [ ] `RoutePattern` class compiles on all platforms (JVM, JS, Wasm, Native)
- [ ] Regex building correctly transforms `{param}` → capturing groups
- [ ] `match()` returns parameter map on match, `null` on mismatch
- [ ] Class is private/internal to the generated file scope
- [ ] `createDestination` lambda invocation works correctly

---

#### Task 3E.2: Regex Building from Route Patterns

Implement the route pattern → regex conversion logic within the `RoutePattern` IR.

**Files to modify:**
- `quo-vadis-compiler-plugin/src/main/kotlin/com/jermey/quo/vadis/compiler/ir/generators/DeepLinkHandlerIrGenerator.kt`

**Description:**
The KSP-generated regex builder handles:
- `{paramName}` → named capturing group
- Fixed path segments → literal match
- Optional trailing slash handling
- Query parameter extraction (for optional params)

Generate IR for `String.replace()` calls and `Regex()` constructor invocation.

**Cross-platform consideration:** Named groups (`(?<name>...)`) are not supported on all JS engines. The KSP implementation uses index-based extraction as fallback. Replicate the same strategy.

**Acceptance Criteria:**
- [ ] Route `"home/feed"` produces regex matching exactly `"home/feed"`
- [ ] Route `"home/detail/{id}"` produces regex capturing `id` parameter
- [ ] Route `"catalog/{category}/{itemId}"` captures both parameters
- [ ] Empty route (`""`) handled gracefully
- [ ] Regex works on JVM, JS, Wasm, and Native

---

#### Task 3E.3: Type-Safe Argument Extraction

Generate IR for converting captured string parameters to typed values.

**Files to modify:**
- `quo-vadis-compiler-plugin/src/main/kotlin/com/jermey/quo/vadis/compiler/ir/generators/DeepLinkHandlerIrGenerator.kt`

**Description:**
Within the `createDestination` lambda of each `RoutePattern`, generate type-safe conversions:

| Argument Type | Conversion IR | Optional Variant |
|--------------|---------------|-----------------|
| `String` | `params["key"]!!` | `params["key"]` |
| `Int` | `params["key"]!!.toInt()` | `params["key"]?.toIntOrNull()` |
| `Long` | `params["key"]!!.toLong()` | `params["key"]?.toLongOrNull()` |
| `Float` | `params["key"]!!.toFloat()` | `params["key"]?.toFloatOrNull()` |
| `Double` | `params["key"]!!.toDouble()` | `params["key"]?.toDoubleOrNull()` |
| `Boolean` | `params["key"]!!.toBoolean()` | `params["key"]?.toBooleanStrictOrNull()` |
| `Enum<T>` | `enumValueOf<T>(params["key"]!!)` | `runCatching { enumValueOf<T>(...) }.getOrNull()` |

**IR construction for each conversion:**
- `IrCall` to `Map.get()` → `IrCall` to `.toXxx()` conversion function
- Non-null assertion (`!!`) or safe call (`?.`) based on `@Argument.optional`
- Enum: `IrCall` to `enumValueOf<T>()` with reified type argument

**Metadata used:** `ArgumentMetadata.type`, `.optional`

**Acceptance Criteria:**
- [ ] All 7 argument types generate correct conversion IR
- [ ] Optional arguments use safe calls / null-safe conversions
- [ ] Required arguments use `!!` assertions
- [ ] Custom `@Argument.key` overrides the parameter name used in `params["key"]`
- [ ] Enum conversion handles the reified type argument correctly

---

#### Task 3E.4: DeepLinkRegistry Interface Implementations

Generate bodies for all `DeepLinkRegistry` interface methods in the `{Prefix}DeepLinkHandler` object.

**Files to modify:**
- `quo-vadis-compiler-plugin/src/main/kotlin/com/jermey/quo/vadis/compiler/ir/generators/DeepLinkHandlerIrGenerator.kt`

**Description:**
Generate the `{Prefix}DeepLinkHandler` object body with:

| Method | Implementation |
|--------|----------------|
| `routes` (private property) | `listOf(RoutePattern(...), RoutePattern(...), ...)` containing all destinations with non-empty routes |
| `patternStrings` (private property) | `routes.map { it.pattern }` |
| `resolve(uri: String): NavDestination?` | Extract path from URI, iterate `routes`, call `match()`, return `createDestination(params)` or null |
| `resolve(deepLink: DeepLink): NavDestination?` | Delegate to `resolve(deepLink.uri)` |
| `register(pattern, factory)` | No-op (generated handler is immutable) |
| `registerAction(pattern, action)` | No-op |
| `handle(uri: String, navigator: Navigator): Boolean` | Resolve destination, if non-null call `navigator.navigate(destination)`, return boolean |
| `createUri(destination: NavDestination, scheme: String): String?` | `when(destination)` dispatch → construct URI string from route pattern + argument values |
| `canHandle(uri: String): Boolean` | `resolve(uri) != null` |
| `getRegisteredPatterns(): List<String>` | Return `patternStrings` |

Private helper:
| `extractPath(uri: String): String` | Strip scheme/authority, return path component |

**Metadata used:** `NavigationMetadata.stacks[*].destinations` (those with non-empty `route`)

**Acceptance Criteria:**
- [ ] All `DeepLinkRegistry` interface methods have bodies
- [ ] `resolve()` correctly matches URIs to destinations
- [ ] `handle()` navigates on match, returns `false` on no match
- [ ] `createUri()` reverse-builds URI from destination constructor args
- [ ] `canHandle()` is consistent with `resolve()`
- [ ] `register()`/`registerAction()` are no-ops
- [ ] `extractPath()` handles schemes (`myapp://path`, `https://host/path`)

---

#### Task 3E.5: URI Creation from Destination Instances

Generate `createUri(destination, scheme)` body that reverses argument extraction — builds URI from destination properties.

**Files to modify:**
- `quo-vadis-compiler-plugin/src/main/kotlin/com/jermey/quo/vadis/compiler/ir/generators/DeepLinkHandlerIrGenerator.kt`

**Description:**
```kotlin
override fun createUri(destination: NavDestination, scheme: String): String? {
    return when (destination) {
        is HomeDestination.Feed -> "$scheme://home/feed"
        is HomeDestination.Detail -> "$scheme://home/detail/${destination.id}"
        // ... other routed destinations
        else -> null
    }
}
```

**IR construction:**
- `IrWhen` on `destination` type checks
- Each branch: string template with scheme prefix + route pattern with `{param}` replaced by `destination.propertyName`
- Access destination data class properties via `IrGetField` or property getter calls on the smart-cast destination
- `else` → `IrConst<Nothing?>(null)`

**Metadata used:** `NavigationMetadata.stacks[*].destinations` with routes, their `ArgumentMetadata`

**Acceptance Criteria:**
- [ ] Each routed destination generates a URI construction branch
- [ ] Route parameters replaced with actual property values from the destination instance
- [ ] Scheme prefix correctly prepended
- [ ] Non-routed destinations return `null`
- [ ] String concatenation uses `StringBuilder` or string template IR efficiently

---

### Sub-phase 3F: Remaining Registries & Wiring

#### Task 3F.1: PaneRoleRegistry When-Dispatch

Generate the `PaneRoleRegistry` anonymous object with when-dispatch for scope+destination → `PaneRole` mapping.

**Files to create:**
- `quo-vadis-compiler-plugin/src/main/kotlin/com/jermey/quo/vadis/compiler/ir/generators/PaneRoleRegistryIrGenerator.kt` — **new**

**Description:**
Generate anonymous `object : PaneRoleRegistry`:

```kotlin
override fun getPaneRole(scopeKey: String, destination: NavDestination): PaneRole? {
    return when {
        scopeKey == "catalog" && destination is CatalogPane.List -> PaneRole.PRIMARY
        scopeKey == "catalog" && destination is CatalogPane.Detail -> PaneRole.SECONDARY
        else -> null
    }
}

override fun getPaneRole(scopeKey: String, destinationClass: KClass<out NavDestination>): PaneRole? {
    return when {
        scopeKey == "catalog" && destinationClass == CatalogPane.List::class -> PaneRole.PRIMARY
        // ...
        else -> null
    }
}

override fun hasPaneRole(scopeKey: String, destination: NavDestination): Boolean {
    return getPaneRole(scopeKey, destination) != null
}
```

**Metadata used:** `NavigationMetadata.panes[*].items` — `PaneItemMetadata.role`

**Acceptance Criteria:**
- [ ] Both `getPaneRole` overloads generate correct when-dispatch
- [ ] `hasPaneRole` delegates to `getPaneRole`
- [ ] Scope key + destination/destinationClass pair uniquely identifies a role
- [ ] All `@PaneItem` entries with explicit roles are covered
- [ ] Destinations without pane roles return `null`

---

#### Task 3F.2: Property Delegation Wiring

Generate property initializers that delegate `scopeRegistry` and `transitionRegistry` to `baseConfig`.

**Files to modify:**
- `quo-vadis-compiler-plugin/src/main/kotlin/com/jermey/quo/vadis/compiler/ir/generators/NavigationConfigIrGenerator.kt`

**Description:**
```kotlin
override val scopeRegistry: ScopeRegistry get() = baseConfig.scopeRegistry
override val transitionRegistry: TransitionRegistry get() = baseConfig.transitionRegistry
```

**IR construction:**
- `IrProperty` with getter body: `IrGetField(baseConfig)` → `IrCall(getProperty(scopeRegistry))`
- Accessing `baseConfig` triggers the lazy delegate (`.value`) automatically via the delegate pattern

**Acceptance Criteria:**
- [ ] `scopeRegistry` returns `baseConfig.scopeRegistry`
- [ ] `transitionRegistry` returns `baseConfig.transitionRegistry`
- [ ] Lazy initialization triggered correctly on first access
- [ ] Types match interface declarations exactly

---

#### Task 3F.3: `buildNavNode()` Delegation

Generate `buildNavNode()` that delegates to `baseConfig.buildNavNode()`.

**Files to modify:**
- `quo-vadis-compiler-plugin/src/main/kotlin/com/jermey/quo/vadis/compiler/ir/generators/NavigationConfigIrGenerator.kt`

**Description:**
```kotlin
override fun buildNavNode(
    destinationClass: KClass<out NavDestination>,
    key: String?,
    parentKey: String?
): NavNode? = baseConfig.buildNavNode(destinationClass, key, parentKey)
```

**IR construction:**
- `IrCall` forwarding all three parameters to `baseConfig.buildNavNode()`

**Acceptance Criteria:**
- [ ] All three parameters forwarded correctly
- [ ] Return type is `NavNode?`
- [ ] Delegates to `baseConfig` (lazy-initialized DSL config)

---

#### Task 3F.4: `plus()` Operator Implementation

Generate the `plus` operator that wraps in `CompositeNavigationConfig`.

**Files to modify:**
- `quo-vadis-compiler-plugin/src/main/kotlin/com/jermey/quo/vadis/compiler/ir/generators/NavigationConfigIrGenerator.kt`

**Description:**
```kotlin
override operator fun plus(other: NavigationConfig): NavigationConfig =
    CompositeNavigationConfig(this, other)
```

**IR construction:**
- `IrCall` to `CompositeNavigationConfig` constructor
- `this` reference: `IrGetValue` for the receiver (`this`)
- `other` reference: `IrGetValue` for the parameter

**Acceptance Criteria:**
- [ ] Creates `CompositeNavigationConfig(this, other)`
- [ ] `CompositeNavigationConfig` symbol resolved via `SymbolResolver`
- [ ] Return type is `NavigationConfig`
- [ ] `operator` modifier preserved

---

#### Task 3F.5: `roots` Property

Generate the `roots: Set<KClass<out NavDestination>>` property containing all root destination classes.

**Files to modify:**
- `quo-vadis-compiler-plugin/src/main/kotlin/com/jermey/quo/vadis/compiler/ir/generators/NavigationConfigIrGenerator.kt`

**Description:**
```kotlin
val roots: Set<KClass<out NavDestination>> = setOf(MainTabs::class, HomeDestination::class, ...)
```

Root classes are:
- `@Tabs`-annotated classes (tab container roots)
- `@Stack`-annotated sealed classes that are **not** tab items within a `@Tabs` (standalone stack roots)
- `@Pane`-annotated sealed classes

**IR construction:**
- `IrCall` to `setOf()` with `IrClassReference` arguments for each root class

**Metadata used:** `NavigationMetadata.tabs`, `.stacks`, `.panes` — determine which are top-level roots

**Acceptance Criteria:**
- [ ] Contains all top-level entry point destination classes
- [ ] Tab items that are nested stacks are NOT included as roots (their parent `@Tabs` is the root)
- [ ] `KClass` references generate correct `IrClassReference` IR
- [ ] Set is immutable (`setOf()`)

---

#### Task 3F.6: End-to-End Integration & Wiring

Final assembly: wire all sub-components together and validate the complete `NavigationConfig`.

**Files to modify:**
- `quo-vadis-compiler-plugin/src/main/kotlin/com/jermey/quo/vadis/compiler/ir/generators/NavigationConfigIrGenerator.kt`
- `quo-vadis-compiler-plugin/src/main/kotlin/com/jermey/quo/vadis/compiler/ir/BodySynthesisTransformer.kt`

**Description:**
Ensure all property initializers and method bodies are wired:

| Property/Method | Source |
|-----------------|--------|
| `baseConfig` | Task 3B.2 (lazy DSL config) |
| `screenRegistry` | Task 3C.1 (anonymous ScreenRegistry) |
| `containerRegistry` | Task 3D.1 (merged anonymous ContainerRegistry) |
| `scopeRegistry` | Task 3F.2 (delegation to baseConfig) |
| `transitionRegistry` | Task 3F.2 (delegation to baseConfig) |
| `deepLinkRegistry` | Reference to `{Prefix}DeepLinkHandler` object |
| `paneRoleRegistry` | Task 3F.1 (anonymous PaneRoleRegistry) |
| `buildNavNode()` | Task 3F.3 (delegation to baseConfig) |
| `plus()` | Task 3F.4 (CompositeNavigationConfig) |
| `roots` | Task 3F.5 (set of root KClasses) |

**Integration points:**
1. `BodySynthesisTransformer` invokes each generator in correct order
2. `deepLinkRegistry` property initializer references the `{Prefix}DeepLinkHandler` IR symbol (resolved in Pass 1)
3. All anonymous objects are properly nested within the `NavigationConfig` object
4. No dangling references — every IR symbol used is resolved

**Acceptance Criteria:**
- [ ] All 6 registry properties have initializer bodies
- [ ] All 3 methods (`buildNavNode`, `plus`, `hasContent` on sub-objects) have bodies
- [ ] `{Prefix}DeepLinkHandler` is referenced (not inlined) as the deep link registry
- [ ] Complete `NavigationConfig` object compiles without IR verification errors
- [ ] `./gradlew build -Xverify-ir` passes
- [ ] Demo app (`composeApp`) runs with compiler plugin producing identical behavior to KSP output

---

## 4. Acceptance Criteria Summary

### Phase 3A: IR Foundation
- [ ] `IrGenerationExtension` registered and invoked during compilation
- [ ] Two-pass transformer infrastructure tested with no-op stubs
- [ ] `SymbolResolver` resolves all ~40 library types without error
- [ ] FIR-to-IR metadata bridge transfers complete annotation data

### Phase 3B: NavigationConfig Object Synthesis
- [ ] Object singleton with `NavigationConfig` supertype compiles
- [ ] `baseConfig` lazy property calls `navigationConfig { }` DSL
- [ ] Stack/Tabs/Pane DSL blocks generate correctly from metadata
- [ ] Scope and transition registrations complete
- [ ] All DSL calls resolve to correct runtime functions

### Phase 3C: Screen Registry
- [ ] `Content()` method dispatches to all `@Screen` composables
- [ ] Parameter detection (destination, shared transition, animated visibility) correct
- [ ] `hasContent()` returns correct booleans
- [ ] Compose compiler processes synthetic `@Composable` methods

### Phase 3D: Container Registry
- [ ] `getContainerInfo()` delegates to baseConfig
- [ ] `TabsContainer()` dispatches to `@TabsContainer` functions
- [ ] `PaneContainer()` dispatches to `@PaneContainer` functions
- [ ] `has*Container()` methods return correct booleans

### Phase 3E: Deep Link Handler
- [ ] Route patterns build correct regex on all platforms
- [ ] Type-safe argument extraction for all 7 types
- [ ] All `DeepLinkRegistry` methods implemented
- [ ] `createUri()` reverse-builds URIs correctly
- [ ] URI resolution works for nested routes

### Phase 3F: Wiring
- [ ] All properties delegate correctly
- [ ] `plus()` creates `CompositeNavigationConfig`
- [ ] `roots` set contains correct root classes
- [ ] End-to-end: single-module navigation works on all platforms

### Overall Phase 3 Gate
- [ ] `./gradlew build` succeeds with compiler plugin enabled
- [ ] `./gradlew build -Xverify-ir` passes (IR verification)
- [ ] Demo `composeApp` runs identically on desktop with compiler plugin vs KSP
- [ ] No regressions on Android, iOS, JS, and Wasm targets
- [ ] All `@Stack` + `@Destination` + `@Screen` combinations work end-to-end
- [ ] `@Tabs` and `@Pane` containers build and render
- [ ] Deep links resolve with type-safe arguments
- [ ] Transitions apply correctly per `@Transition` annotations

---

## 5. Risks

| Risk | Impact | Probability | Mitigation |
|------|--------|------------|------------|
| **Compose compiler ordering** — Compose IR plugin may run before our plugin, finding empty composable stubs | High | Medium | Investigate plugin ordering. If needed, use `CompilerPluginRegistrar` priority or restructure to generate non-composable wrappers that Compose doesn't need to transform |
| **Reified inline function calls in IR** — DSL functions like `stack<D>()` are `inline reified`; generating IR calls to inline functions may require special handling | High | Medium | Alternative: generate non-inlined overloads in the DSL, or manually inline the type argument as a `KClass` parameter in the generated IR |
| **Cross-platform regex divergence** — Named capturing groups behave differently on JS/Wasm vs JVM/Native | Medium | High | Replicate KSP's platform-aware regex strategy; use index-based capture as fallback; test on all targets |
| **IR API instability** — `IrPluginContext` and related APIs may change between Kotlin versions | Medium | Medium | Abstract IR construction behind internal utility functions; pin Kotlin version; wrap API calls for easy migration |
| **Large IR trees** — Modules with many destinations produce large `when` expressions; IR tree size may impact compile time | Low | Low | Profile after initial implementation; consider batching if >100 destinations |
| **Anonymous object scoping** — Multiple anonymous objects nested within the `NavigationConfig` object may cause IR scoping issues | Medium | Low | Test with nested anonymous classes; verify that each object gets a unique `IrDeclarationOrigin` and doesn't clash |
| **Lazy delegate IR generation** — Kotlin's `by lazy {}` involves delegate property lowering which may conflict with synthetic properties | Medium | Medium | Alternative: use a private `val` with manual `lazy {}` backing field + custom getter, avoiding the `by` delegate syntax entirely |

---

## 6. Open Questions

### OQ-3.1: Compose Plugin Execution Order
**Question:** Does the Kotlin Compose compiler plugin's IR pass run before or after custom `IrGenerationExtension` implementations?  
**Impact:** If Compose runs first and finds our empty composable stubs, it may skip them or produce incorrect lowering. If it runs after, our fully-materialized composable methods will be processed correctly.  
**Investigation:** Test with a minimal project; check `CompilerPluginRegistrar` ordering semantics.

### OQ-3.2: Inline Reified Function Calls in IR
**Question:** Can we generate `IrCall` nodes targeting `inline reified` functions (like `NavigationConfigBuilder.stack<D>()`), or must we use non-inline alternatives?  
**Impact:** If inline reified calls are not supported in generated IR, we need to either: (a) add non-inline overloads to the DSL API accepting `KClass<D>` explicitly, or (b) manually construct the IR that the inliner would produce.  
**Investigation:** Create a test case with a simple inline reified call from IR; check if the Kotlin inliner processes it correctly.

### OQ-3.3: FIR-to-IR Metadata Transfer Mechanism
**Question:** What is the canonical way to pass data from FIR extensions to IR extensions? Options: (a) `CompilerConfiguration` extra keys, (b) static store keyed by module, (c) FIR session services accessible from IR, (d) re-read annotations from the lowered IR tree.  
**Impact:** Affects architecture of the metadata bridge (Task 3A.4).  
**Investigation:** Study how Compose, Koin, and other plugins transfer FIR-phase data to IR.

### OQ-3.4: `@Composable` Annotation in IR
**Question:** Is it sufficient to add the `@Composable` annotation to our synthetic `IrFunction` declarations, or does the Compose compiler require additional metadata (e.g., `ComposableInferredTarget`)?  
**Impact:** If additional metadata is required, we need to understand and replicate it.  
**Investigation:** Inspect IR dumps of manually-written composable functions after Compose lowering.

### OQ-3.5: Named Regex Groups on JS/Wasm
**Question:** Does the current KSP `DeepLinkHandler` implementation use named groups or index-based capture for JS/Wasm compatibility?  
**Impact:** Determines whether the IR-generated regex builder needs platform-specific logic or can use a unified approach.  
**Investigation:** Read the `DeepLinkHandlerGenerator` output and test on JS target.

### OQ-3.6: IR Verification Strictness
**Question:** How strict is `-Xverify-ir` for synthetic declarations? Will it flag missing source locations, non-standard origins, or unusual class nesting?  
**Impact:** May require adding source offsets, fake source files, or specific `IrDeclarationOrigin` values.  
**Investigation:** Run IR verification on the Phase 3A no-op stubs and catalog any warnings/errors.

---

## 7. File Summary

### New Files

| File | Task | Purpose |
|------|------|---------|
| `compiler/ir/QuoVadisIrGenerationExtension.kt` | 3A.1 | IR generation entry point |
| `compiler/ir/StubMaterializationTransformer.kt` | 3A.2 | Pass 1: locate and verify FIR stubs |
| `compiler/ir/BodySynthesisTransformer.kt` | 3A.2 | Pass 2: inject method/property bodies |
| `compiler/ir/SymbolResolver.kt` | 3A.3 | Cached resolution of all library symbols |
| `compiler/common/PluginMetadataStore.kt` | 3A.4 | FIR → IR metadata bridge |
| `compiler/common/NavigationMetadata.kt` | 3A.4 | Structured metadata data model |
| `compiler/ir/generators/NavigationConfigIrGenerator.kt` | 3B.1, 3F.2–3F.6 | Top-level NavigationConfig object IR |
| `compiler/ir/generators/BaseConfigIrGenerator.kt` | 3B.2 | `baseConfig` lazy property with DSL call |
| `compiler/ir/generators/dsl/StackDslIrGenerator.kt` | 3B.3 | Stack DSL block IR |
| `compiler/ir/generators/dsl/TabsDslIrGenerator.kt` | 3B.4 | Tabs DSL block IR |
| `compiler/ir/generators/dsl/PanesDslIrGenerator.kt` | 3B.5 | Panes DSL block IR |
| `compiler/ir/generators/dsl/ScopeDslIrGenerator.kt` | 3B.6 | Scope registration IR |
| `compiler/ir/generators/dsl/TransitionDslIrGenerator.kt` | 3B.7 | Transition registration IR |
| `compiler/ir/generators/ScreenRegistryIrGenerator.kt` | 3C.1–3C.4 | ScreenRegistry anonymous object + when-dispatch |
| `compiler/ir/generators/ContainerRegistryIrGenerator.kt` | 3D.1–3D.5 | ContainerRegistry merged anonymous object |
| `compiler/ir/generators/DeepLinkHandlerIrGenerator.kt` | 3E.1–3E.5 | DeepLinkHandler object + RoutePattern |
| `compiler/ir/generators/PaneRoleRegistryIrGenerator.kt` | 3F.1 | PaneRoleRegistry anonymous object |

All file paths relative to: `quo-vadis-compiler-plugin/src/main/kotlin/com/jermey/quo/vadis/`

### Modified Files (from Phase 2)

| File | Task | Change |
|------|------|--------|
| `compiler/QuoVadisCompilerPluginRegistrar.kt` | 3A.1 | Register `IrGenerationExtension` |
| FIR extensions (Phase 2) | 3A.4 | Populate `PluginMetadataStore` |

---

## 8. Sequencing

```
3A.1 (IrGenerationExtension registration)
  │
  ├──→ 3A.2 (Two-pass transformer infrastructure)
  │
  ├──→ 3A.3 (Symbol resolution utilities)
  │
  └──→ 3A.4 (FIR-to-IR metadata bridge)
         │
         ▼
       3B.1 (NavigationConfig object structure)
         │
         ├──→ 3B.2 (baseConfig lazy property)
         │      │
         │      ├──→ 3B.3 (Stack DSL)  ─┐
         │      ├──→ 3B.4 (Tabs DSL)   ─┤
         │      ├──→ 3B.5 (Panes DSL)  ─┤── can be parallelized
         │      ├──→ 3B.6 (Scope DSL)  ─┤
         │      └──→ 3B.7 (Transition) ─┘
         │
         ├──→ 3C.1 → 3C.2 → 3C.3 → 3C.4  (Screen Registry, sequential)
         │
         ├──→ 3D.1 → 3D.2 ─┐
         │           3D.3  ─┤── 3D.3/3D.4 can be parallelized
         │           3D.4  ─┤
         │           3D.5  ─┘
         │
         ├──→ 3E.1 → 3E.2 → 3E.3 → 3E.4 → 3E.5  (Deep Link, mostly sequential)
         │
         ├──→ 3F.1 (PaneRoleRegistry)
         ├──→ 3F.2 (Property delegation)
         ├──→ 3F.3 (buildNavNode delegation)
         ├──→ 3F.4 (plus operator)
         └──→ 3F.5 (roots property)
                │
                ▼
              3F.6 (End-to-end integration & wiring)
```

**Critical path:** 3A.1 → 3A.4 → 3B.1 → 3B.2 → 3B.3 → 3C.2 → 3F.6

**Parallelizable work (once 3A.* and 3B.1 complete):**
- 3C.* (Screen Registry) and 3D.* (Container Registry) and 3E.* (Deep Link) can proceed in parallel
- Within 3B: DSL generators (3B.3–3B.7) can be developed in parallel
- 3F.1–3F.5 can proceed in parallel once their dependent generators exist
