# Quo Vadis Architecture Refactoring - Execution Plan

## Executive Summary

This document outlines the comprehensive execution plan for refactoring the **Quo Vadis** navigation library from a linear backstack model to a **tree-based NavNode architecture** with a **Single Rendering Component (QuoVadisHost)**.

### Vision

Transform Quo Vadis from a "Direct BackStack Manager" into a holistic **"Navigation Rendering Engine"** capable of:

- **Unified rendering** of linear stacks, tabbed navigators, and adaptive panes
- **Seamless Shared Element Transitions** across all navigation types
- **Physics-based Predictive Back** gestures with speculative pop
- **Deep linking** with automatic path reconstruction
- **Adaptive layouts** that morph between compact and expanded modes
- **Minimal boilerplate** via annotation-driven code generation

### Current State vs Target State

| Aspect | Current State | Target State |
|--------|---------------|--------------|
| **State Model** | Linear `List<Destination>` | Recursive `NavNode` Tree |
| **Navigation Hosts** | Multiple nested (`NavHost`, `GraphNavHost`, `TabbedNavHost`) | Single `QuoVadisHost` |
| **Shared Elements** | Limited to single host boundaries | Seamless across all navigation |
| **Predictive Back** | Per-host handling | Unified speculative pop |
| **Layout Types** | Stack-only (tabs via separate hosts) | Stack, Tab, Pane (unified) |
| **Deep Linking** | Flat route matching | Tree path reconstruction |
| **Annotations** | `@Graph`, `@Route`, `@Content`, `@TabGraph` | `@Destination`, `@Stack`, `@Tab`, `@Pane`, `@Screen` |
| **Navigation API** | Extension functions | Service locator with destination instances |

---

## Task Summary

### Phase 1: Core State Refactoring (5 Tasks)
Replaces the linear backstack with a recursive tree structure.

| ID | Task | Complexity | Est. Time |
|----|------|------------|-----------|
| [CORE-001](./phase1-core/CORE-001-navnode-hierarchy.md) | Define NavNode Sealed Hierarchy | Medium | 2-3 days |
| [CORE-002](./phase1-core/CORE-002-tree-mutator.md) | Implement TreeMutator Operations | High | 3-4 days |
| [CORE-003](./phase1-core/CORE-003-navigator-refactor.md) | Refactor Navigator to StateFlow<NavNode> | High | 4-5 days |
| [CORE-004](./phase1-core/CORE-004-state-serialization.md) | Implement NavNode Serialization | Medium | 2-3 days |
| [CORE-005](./phase1-core/CORE-005-unit-tests.md) | Comprehensive Unit Tests | Medium | 3-4 days |

**Phase 1 Total: ~14-19 days**

---

### Phase 2: Unified Renderer (12 Tasks)
Implements the single rendering component that projects the NavNode tree with user-controlled wrappers for TabNode and PaneNode.

| ID | Task | Complexity | Est. Time |
|----|------|------------|-----------|
| [RENDER-001](./phase2-renderer/RENDER-001-renderable-surface.md) | Define RenderableSurface Data Class | Low | 1.5 days |
| [RENDER-002A](./phase2-renderer/RENDER-002A-core-flatten.md) | Core flattenState Algorithm (Screen/Stack) | High | 2 days |
| [RENDER-002B](./phase2-renderer/RENDER-002B-tab-flattening.md) | TabNode Flattening with User Wrapper | High | 2 days |
| [RENDER-002C](./phase2-renderer/RENDER-002C-pane-flattening.md) | PaneNode Adaptive Flattening | High | 2 days |
| [RENDER-003](./phase2-renderer/RENDER-003-transition-state.md) | Create TransitionState Sealed Class | Medium | 2 days |
| [RENDER-004](./phase2-renderer/RENDER-004-quovadis-host.md) | Build QuoVadisHost Composable | High | 6-8 days |
| [RENDER-005](./phase2-renderer/RENDER-005-predictive-back.md) | Integrate Predictive Back with Speculative Pop | High | 4-5 days |
| [RENDER-006](./phase2-renderer/RENDER-006-animation-registry.md) | Create AnimationRegistry | Medium | 2-3 days |
| [RENDER-007](./phase2-renderer/RENDER-007-saveable-state.md) | SaveableStateHolder Integration | Medium | 3-4 days |
| [RENDER-008](./phase2-renderer/RENDER-008-user-wrapper-api.md) | User Wrapper API (TabNode/PaneNode) | Medium | 2-3 days |
| [RENDER-009](./phase2-renderer/RENDER-009-window-size-integration.md) | WindowSizeClass Integration | Medium | 2 days |
| [RENDER-010](./phase2-renderer/RENDER-010-animation-pair-tracking.md) | Animation Pair Tracking | Medium | 2 days |

**Phase 2 Total: ~31-37.5 days**

**Key Architecture Changes:**
- **User Wrapper API**: Users control wrapper composables (scaffold, tabs, bottom nav) while library manages content
- **Differentiated Caching**: Whole wrapper cached for cross-node navigation; only content cached for intra-tab/pane navigation
- **WindowSizeClass Adaptive**: PaneNode renders as StackNode on small screens, multi-pane on large screens
- **Animation Pair Tracking**: Direct access to current AND previous screens for animations

---

### Phase 3: Annotations Redesign (5 Tasks)
Introduces the new annotation system mapping directly to NavNode types.

| ID | Task | Complexity | Est. Time |
|----|------|------------|-----------|
| [ANN-001](./phase4-annotations/ANN-001-graph-type.md) | Define `@Destination` Annotation | Low | 0.5 days |
| [ANN-002](./phase4-annotations/ANN-002-pane-graph.md) | Define `@Stack` Container Annotation | Low | 0.5 days |
| [ANN-003](./phase4-annotations/ANN-003-route-transitions.md) | Define `@Tab` and `@TabItem` Annotations | Medium | 1 day |
| [ANN-004](./phase4-annotations/ANN-004-shared-element.md) | Define `@Pane` and `@PaneItem` Annotations | Medium | 1 day |
| [ANN-005](./phase4-annotations/ANN-005-screen.md) | Define `@Screen` Content Binding Annotation | Low | 0.5 days |

**Phase 3 Total: ~3.5 days**

**New Annotation Summary:**
| Annotation | Maps To | Purpose |
|------------|---------|---------|
| `@Destination(route)` | `ScreenNode` | Navigation target with deep link route |
| `@Stack(name, startDestination)` | `StackNode` | Linear navigation container |
| `@Tab(name, initialTab)` | `TabNode` | Tabbed navigation container |
| `@TabItem(label, icon, rootGraph)` | Tab metadata | Tab UI configuration |
| `@Pane(name, backBehavior)` | `PaneNode` | Adaptive layout container |
| `@PaneItem(role, adaptStrategy)` | Pane metadata | Pane behavior configuration |
| `@Screen(destination)` | Registry entry | Composable-to-destination binding |

---

### Phase 4: KSP Processor Rewrite (8 Tasks)
Complete rewrite of code generation for the new annotation system.

| ID | Task | Complexity | Est. Time |
|----|------|------------|-----------|
| [KSP-001](./phase3-ksp/KSP-001-graph-type-enum.md) | Create Annotation Extractors | Medium | 2-3 days |
| [KSP-002](./phase3-ksp/KSP-002-class-references.md) | Create NavNode Builder Generator | High | 4-5 days |
| [KSP-003](./phase3-ksp/KSP-003-graph-extractor.md) | Create Screen Registry Generator | Medium | 2-3 days |
| [KSP-004](./phase3-ksp/KSP-004-deep-link-handler.md) | Create Deep Link Handler Generator | High | 3-4 days |
| [KSP-005](./phase3-ksp/KSP-005-navigator-extensions.md) | Create Navigator Extensions Generator | Low | 1 day |
| [KSP-006](./phase3-ksp/KSP-006-validation.md) | Validation and Error Reporting | Medium | 2-3 days |
| [KSP-007](./phase3-ksp/KSP-007-remove-legacy-tabgraph.md) | Remove Legacy TabGraphExtractor | Low | 0.5 days |
| [KSP-008](./phase3-ksp/KSP-008-deep-link-handler-imports.md) | Fix Deep Link Handler Generator Imports | Low | 0.5 days |

**Phase 4 Total: ~15-20 days**

**Generated Artifacts:**
| Input | Output | Purpose |
|-------|--------|---------|
| `@Stack` class | `build{Name}NavNode()` | Initial StackNode tree |
| `@Tab` class | `build{Name}NavNode()` | Initial TabNode tree |
| `@Pane` class | `build{Name}NavNode()` | Initial PaneNode tree |
| All `@Screen` | `GeneratedScreenRegistry` | Destination → Composable mapping |
| All `@Destination` | `GeneratedDeepLinkHandler` | URI → Destination parsing |

---

### Phase 5: Recipes Module, Migration & Demo App (10 Tasks)
Creates `quo-vadis-recipes` module for LLM-assisted integration, marks legacy APIs as `@Deprecated`, and migrates demo app.

> **Note**: This phase creates a new `quo-vadis-recipes` module with pattern-based examples optimized for LLM consumption. All legacy APIs will be marked `@Deprecated` with `replaceWith` guidance. GitHub permalinks reference the main branch for stable "migrating from" code.

| ID | Task | Complexity | Est. Time |
|----|------|------------|-----------|
| [PREP-001](./phase5-migration/PREP-001-recipes-module.md) | Create quo-vadis-recipes Module | Medium | 1.5 days |
| [PREP-002](./phase5-migration/PREP-002-deprecated-annotations.md) | Add @Deprecated Annotations | Medium | 2 days |
| [PREP-003](./phase5-migration/PREP-003-permalink-reference.md) | GitHub Permalink Reference Doc | Low | 0.5 days |
| [MIG-001](./phase5-migration/MIG-001-simple-stack-example.md) | Simple Stack Navigation Recipe | Low | 1 day |
| [MIG-002](./phase5-migration/MIG-002-master-detail-example.md) | Master-Detail Pattern Recipe | Medium | 1.5 days |
| [MIG-003](./phase5-migration/MIG-003-tabbed-navigation-example.md) | Tabbed Navigation Recipe | Medium | 2 days |
| [MIG-004](./phase5-migration/MIG-004-process-flow-example.md) | Process/Wizard Flow Recipe | Low | 1 day |
| [MIG-005](./phase5-migration/MIG-005-nested-tabs-detail-example.md) | Nested Tabs + Detail Recipe | Medium | 1.5 days |
| [MIG-006](./phase5-migration/MIG-006-deep-linking-recipe.md) | Deep Linking Recipe | Medium | 1 day |
| [MIG-007](./phase5-migration/MIG-007-demo-app-rewrite.md) | Demo App Rewrite | High | 3-4 days |
| [MIG-008](./phase5-migration/MIG-008-api-change-summary.md) | API Change Summary Document | Low | 0.5 days |

**Phase 5 Total: ~14-17 days**

**New Module Structure:**
```
quo-vadis-recipes/
├── build.gradle.kts           # KMP module (NOT published)
└── src/commonMain/.../recipes/
    ├── stack/                 # Linear stack patterns
    ├── tabs/                  # Tabbed navigation patterns
    ├── masterdetail/          # Master-detail patterns
    ├── wizard/                # Process/wizard patterns
    ├── deeplink/              # Deep linking patterns
    ├── pane/                  # Adaptive layout patterns
    └── shared/                # Common utilities
```

**Key Changes from Original Phase 5:**
| Aspect | Original | Revised |
|--------|----------|---------|
| Example Location | `docs/migration-examples/*.md` | `quo-vadis-recipes/` module |
| Legacy API Handling | Documentation only | `@Deprecated` with `replaceWith` |
| Code References | Inline code blocks | GitHub permalinks to main |
| LLM Support | None | Pattern-based packages, rich KDoc |

---

### Phase 6: Risk Mitigation (5 Tasks)
Implements safeguards against identified risks.

| ID | Task | Complexity | Est. Time |
|----|------|------------|-----------|
| RISK-001 | Memoized Tree Flattening | Medium | 2 days |
| RISK-002 | Gesture Exclusion Modifier | Medium | 2 days |
| RISK-003 | Deep Link Tree Validator | Medium | 2 days |
| RISK-004 | State Restoration Validation | Medium | 2 days |
| RISK-005 | Performance Benchmarks | Medium | 3 days |

**Phase 6 Total: ~11 days**

---

### Phase 7: Documentation (4 Tasks)
Updates all documentation for the new architecture.

| ID | Task | Complexity | Est. Time |
|----|------|------------|-----------|
| DOC-001 | Update Annotation KDoc | Low | 1 day |
| DOC-002 | Update README with New Architecture | Medium | 2 days |
| DOC-003 | Create Deep Linking Documentation | Medium | 2 days |
| DOC-004 | Create Pane Navigation Guide | Medium | 2 days |

**Phase 7 Total: ~7 days**

---

### Phase 8: Testing Infrastructure (5 Tasks)
Ensures comprehensive test coverage.

| ID | Task | Complexity | Est. Time |
|----|------|------------|-----------|
| TEST-001 | KSP Generator Unit Tests | Medium | 3 days |
| TEST-002 | Path Reconstructor Tests | High | 3 days |
| TEST-003 | Integration Tests with Demo App | High | 4 days |
| TEST-004 | Performance Regression Tests | Medium | 2 days |
| TEST-005 | Multiplatform Compatibility Tests | High | 4 days |

**Phase 8 Total: ~16 days**

---

## Dependency Graph (Revised)

```
Phase 1 (Core) ─────────────────────────────────────────────────┐
    │                                                            │
    ├──► Phase 2 (Renderer)                                     │
    │    ├── RENDER-001 → RENDER-002A → 002B/002C               │
    │    │                    ├── RENDER-008 (User Wrapper)     │
    │    │                    └── RENDER-009 (WindowSize)       │
    │    ├── RENDER-003 → RENDER-010 (Animation Pairs)          │
    │    └── → RENDER-004 → 005, 006, 007                       │
    │                                                            │
    ├──► Phase 3 (Annotations) ──► Phase 4 (KSP) ───────────────┤
    │                                                            │
    ├──► Phase 5 (Recipes & Migration) ─────────────────────────┤
    │    ├── PREP-001 (recipes module) ──► MIG-001..006         │
    │    ├── PREP-002 (@Deprecated) ──► MIG-008                 │
    │    └── MIG-001..006 ──► MIG-007 (Demo Rewrite)            │
    │                                                            │
    └──► Phase 6 (Risks) ───────────────────────────────────────┤
                                                                 │
                                                                 ▼
                                                    Phase 7 (Docs) + Phase 8 (Testing)
```

### Critical Path

1. **CORE-001** → **CORE-002** → **CORE-003** (Foundation)
2. **ANN-001..005** → **KSP-001..006** (Code Generation)
3. **RENDER-001** → **RENDER-002A** → **RENDER-004** (Renderer Core)
4. **PREP-001** → **MIG-001..006** → **MIG-007** (Phase 5 Critical Path)

---

## Total Estimated Effort (Revised)

| Phase | Tasks | Estimated Days |
|-------|-------|----------------|
| Phase 1: Core State | 5 | 14-19 |
| Phase 2: Renderer | 12 | 31-37.5 |
| Phase 3: Annotations | 5 | 3.5 |
| Phase 4: KSP | 6 | 14-19 |
| Phase 5: Recipes & Migration | 10 | 14-17 |
| Phase 6: Risks | 5 | 11 |
| Phase 7: Documentation | 4 | 7 |
| Phase 8: Testing | 5 | 16 |
| **TOTAL** | **52** | **110.5-132 days** |

> **Note**: Phase 5 now includes `quo-vadis-recipes` module creation and `@Deprecated` annotations for all legacy APIs. All "migrating from" code references use GitHub permalinks to the main branch.
>
> **Note**: Many tasks can be parallelized. With 2-3 developers, timeline can be reduced to **8-10 weeks**.

---

## Quick Links

- **Phase Directories**:
  - [Phase 1: Core State Refactoring](./phase1-core/)
  - [Phase 2: Unified Renderer](./phase2-renderer/)
  - [Phase 3: Annotations](./phase4-annotations/)
  - [Phase 4: KSP Processor](./phase3-ksp/)
  - [Phase 5: Recipes & Migration](./phase5-migration/)
  - [Phase 6: Risk Mitigation](./phase6-risks/)
  - [Phase 7: Documentation](./phase7-docs/)
  - [Phase 8: Testing](./phase8-testing/)

---

## Key Decisions (Updated)

| Decision | Choice | Rationale |
|----------|--------|-----------|
| **Node ID Strategy** | Structured path (e.g., `main-tabs/home/detail-123`) | Better debugging, deterministic |
| **Cascading Pop** | Configurable | Support both behaviors |
| **TabNode Init** | Lazy initialization | Memory efficiency |
| **Backward Compat** | **None** | Library in development |
| **Deep Link Resolution** | KSP-generated handlers | Compile-time safety |
| **Navigation API** | Destination instance via Navigator | Clean service locator pattern |

---

## New Annotation Examples

### Basic Stack Navigation

```kotlin
@Stack(name = "home", startDestination = "Feed")
sealed class HomeDestination : Destination {
    
    @Destination(route = "home/feed")
    data object Feed : HomeDestination()
    
    @Destination(route = "home/article/{articleId}")
    data class Article(val articleId: String) : HomeDestination()
}

@Screen(HomeDestination.Feed::class)
@Composable
fun FeedScreen(navigator: Navigator) {
    Button(onClick = { navigator.navigate(HomeDestination.Article("123")) }) {
        Text("View Article")
    }
}

@Screen(HomeDestination.Article::class)
@Composable
fun ArticleScreen(destination: HomeDestination.Article, navigator: Navigator) {
    Text("Article: ${destination.articleId}")
}
```

### Tabbed Navigation

```kotlin
@Tab(name = "main", initialTab = "Home")
sealed class MainTabs : Destination {
    
    @TabItem(label = "Home", icon = "home", rootGraph = HomeDestination::class)
    @Destination(route = "tab/home")
    data object Home : MainTabs()
    
    @TabItem(label = "Profile", icon = "person", rootGraph = ProfileDestination::class)
    @Destination(route = "tab/profile")
    data object Profile : MainTabs()
}
```

### App Entry Point

```kotlin
@Composable
fun App() {
    val navTree = remember { buildMainTabsNavNode() }  // KSP-generated
    val navigator = rememberNavigator(navTree)
    
    QuoVadisHost(
        navigator = navigator,
        screenRegistry = GeneratedScreenRegistry  // KSP-generated
    )
}
```

---

## References

- [Original Refactoring Plan](../../Refactoring%20Quo-Vadis%20Navigation%20Architecture.md)
- [Current Architecture Documentation](../../quo-vadis-core/docs/ARCHITECTURE.md)
- [API Reference](../../quo-vadis-core/docs/API_REFERENCE.md)
- [Shared Element Transitions](../../quo-vadis-core/docs/SHARED_ELEMENT_TRANSITIONS.md)
