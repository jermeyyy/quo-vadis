# Quo Vadis Core Module: Package Structure & API Refactoring Plan

## Executive Summary

This document outlines a comprehensive refactoring plan for the `quo-vadis-core` module to improve package organization, API visibility, and overall code structure. The goal is to align with SOLID principles, Kotlin idioms, and prepare the codebase for future growth.

**Key Objectives:**
1. Separate **public APIs** from **internal implementations**
2. Organize code by **domain** (navigation core, compose rendering, DSL/registry)
3. Ensure **compose-free navigation core** (pure Kotlin navigation logic)
4. Improve **discoverability** for library consumers
5. Reduce **cognitive load** through clear boundaries

**Breaking Changes:** Acceptable for this refactoring.

---

## Table of Contents

1. [Current State Analysis](#current-state-analysis)
2. [Identified Issues](#identified-issues)
3. [Proposed Package Structure](#proposed-package-structure)
4. [API Visibility Classification](#api-visibility-classification)
5. [Domain Separation Strategy](#domain-separation-strategy)
6. [Migration Phases](#migration-phases)
7. [Implementation Checklist](#implementation-checklist)
8. [File-by-File Migration Guide](#file-by-file-migration-guide)

---

## Current State Analysis

### Current Package Structure

```
com.jermey.quo.vadis.core/
├── InternalQuoVadisApi.kt
├── compose/
│   ├── NavigationHost.kt
│   ├── platformDefaultPredictiveBack.kt
│   ├── animation/
│   │   ├── AnimationCoordinator.kt
│   │   ├── BackAnimationController.kt
│   │   ├── NavTransition.kt
│   │   ├── SharedElementModifiers.kt
│   │   └── TransitionScope.kt
│   ├── navback/
│   │   ├── BackNavigationEvent.kt
│   │   ├── CascadeBackState.kt
│   │   ├── NavBackHandler.kt
│   │   ├── NavigateBackHandler.kt
│   │   ├── PlatformBackInput.kt
│   │   ├── PredictiveBackController.kt
│   │   └── ScreenNavigationInfo.kt
│   ├── render/
│   │   ├── AnimatedNavContent.kt
│   │   ├── ComposableCache.kt
│   │   ├── CompositionLocals.kt
│   │   ├── NavRenderScope.kt
│   │   ├── NavTreeRenderer.kt
│   │   ├── PaneRenderer.kt
│   │   ├── PredictiveBackContent.kt
│   │   ├── ScreenRenderer.kt
│   │   ├── StackRenderer.kt
│   │   ├── StaticAnimatedVisibilityScope.kt
│   │   └── TabRenderer.kt
│   └── wrapper/
│       ├── PaneContainerScope.kt
│       ├── TabsContainerScope.kt
│       └── WindowSizeClass.kt
├── dsl/
│   ├── BuilderDataClasses.kt
│   ├── ContainerBuilder.kt
│   ├── DslContainerRegistry.kt
│   ├── DslNavigationConfig.kt
│   ├── DslScreenRegistry.kt
│   ├── DslScopeRegistry.kt
│   ├── DslTransitionRegistry.kt
│   ├── NavigationConfigBuilder.kt
│   ├── NavigationConfigDsl.kt
│   ├── PanesBuilder.kt
│   ├── StackBuilder.kt
│   └── TabsBuilder.kt
│   └── registry/
│       ├── BackHandlerRegistry.kt
│       ├── CompositeContainerRegistry.kt
│       ├── CompositeDeepLinkRegistry.kt
│       ├── CompositeScreenRegistry.kt
│       ├── CompositeScopeRegistry.kt
│       ├── CompositeTransitionRegistry.kt
│       ├── ContainerRegistry.kt
│       ├── DeepLinkRegistry.kt
│       ├── PaneRoleRegistry.kt
│       ├── RouteRegistry.kt
│       ├── RuntimeDeepLinkRegistry.kt
│       ├── ScopeRegistry.kt
│       ├── ScreenRegistry.kt
│       └── TransitionRegistry.kt
└── navigation/
    ├── BackPressHandler.kt
    ├── DeepLink.kt
    ├── DeepLinkResult.kt
    ├── GeneratedTabMetadata.kt
    ├── LifecycleAwareNode.kt
    ├── NavDestination.kt
    ├── NavigationResultManager.kt
    ├── NavigationTransition.kt
    ├── Navigator.kt
    ├── NavigatorResultExtensions.kt
    ├── NavKeyGenerator.kt
    ├── NavNode.kt
    ├── PaneNavigator.kt
    ├── PaneNode.kt
    ├── ResultCapable.kt
    ├── ReturnsResult.kt
    ├── ScreenNode.kt
    ├── StackNode.kt
    ├── TabNode.kt
    ├── TransitionController.kt
    ├── TransitionState.kt
    ├── config/
    │   ├── CompositeNavigationConfig.kt
    │   ├── EmptyNavigationConfig.kt
    │   └── NavigationConfig.kt
    ├── pane/
    │   ├── AdaptStrategy.kt
    │   ├── PaneBackBehavior.kt
    │   ├── PaneConfiguration.kt
    │   └── PaneRole.kt
    └── tree/
        ├── TreeMutator.kt
        ├── TreeNavigator.kt
        ├── config/PopBehavior.kt
        ├── operations/
        │   ├── BackOperations.kt
        │   ├── PaneOperations.kt
        │   ├── PopOperations.kt
        │   ├── PushOperations.kt
        │   ├── TabOperations.kt
        │   └── TreeNodeOperations.kt
        ├── result/
        │   ├── BackResult.kt
        │   ├── PopResult.kt
        │   └── PushStrategy.kt
        └── util/KeyGenerator.kt
```

### File Count Summary

| Package | Files | ~Lines |
|---------|-------|--------|
| `navigation/` | 25+ files | ~3,000 |
| `navigation/tree/` | 15+ files | ~2,000 |
| `compose/` | 20+ files | ~2,500 |
| `dsl/` | 25+ files | ~1,500 |
| **Total** | ~85 files | ~9,000 |

---

## Identified Issues

### Issue 1: Mixed API Visibility

**Problem:** Many internal implementation classes are publicly accessible without proper markers.

**Examples:**
- `AnimationCoordinator` - NavigationHost implementation detail, but public
- `PredictiveBackController` - Internal gesture handling, but public
- `ComposableCache` - Rendering optimization detail, but public
- Tree operations (`PushOperations`, etc.) - Implementation details, but public

### Issue 2: Compose Dependencies in Navigation Package

**Problem:** `NavigationTransition.kt` in `navigation/` contains Compose-specific code.

**Affected Code:**
- `TransitionBuilder` class with `AnimatedContentTransitionScope`
- `sharedElement()` extension function
- `sharedBounds()` extension function
- `SharedElementConfig`, `SharedElementType` classes

**Impact:** Navigation layer cannot be used independently of Compose.

### Issue 3: Misplaced Domain Components

**Problem:** Some components are placed in wrong domain packages.

**Examples:**
- `BackHandlerRegistry` in `dsl/registry/` - Should be in `compose/navback/`
- `WindowSizeClass` in `compose/wrapper/` - Could be more generic
- `NavigationResultManager` mixed with core navigation - Should be in `result/` subpackage

### Issue 4: Flat Package Structure in Some Areas

**Problem:** Some packages have too many files at the same level.

**Examples:**
- `navigation/` has 20+ files directly
- `dsl/registry/` has 15+ files directly

### Issue 5: Inconsistent Internal Marking

**Problem:** `@InternalQuoVadisApi` annotation exists but is underutilized.

**Currently Marked:**
- `TransitionController`
- `ResultCapable`

**Should Also Be Marked:**
- All tree operation objects
- Composite registry implementations
- Render internals
- Animation coordination classes

---

## Proposed Package Structure

### New Structure

```
com.jermey.quo.vadis.core/
│
├── InternalQuoVadisApi.kt              # Opt-in annotation
│
├── navigation/                          # 🔵 DOMAIN: Pure navigation logic
│   │
│   ├── node/                           # NavNode types (PUBLIC)
│   │   ├── NavNode.kt                  # Sealed interface
│   │   ├── ScreenNode.kt
│   │   ├── StackNode.kt
│   │   ├── TabNode.kt
│   │   └── PaneNode.kt
│   │
│   ├── destination/                    # Destination types (PUBLIC)
│   │   ├── NavDestination.kt
│   │   ├── DeepLink.kt
│   │   └── DeepLinkResult.kt
│   │
│   ├── navigator/                      # Navigator API (PUBLIC)
│   │   ├── Navigator.kt                # Main interface
│   │   ├── PaneNavigator.kt            # Pane extension
│   │   ├── BackPressHandler.kt         # Back handling contract
│   │   └── LifecycleAwareNode.kt       # Lifecycle callbacks
│   │
│   ├── result/                         # Navigation results (PUBLIC)
│   │   ├── ReturnsResult.kt
│   │   └── NavigatorResultExtensions.kt
│   │
│   ├── transition/                     # Transition definitions (PUBLIC)
│   │   ├── NavigationTransition.kt     # Interface only (no Compose)
│   │   └── TransitionState.kt
│   │
│   ├── pane/                           # Pane configuration (PUBLIC)
│   │   ├── PaneRole.kt
│   │   ├── PaneBackBehavior.kt
│   │   ├── AdaptStrategy.kt
│   │   └── PaneConfiguration.kt
│   │
│   ├── config/                         # Configuration (PUBLIC)
│   │   └── NavigationConfig.kt
│   │
│   └── internal/                       # Internal implementation (INTERNAL)
│       ├── TransitionController.kt     # @InternalQuoVadisApi
│       ├── ResultCapable.kt            # @InternalQuoVadisApi
│       ├── NavigationResultManager.kt  # @InternalQuoVadisApi
│       ├── NavKeyGenerator.kt          # @InternalQuoVadisApi
│       ├── GeneratedTabMetadata.kt     # @InternalQuoVadisApi (KSP)
│       │
│       ├── tree/                       # Tree operations (INTERNAL)
│       │   ├── TreeNavigator.kt        # Navigator implementation
│       │   ├── TreeMutator.kt          # Operations facade
│       │   ├── KeyGenerator.kt
│       │   ├── PopBehavior.kt
│       │   ├── PopResult.kt
│       │   ├── BackResult.kt
│       │   └── operations/
│       │       ├── TreeNodeOperations.kt
│       │       ├── PushOperations.kt
│       │       ├── PopOperations.kt
│       │       ├── TabOperations.kt
│       │       ├── PaneOperations.kt
│       │       └── BackOperations.kt
│       │
│       └── config/                     # Config implementations (INTERNAL)
│           ├── CompositeNavigationConfig.kt
│           └── EmptyNavigationConfig.kt
│
├── compose/                            # 🟢 DOMAIN: Compose rendering
│   │
│   ├── NavigationHost.kt               # Main entry point (PUBLIC)
│   │
│   ├── transition/                     # Compose transitions (PUBLIC)
│   │   ├── NavTransition.kt            # Transition data class
│   │   ├── TransitionBuilder.kt        # Builder with Compose deps
│   │   └── TransitionScope.kt          # Shared element scope
│   │
│   ├── animation/                      # Shared elements (PUBLIC)
│   │   └── SharedElementModifiers.kt   # quoVadisSharedElement()
│   │
│   ├── navback/                        # Back handling (PUBLIC)
│   │   ├── NavBackHandler.kt           # Consumer-facing composables
│   │   └── ConsumingNavBackHandler.kt  # Back consumption
│   │
│   ├── scope/                          # Render scopes (PUBLIC)
│   │   ├── NavRenderScope.kt           # Render context interface
│   │   ├── TabsContainerScope.kt       # Tab wrapper scope
│   │   ├── PaneContainerScope.kt       # Pane wrapper scope
│   │   └── CompositionLocals.kt        # LocalNavigator, LocalScreenNode, etc.
│   │
│   ├── util/                           # Utilities (PUBLIC)
│   │   └── WindowSizeClass.kt          # Size class utilities
│   │
│   └── internal/                       # Internal implementation (INTERNAL)
│       ├── AnimationCoordinator.kt     # @InternalQuoVadisApi
│       ├── BackAnimationController.kt  # @InternalQuoVadisApi
│       ├── PredictiveBackController.kt # @InternalQuoVadisApi
│       ├── ComposableCache.kt          # @InternalQuoVadisApi
│       │
│       ├── navback/                    # Back handling internals
│       │   ├── BackNavigationEvent.kt
│       │   ├── CascadeBackState.kt
│       │   ├── NavigateBackHandler.kt
│       │   ├── PlatformBackInput.kt
│       │   └── ScreenNavigationInfo.kt
│       │
│       ├── render/                     # Tree renderers
│       │   ├── NavTreeRenderer.kt
│       │   ├── ScreenRenderer.kt
│       │   ├── StackRenderer.kt
│       │   ├── TabRenderer.kt
│       │   ├── PaneRenderer.kt
│       │   ├── AnimatedNavContent.kt
│       │   ├── PredictiveBackContent.kt
│       │   └── StaticAnimatedVisibilityScope.kt
│       │
│       └── scope/                      # Scope implementations
│           ├── TabsContainerScopeImpl.kt
│           └── PaneContainerScopeImpl.kt
│
├── registry/                           # 🟡 DOMAIN: Registry interfaces
│   │
│   ├── ScreenRegistry.kt               # Screen content (PUBLIC)
│   ├── ContainerRegistry.kt            # Container wrappers (PUBLIC)
│   ├── TransitionRegistry.kt           # Transitions (PUBLIC)
│   ├── ScopeRegistry.kt                # Scoped navigation (PUBLIC)
│   ├── DeepLinkRegistry.kt             # Deep links (PUBLIC)
│   ├── PaneRoleRegistry.kt             # Pane roles (PUBLIC)
│   ├── RouteRegistry.kt                # Route mapping (PUBLIC - KSP)
│   ├── BackHandlerRegistry.kt          # Back handlers (PUBLIC)
│   │
│   └── internal/                       # Registry implementations (INTERNAL)
│       ├── CompositeScreenRegistry.kt
│       ├── CompositeContainerRegistry.kt
│       ├── CompositeTransitionRegistry.kt
│       ├── CompositeScopeRegistry.kt
│       ├── CompositeDeepLinkRegistry.kt
│       └── RuntimeDeepLinkRegistry.kt
│
└── dsl/                                # 🟠 DOMAIN: Configuration DSL
    │
    ├── NavigationConfigDsl.kt          # navigationConfig {} (PUBLIC)
    ├── NavigationConfigBuilder.kt      # Main builder (PUBLIC)
    ├── StackBuilder.kt                 # stack {} (PUBLIC)
    ├── TabsBuilder.kt                  # tabs {} (PUBLIC)
    ├── PanesBuilder.kt                 # panes {} (PUBLIC)
    ├── ContainerBuilder.kt             # Container config (PUBLIC)
    │
    └── internal/                       # DSL implementations (INTERNAL)
        ├── DslNavigationConfig.kt
        ├── DslScreenRegistry.kt
        ├── DslContainerRegistry.kt
        ├── DslTransitionRegistry.kt
        ├── DslScopeRegistry.kt
        └── BuilderDataClasses.kt
```

### Visual Package Hierarchy

```
┌────────────────────────────────────────────────────────────────────┐
│                        quo-vadis-core                              │
├────────────────────────────────────────────────────────────────────┤
│                                                                    │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐   │
│  │   navigation/   │  │    compose/     │  │    registry/    │   │
│  │  (Pure Kotlin)  │  │ (Compose deps)  │  │  (Interfaces)   │   │
│  ├─────────────────┤  ├─────────────────┤  ├─────────────────┤   │
│  │ PUBLIC:         │  │ PUBLIC:         │  │ PUBLIC:         │   │
│  │ • NavNode types │  │ • NavigationHost│  │ • ScreenRegistry│   │
│  │ • Navigator     │  │ • NavTransition │  │ • ContainerReg. │   │
│  │ • NavDestination│  │ • SharedElement │  │ • TransitionReg.│   │
│  │ • DeepLink      │  │ • NavBackHandler│  │ • ScopeRegistry │   │
│  │ • PaneConfig    │  │ • RenderScope   │  │ • DeepLinkReg.  │   │
│  │ • Lifecycle     │  │ • ContainerScope│  │                 │   │
│  ├─────────────────┤  ├─────────────────┤  ├─────────────────┤   │
│  │ INTERNAL:       │  │ INTERNAL:       │  │ INTERNAL:       │   │
│  │ • TreeNavigator │  │ • Renderers     │  │ • Composite*    │   │
│  │ • TreeMutator   │  │ • Animation     │  │ • Runtime*      │   │
│  │ • ResultManager │  │ • PredictiveBack│  │                 │   │
│  │ • Operations    │  │ • Cache         │  │                 │   │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘   │
│                                                                    │
│  ┌───────────────────────────────────────────────────────────┐   │
│  │                          dsl/                             │   │
│  │                    (Configuration DSL)                    │   │
│  ├───────────────────────────────────────────────────────────┤   │
│  │ PUBLIC:  navigationConfig {}, StackBuilder, TabsBuilder   │   │
│  │ INTERNAL: Dsl*Registry implementations                    │   │
│  └───────────────────────────────────────────────────────────┘   │
│                                                                    │
└────────────────────────────────────────────────────────────────────┘
```

---

## API Visibility Classification

### Tier 1: PUBLIC API (Stable, documented)

These APIs are intended for library consumers:

| Package | Class/Interface | Description |
|---------|-----------------|-------------|
| `navigation.node` | `NavNode`, `ScreenNode`, `StackNode`, `TabNode`, `PaneNode` | Core node types |
| `navigation.destination` | `NavDestination`, `DeepLink`, `DeepLinkResult` | Destination types |
| `navigation.navigator` | `Navigator`, `PaneNavigator`, `BackPressHandler` | Navigator interfaces |
| `navigation.result` | `ReturnsResult`, result extensions | Result handling |
| `navigation.transition` | `NavigationTransition`, `TransitionState` | Transition definition |
| `navigation.pane` | `PaneRole`, `PaneBackBehavior`, `PaneConfiguration`, `AdaptStrategy` | Pane config |
| `navigation.config` | `NavigationConfig` | Configuration interface |
| `compose` | `NavigationHost` | Main entry point |
| `compose.transition` | `NavTransition`, `TransitionBuilder`, `TransitionScope` | Compose transitions |
| `compose.animation` | `quoVadisSharedElement()`, `quoVadisSharedBounds()` | Shared elements |
| `compose.navback` | `NavBackHandler`, `ConsumingNavBackHandler` | Back handling |
| `compose.scope` | `NavRenderScope`, `TabsContainerScope`, `PaneContainerScope` | Scopes |
| `compose.scope` | `LocalNavigator`, `LocalScreenNode`, `LocalContainerNode` | CompositionLocals |
| `compose.util` | `WindowSizeClass` | Window utilities |
| `registry` | All registry interfaces | Extensibility points |
| `dsl` | `navigationConfig {}`, all builders | Configuration DSL |

### Tier 2: FRAMEWORK API (@InternalQuoVadisApi)

For KSP-generated code and advanced extension:

| Package | Class/Interface | Consumer |
|---------|-----------------|----------|
| `navigation.internal` | `GeneratedTabMetadata` | KSP-generated containers |
| `navigation.internal` | `NavKeyGenerator` | TreeNavigator |
| `navigation.internal.tree` | `TreeNavigator` | Navigator implementation |
| `navigation.internal.tree` | `TreeMutator`, `PopResult`, `BackResult` | Advanced state manipulation |
| `compose.internal` | `ComposableCache` | NavigationHost |
| `compose.internal` | `AnimationCoordinator` | NavigationHost |
| `compose.internal` | `PredictiveBackController` | NavigationHost |
| `registry` | `RouteRegistry` | KSP-generated code |

### Tier 3: INTERNAL (Not part of API)

Implementation details, should be `internal`:

| Package | Class/Interface | Notes |
|---------|-----------------|-------|
| `navigation.internal` | `TransitionController`, `ResultCapable` | Already marked |
| `navigation.internal` | `NavigationResultManager` | Result plumbing |
| `navigation.internal.tree.operations` | All operation objects | TreeMutator delegates |
| `navigation.internal.config` | `CompositeNavigationConfig`, `EmptyNavigationConfig` | Config implementations |
| `compose.internal.render` | All renderers | Rendering implementation |
| `compose.internal.navback` | All internal back handling | Gesture implementation |
| `compose.internal.scope` | Scope implementations | Container scope impls |
| `registry.internal` | All Composite* and Runtime* | Registry implementations |
| `dsl.internal` | All Dsl* classes | DSL implementations |

---

## Domain Separation Strategy

### Domain 1: Navigation Core (`navigation/`)

**Responsibility:** Pure Kotlin navigation state model and operations.

**Compose-Free Rule:** This package MUST NOT have any Compose dependencies.

**Sub-domains:**

| Sub-package | Contents | Access |
|-------------|----------|--------|
| `node/` | NavNode sealed hierarchy | Public |
| `destination/` | NavDestination, DeepLink | Public |
| `navigator/` | Navigator interface, PaneNavigator | Public |
| `result/` | ReturnsResult, result extensions | Public |
| `transition/` | NavigationTransition (interface only) | Public |
| `pane/` | Pane configuration enums/classes | Public |
| `config/` | NavigationConfig interface | Public |
| `internal/` | Implementation details | Internal |
| `internal/tree/` | TreeNavigator, TreeMutator, operations | Internal |
| `internal/config/` | Config implementations | Internal |

### Domain 2: Compose Rendering (`compose/`)

**Responsibility:** Rendering NavNode tree to Compose UI.

**Compose Dependency:** This package has full Compose dependencies.

**Sub-domains:**

| Sub-package | Contents | Access |
|-------------|----------|--------|
| Root | `NavigationHost.kt` | Public |
| `transition/` | NavTransition, TransitionBuilder | Public |
| `animation/` | SharedElement modifiers | Public |
| `navback/` | Consumer-facing back handlers | Public |
| `scope/` | NavRenderScope, container scopes, locals | Public |
| `util/` | WindowSizeClass | Public |
| `internal/` | Animation, cache, controllers | Internal |
| `internal/render/` | All tree renderers | Internal |
| `internal/navback/` | Back handling implementation | Internal |
| `internal/scope/` | Scope implementations | Internal |

### Domain 3: Registry System (`registry/`)

**Responsibility:** Extensibility interfaces for KSP-generated and manual registration.

**Sub-domains:**

| Sub-package | Contents | Access |
|-------------|----------|--------|
| Root | All registry interfaces | Public |
| `internal/` | Composite* and Runtime* implementations | Internal |

### Domain 4: DSL Configuration (`dsl/`)

**Responsibility:** Fluent DSL for manual navigation configuration.

**Sub-domains:**

| Sub-package | Contents | Access |
|-------------|----------|--------|
| Root | `navigationConfig {}`, builders | Public |
| `internal/` | Dsl* implementations | Internal |

---

## Migration Phases

### Phase 1: Prepare Foundation (Estimated: 2-3 hours)

**Goals:**
- Create new package directories
- Update `@InternalQuoVadisApi` annotation usage
- No file moves yet

**Tasks:**
1. Add `@InternalQuoVadisApi` to classes that should be internal:
   - `AnimationCoordinator`
   - `ComposableCache`
   - `PredictiveBackController`
   - `BackAnimationController`
   - `NavigationResultManager`
   - `NavKeyGenerator`
   - All operation objects in `tree/operations/`
   - All Composite* registries
   - All Dsl* implementations

2. Mark existing internal implementations:
   - Ensure `internal` visibility on implementations
   - Add KDoc explaining internal status

### Phase 2: Split NavigationTransition (Estimated: 2-3 hours)

**Goals:**
- Remove Compose dependencies from `navigation/` package
- Establish clean domain boundary

**Tasks:**
1. Create `navigation/transition/NavigationTransition.kt`:
   - Keep only the interface/data class definition
   - Remove `TransitionBuilder`, shared element functions

2. Create `compose/transition/TransitionBuilder.kt`:
   - Move `TransitionBuilder` class
   - Move `customTransition()` function

3. Create `compose/transition/SharedElementTransitions.kt`:
   - Move `sharedElement()` extension
   - Move `sharedBounds()` extension
   - Move `SharedElementConfig`, `SharedElementType`

4. Update imports throughout codebase

### Phase 3: Reorganize Navigation Package (Estimated: 3-4 hours)

**Goals:**
- Create sub-packages for logical grouping
- Move files to correct locations

**File Moves:**

| Current Location | New Location |
|------------------|--------------|
| `navigation/NavNode.kt` | `navigation/node/NavNode.kt` |
| `navigation/ScreenNode.kt` | `navigation/node/ScreenNode.kt` |
| `navigation/StackNode.kt` | `navigation/node/StackNode.kt` |
| `navigation/TabNode.kt` | `navigation/node/TabNode.kt` |
| `navigation/PaneNode.kt` | `navigation/node/PaneNode.kt` |
| `navigation/NavDestination.kt` | `navigation/destination/NavDestination.kt` |
| `navigation/DeepLink.kt` | `navigation/destination/DeepLink.kt` |
| `navigation/DeepLinkResult.kt` | `navigation/destination/DeepLinkResult.kt` |
| `navigation/Navigator.kt` | `navigation/navigator/Navigator.kt` |
| `navigation/PaneNavigator.kt` | `navigation/navigator/PaneNavigator.kt` |
| `navigation/BackPressHandler.kt` | `navigation/navigator/BackPressHandler.kt` |
| `navigation/LifecycleAwareNode.kt` | `navigation/navigator/LifecycleAwareNode.kt` |
| `navigation/ReturnsResult.kt` | `navigation/result/ReturnsResult.kt` |
| `navigation/NavigatorResultExtensions.kt` | `navigation/result/NavigatorResultExtensions.kt` |
| `navigation/NavigationTransition.kt` | `navigation/transition/NavigationTransition.kt` (cleaned) |
| `navigation/TransitionState.kt` | `navigation/transition/TransitionState.kt` |
| `navigation/TransitionController.kt` | `navigation/internal/TransitionController.kt` |
| `navigation/ResultCapable.kt` | `navigation/internal/ResultCapable.kt` |
| `navigation/NavigationResultManager.kt` | `navigation/internal/NavigationResultManager.kt` |
| `navigation/NavKeyGenerator.kt` | `navigation/internal/NavKeyGenerator.kt` |
| `navigation/GeneratedTabMetadata.kt` | `navigation/internal/GeneratedTabMetadata.kt` |
| `navigation/tree/*` | `navigation/internal/tree/*` |
| `navigation/config/CompositeNavigationConfig.kt` | `navigation/internal/config/CompositeNavigationConfig.kt` |
| `navigation/config/EmptyNavigationConfig.kt` | `navigation/internal/config/EmptyNavigationConfig.kt` |

### Phase 4: Reorganize Compose Package (Estimated: 3-4 hours)

**Goals:**
- Consolidate transition-related code
- Separate internal renderers from public API
- Move back handler registry

**File Moves:**

| Current Location | New Location |
|------------------|--------------|
| `compose/animation/NavTransition.kt` | `compose/transition/NavTransition.kt` |
| `compose/animation/TransitionScope.kt` | `compose/transition/TransitionScope.kt` |
| NEW | `compose/transition/TransitionBuilder.kt` (from Phase 2) |
| `compose/animation/SharedElementModifiers.kt` | `compose/animation/SharedElementModifiers.kt` (keep) |
| `compose/navback/NavBackHandler.kt` | `compose/navback/NavBackHandler.kt` (keep public) |
| `compose/navback/*` (rest) | `compose/internal/navback/*` |
| `dsl/registry/BackHandlerRegistry.kt` | `registry/BackHandlerRegistry.kt` |
| `compose/render/CompositionLocals.kt` | `compose/scope/CompositionLocals.kt` |
| `compose/render/NavRenderScope.kt` | `compose/scope/NavRenderScope.kt` |
| `compose/wrapper/TabsContainerScope.kt` | `compose/scope/TabsContainerScope.kt` |
| `compose/wrapper/PaneContainerScope.kt` | `compose/scope/PaneContainerScope.kt` |
| `compose/wrapper/WindowSizeClass.kt` | `compose/util/WindowSizeClass.kt` |
| `compose/render/*` (rest) | `compose/internal/render/*` |
| `compose/animation/AnimationCoordinator.kt` | `compose/internal/AnimationCoordinator.kt` |
| `compose/animation/BackAnimationController.kt` | `compose/internal/BackAnimationController.kt` |
| NEW | `compose/internal/scope/TabsContainerScopeImpl.kt` (extracted) |
| NEW | `compose/internal/scope/PaneContainerScopeImpl.kt` (extracted) |

### Phase 5: Reorganize Registry and DSL (Estimated: 2-3 hours)

**Goals:**
- Move registry interfaces to top-level package
- Consolidate DSL implementations

**File Moves:**

| Current Location | New Location |
|------------------|--------------|
| `dsl/registry/ScreenRegistry.kt` | `registry/ScreenRegistry.kt` |
| `dsl/registry/ContainerRegistry.kt` | `registry/ContainerRegistry.kt` |
| `dsl/registry/TransitionRegistry.kt` | `registry/TransitionRegistry.kt` |
| `dsl/registry/ScopeRegistry.kt` | `registry/ScopeRegistry.kt` |
| `dsl/registry/DeepLinkRegistry.kt` | `registry/DeepLinkRegistry.kt` |
| `dsl/registry/PaneRoleRegistry.kt` | `registry/PaneRoleRegistry.kt` |
| `dsl/registry/RouteRegistry.kt` | `registry/RouteRegistry.kt` |
| `dsl/registry/BackHandlerRegistry.kt` | `registry/BackHandlerRegistry.kt` |
| `dsl/registry/Composite*.kt` | `registry/internal/Composite*.kt` |
| `dsl/registry/Runtime*.kt` | `registry/internal/Runtime*.kt` |
| `dsl/Dsl*.kt` | `dsl/internal/Dsl*.kt` |
| `dsl/BuilderDataClasses.kt` | `dsl/internal/BuilderDataClasses.kt` |

### Phase 6: Update Imports and Tests (Estimated: 3-4 hours)

**Goals:**
- Update all import statements
- Ensure tests still pass
- Update KDoc references

**Tasks:**
1. Run IDE "Optimize Imports" across project
2. Fix compilation errors from moved classes
3. Update test imports
4. Run full test suite
5. Update KDoc cross-references

### Phase 7: Documentation and Cleanup (Estimated: 2 hours)

**Goals:**
- Update architecture documentation
- Add package-info files
- Clean up dead code

**Tasks:**
1. Update `ARCHITECTURE.md`
2. Create `package-info.kt` for each top-level package with KDoc
3. Update instruction files
4. Update memory files

---

## Implementation Checklist

### Pre-Migration
- [ ] Create feature branch: `refactor/package-structure`
- [ ] Ensure all tests pass on current `main`
- [ ] Document current public API surface

### Phase 1: Foundation
- [ ] Add `@InternalQuoVadisApi` to `AnimationCoordinator`
- [ ] Add `@InternalQuoVadisApi` to `ComposableCache`
- [ ] Add `@InternalQuoVadisApi` to `PredictiveBackController`
- [ ] Add `@InternalQuoVadisApi` to `BackAnimationController`
- [ ] Add `@InternalQuoVadisApi` to `NavigationResultManager`
- [ ] Add `@InternalQuoVadisApi` to `NavKeyGenerator`
- [ ] Add `@InternalQuoVadisApi` to `GeneratedTabMetadata`
- [ ] Add `@InternalQuoVadisApi` to all operation objects
- [ ] Add `@InternalQuoVadisApi` to all Composite* registries
- [ ] Add `@InternalQuoVadisApi` to all Dsl* implementations
- [ ] Verify annotation opt-in requirement works

### Phase 2: Split NavigationTransition
- [ ] Create `navigation/transition/` package
- [ ] Create `compose/transition/` package
- [ ] Move `NavigationTransition` (interface only) to `navigation/transition/`
- [ ] Move `TransitionBuilder` to `compose/transition/`
- [ ] Move shared element functions to `compose/transition/`
- [ ] Move `SharedElementConfig`, `SharedElementType` to `compose/transition/`
- [ ] Update all imports
- [ ] Verify no Compose imports in `navigation/`

### Phase 3: Reorganize Navigation
- [ ] Create `navigation/node/` package
- [ ] Create `navigation/destination/` package
- [ ] Create `navigation/navigator/` package
- [ ] Create `navigation/result/` package
- [ ] Create `navigation/internal/` package
- [ ] Create `navigation/internal/tree/` package
- [ ] Create `navigation/internal/config/` package
- [ ] Move all NavNode types to `node/`
- [ ] Move destination types to `destination/`
- [ ] Move navigator interfaces to `navigator/`
- [ ] Move result types to `result/`
- [ ] Move internal classes to `internal/`
- [ ] Move tree implementation to `internal/tree/`
- [ ] Move config implementations to `internal/config/`
- [ ] Update all imports
- [ ] Add `internal` visibility where appropriate

### Phase 4: Reorganize Compose
- [ ] Create `compose/transition/` package (if not exists)
- [ ] Create `compose/scope/` package
- [ ] Create `compose/util/` package
- [ ] Create `compose/internal/` package
- [ ] Create `compose/internal/render/` package
- [ ] Create `compose/internal/navback/` package
- [ ] Create `compose/internal/scope/` package
- [ ] Move transition classes to `compose/transition/`
- [ ] Move scope interfaces to `compose/scope/`
- [ ] Move `WindowSizeClass` to `compose/util/`
- [ ] Move renderers to `compose/internal/render/`
- [ ] Move back handling internals to `compose/internal/navback/`
- [ ] Extract scope implementations to `compose/internal/scope/`
- [ ] Update all imports

### Phase 5: Reorganize Registry and DSL
- [ ] Create `registry/` package
- [ ] Create `registry/internal/` package
- [ ] Create `dsl/internal/` package
- [ ] Move registry interfaces to `registry/`
- [ ] Move Composite* to `registry/internal/`
- [ ] Move Runtime* to `registry/internal/`
- [ ] Move Dsl* to `dsl/internal/`
- [ ] Update all imports

### Phase 6: Testing and Imports
- [ ] Run "Optimize Imports" on entire project
- [ ] Fix all compilation errors
- [ ] Update test imports
- [ ] Run `:quo-vadis-core:desktopTest`
- [ ] Run `:quo-vadis-core:allTests`
- [ ] Run `:composeApp:assembleDebug`
- [ ] Run `:quo-vadis-ksp:test`

### Phase 7: Documentation
- [ ] Update `ARCHITECTURE.md`
- [ ] Update `codebase_structure` memory
- [ ] Create `package-info.kt` for `navigation/`
- [ ] Create `package-info.kt` for `compose/`
- [ ] Create `package-info.kt` for `registry/`
- [ ] Create `package-info.kt` for `dsl/`
- [ ] Update copilot instructions if needed

### Post-Migration
- [ ] Review PR diff for any missed items
- [ ] Ensure public API surface is correct
- [ ] Squash commits if desired
- [ ] Merge to main

---

## File-by-File Migration Guide

### Navigation Package Files

| File | Current Package | New Package | Visibility |
|------|-----------------|-------------|------------|
| `NavNode.kt` | `navigation` | `navigation.node` | Public |
| `ScreenNode.kt` | `navigation` | `navigation.node` | Public |
| `StackNode.kt` | `navigation` | `navigation.node` | Public |
| `TabNode.kt` | `navigation` | `navigation.node` | Public |
| `PaneNode.kt` | `navigation` | `navigation.node` | Public |
| `NavDestination.kt` | `navigation` | `navigation.destination` | Public |
| `DeepLink.kt` | `navigation` | `navigation.destination` | Public |
| `DeepLinkResult.kt` | `navigation` | `navigation.destination` | Public |
| `Navigator.kt` | `navigation` | `navigation.navigator` | Public |
| `PaneNavigator.kt` | `navigation` | `navigation.navigator` | Public |
| `BackPressHandler.kt` | `navigation` | `navigation.navigator` | Public |
| `LifecycleAwareNode.kt` | `navigation` | `navigation.navigator` | Public |
| `ReturnsResult.kt` | `navigation` | `navigation.result` | Public |
| `NavigatorResultExtensions.kt` | `navigation` | `navigation.result` | Public |
| `NavigationTransition.kt` | `navigation` | `navigation.transition` | Public (cleaned) |
| `TransitionState.kt` | `navigation` | `navigation.transition` | Public |
| `NavigationConfig.kt` | `navigation.config` | `navigation.config` | Public |
| `PaneRole.kt` | `navigation.pane` | `navigation.pane` | Public |
| `PaneBackBehavior.kt` | `navigation.pane` | `navigation.pane` | Public |
| `PaneConfiguration.kt` | `navigation.pane` | `navigation.pane` | Public |
| `AdaptStrategy.kt` | `navigation.pane` | `navigation.pane` | Public |
| `TransitionController.kt` | `navigation` | `navigation.internal` | Internal |
| `ResultCapable.kt` | `navigation` | `navigation.internal` | Internal |
| `NavigationResultManager.kt` | `navigation` | `navigation.internal` | Internal |
| `NavKeyGenerator.kt` | `navigation` | `navigation.internal` | Internal |
| `GeneratedTabMetadata.kt` | `navigation` | `navigation.internal` | Internal |
| `TreeNavigator.kt` | `navigation.tree` | `navigation.internal.tree` | Internal |
| `TreeMutator.kt` | `navigation.tree` | `navigation.internal.tree` | Internal |
| `PopBehavior.kt` | `navigation.tree.config` | `navigation.internal.tree` | Public* |
| `PopResult.kt` | `navigation.tree.result` | `navigation.internal.tree` | Public* |
| `BackResult.kt` | `navigation.tree.result` | `navigation.internal.tree` | Public* |
| `KeyGenerator.kt` | `navigation.tree.util` | `navigation.internal.tree` | Internal |
| `PushStrategy.kt` | `navigation.tree.result` | `navigation.internal.tree` | Internal |
| `*Operations.kt` | `navigation.tree.operations` | `navigation.internal.tree.operations` | Internal |
| `CompositeNavigationConfig.kt` | `navigation.config` | `navigation.internal.config` | Internal |
| `EmptyNavigationConfig.kt` | `navigation.config` | `navigation.internal.config` | Internal |

*Note: `PopBehavior`, `PopResult`, `BackResult` should remain public but with `@InternalQuoVadisApi` for advanced use cases.

### Compose Package Files

| File | Current Package | New Package | Visibility |
|------|-----------------|-------------|------------|
| `NavigationHost.kt` | `compose` | `compose` | Public |
| `platformDefaultPredictiveBack.kt` | `compose` | `compose` | Public |
| `NavTransition.kt` | `compose.animation` | `compose.transition` | Public |
| `TransitionScope.kt` | `compose.animation` | `compose.transition` | Public |
| NEW: `TransitionBuilder.kt` | (from navigation) | `compose.transition` | Public |
| `SharedElementModifiers.kt` | `compose.animation` | `compose.animation` | Public |
| `NavBackHandler.kt` | `compose.navback` | `compose.navback` | Public |
| `CompositionLocals.kt` | `compose.render` | `compose.scope` | Public |
| `NavRenderScope.kt` | `compose.render` | `compose.scope` | Public |
| `TabsContainerScope.kt` | `compose.wrapper` | `compose.scope` | Public |
| `PaneContainerScope.kt` | `compose.wrapper` | `compose.scope` | Public |
| `WindowSizeClass.kt` | `compose.wrapper` | `compose.util` | Public |
| `AnimationCoordinator.kt` | `compose.animation` | `compose.internal` | Internal |
| `BackAnimationController.kt` | `compose.animation` | `compose.internal` | Internal |
| `PredictiveBackController.kt` | `compose.navback` | `compose.internal` | Internal |
| `ComposableCache.kt` | `compose.render` | `compose.internal` | Internal |
| `NavTreeRenderer.kt` | `compose.render` | `compose.internal.render` | Internal |
| `ScreenRenderer.kt` | `compose.render` | `compose.internal.render` | Internal |
| `StackRenderer.kt` | `compose.render` | `compose.internal.render` | Internal |
| `TabRenderer.kt` | `compose.render` | `compose.internal.render` | Internal |
| `PaneRenderer.kt` | `compose.render` | `compose.internal.render` | Internal |
| `AnimatedNavContent.kt` | `compose.render` | `compose.internal.render` | Internal |
| `PredictiveBackContent.kt` | `compose.render` | `compose.internal.render` | Internal |
| `StaticAnimatedVisibilityScope.kt` | `compose.render` | `compose.internal.render` | Internal |
| `BackNavigationEvent.kt` | `compose.navback` | `compose.internal.navback` | Internal |
| `CascadeBackState.kt` | `compose.navback` | `compose.internal.navback` | Internal |
| `NavigateBackHandler.kt` | `compose.navback` | `compose.internal.navback` | Internal |
| `PlatformBackInput.kt` | `compose.navback` | `compose.internal.navback` | Internal |
| `ScreenNavigationInfo.kt` | `compose.navback` | `compose.internal.navback` | Internal |

### Registry Package Files

| File | Current Package | New Package | Visibility |
|------|-----------------|-------------|------------|
| `ScreenRegistry.kt` | `dsl.registry` | `registry` | Public |
| `ContainerRegistry.kt` | `dsl.registry` | `registry` | Public |
| `TransitionRegistry.kt` | `dsl.registry` | `registry` | Public |
| `ScopeRegistry.kt` | `dsl.registry` | `registry` | Public |
| `DeepLinkRegistry.kt` | `dsl.registry` | `registry` | Public |
| `PaneRoleRegistry.kt` | `dsl.registry` | `registry` | Public |
| `RouteRegistry.kt` | `dsl.registry` | `registry` | Public |
| `BackHandlerRegistry.kt` | `dsl.registry` | `registry` | Public |
| `CompositeScreenRegistry.kt` | `dsl.registry` | `registry.internal` | Internal |
| `CompositeContainerRegistry.kt` | `dsl.registry` | `registry.internal` | Internal |
| `CompositeTransitionRegistry.kt` | `dsl.registry` | `registry.internal` | Internal |
| `CompositeScopeRegistry.kt` | `dsl.registry` | `registry.internal` | Internal |
| `CompositeDeepLinkRegistry.kt` | `dsl.registry` | `registry.internal` | Internal |
| `RuntimeDeepLinkRegistry.kt` | `dsl.registry` | `registry.internal` | Internal |

### DSL Package Files

| File | Current Package | New Package | Visibility |
|------|-----------------|-------------|------------|
| `NavigationConfigDsl.kt` | `dsl` | `dsl` | Public |
| `NavigationConfigBuilder.kt` | `dsl` | `dsl` | Public |
| `StackBuilder.kt` | `dsl` | `dsl` | Public |
| `TabsBuilder.kt` | `dsl` | `dsl` | Public |
| `PanesBuilder.kt` | `dsl` | `dsl` | Public |
| `ContainerBuilder.kt` | `dsl` | `dsl` | Public |
| `DslNavigationConfig.kt` | `dsl` | `dsl.internal` | Internal |
| `DslScreenRegistry.kt` | `dsl` | `dsl.internal` | Internal |
| `DslContainerRegistry.kt` | `dsl` | `dsl.internal` | Internal |
| `DslTransitionRegistry.kt` | `dsl` | `dsl.internal` | Internal |
| `DslScopeRegistry.kt` | `dsl` | `dsl.internal` | Internal |
| `BuilderDataClasses.kt` | `dsl` | `dsl.internal` | Internal |

---

## Estimated Total Effort

| Phase | Tasks | Hours |
|-------|-------|-------|
| Phase 1: Foundation | Add @InternalQuoVadisApi | 2-3 |
| Phase 2: Split NavigationTransition | Extract Compose code | 2-3 |
| Phase 3: Reorganize Navigation | Move ~30 files | 3-4 |
| Phase 4: Reorganize Compose | Move ~25 files | 3-4 |
| Phase 5: Reorganize Registry/DSL | Move ~20 files | 2-3 |
| Phase 6: Testing/Imports | Fix imports, run tests | 3-4 |
| Phase 7: Documentation | Update docs | 2 |
| **Total** | | **17-23 hours** |

---

## Risk Mitigation

### Risk 1: Breaking KSP-Generated Code
**Mitigation:** Update `quo-vadis-ksp` code generation templates in parallel with this refactoring.

### Risk 2: Large PR Size
**Mitigation:** Can be split into sub-PRs per phase if needed.

### Risk 3: Missed Import Updates
**Mitigation:** IDE "Optimize Imports" + full test suite + sample app build verification.

### Risk 4: Platform-Specific Code Breakage
**Mitigation:** Test on all platforms (Android, iOS, Desktop, Web) after migration.

---

## Success Criteria

1. ✅ All tests pass
2. ✅ Demo app (`composeApp`) builds and runs on all platforms
3. ✅ No Compose dependencies in `navigation/` package (except `navigation.internal.tree.TreeNavigator` which may need state flow)
4. ✅ Clear separation between public and internal APIs
5. ✅ Each domain package has focused responsibility
6. ✅ `@InternalQuoVadisApi` required for framework-level APIs
7. ✅ Documentation updated

---

## Related Documents

- [Navigator Interface Refactoring](navigator-interface-refactoring.md) - Interface segregation
- [TreeMutator Refactoring](tree-mutator-refactoring.md) - TreeMutator decomposition
- [ARCHITECTURE.md](../../ARCHITECTURE.md) - Overall architecture

---

*Last Updated: January 3, 2026*
