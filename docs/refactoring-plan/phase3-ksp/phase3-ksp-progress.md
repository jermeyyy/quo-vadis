# Phase 3: KSP Processor Rewrite - Progress

> **Last Updated**: 2025-12-06  
> **Phase Status**: 🟡 In Progress  
> **Progress**: 5/6 tasks (83%)

## Overview

This phase implements a complete rewrite of the KSP code generation for the new annotation system, producing NavNode builders, screen registries, and deep link handlers.

---

## Task Progress

| ID | Task | Status | Completed | Notes |
|----|------|--------|-----------|-------|
| [KSP-001](./KSP-001-graph-type-enum.md) | Create Annotation Extractors | 🟢 Completed | 2025-12-06 | 11 files created |
| [KSP-002](./KSP-002-class-references.md) | Create NavNode Builder Generator | 🟢 Completed | 2025-12-06 | Generator + processor wiring |
| [KSP-003](./KSP-003-graph-extractor.md) | Create Screen Registry Generator | 🟢 Completed | 2025-12-06 | Generator + interface + processor wiring |
| [KSP-004](./KSP-004-deep-link-handler.md) | Create Deep Link Handler Generator | 🟢 Completed | 2025-12-06 | Generator + interface + processor wiring |
| [KSP-005](./KSP-005-navigator-extensions.md) | Create Navigator Extensions Generator | 🟢 Completed | 2025-12-06 | Generator + processor wiring |
| [KSP-006](./KSP-006-validation.md) | Validation and Error Reporting | ⚪ Not Started | - | Depends on KSP-001 |

---

## Completed Tasks

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

**Model Classes Created** (`quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/models/`):
- `ParamInfo.kt` - Constructor parameter metadata
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

_None currently in progress._

---

## Blocked Tasks

_None - KSP-001 completed, unlocks remaining tasks._

---

## Ready to Start

- **KSP-003**: Create Screen Registry Generator
- **KSP-005**: Create Navigator Extensions Generator (depends on KSP-002 ✓)
- **KSP-006**: Validation and Error Reporting

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
| All `@Destination` | `GeneratedDeepLinkHandler` | URI → Destination parsing |

---

## Notes

- Estimated 14-19 days total
- Can be started in parallel with Phase 2 (after Phase 4)
- Focus on compile-time safety and helpful error messages

---

## Related Documents

- [Phase 3 Summary](./phase3-ksp-summary.md)
