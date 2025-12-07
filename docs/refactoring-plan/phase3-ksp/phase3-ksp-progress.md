# Phase 3: KSP Processor Rewrite - Progress

> **Last Updated**: 2025-12-07  
> **Phase Status**: 🚧 In Progress  
> **Progress**: 6/8 tasks (75%)

## Overview

This phase implements a complete rewrite of the KSP code generation for the new annotation system, producing NavNode builders, screen registries, and deep link handlers.

---

## Task Progress

| ID | Task | Status | Completed | Notes |
|----|------|--------|-----------|-------|
| [KSP-001](./KSP-001-graph-type-enum.md) | Create Annotation Extractors (with @Argument) | � Needs Update | 2025-12-06 | ParamInfo enhanced in docs, implementation pending |
| [KSP-002](./KSP-002-class-references.md) | Create NavNode Builder Generator | 🟢 Completed | 2025-12-06 | Generator + processor wiring |
| [KSP-003](./KSP-003-graph-extractor.md) | Create Screen Registry Generator | 🟢 Completed | 2025-12-06 | Generator + interface + processor wiring |
| [KSP-004](./KSP-004-deep-link-handler.md) | Create Deep Link Handler Generator (type conversion) | � Needs Update | 2025-12-06 | Type conversion enhanced in docs, implementation pending |
| [KSP-005](./KSP-005-navigator-extensions.md) | Create Navigator Extensions Generator | 🟢 Completed | 2025-12-06 | Generator + processor wiring |
| [KSP-006](./KSP-006-validation.md) | Validation and Error Reporting | 🟢 Completed | 2025-12-06 | ValidationEngine + processor integration |
| [KSP-007](./KSP-007-remove-legacy-tabgraph.md) | Remove Legacy TabGraphExtractor | 🟢 Completed | 2025-12-06 | 10 legacy files removed, processor cleaned up |
| [KSP-008](./KSP-008-deep-link-handler-imports.md) | Fix Deep Link Handler Generator Imports | 🟢 Completed | 2025-12-07 | Uses KotlinPoet ClassName with %T for auto-imports |

---

## Completed Tasks

### KSP-008: Fix Deep Link Handler Generator Imports (2025-12-07)

Fixed the `DeepLinkHandlerGenerator` to properly import destination classes in generated code.

**Problem**: Generated `GeneratedDeepLinkHandlerImpl.kt` referenced destination classes (e.g., `ProductsDestination.List`) without importing them, causing "Unresolved reference" compilation errors.

**Root Cause**: `buildDestinationClassName()` returned simple class names without package prefixes, and the generated file didn't include imports for these classes.

**Solution**: Used KotlinPoet's `ClassName` with `%T` format specifier for automatic import generation:

**Changes to `DeepLinkHandlerGenerator.kt`**:

1. **`buildDestinationClassName()`** - Changed return type from `String` to `ClassName`:
   ```kotlin
   private fun buildDestinationClassName(dest: DestinationInfo): ClassName {
       val packageName = dest.classDeclaration.packageName.asString()
       return if (dest.parentSealedClass != null) {
           ClassName(packageName, dest.parentSealedClass, dest.className)
       } else {
           ClassName(packageName, dest.className)
       }
   }
   ```

2. **`buildRoutePatternInitializer()`** - Changed from `%L` to `%T` for destination classes:
   ```kotlin
   CodeBlock.of("RoutePattern(%S, emptyList()) { %T }", route, destClassName)
   CodeBlock.of("RoutePattern(%S, listOf(%L)) { params -> %T(%L) }", ...)
   ```

3. **`buildWhenCases()`** - Changed return type from `List<String>` to `List<CodeBlock>`:
   ```kotlin
   CodeBlock.of("%T -> %S", destClassName, "\$scheme://$route")
   CodeBlock.of("is %T -> %P", destClassName, "\$scheme://$uriPath")
   ```

4. **`buildCreateDeepLinkUriFunction()`** - Updated to use `addCode()` instead of `addStatement()`:
   ```kotlin
   whenCases.forEach { caseBlock ->
       addCode(caseBlock)
       addCode("\n")
   }
   ```

**Verification**:
- `:quo-vadis-ksp:build -x detekt` ✓
- `:quo-vadis-ksp:test` ✓
- `:quo-vadis-recipes:compileKotlinDesktop` ✓

**Note**: With this fix, the `@Destination` annotations in `quo-vadis-recipes` deep linking examples can now be uncommented in production code.

**Unblocks**: MIG-006 (Deep Linking Recipe) - annotations can now be enabled in recipes module.

---

### KSP-007: Remove Legacy TabGraphExtractor (2025-12-06)

Removed all legacy KSP extractors, generators, and models that were causing build failures.

**Files Deleted** (`quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/`):
- `TabGraphExtractor.kt` - Legacy @Tab extraction
- `TabGraphInfo.kt` - Legacy tab data model
- `TabGraphGenerator.kt` - Legacy tab code generation
- `GraphInfoExtractor.kt` - Legacy @Graph extraction
- `GraphInfo.kt` - Legacy graph data model
- `GraphGenerator.kt` - Legacy graph code generation
- `GraphBuilderGenerator.kt` - Legacy builder generation
- `RouteConstantsGenerator.kt` - Legacy route constants
- `RouteInitializationGenerator.kt` - Legacy route initialization
- `DestinationExtensionsGenerator.kt` - Legacy destination extensions

**QuoVadisSymbolProcessor.kt Changes**:
- Removed imports: `Content`, `Graph`, `KSFunctionDeclaration`, `KSType`
- Removed fields: `contentMappings`, `allGraphInfos`
- Removed methods: `processContentFunction()`, `processGraphClass()`, `processTabGraphClass()`, `finish()`
- Removed legacy processing passes (first three passes from `process()`)
- Removed `ContentFunctionInfo` data class
- Updated KDoc to reflect new NavNode-based architecture

**Processor Now**:
- First pass: `processNavNodeBuilders()` - NavNode builder generation with validation
- Second pass: `processDeepLinkHandler()` - Deep link handler generation

**Verification**:
- `:quo-vadis-ksp:build -x detekt` ✓
- NoSuchElementException error resolved
- Demo app compilation errors are expected (uses legacy generated code, will be fixed in Phase 5 Migration)

**Note**: Demo app requires migration to new annotations (Phase 5).

---

### KSP-006: Validation and Error Reporting (2025-12-06)

Created comprehensive validation engine that validates annotation usage and reports clear, actionable errors.

**File Created** (`quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/validation/`):
- `ValidationEngine.kt` - Main validation engine (~460 lines)

**Validation Categories**:

| Category | Validation | Severity | Description |
|----------|------------|----------|-------------|
| **Structural** | Orphan Destination | Error | `@Destination` not inside `@Stack`, `@Tab`, or `@Pane` |
| **Structural** | Invalid Start Destination | Error | `@Stack(startDestination)` references non-existent destination |
| **Structural** | Invalid Initial Tab | Error | `@Tab(initialTab)` references non-existent tab |
| **Structural** | Empty Container | Error | `@Stack`, `@Tab`, or `@Pane` with no destinations |
| **Route** | Route Parameter Mismatch | Error | Route param `{name}` has no matching constructor parameter |
| **Route** | Missing Route Parameter | Warning | Constructor param not in route (data classes only) |
| **Route** | Duplicate Routes | Error | Same route pattern on multiple destinations |
| **Reference** | Invalid Root Graph | Error | `@TabItem`/`@PaneItem(rootGraph)` references class without `@Stack` |
| **Reference** | Missing Screen Binding | Warning | Destination has no `@Screen` function |
| **Reference** | Duplicate Screen Binding | Error | Multiple `@Screen` for same destination |
| **Reference** | Invalid Destination Reference | Error | `@Screen(destination)` references non-destination class |
| **Type** | Non-Sealed Container | Error | `@Stack`/`@Tab`/`@Pane` on non-sealed class |
| **Type** | Non-Data Destination | Error | `@Destination` on class that's not data object/class |

**Engine API**:
```kotlin
class ValidationEngine(logger: KSPLogger) {
    fun validate(
        stacks: List<StackInfo>,
        tabs: List<TabInfo>,
        panes: List<PaneInfo>,
        screens: List<ScreenInfo>,
        allDestinations: List<DestinationInfo>,
        resolver: Resolver
    ): Boolean  // true = no errors
}
```

**Error Message Format**:
```
@Stack(startDestination = "Unknown") - No destination named "Unknown" found in HomeDestination. Available destinations: [Feed, Detail]
```

**Processor Integration** (modified `QuoVadisSymbolProcessor.kt`):
- Added `ValidationEngine` import and property
- Refactored `processNavNodeBuilders()` to separate extraction from generation:
  1. Extract all stacks, tabs, panes
  2. Extract screens and all destinations
  3. Run validation
  4. Generate code only if validation passes
- Added helper methods: `extractStackInfo()`, `extractTabInfo()`, `extractPaneInfo()`
- Added `collectAllDestinations()` to gather destinations from all containers
- Added generation methods: `generateStackBuilders()`, `generateTabBuilders()`, `generatePaneBuilders()`
- Screen registry generation now integrated into `processNavNodeBuilders()`

**Verified**: `:quo-vadis-ksp:build -x detekt` ✓

**Note**: Full app build has pre-existing TabGraphExtractor error in legacy code (unrelated to this task).

---

### KSP-005: Create Navigator Extensions Generator (2025-12-06)

Created the navigator extensions generator that provides convenient type-safe navigation methods.

**File Created** (`quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/generators/`):
- `NavigatorExtGenerator.kt` - Main generator class (~300 lines)

**Generator Features**:
- `generate(stacks, tabs, panes, basePackage)` - Main entry point
- `addStackExtensions()` - Generates `to{Destination}()` extensions
- `addTabSwitchingExtensions()` - Generates `switchTo{Tab}Tab()` extensions
- `addPaneSwitchingExtensions()` - Generates `switchTo{Pane}Pane()` extensions

**Generated Code Features**:
- Single file: `{package}.generated/NavigatorExtensions.kt`
- Data object destinations → parameterless extensions
- Data class destinations → extensions with constructor params
- Full KDoc with `@param` documentation
- Uses KotlinPoet for type-safe code generation
- `KSType.toTypeName()` extension for param type conversion

**Generated Extension Examples**:
```kotlin
// Data object destination
fun Navigator.toFeed() = navigate(HomeDestination.Feed)

// Data class destination
fun Navigator.toDetail(id: String) = navigate(HomeDestination.Detail(id))

// Tab switching
fun Navigator.switchToHomeTab() = switchTab(MainTabs.Home)

// Pane switching
fun Navigator.switchToDetailPanePane() = switchPane(MainPanes.DetailPane)
```

**Processor Integration** (modified `QuoVadisSymbolProcessor.kt`):
- Added `navigatorExtGenerator` field
- Added `collectedStacks`, `collectedTabs`, `collectedPanes` lists
- Modified `processStackNavNodeBuilder()` to collect StackInfo
- Modified `processTabNavNodeBuilder()` to collect TabInfo
- Modified `processPaneNavNodeBuilder()` to collect PaneInfo
- Added `generateNavigatorExtensions()` method
- Called at end of `processNavNodeBuilders()` (Step 4)

**Verified**: `:quo-vadis-ksp:build -x detekt` ✓

**Note**: Full app build has pre-existing TabGraphExtractor error (unrelated to this task).

---

### KSP-004: Create Deep Link Handler Generator (2025-12-06)

Created the deep link handler generator that maps URIs to destinations.

**Updated 2025-12-07**: Documentation enhanced with type-safe argument serialization:
- Uses `SerializerType` from `ParamInfo` for type conversion
- Supports primitives (Int, Long, Float, Double, Boolean)
- Supports Enum types via `enumValueOf<T>()`
- Supports `@Serializable` types via kotlinx.serialization JSON
- Implementation pending for ANN-006 completion

**Files Created**:

1. **Core Interface** (`quo-vadis-core/src/commonMain/kotlin/.../navigation/core/`):
   - `GeneratedDeepLinkHandler.kt` - Interface for generated deep link handlers
     - `handleDeepLink(uri: String): DeepLinkResult` - Parse URI and return destination
     - `createDeepLinkUri(destination, scheme): String?` - Generate URI from destination
   - `DeepLinkResult` sealed class with `Matched` and `NotMatched`

2. **Generator** (`quo-vadis-ksp/src/main/kotlin/.../generators/`):
   - `DeepLinkHandlerGenerator.kt` - Main generator class (~320 lines)
     - Generates `GeneratedDeepLinkHandlerImpl.kt` implementing `GeneratedDeepLinkHandler`
     - Generates private `RoutePattern` data class with regex matching
     - `handleDeepLink()` iterates through routes and matches regex patterns
     - `createDeepLinkUri()` with `when` expression generating URIs
     - `extractPath()` helper to strip scheme from URIs
     - Filters destinations to those with non-null routes
     - Logs warning when no routable destinations found

**Route Pattern Matching**:
| Pattern | Example URI | Extracted Params |
|---------|-------------|------------------|
| `home/feed` | `myapp://home/feed` | (none) |
| `home/detail/{id}` | `myapp://home/detail/123` | `id="123"` |
| `user/{userId}/post/{postId}` | `myapp://user/42/post/99` | `userId="42"`, `postId="99"` |

**Generated Code Features**:
- Uses regex for pattern matching with `([^/]+)` capture groups
- Escapes special regex characters in static route parts
- Supports data objects (no params) and data classes (with params)
- Fully qualified destination names for nested sealed class members
- Comprehensive KDoc documentation

**QuoVadisClassNames Additions**:
- `GENERATED_DEEP_LINK_HANDLER` - Reference to `GeneratedDeepLinkHandler` interface
- `DEEP_LINK_RESULT` - Reference to `DeepLinkResult` sealed class

**Processor Integration** (modified `QuoVadisSymbolProcessor.kt`):
- Added `deepLinkHandlerGenerator` field
- Added `processDeepLinkHandler(resolver)` method (sixth pass)
- Uses existing DestinationExtractor from KSP-001
- Filters to @Destination annotations with routes

**Verified**: `:quo-vadis-ksp:build -x detekt` ✓, `:quo-vadis-core:desktopTest` ✓

**Note**: Full app build has pre-existing TabGraphExtractor error (unrelated to this task).

---

### KSP-003: Create Screen Registry Generator (2025-12-06)

Created the screen registry generator that maps destinations to their composable screen functions.

**Files Created**:

1. **Core Interface** (`quo-vadis-core/src/commonMain/kotlin/.../navigation/core/`):
   - `ScreenRegistry.kt` - Interface for screen registries
     - `Content()` - Composable method dispatching to @Screen functions
     - `hasContent()` - Check if destination is registered
     - Supports SharedTransitionScope and AnimatedVisibilityScope

2. **Generator** (`quo-vadis-ksp/src/main/kotlin/.../generators/`):
   - `ScreenRegistryGenerator.kt` - Main generator class (~190 lines)
     - Generates `GeneratedScreenRegistry.kt` implementing `ScreenRegistry`
     - `Content()` with `when` expression dispatching to @Screen functions
     - `hasContent()` returning boolean for all registered destinations
     - Groups destinations by parent class in generated comments
     - Logs warning when no @Screen annotations found

**Three Function Signature Patterns**:
1. **Simple** (Navigator only): `FeedScreen(navigator)`
2. **With destination**: `DetailScreen(destination as DetailDest, navigator)`
3. **With shared scopes**: `GalleryScreen(destination as GalleryDest, navigator, sharedTransitionScope!!, animatedVisibilityScope!!)`

**QuoVadisClassNames Additions**:
- `DESTINATION` - Reference to `Destination` class
- `SCREEN_REGISTRY` - Reference to `ScreenRegistry` interface

**Processor Integration** (modified `QuoVadisSymbolProcessor.kt`):
- Added `screenExtractor` and `screenRegistryGenerator` fields
- Added `processScreenRegistry(resolver)` method (fifth pass)
- Integrated with existing ScreenExtractor from KSP-001

**Verified**: `:quo-vadis-ksp:compileKotlin` ✓, `:quo-vadis-core:compileKotlinDesktop` ✓

**Note**: Full app build has pre-existing TabGraphExtractor error (unrelated to this task).

---

### KSP-002: Create NavNode Builder Generator (2025-12-06)

Created the core generator that transforms extracted annotation metadata into `build{Name}NavNode()` functions.

**File Created** (`quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/generators/`):
- `NavNodeBuilderGenerator.kt` - Main generator class (~350 lines)

**Generator Methods**:
- `generateStackBuilder(stackInfo)` - Generates StackNode builder with start destination
- `generateTabBuilder(tabInfo, stackBuilders)` - Generates TabNode builder referencing stack builders
- `generatePaneBuilder(paneInfo)` - Generates PaneNode builder with pane configurations

**Generated Code Features**:
- Files placed in `{package}.generated` subpackage
- File header: "Generated by Quo Vadis KSP - DO NOT EDIT"
- Full KDoc with `@param` and `@return` documentation
- Default parameters (key, parentKey, initialTabIndex, activePaneRole)
- Uses KotlinPoet for type-safe code generation

**Enum Mapping (Model → Core)**:
- `PaneRole.PRIMARY` → `PaneRole.Primary`
- `PaneRole.SECONDARY` → `PaneRole.Supporting`
- `PaneRole.EXTRA` → `PaneRole.Extra`
- `AdaptStrategy.HIDE/COLLAPSE` → `AdaptStrategy.Hide`
- `AdaptStrategy.OVERLAY` → `AdaptStrategy.Levitate`
- `AdaptStrategy.REFLOW` → `AdaptStrategy.Reflow`

**Processor Integration** (modified `QuoVadisSymbolProcessor.kt`):
- Added extractors and generator as class fields
- Added `stackInfoMap` for tab builder dependencies
- New `processNavNodeBuilders(resolver)` method
- Ordered processing: Stack → Tab → Pane
- Proper error handling with `IllegalStateException`

**Verified**: `:quo-vadis-ksp:compileKotlin` ✓, `:quo-vadis-ksp:build -x detekt` ✓

**Note**: Pre-existing detekt issues in extractor files (from KSP-001) remain - out of scope.

---

### KSP-001: Create Annotation Extractors (2025-12-06)

Created the extraction layer for parsing annotations into strongly-typed intermediate models.

**Updated 2025-12-07**: Enhanced `ParamInfo` with `@Argument` annotation support:
- `isArgument: Boolean` - whether parameter has @Argument annotation
- `argumentKey: String` - custom key for URL serialization
- `isOptionalArgument: Boolean` - whether argument is optional in deep links
- `serializerType: SerializerType` - type conversion strategy (STRING, INT, LONG, BOOLEAN, ENUM, JSON)

**Model Classes Created** (`quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/models/`):
- `ParamInfo.kt` - Constructor parameter metadata (enhanced with @Argument fields)
- `DestinationInfo.kt` - @Destination annotation metadata
- `StackInfo.kt` - @Stack annotation metadata
- `TabInfo.kt` - @Tab/@TabItem annotation metadata
- `PaneInfo.kt` - @Pane/@PaneItem metadata + enums (PaneRole, AdaptStrategy, PaneBackBehavior)
- `ScreenInfo.kt` - @Screen annotation metadata

**Extractor Classes Created** (`quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/extractors/`):
- `DestinationExtractor.kt` - Extracts @Destination, route params, constructor params
- `StackExtractor.kt` - Extracts @Stack with start destination resolution
- `TabExtractor.kt` - Extracts @Tab/@TabItem with rootGraph resolution
- `PaneExtractor.kt` - Extracts @Pane/@PaneItem with role/strategy parsing
- `ScreenExtractor.kt` - Extracts @Screen with scope parameter detection

**Bug Fix**: Fixed pre-existing compilation error in `QuoVadisSymbolProcessor.kt` - changed `TabGraph` import to `Tab`.

**Verified**: `:quo-vadis-ksp:compileKotlin` ✓

---

## In Progress Tasks

### Pending Implementation Updates

**KSP-001**: Create Annotation Extractors (with @Argument)
- **Status**: Documentation updated, implementation pending
- **Depends on**: ANN-006 completion
- **Changes needed**: 
  - Update `ParamInfo.kt` with new fields (isArgument, argumentKey, isOptionalArgument, serializerType)
  - Create `SerializerType.kt` enum
  - Update `DestinationExtractor.kt` to extract @Argument annotations
  - Add serializer type detection logic

**KSP-004**: Create Deep Link Handler Generator (type conversion)
- **Status**: Documentation updated, implementation pending
- **Depends on**: ANN-006 and KSP-001 updates
- **Changes needed**:
  - Update `DeepLinkHandlerGenerator.kt` to use SerializerType
  - Add type conversion code generation (toInt(), toLong(), enumValueOf, JSON)
  - Add query parameter extraction support
  - Update RoutePattern to handle typed parameters

---

## Blocked Tasks

_None - KSP-001 completed, unlocks remaining tasks._

---

## Ready to Start

_All remaining tasks completed. Waiting for ANN-006 to enable KSP-001 and KSP-004 implementation updates._

---

## Dependencies

```
Phase 4 (Annotations) ─► KSP-001 ─┬─► KSP-002 ─┬─► KSP-004
                                  │            └─► KSP-005
                                  │
                                  ├─► KSP-003 ───► KSP-004
                                  │
                                  └─► KSP-006
```

---

## Generated Artifacts

| Input | Output | Purpose |
|-------|--------|---------|
| `@Stack` class | `build{Name}NavNode()` | Initial StackNode tree |
| `@Tab` class | `build{Name}NavNode()` | Initial TabNode tree |
| `@Pane` class | `build{Name}NavNode()` | Initial PaneNode tree |
| All `@Screen` | `GeneratedScreenRegistry` | Destination → Composable mapping |
| All `@Destination` | `GeneratedDeepLinkHandler` | URI → Destination parsing with type conversion |
| `@Argument` params | Type converters | Primitive/Enum/JSON serialization (pending ANN-006) |

---

## Notes

- Estimated 18-23 days total (updated with @Argument support)
- Can be started in parallel with Phase 2 (after Phase 4)
- Focus on compile-time safety and helpful error messages
- KSP-001 and KSP-004 ready for @Argument implementation once ANN-006 completed

---

## Related Documents

- [Phase 3 Summary](./phase3-ksp-summary.md)
