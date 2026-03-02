# Phase 2: FIR Frontend — Synthetic Declarations & Diagnostics

**Status**: Not Started  
**Prerequisites**: Phase 1 complete (modules compile, Gradle plugin passes config)  
**Outcome**: IDE shows synthetic `{Prefix}NavigationConfig` and `{Prefix}DeepLinkHandler` in autocomplete; real-time diagnostics for annotation errors

---

## Overview

Phase 2 implements the Frontend Intermediate Representation (FIR) extensions. In the K2 compiler, FIR is responsible for:
- Parsing and semantic analysis
- Type resolution and inference
- Symbol table construction

The FIR phase declares the **existence and shape** of synthetic types — but NOT their implementation logic (that's Phase 3/IR). After Phase 2:

- Developers see `{Prefix}NavigationConfig` and `{Prefix}DeepLinkHandler` in IDE autocomplete
- `val config = MyAppNavigationConfig` type-checks correctly
- Route collisions, argument mismatches, and container role errors appear as IDE squiggles in real-time
- No physical `.kt` files are generated

---

## Technical Approach

### FIR Extension Points Used

| Extension | Purpose | Phase |
|-----------|---------|-------|
| `FirDeclarationGenerationExtension` | Create synthetic `{Prefix}NavigationConfig` object and `{Prefix}DeepLinkHandler` object | 2B |
| `FirAdditionalCheckersExtension` | Register diagnostic checkers for annotation validation | 2C |
| `FirStatusTransformerExtension` | Control visibility (public/internal) of synthetic declarations | 2B |

### Annotation Discovery Strategy

The plugin uses `FirPredicateBasedProvider` with `DeclarationPredicate` instances targeting:
- `com.jermey.quo.vadis.annotations.Stack`
- `com.jermey.quo.vadis.annotations.Destination`
- `com.jermey.quo.vadis.annotations.Screen`
- `com.jermey.quo.vadis.annotations.Tabs`
- `com.jermey.quo.vadis.annotations.TabItem`
- `com.jermey.quo.vadis.annotations.Pane`
- `com.jermey.quo.vadis.annotations.PaneItem`
- `com.jermey.quo.vadis.annotations.TabsContainer`
- `com.jermey.quo.vadis.annotations.PaneContainer`
- `com.jermey.quo.vadis.annotations.Transition`
- `com.jermey.quo.vadis.annotations.Argument`

The predicates aggregate all matching `FirClassSymbol` / `FirFunctionSymbol` references during resolution, making them available for generation and checking.

### Synthetic Declaration Structure

Two top-level declarations are synthesized:

1. **`{Prefix}NavigationConfig`** — an `object` implementing `NavigationConfig`
   - Supertypes: `NavigationConfig`
   - Members: `screenRegistry`, `scopeRegistry`, `transitionRegistry`, `containerRegistry`, `deepLinkRegistry`, `paneRoleRegistry`, `buildNavNode()`, `plus()`, `roots`
   - FIR only declares signatures — IR provides bodies

2. **`{Prefix}DeepLinkHandler`** — an `object` implementing `DeepLinkRegistry`
   - Supertypes: `DeepLinkRegistry`
   - Members: `resolve(uri)`, `resolve(deepLink)`, `handle(uri, navigator)`, `canHandle(uri)`, `createUri(destination, scheme)`, `getRegisteredPatterns()`, `register(...)`, `registerAction(...)`
   - FIR only declares signatures — IR provides bodies

---

## Tasks

### Sub-phase 2A: Annotation Discovery Infrastructure

#### Task 2A.1: Create `QuoVadisFirExtensionRegistrar`

**Description**: Create the FIR extension registrar that binds all FIR extensions into the compiler pipeline. This is called from `QuoVadisCompilerPluginRegistrar` (Phase 1).

**File to create**:
- `quo-vadis-compiler-plugin/src/main/kotlin/com/jermey/quo/vadis/compiler/fir/QuoVadisFirExtensionRegistrar.kt`

**File to modify**:
- `quo-vadis-compiler-plugin/src/main/kotlin/com/jermey/quo/vadis/compiler/QuoVadisCompilerPluginRegistrar.kt` — register FIR extensions

**Implementation sketch**:
```kotlin
class QuoVadisFirExtensionRegistrar(private val modulePrefix: String) : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +::QuoVadisDeclarationGenerationExtension.bind(modulePrefix)
        +::QuoVadisAdditionalCheckersExtension.bind()
    }
}
```

And in the registrar:
```kotlin
override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
    val modulePrefix = configuration.get(QuoVadisConfigurationKeys.MODULE_PREFIX) ?: return
    FirExtensionRegistrarAdapter.registerExtension(QuoVadisFirExtensionRegistrar(modulePrefix))
}
```

**Acceptance Criteria**:
- [ ] FIR extensions are registered during compilation
- [ ] `modulePrefix` is available to all FIR extensions

---

#### Task 2A.2: Create predicate-based annotation provider

**Description**: Define `DeclarationPredicate` instances for all Quo-Vadis annotations and register them with the `FirPredicateBasedProvider`.

**File to create**:
- `quo-vadis-compiler-plugin/src/main/kotlin/com/jermey/quo/vadis/compiler/fir/QuoVadisPredicates.kt`

**Implementation must**:
- Define FQN constants for all 11 annotations
- Create `DeclarationPredicate.create { annotated(fqName) }` for each
- Register predicates via `FirDeclarationPredicateRegistrar`
- Provide helper functions to query matched declarations

**Example**:
```kotlin
object QuoVadisPredicates {
    val STACK_FQN = FqName("com.jermey.quo.vadis.annotations.Stack")
    val DESTINATION_FQN = FqName("com.jermey.quo.vadis.annotations.Destination")
    // ... all 11 annotations
    
    val HAS_STACK = DeclarationPredicate.create { annotated(STACK_FQN) }
    val HAS_DESTINATION = DeclarationPredicate.create { annotated(DESTINATION_FQN) }
    // ... etc
}
```

**Acceptance Criteria**:
- [ ] All 11 annotation FQNames are defined
- [ ] Predicates are created for each annotation
- [ ] Predicates can be used in `FirDeclarationGenerationExtension` to discover annotated elements

---

#### Task 2A.3: Create annotation data extraction utilities

**Description**: Create utilities to extract annotation parameters from `FirAnnotation` instances. This maps annotation properties to structured data models (similar to KSP extractors but for FIR).

**File to create**:
- `quo-vadis-compiler-plugin/src/main/kotlin/com/jermey/quo/vadis/compiler/fir/AnnotationExtractor.kt`

**Must extract**:
- `@Stack`: `name: String`, `startDestination: KClass<*>` (resolved to `ClassId`)
- `@Destination`: `route: String`
- `@Screen`: `destination: KClass<*>` (resolved to `ClassId`)
- `@Argument`: `key: String`, `optional: Boolean`
- `@Tabs`: `name: String`, `initialTab: KClass<*>`, `items: Array<KClass<*>>`
- `@TabItem`: (marker — no params)
- `@Pane`: `name: String`, `backBehavior: PaneBackBehavior`
- `@PaneItem`: `role: PaneRole`, `adaptStrategy: AdaptStrategy`
- `@TabsContainer`: `tabClass: KClass<*>`
- `@PaneContainer`: `paneClass: KClass<*>`
- `@Transition`: `type: TransitionType`, `customTransition: KClass<*>`

**Acceptance Criteria**:
- [ ] Can extract all annotation parameters from FIR annotation instances
- [ ] KClass parameters are resolved to `ClassId`
- [ ] Enum parameters are resolved to their constant values
- [ ] String and boolean parameters are extracted correctly
- [ ] Array parameters (like `@Tabs.items`) are handled

---

### Sub-phase 2B: Synthetic Declaration Generation

#### Task 2B.1: Implement `FirDeclarationGenerationExtension` — top-level class IDs

**Description**: Override `getTopLevelClassIds()` to declare the intent to create `{Prefix}NavigationConfig` and `{Prefix}DeepLinkHandler`.

**File to create**:
- `quo-vadis-compiler-plugin/src/main/kotlin/com/jermey/quo/vadis/compiler/fir/QuoVadisDeclarationGenerationExtension.kt`

**Implementation**:
```kotlin
class QuoVadisDeclarationGenerationExtension(
    session: FirSession,
    private val modulePrefix: String
) : FirDeclarationGenerationExtension(session) {
    
    companion object {
        const val GENERATED_PACKAGE = "com.jermey.quo.vadis.generated"
    }
    
    private val configClassId = ClassId(
        FqName(GENERATED_PACKAGE),
        Name.identifier("${modulePrefix}NavigationConfig")
    )
    
    private val deepLinkHandlerClassId = ClassId(
        FqName(GENERATED_PACKAGE),
        Name.identifier("${modulePrefix}DeepLinkHandler")
    )
    
    override fun getTopLevelClassIds(): Set<ClassId> = setOf(
        configClassId,
        deepLinkHandlerClassId
    )
}
```

**Acceptance Criteria**:
- [ ] `getTopLevelClassIds()` returns both class IDs
- [ ] Class names use the configured `modulePrefix`
- [ ] Package is `com.jermey.quo.vadis.generated`

---

#### Task 2B.2: Generate `{Prefix}NavigationConfig` object declaration

**Description**: Override `generateTopLevelClassLikeDeclaration()` to construct the FIR object declaration for the NavigationConfig.

**Key requirements**:
- Class kind: `OBJECT` (Kotlin object/singleton)
- Supertype: `NavigationConfig` (resolved via `ClassId`)
- Register `GeneratedDeclarationKey` for plugin origin tracking
- Status: `FirResolvedDeclarationStatus` with `PUBLIC` visibility (configurable via Phase 4)

**Acceptance Criteria**:
- [ ] Synthetic object is created with correct name
- [ ] `NavigationConfig` is listed as supertype
- [ ] IDE recognizes the type (visible in autocomplete after build)

---

#### Task 2B.3: Generate `{Prefix}DeepLinkHandler` object declaration

**Description**: Same as 2B.2 but for the DeepLinkHandler.

**Key requirements**:
- Class kind: `OBJECT`
- Supertype: `DeepLinkRegistry`
- Register `GeneratedDeclarationKey`

**Acceptance Criteria**:
- [ ] Synthetic object is created with correct name
- [ ] `DeepLinkRegistry` is listed as supertype
- [ ] IDE recognizes the type

---

#### Task 2B.4: Declare NavigationConfig member signatures

**Description**: Override `getCallableNamesForClass()` and `generateProperties()` / `generateFunctions()` to define the member signatures of the NavigationConfig object.

**Members to declare**:

Properties:
| Name | Type | Override |
|------|------|---------|
| `screenRegistry` | `ScreenRegistry` | Yes |
| `scopeRegistry` | `ScopeRegistry` | Yes |
| `transitionRegistry` | `TransitionRegistry` | Yes |
| `containerRegistry` | `ContainerRegistry` | Yes |
| `deepLinkRegistry` | `DeepLinkRegistry` | Yes |
| `paneRoleRegistry` | `PaneRoleRegistry` | Yes |
| `roots` | `Set<KClass<out NavDestination>>` | No (library-specific) |

Functions:
| Name | Parameters | Return Type | Override |
|------|-----------|------------|---------|
| `buildNavNode` | `destinationClass: KClass<out NavDestination>, key: String?, parentKey: String?` | `NavNode?` | Yes |
| `plus` | `other: NavigationConfig` | `NavigationConfig` | Yes (operator) |

**Acceptance Criteria**:
- [ ] All property signatures are declared with correct types
- [ ] All function signatures are declared with correct parameter and return types
- [ ] Override status is correctly set for interface members
- [ ] IDE shows all members when accessing the synthetic object

---

#### Task 2B.5: Declare DeepLinkHandler member signatures

**Description**: Define member signatures for the DeepLinkHandler object.

**Members to declare** (from `DeepLinkRegistry` interface):

Functions:
| Name | Parameters | Return Type |
|------|-----------|------------|
| `resolve` | `uri: String` | `NavDestination?` |
| `resolve` | `deepLink: DeepLink` | `NavDestination?` |
| `register` | `pattern: String, factory: (Map<String, String>) -> NavDestination` | `Unit` |
| `registerAction` | `pattern: String, action: (Map<String, String>, Navigator) -> Unit` | `Unit` |
| `handle` | `uri: String, navigator: Navigator` | `Boolean` |
| `createUri` | `destination: NavDestination, scheme: String` | `String?` |
| `canHandle` | `uri: String` | `Boolean` |
| `getRegisteredPatterns` | (none) | `List<String>` |

Additional (non-interface):
| Name | Parameters | Return Type |
|------|-----------|------------|
| `handleDeepLink` | `uri: String` | `DeepLinkResult` |

**Acceptance Criteria**:
- [ ] All DeepLinkRegistry interface methods are declared
- [ ] `handleDeepLink` convenience method is declared
- [ ] Return types and parameter types are correct
- [ ] IDE shows all members when accessing the synthetic object

---

#### Task 2B.6: Implement `GeneratedDeclarationKey`

**Description**: Create the key class that links synthetic FIR declarations to the Quo-Vadis plugin, enabling the IR phase to identify which declarations to materialize.

**File to create**:
- `quo-vadis-compiler-plugin/src/main/kotlin/com/jermey/quo/vadis/compiler/QuoVadisGeneratedKey.kt`

**Implementation**:
```kotlin
object QuoVadisGeneratedKey : GeneratedDeclarationKey() {
    override fun toString(): String = "QuoVadisGeneratedKey"
}
```

**Acceptance Criteria**:
- [ ] All synthetic declarations use this key
- [ ] IR phase can filter declarations by this key

---

### Sub-phase 2C: Diagnostic Checkers

#### Task 2C.1: Create `QuoVadisAdditionalCheckersExtension`

**Description**: Register all custom diagnostic checkers with the compiler.

**File to create**:
- `quo-vadis-compiler-plugin/src/main/kotlin/com/jermey/quo/vadis/compiler/fir/checkers/QuoVadisAdditionalCheckersExtension.kt`

**Implementation**:
```kotlin
class QuoVadisAdditionalCheckersExtension(session: FirSession) : FirAdditionalCheckersExtension(session) {
    override val declarationCheckers = FirDeclarationCheckers().apply {
        classCheckers += RouteCollisionChecker
        classCheckers += ArgumentParityChecker
        classCheckers += ContainerRoleChecker
        classCheckers += TransitionCompatibilityChecker
        functionCheckers += ScreenValidationChecker
    }
}
```

**Acceptance Criteria**:
- [ ] All checkers are registered
- [ ] Extension is bound in `QuoVadisFirExtensionRegistrar`

---

#### Task 2C.2: Define diagnostic factories

**Description**: Create the error/warning message definitions that checkers will emit.

**File to create**:
- `quo-vadis-compiler-plugin/src/main/kotlin/com/jermey/quo/vadis/compiler/fir/checkers/QuoVadisDiagnostics.kt`

**Diagnostics to define**:

| ID | Severity | Message Template |
|----|----------|-----------------|
| `DUPLICATE_ROUTE` | ERROR | `Duplicate route "{0}" detected. Routes must be unique within a module.` |
| `ARGUMENT_ROUTE_MISMATCH` | ERROR | `@Argument property "{0}" does not match any placeholder in route "{1}".` |
| `MISSING_ROUTE_ARGUMENT` | ERROR | `Route placeholder "{0}" has no matching @Argument property.` |
| `MISSING_PRIMARY_PANE` | ERROR | `@Pane "{0}" must have exactly one @PaneItem with role = PaneRole.PRIMARY.` |
| `DUPLICATE_PANE_ROLE` | ERROR | `@Pane "{0}" has multiple @PaneItem entries with role = {1}.` |
| `INCOMPATIBLE_TRANSITION` | WARNING | `Transition {0} may not work correctly within a {1} container.` |
| `ORPHAN_SCREEN` | WARNING | `@Screen references destination {0} which has no @Destination annotation.` |
| `STACK_NOT_SEALED` | ERROR | `@Stack must be applied to a sealed class or sealed interface.` |
| `DESTINATION_NOT_IN_STACK` | ERROR | `@Destination must be a direct subclass of a @Stack-annotated sealed class.` |
| `SCREEN_INVALID_PARAMS` | WARNING | `@Screen function has unexpected parameter types. Expected: destination, Navigator, SharedTransitionScope?, AnimatedVisibilityScope?` |

**Acceptance Criteria**:
- [ ] All diagnostic IDs are defined
- [ ] Severity levels are appropriate (errors vs warnings)
- [ ] Message templates support parameter substitution

---

#### Task 2C.3: Implement `RouteCollisionChecker`

**Description**: Check for duplicate `@Destination(route = ...)` values within the compilation module.

**File to create**:
- `quo-vadis-compiler-plugin/src/main/kotlin/com/jermey/quo/vadis/compiler/fir/checkers/RouteCollisionChecker.kt`

**Logic**:
1. Collect all `@Destination`-annotated classes
2. Extract `route` parameter values
3. Group by route string (ignoring empty routes)
4. If any route has > 1 destination, emit `DUPLICATE_ROUTE` on each duplicate

**Acceptance Criteria**:
- [ ] Duplicate routes produce `DUPLICATE_ROUTE` error
- [ ] Empty routes are excluded from collision detection
- [ ] Error appears on the correct annotation site

---

#### Task 2C.4: Implement `ArgumentParityChecker`

**Description**: Validate that route placeholders (`{param}`) match `@Argument`-annotated constructor parameters.

**File to create**:
- `quo-vadis-compiler-plugin/src/main/kotlin/com/jermey/quo/vadis/compiler/fir/checkers/ArgumentParityChecker.kt`

**Logic**:
1. For each `@Destination` with a non-empty route, extract `{placeholder}` names from route string
2. Find the annotated class's constructor parameters marked with `@Argument`
3. Map `@Argument` key (or property name if key is empty) to placeholder names
4. Report `ARGUMENT_ROUTE_MISMATCH` for unmatched @Argument properties
5. Report `MISSING_ROUTE_ARGUMENT` for unmatched route placeholders (unless @Argument.optional = true)

**Acceptance Criteria**:
- [ ] Unmatched @Argument properties produce error
- [ ] Unmatched route placeholders (non-optional) produce error
- [ ] Optional arguments are excluded from missing-placeholder check
- [ ] Custom `@Argument(key = "custom")` is matched against route `{custom}`

---

#### Task 2C.5: Implement `ContainerRoleChecker`

**Description**: Validate `@Pane` containers have valid `@PaneItem` role configurations.

**File to create**:
- `quo-vadis-compiler-plugin/src/main/kotlin/com/jermey/quo/vadis/compiler/fir/checkers/ContainerRoleChecker.kt`

**Logic**:
1. For each `@Pane`-annotated class, find all `@PaneItem` subclasses
2. Ensure exactly one `PaneRole.PRIMARY` item exists → `MISSING_PRIMARY_PANE` if missing
3. Ensure no duplicate roles within same pane → `DUPLICATE_PANE_ROLE` if found

**Acceptance Criteria**:
- [ ] Missing PRIMARY role produces error
- [ ] Duplicate roles produce error
- [ ] Valid configurations pass without diagnostics

---

#### Task 2C.6: Implement `TransitionCompatibilityChecker`

**Description**: Warn about potentially incompatible transition types for specific container types.

**File to create**:
- `quo-vadis-compiler-plugin/src/main/kotlin/com/jermey/quo/vadis/compiler/fir/checkers/TransitionCompatibilityChecker.kt`

**Logic**:
1. For each `@Transition`-annotated destination, determine its parent container type
2. Warn if transition type is structurally inappropriate (e.g., `SlideVertical` in a horizontal tabs container)

**Acceptance Criteria**:
- [ ] Incompatible transitions produce warning (not error — transitions are stylistic)
- [ ] Compatible transitions pass without diagnostics

---

#### Task 2C.7: Implement `ScreenValidationChecker`

**Description**: Validate `@Screen`-annotated functions reference valid destinations.

**File to create**:
- `quo-vadis-compiler-plugin/src/main/kotlin/com/jermey/quo/vadis/compiler/fir/checkers/ScreenValidationChecker.kt`

**Logic**:
1. For each `@Screen(destination = X::class)` function, verify X has `@Destination` annotation
2. Validate function parameter types are within expected set
3. Emit `ORPHAN_SCREEN` warning for screens without matching destinations
4. Emit `SCREEN_INVALID_PARAMS` warning for unexpected parameter types

**Acceptance Criteria**:
- [ ] Screens referencing non-existent destinations produce warning
- [ ] Invalid parameter types produce warning
- [ ] Valid screens pass without diagnostics

---

#### Task 2C.8: Implement structural checkers (`@Stack` sealed, `@Destination` placement)

**Description**: Validate fundamental structural constraints.

**File to create**:
- `quo-vadis-compiler-plugin/src/main/kotlin/com/jermey/quo/vadis/compiler/fir/checkers/StructuralChecker.kt`

**Logic**:
1. `@Stack` must be applied to `sealed class` or `sealed interface` → `STACK_NOT_SEALED`
2. `@Destination` must be on a direct subclass of a `@Stack` class → `DESTINATION_NOT_IN_STACK`

**Acceptance Criteria**:
- [ ] Non-sealed @Stack classes produce error
- [ ] @Destination outside sealed hierarchy produces error
- [ ] Valid structures pass without diagnostics

---

### Sub-phase 2D: Integration & Verification

#### Task 2D.1: Wire FIR extensions into CompilerPluginRegistrar

**Description**: Update `QuoVadisCompilerPluginRegistrar` to register all FIR extensions from Phase 2.

**File to modify**:
- `quo-vadis-compiler-plugin/src/main/kotlin/com/jermey/quo/vadis/compiler/QuoVadisCompilerPluginRegistrar.kt`

**Acceptance Criteria**:
- [ ] `QuoVadisFirExtensionRegistrar` is registered
- [ ] All checkers and generation extensions are active during compilation

---

#### Task 2D.2: Verify IDE integration

**Description**: With the compiler plugin active in `composeApp`, verify IDE behavior.

**Acceptance Criteria**:
- [ ] `{Prefix}NavigationConfig` appears in IDE autocomplete
- [ ] `{Prefix}DeepLinkHandler` appears in IDE autocomplete
- [ ] Type-checking `val config: NavigationConfig = MyAppNavigationConfig` succeeds
- [ ] Members (screenRegistry, etc.) are visible via dot-completion
- [ ] No physical `.kt` files generated in build directories

---

#### Task 2D.3: Verify diagnostic reporting

**Description**: Create test scenarios with intentional annotation errors and verify diagnostics appear.

**Test scenarios**:
1. Two `@Destination` with same route → expect `DUPLICATE_ROUTE` error
2. `@Argument` property not in route → expect `ARGUMENT_ROUTE_MISMATCH` error
3. `@Pane` with no PRIMARY role → expect `MISSING_PRIMARY_PANE` error
4. `@Screen` for non-annotated class → expect `ORPHAN_SCREEN` warning
5. `@Stack` on non-sealed class → expect `STACK_NOT_SEALED` error

**Acceptance Criteria**:
- [ ] All diagnostic scenarios produce expected errors/warnings
- [ ] Diagnostics appear at correct source locations
- [ ] Fix suggestions (if implemented) are actionable

---

## Files Created/Modified Summary

| Action | File |
|--------|------|
| Create | `compiler/fir/QuoVadisFirExtensionRegistrar.kt` |
| Create | `compiler/fir/QuoVadisPredicates.kt` |
| Create | `compiler/fir/AnnotationExtractor.kt` |
| Create | `compiler/fir/QuoVadisDeclarationGenerationExtension.kt` |
| Create | `compiler/QuoVadisGeneratedKey.kt` |
| Create | `compiler/fir/checkers/QuoVadisAdditionalCheckersExtension.kt` |
| Create | `compiler/fir/checkers/QuoVadisDiagnostics.kt` |
| Create | `compiler/fir/checkers/RouteCollisionChecker.kt` |
| Create | `compiler/fir/checkers/ArgumentParityChecker.kt` |
| Create | `compiler/fir/checkers/ContainerRoleChecker.kt` |
| Create | `compiler/fir/checkers/TransitionCompatibilityChecker.kt` |
| Create | `compiler/fir/checkers/ScreenValidationChecker.kt` |
| Create | `compiler/fir/checkers/StructuralChecker.kt` |
| Modify | `compiler/QuoVadisCompilerPluginRegistrar.kt` |

All files under `quo-vadis-compiler-plugin/src/main/kotlin/com/jermey/quo/vadis/`

## Task Dependency Graph

```
2A.1 (FIR Registrar) ──→ 2B.1 (Top-level ClassIds)
2A.2 (Predicates)    ──→ 2B.2 (NavigationConfig object)
2A.3 (Extractors)    ──→ 2B.3 (DeepLinkHandler object)
                     ──→ 2B.4 (Config member signatures)
2B.6 (Generated Key) ──→ 2B.5 (Handler member signatures)
                              │
                              ↓
2C.1 (Checker Extension) ──→ 2C.2 (Diagnostics defs)
                              │
                    ┌─────────┼─────────┐
                    ↓         ↓         ↓
              2C.3 (Route) 2C.4 (Args) 2C.5 (Roles)
              2C.6 (Trans) 2C.7 (Screen) 2C.8 (Struct)
                    │         │         │
                    └─────────┼─────────┘
                              ↓
                   2D.1 (Wire to Registrar)
                   2D.2 (IDE verification)
                   2D.3 (Diagnostic verification)
```

**Parallelizable**: 2A.1–2A.3 can be done in parallel. 2B.2–2B.5 can be done in parallel. 2C.3–2C.8 can all be done in parallel once 2C.1–2C.2 are complete.

## Open Questions

1. **FIR session lifecycle**: How does the `FirSession` persist across incremental compilations? Need to verify that predicate providers re-collect annotations on incremental changes.

2. **Generated package**: Should the generated package match the KSP output (`com.jermey.quo.vadis.generated`) for migration ease, or use a different package?

3. **IDE K2 mode requirement**: FIR synthetic declarations only appear in IDE when K2 mode is enabled. What is the minimum Android Studio / IntelliJ version required? Document this for users.

4. **Checker ordering**: Do FIR checkers run before or after declaration generation? If before, checkers may not see synthetic declarations. Need to verify execution order.

5. **Compose function detection**: Can FIR-level checkers detect `@Composable` annotation on `@Screen` functions, or is this only visible in IR? This affects `ScreenValidationChecker` scope.
