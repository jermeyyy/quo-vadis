# Phase 5: Migration - Summary

## Phase Overview

| Attribute | Value |
|-----------|-------|
| **Phase** | 5 |
| **Focus** | Migration Examples & API Documentation |
| **Total Tasks** | 7 |
| **Total Estimated Time** | 10-11 days |
| **Dependencies** | Phases 1-4 complete |

## Objectives

Phase 5 provides comprehensive migration guidance for transitioning from the old Quo Vadis navigation API to the new NavNode architecture. This phase creates:

1. **Step-by-step migration examples** for all major navigation patterns
2. **Before/after code comparisons** showing exact transformation steps
3. **A rewritten demo application** serving as a reference implementation
4. **Quick-reference documentation** for all API changes

---

## Task Summary

### MIG-001: Simple Stack Navigation Example

| Attribute | Value |
|-----------|-------|
| **Complexity** | Low |
| **Estimated Time** | 1 day |
| **Output** | `docs/migration-examples/01-simple-stack.md` |

**Purpose**: Demonstrates basic linear stack navigation migration.

**Key Transformations**:
- `@Graph` → `@Stack`
- `@Route` → `@Destination`
- `@Content` → `@Screen`
- `GraphNavHost` → `QuoVadisHost`
- Remove manual setup code (`initializeQuoVadisRoutes()`, `registerGraph()`, `setStartDestination()`)

---

### MIG-002: Master-Detail Pattern Example

| Attribute | Value |
|-----------|-------|
| **Complexity** | Medium |
| **Estimated Time** | 1.5 days |
| **Dependencies** | MIG-001 |
| **Output** | `docs/migration-examples/02-master-detail.md` |

**Purpose**: Shows migration of typed arguments, deep linking, and shared element transitions.

**Key Transformations**:
- Remove `@Argument` annotation and `TypedDestination<T>` interface
- Use route templates with `{param}` placeholders
- Screen functions receive destination instance directly (not separate data class)
- Configure transitions via `AnimationRegistry` instead of per-call
- `SharedTransitionLayout` wrapper becomes built-in to `QuoVadisHost`

---

### MIG-003: Tabbed Navigation Example

| Attribute | Value |
|-----------|-------|
| **Complexity** | Medium |
| **Estimated Time** | 2 days |
| **Dependencies** | MIG-001, MIG-002 |
| **Output** | `docs/migration-examples/03-tabbed-navigation.md` |

**Purpose**: Demonstrates migration from `TabbedNavHost` to declarative `@Tab` annotations.

**Key Transformations**:
- `TabbedNavigatorConfig` object → `@Tab` + `@TabItem` annotations
- `TabbedNavHost` → `QuoVadisHost` with `tabWrapper` parameter
- `rememberTabNavigator()` → state managed in NavNode tree
- `tabState.switchTab()` → `navigator.switchTab()`
- Tab UI moved to user-controlled `tabWrapper` composable

---

### MIG-004: Process/Wizard Flow Example

| Attribute | Value |
|-----------|-------|
| **Complexity** | Low |
| **Estimated Time** | 1 day |
| **Dependencies** | MIG-001 |
| **Output** | `docs/migration-examples/04-process-flow.md` |

**Purpose**: Shows migration of multi-step flows with conditional branching and stack clearing.

**Key Transformations**:
- `navigateAndClearTo(dest, route, inclusive)` → `navigateAndClear(dest, clearUpTo::class, inclusive)` (type-safe)
- `popTo()` uses destination class or instance instead of string route
- New `navigator.exitFlow(DestClass)` convenience method
- Animation configuration via `AnimationRegistry` with `withinGraph()` scope

---

### MIG-005: Nested Tabs + Detail Example

| Attribute | Value |
|-----------|-------|
| **Complexity** | Medium |
| **Estimated Time** | 1.5 days |
| **Dependencies** | MIG-002, MIG-003 |
| **Output** | `docs/migration-examples/05-nested-tabs-detail.md` |

**Purpose**: Demonstrates complex hierarchies where detail screens cover the tab bar.

**Key Transformations**:
- Multiple nested `NavHost` instances → single unified `QuoVadisHost`
- Parent navigator passing → single navigator for all navigation
- Manual z-index management → automatic z-ordering via flattening
- Shared elements across tab/detail boundary now possible
- Predictive back coordination → unified speculative pop

---

### MIG-006: Demo App Rewrite

| Attribute | Value |
|-----------|-------|
| **Complexity** | High |
| **Estimated Time** | 3-4 days |
| **Dependencies** | Phase 1-4 complete, MIG-001 through MIG-005 |
| **Output** | Updated `composeApp/` module |

**Purpose**: Complete rewrite of the demo application as a reference implementation.

**Phases**:
1. **Phase A (Day 1)**: Destination definitions conversion
2. **Phase B (Day 1-2)**: Screen bindings update
3. **Phase C (Day 2)**: App entry point migration
4. **Phase D (Day 2-3)**: Tab navigation conversion
5. **Phase E (Day 3)**: Navigation calls update
6. **Phase F (Day 3-4)**: Testing & verification

**Features Showcased**:
- Simple stack navigation (Settings flow)
- Master-detail pattern (Home → Article)
- Tabbed navigation (Home/Search/Profile)
- Tab state preservation
- Full-screen over tabs
- Process/wizard flow (Onboarding)
- Deep linking
- Shared element transitions
- Predictive back gestures
- Conditional navigation

---

### MIG-007: API Change Summary Document

| Attribute | Value |
|-----------|-------|
| **Complexity** | Low |
| **Estimated Time** | 0.5 days |
| **Dependencies** | None |
| **Output** | `docs/migration-examples/API-CHANGES.md` |

**Purpose**: Quick-reference document for all API changes.

**Content Sections**:
- Annotation changes table
- Destination definition changes
- Navigation host changes
- Navigator API changes
- Setup/initialization changes
- Typed arguments migration
- Transition configuration
- Tab navigation migration
- Shared elements migration
- Predictive back changes
- Per-file and per-app migration checklists
- Common error messages with fixes
- KSP generated files explanation

---

## Key Components/Features Implemented

### Annotation Changes
| Old | New |
|-----|-----|
| `@Graph` | `@Stack`, `@Tab`, or `@Pane` |
| `@Route` | `@Destination` (with route templates) |
| `@Argument` | Removed (use route templates) |
| `@Content` | `@Screen` |
| — | `@Tab`, `@TabItem` (new) |
| — | `@Pane`, `@PaneItem` (new) |

### Navigation Host Changes
| Old | New |
|-----|-----|
| `GraphNavHost` | `QuoVadisHost` |
| `TabbedNavHost` | `QuoVadisHost` + `tabWrapper` |
| `NavHost` | `QuoVadisHost` |

### Navigator API Changes
| Old | New |
|-----|-----|
| `navigator.backStack` | `navigator.state` |
| `navigate(dest, transition)` | `navigate(dest)` + `AnimationRegistry` |
| `navigateAndClearTo(dest, route, inclusive)` | `navigateAndClear(dest, clearUpTo::class, inclusive)` |
| `tabState.switchTab(tab)` | `navigator.switchTab(tab)` |
| `registerGraph()`, `setStartDestination()` | Removed (KSP generates tree) |
| — | `exitFlow(DestClass)` (new) |

### Setup Pattern Changes
```kotlin
// OLD
remember { initializeQuoVadisRoutes() }
val navigator = rememberNavigator()
val graph = remember { appGraph() }
LaunchedEffect(navigator, graph) {
    navigator.registerGraph(graph)
    navigator.setStartDestination(StartDest)
}
GraphNavHost(graph, navigator, ...)

// NEW
val navTree = remember { buildAppNavNode() }  // KSP-generated
val navigator = rememberNavigator(navTree)
QuoVadisHost(navigator, screenRegistry = GeneratedScreenRegistry, ...)
```

---

## Dependencies on Other Phases

| Phase | Dependency Type | Notes |
|-------|-----------------|-------|
| **Phase 1 (Core)** | Required | NavNode tree structure, TreeMutator operations |
| **Phase 2 (Renderer)** | Required | `QuoVadisHost`, `tabWrapper`, flattening, predictive back |
| **Phase 3 (KSP)** | Required | Generated builders, screen registry, deep link handlers |
| **Phase 4 (Annotations)** | Required | `@Stack`, `@Tab`, `@Destination`, `@Screen`, etc. |
| **Phase 8 (Testing)** | Related | Integration tests use demo app (TEST-003) |

---

## File References

### Migration Example Outputs
- `docs/migration-examples/01-simple-stack.md`
- `docs/migration-examples/02-master-detail.md`
- `docs/migration-examples/03-tabbed-navigation.md`
- `docs/migration-examples/04-process-flow.md`
- `docs/migration-examples/05-nested-tabs-detail.md`
- `docs/migration-examples/API-CHANGES.md`

### Demo App Files (to be modified)
```
composeApp/src/commonMain/kotlin/com/jermey/navplayground/
├── App.kt
├── demo/
│   ├── DemoApp.kt
│   ├── destinations/
│   │   ├── AppDestination.kt
│   │   ├── MainTabsDestination.kt
│   │   ├── HomeDestination.kt
│   │   ├── SearchDestination.kt
│   │   ├── ProfileDestination.kt
│   │   ├── SettingsDestination.kt
│   │   └── OnboardingDestination.kt
│   ├── screens/
│   │   ├── home/
│   │   ├── search/
│   │   ├── profile/
│   │   ├── settings/
│   │   └── onboarding/
│   └── ui/
│       ├── components/
│       └── theme/
```

### Cross-References to Other Phases
- [RENDER-002B: TabNode Flattening](../phase2-renderer/RENDER-002B-tab-flattening.md)
- [RENDER-005: Predictive Back](../phase2-renderer/RENDER-005-predictive-back.md)
- [RENDER-006: AnimationRegistry](../phase2-renderer/RENDER-006-animation-registry.md)
- [RENDER-008: User Wrapper API](../phase2-renderer/RENDER-008-user-wrapper-api.md)
- [CORE-002: TreeMutator Operations](../phase1-core/CORE-002-tree-mutator.md)
- [ANN-001 through ANN-005](../phase4-annotations/) - Annotation definitions
- [TEST-003: Integration Tests](../phase8-testing/TEST-003-integration-tests.md)

---

## Complexity & Effort Summary

| Task | Complexity | Estimated Time |
|------|------------|----------------|
| MIG-001: Simple Stack | Low | 1 day |
| MIG-002: Master-Detail | Medium | 1.5 days |
| MIG-003: Tabbed Navigation | Medium | 2 days |
| MIG-004: Process/Wizard Flow | Low | 1 day |
| MIG-005: Nested Tabs + Detail | Medium | 1.5 days |
| MIG-006: Demo App Rewrite | High | 3-4 days |
| MIG-007: API Change Summary | Low | 0.5 days |
| **Total** | | **10-11 days** |

---

## Success Criteria

- [ ] All 7 migration example documents created
- [ ] Before/after code comparisons are clear and accurate
- [ ] Demo app fully migrated and functional
- [ ] All navigation patterns work correctly post-migration
- [ ] API change summary serves as effective quick-reference
- [ ] Platform testing complete (Android, iOS, Desktop, Web where applicable)
- [ ] No regressions from old behavior
