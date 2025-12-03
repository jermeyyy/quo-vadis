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

### Phase 2: Unified Renderer (7 Tasks)
Implements the single rendering component that projects the NavNode tree.

| ID | Task | Complexity | Est. Time |
|----|------|------------|-----------|
| [RENDER-001](./phase2-renderer/RENDER-001-renderable-surface.md) | Define RenderableSurface Data Class | Low | 1 day |
| [RENDER-002](./phase2-renderer/RENDER-002-flatten-algorithm.md) | Implement flattenState Algorithm | High | 3-4 days |
| [RENDER-003](./phase2-renderer/RENDER-003-transition-state.md) | Create TransitionState Sealed Class | Medium | 2 days |
| [RENDER-004](./phase2-renderer/RENDER-004-quovadis-host.md) | Build QuoVadisHost Composable | High | 5-7 days |
| [RENDER-005](./phase2-renderer/RENDER-005-predictive-back.md) | Integrate Predictive Back with Speculative Pop | High | 4-5 days |
| [RENDER-006](./phase2-renderer/RENDER-006-animation-registry.md) | Create AnimationRegistry | Medium | 2-3 days |
| [RENDER-007](./phase2-renderer/RENDER-007-saveable-state.md) | SaveableStateHolder Integration | Medium | 2-3 days |

**Phase 2 Total: ~19-25 days**

> ~~RENDER-008-legacy-migration~~ **REMOVED** - No legacy host migration needed

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

### Phase 4: KSP Processor Rewrite (6 Tasks)
Complete rewrite of code generation for the new annotation system.

| ID | Task | Complexity | Est. Time |
|----|------|------------|-----------|
| [KSP-001](./phase3-ksp/KSP-001-graph-type-enum.md) | Create Annotation Extractors | Medium | 2-3 days |
| [KSP-002](./phase3-ksp/KSP-002-class-references.md) | Create NavNode Builder Generator | High | 4-5 days |
| [KSP-003](./phase3-ksp/KSP-003-graph-extractor.md) | Create Screen Registry Generator | Medium | 2-3 days |
| [KSP-004](./phase3-ksp/KSP-004-deep-link-handler.md) | Create Deep Link Handler Generator | High | 3-4 days |
| [KSP-005](./phase3-ksp/KSP-005-navigator-extensions.md) | Create Navigator Extensions Generator | Low | 1 day |
| [KSP-006](./phase3-ksp/KSP-006-validation.md) | Validation and Error Reporting | Medium | 2-3 days |

**Phase 4 Total: ~14-19 days**

**Generated Artifacts:**
| Input | Output | Purpose |
|-------|--------|---------|
| `@Stack` class | `build{Name}NavNode()` | Initial StackNode tree |
| `@Tab` class | `build{Name}NavNode()` | Initial TabNode tree |
| `@Pane` class | `build{Name}NavNode()` | Initial PaneNode tree |
| All `@Screen` | `GeneratedScreenRegistry` | Destination → Composable mapping |
| All `@Destination` | `GeneratedDeepLinkHandler` | URI → Destination parsing |

---

### ~~Phase 5: Migration Utilities~~ **REMOVED**

> **No longer needed** - Library is in development stage; breaking changes acceptable.
> Users will adopt the new API directly without migration utilities.

---

### Phase 5: Risk Mitigation (5 Tasks)
Implements safeguards against identified risks.

| ID | Task | Complexity | Est. Time |
|----|------|------------|-----------|
| RISK-001 | Memoized Tree Flattening | Medium | 2 days |
| RISK-002 | Gesture Exclusion Modifier | Medium | 2 days |
| RISK-003 | Deep Link Tree Validator | Medium | 2 days |
| RISK-004 | State Restoration Validation | Medium | 2 days |
| RISK-005 | Performance Benchmarks | Medium | 3 days |

**Phase 5 Total: ~11 days**

> ~~RISK-004-api-checker~~ **REMOVED** - No backward API compatibility required

---

### Phase 6: Documentation (4 Tasks)
Updates all documentation for the new architecture.

| ID | Task | Complexity | Est. Time |
|----|------|------------|-----------|
| DOC-001 | Update Annotation KDoc | Low | 1 day |
| DOC-002 | Update README with New Architecture | Medium | 2 days |
| DOC-003 | Create Deep Linking Documentation | Medium | 2 days |
| DOC-004 | Create Pane Navigation Guide | Medium | 2 days |

**Phase 6 Total: ~7 days**

> ~~DOC-002-migration-guide~~ **REMOVED** - No migration guide needed

---

### Phase 7: Testing Infrastructure (5 Tasks)
Ensures comprehensive test coverage.

| ID | Task | Complexity | Est. Time |
|----|------|------------|-----------|
| TEST-001 | KSP Generator Unit Tests | Medium | 3 days |
| TEST-002 | Path Reconstructor Tests | High | 3 days |
| TEST-003 | Integration Tests with Demo App | High | 4 days |
| TEST-004 | Performance Regression Tests | Medium | 2 days |
| TEST-005 | Multiplatform Compatibility Tests | High | 4 days |

**Phase 7 Total: ~16 days**

> ~~TEST-003-migration-utils~~ **REMOVED** - No migration utilities to test

---

## Dependency Graph (Revised)

```
Phase 1 (Core) ─────────────────────────────────────────────────┐
    │                                                            │
    ├──► Phase 2 (Renderer) ────────────────────────────────────┤
    │                                                            │
    ├──► Phase 3 (Annotations) ──► Phase 4 (KSP) ───────────────┤
    │                                                            │
    └──► Phase 5 (Risks) ───────────────────────────────────────┤
                                                                 │
                                                                 ▼
                                                    Phase 6 (Docs) + Phase 7 (Testing)
```

### Critical Path

1. **CORE-001** → **CORE-002** → **CORE-003** (Foundation)
2. **ANN-001..005** → **KSP-001..006** (Code Generation)
3. **RENDER-002** → **RENDER-004** (Renderer Core)

---

## Total Estimated Effort (Revised)

| Phase | Tasks | Estimated Days |
|-------|-------|----------------|
| Phase 1: Core State | 5 | 14-19 |
| Phase 2: Renderer | 7 | 19-25 |
| Phase 3: Annotations | 5 | 3.5 |
| Phase 4: KSP | 6 | 14-19 |
| Phase 5: Risks | 5 | 11 |
| Phase 6: Documentation | 4 | 7 |
| Phase 7: Testing | 5 | 16 |
| **TOTAL** | **37** | **84.5-100.5 days** |

> **Reduction from original**: ~22-26 days saved by removing backward compatibility, migration utilities, and legacy adapters.
>
> **Note**: Many tasks can be parallelized. With 2-3 developers, timeline can be reduced to **6-8 weeks**.

---

## Quick Links

- **Phase Directories**:
  - [Phase 1: Core State Refactoring](./phase1-core/)
  - [Phase 2: Unified Renderer](./phase2-renderer/)
  - [Phase 3: Annotations](./phase4-annotations/)
  - [Phase 4: KSP Processor](./phase3-ksp/)
  - [Phase 5: Risk Mitigation](./phase6-risks/)
  - [Phase 6: Documentation](./phase7-docs/)
  - [Phase 7: Testing](./phase8-testing/)

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
