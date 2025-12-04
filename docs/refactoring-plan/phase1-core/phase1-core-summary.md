# Phase 1: Core State Refactoring - Summary

## Phase Overview

Phase 1 establishes the foundation of the new Quo Vadis navigation architecture by replacing the linear `List<Destination>` backstack with a recursive tree structure (`NavNode`). This fundamental change enables modeling of complex navigation patterns including linear stacks, tabbed navigators, and adaptive multi-pane layouts.

### Key Objectives

1. **Define NavNode Sealed Hierarchy** - Create immutable tree structure for navigation state
2. **Implement TreeMutator Operations** - Pure functional state transformations
3. **Refactor Navigator to StateFlow<NavNode>** - Single source of truth via StateFlow
4. **Implement State Serialization** - Process death survival and state restoration
5. **Comprehensive Unit Testing** - Ensure correctness and prevent regressions

### Design Principles

- **Immutability**: All nodes are data classes enabling structural sharing and efficient diffing
- **Separation of Concerns**: NavNode stores logical navigation state; visual layout is renderer responsibility
- **Type Safety**: Sealed hierarchy ensures exhaustive pattern matching
- **Thread Safety**: StateFlow atomic updates without locks

---

## Task Summaries

### CORE-001: Define NavNode Sealed Hierarchy

| Property | Value |
|----------|-------|
| **Complexity** | Medium |
| **Estimated Time** | 2-3 days |
| **Dependencies** | None |
| **Blocks** | CORE-002, CORE-003, CORE-004 |

**Summary**: Establishes the `NavNode` sealed interface hierarchy as the foundation for tree-based navigation state.

**Key Components**:
- `NavNode` - Base sealed interface with `key` and `parentKey` properties
- `ScreenNode` - Leaf node holding a single `Destination`
- `StackNode` - Linear navigation stack with ordered children
- `TabNode` - Parallel stacks with active tab tracking
- `PaneNode` - Role-based adaptive pane layouts with `Map<PaneRole, PaneConfiguration>`

**Supporting Types**:
- `PaneRole` enum: Primary, Supporting, Extra
- `AdaptStrategy` enum: Hide, Levitate, Reflow
- `PaneBackBehavior` enum: PopUntilScaffoldValueChange, PopUntilCurrentDestinationChange, PopUntilContentChange, PopLatest
- `PaneConfiguration` data class with content node and adapt strategy

**Extension Functions**:
- `findByKey()` - Recursive node lookup
- `activePathToLeaf()` - Depth-first path to active leaf
- `activeLeaf()` - Deepest active ScreenNode
- `activeStack()` - Deepest active StackNode
- `allScreens()` - All ScreenNodes in subtree
- `paneForRole()` - Find pane content by role
- `allPaneNodes()` - Collect all PaneNodes

---

### CORE-002: Implement TreeMutator Operations

| Property | Value |
|----------|-------|
| **Complexity** | High |
| **Estimated Time** | 3-4 days |
| **Dependencies** | CORE-001 |
| **Blocks** | CORE-003 |

**Summary**: Pure functional utility for immutable tree transformations following the reducer pattern: $S_{new} = f(S_{old}, Action)$

**Core Operations**:

| Category | Operations |
|----------|------------|
| **Push** | `push()`, `pushToStack()` |
| **Pop** | `pop()`, `popTo()`, `popToRoute()` |
| **Tab** | `switchTab()`, `switchActiveTab()` |
| **Pane** | `replacePane()`, `setActivePane()`, `navigateToPane()`, `switchActivePane()`, `popPane()` |
| **Utility** | `replaceNode()`, `removeNode()`, `clearAndPush()`, `replaceCurrent()` |

**Pane-Specific Operations** (from CORE-002-pane-impact-notes):
- `navigateToPane(role, destination, switchFocus)` - Navigate within specific pane
- `switchActivePane(role)` - Change focused pane without navigation
- `popPane(role)` - Pop from specific pane's stack
- `setPaneConfiguration(role, config)` - Add/update pane configuration
- `removePaneConfiguration(role)` - Remove pane (except Primary)
- `popWithBehavior()` - Respects `PaneBackBehavior` settings

**Key Features**:
- Structural sharing for efficiency
- `PopBehavior` enum: CASCADE, PRESERVE_EMPTY
- Thread-safe pure functions

---

### CORE-003: Refactor Navigator to StateFlow<NavNode>

| Property | Value |
|----------|-------|
| **Complexity** | High |
| **Estimated Time** | 4-5 days |
| **Dependencies** | CORE-001, CORE-002 |
| **Blocks** | CORE-004, CORE-005, RENDER-* |

**Summary**: Refactors the `Navigator` interface to use `StateFlow<NavNode>` as single source of truth, replacing the `BackStack`-based approach.

**Architecture Changes**:

| Aspect | Current | Target |
|--------|---------|--------|
| State Model | `MutableBackStack` with `SnapshotStateList` | `MutableStateFlow<NavNode>` |
| State Access | `backStack`, `entries` | `state: StateFlow<NavNode>` |
| Mutations | Direct list manipulation | Pure functions via TreeMutator |
| Thread Safety | Snapshot state sync | StateFlow atomic updates |

**New Navigator Interface Properties**:
- `state: StateFlow<NavNode>` - Single source of truth
- `transitionState: StateFlow<TransitionState>` - Animation coordination
- `currentDestination: StateFlow<Destination?>` - Derived property
- `previousDestination: StateFlow<Destination?>` - Derived property

**TransitionState Sealed Interface**:
- `Idle(currentState)` - No transition in progress
- `Proposed(exitingState, enteringState, progress, transition)` - Predictive back gesture
- `Animating(exitingState, enteringState, progress, transition)` - Automated animation

**New TreeNavigator Implementation**:
- Full StateFlow-based state management
- Integration with TreeMutator for all operations
- Predictive back support via `updateTransitionProgress()`
- Legacy `BackStack` adapter for backward compatibility

**Pane Navigation API** (from CORE-003-pane-impact-notes):
- `navigateToPane(role, destination, switchFocus, transition)`
- `switchPane(role)`
- `isPaneAvailable(role)`
- `paneContent(role)`
- `navigateBackInPane(role)`
- `clearPane(role)`

**Convenience Extensions**:
- `showInPane()`, `preloadPane()`, `showDetail()`, `showPrimary()`

---

### CORE-004: Implement NavNode Serialization

| Property | Value |
|----------|-------|
| **Complexity** | Medium |
| **Estimated Time** | 2-3 days |
| **Dependencies** | CORE-001 |
| **Blocks** | None |

**Summary**: Comprehensive serialization support using `kotlinx.serialization` for process death survival and state restoration.

**Key Components**:
- `navNodeSerializersModule` - Polymorphic serialization module
- `navNodeJson` - Pre-configured Json instance
- `NavNodeSerializer` object with `toJson()`, `fromJson()`, `fromJsonOrNull()`
- `StateRestoration` interface for platform abstraction
- `InMemoryStateRestoration` for testing

**Platform Implementations**:
- `AndroidStateRestoration` - SavedStateHandle integration
- `IosStateRestoration` - NSUserDefaults (optional)

**Features**:
- `@SerialName` annotations for stable serialization
- Auto-save capability via StateFlow observation
- Custom Destination serializer registration
- Graceful error handling

---

### CORE-005: Comprehensive Unit Test Suite

| Property | Value |
|----------|-------|
| **Complexity** | Medium |
| **Estimated Time** | 3-4 days |
| **Dependencies** | CORE-001, CORE-002, CORE-003, CORE-004 |
| **Blocks** | Phase 2 |

**Summary**: Complete unit test suite for all Phase 1 components ensuring correctness and preventing regressions.

**Test Coverage by Component**:

| Component | Minimum Coverage | Test Focus |
|-----------|-----------------|------------|
| NavNode hierarchy | 90% | Instantiation, validation, properties |
| TreeMutator | 95% | All operations, edge cases |
| TreeNavigator | 85% | Navigation flow, state updates |
| Serialization | 90% | Round-trip, error handling |
| Backward compat | 80% | API equivalence |

**Test Categories**:
- `NavNodeTest.kt` - Node creation, validation, extension functions
- `TreeMutatorPushTest.kt` - Push operations and structural sharing
- `TreeMutatorPopTest.kt` - Pop operations and cascade behavior
- `TreeMutatorTabTest.kt` - Tab switching operations
- `TreeMutatorPaneTest.kt` - Pane manipulation operations
- `TreeNavigatorTest.kt` - Navigator state management
- `NavNodeSerializerTest.kt` - Serialization round-trips
- `BackwardCompatExtensionsTest.kt` - Legacy API compatibility

---

## Key Components/Features to Implement

### Data Structures

```
NavNode (sealed interface)
├── ScreenNode (leaf - holds Destination)
├── StackNode (linear history)
├── TabNode (parallel stacks)
└── PaneNode (role-based adaptive layouts)

Supporting Types:
├── PaneRole (Primary, Supporting, Extra)
├── AdaptStrategy (Hide, Levitate, Reflow)
├── PaneBackBehavior (4 strategies)
├── PaneConfiguration (content + adapt strategy)
└── TransitionState (Idle, Proposed, Animating)
```

### Core Classes

| Class | Purpose |
|-------|---------|
| `TreeMutator` | Pure functional tree transformations |
| `TreeNavigator` | StateFlow-based Navigator implementation |
| `NavNodeSerializer` | JSON serialization utilities |
| `StateRestoration` | Platform-agnostic state persistence |
| `LegacyBackStackAdapter` | Backward compatibility bridge |

### Extension Functions

- Tree traversal: `findByKey()`, `activePathToLeaf()`, `activeLeaf()`, `activeStack()`
- Collection: `allScreens()`, `allPaneNodes()`
- Pane access: `paneForRole()`, `paneContent()`
- Conversion: `toBackStack()`, `toStackNode()`

---

## Dependencies

### Internal Dependencies (Task Order)

```
CORE-001 (NavNode) ──┬──► CORE-002 (TreeMutator) ──► CORE-003 (Navigator)
                     │                                      │
                     └──► CORE-004 (Serialization)         │
                                                           │
                                    All CORE tasks ──────► CORE-005 (Tests)
                                                           │
                                                           ▼
                                                        Phase 2
```

### External Dependencies

| Dependency | Purpose |
|------------|---------|
| `kotlinx.serialization` | JSON serialization |
| `kotlinx.coroutines` | StateFlow, coroutine testing |
| `kotlin("test")` | Unit testing framework |
| `kotlinx-coroutines-test` | Coroutine test utilities |

### Downstream Dependencies

Phase 1 blocks the following in subsequent phases:
- **Phase 2 (Renderer)**: All RENDER-* tasks depend on CORE-003
- **Phase 3 (KSP)**: Depends on NavNode hierarchy from CORE-001
- **Phase 4 (Annotations)**: Maps directly to NavNode types

---

## File References

### Files to Create

| File Path | Task |
|-----------|------|
| `quo-vadis-core/.../core/NavNode.kt` | CORE-001 |
| `quo-vadis-core/.../core/TreeMutator.kt` | CORE-002 |
| `quo-vadis-core/.../core/TreeNavigator.kt` | CORE-003 |
| `quo-vadis-core/.../core/TransitionState.kt` | CORE-003 |
| `quo-vadis-core/.../core/LegacyBackStackAdapter.kt` | CORE-003 |
| `quo-vadis-core/.../serialization/NavNodeSerializer.kt` | CORE-004 |
| `quo-vadis-core/.../serialization/StateRestoration.kt` | CORE-004 |
| `quo-vadis-core/src/androidMain/.../AndroidStateRestoration.kt` | CORE-004 |
| `quo-vadis-core/src/iosMain/.../IosStateRestoration.kt` | CORE-004 (optional) |
| All test files in `commonTest/` | CORE-005 |

### Files to Modify

| File Path | Task | Changes |
|-----------|------|---------|
| `quo-vadis-core/.../core/Navigator.kt` | CORE-003 | Add tree-based properties, deprecate legacy |
| `quo-vadis-core/build.gradle.kts` | CORE-001, CORE-005 | Add serialization plugin, test dependencies |

### Reference Files (Existing)

| File Path | Purpose |
|-----------|---------|
| `quo-vadis-core/.../core/Destination.kt` | Current Destination interface |
| `quo-vadis-core/.../core/Navigator.kt` | Current Navigator implementation |

---

## Estimated Effort

| Task | Complexity | Time Estimate |
|------|------------|---------------|
| CORE-001 | Medium | 2-3 days |
| CORE-002 | High | 3-4 days |
| CORE-003 | High | 4-5 days |
| CORE-004 | Medium | 2-3 days |
| CORE-005 | Medium | 3-4 days |
| **Total** | | **14-19 days** |

### Risk Factors

- TreeNavigator integration with existing codebase
- Cross-platform serialization compatibility
- Performance of deep tree traversals
- Backward compatibility adapter edge cases

---

## Acceptance Criteria Summary

### CORE-001 (NavNode Hierarchy)
- [ ] All node types defined with proper validation
- [ ] Serialization annotations in place
- [ ] Extension functions for tree traversal
- [ ] KDoc documentation complete

### CORE-002 (TreeMutator)
- [ ] All push/pop/switch operations implemented
- [ ] Pane-specific operations for role-based navigation
- [ ] Pure functions with no side effects
- [ ] Structural sharing verified

### CORE-003 (Navigator Refactor)
- [ ] `StateFlow<NavNode>` as single source of truth
- [ ] TransitionState for animation coordination
- [ ] All TreeMutator operations integrated
- [ ] Pane navigation API complete
- [ ] Legacy BackStack adapter functional

### CORE-004 (Serialization)
- [ ] Polymorphic serialization working
- [ ] Round-trip integrity verified
- [ ] Platform-specific restoration implemented
- [ ] Error handling graceful

### CORE-005 (Unit Tests)
- [ ] Coverage targets met
- [ ] All platforms passing
- [ ] Edge cases documented and tested
- [ ] CI integration configured

---

## References

- [Original Architecture Plan](../../Refactoring%20Quo-Vadis%20Navigation%20Architecture.md)
- [Phase Index](../INDEX.md)
- [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization)
- [Android SavedStateHandle](https://developer.android.com/topic/libraries/architecture/viewmodel-savedstate)
