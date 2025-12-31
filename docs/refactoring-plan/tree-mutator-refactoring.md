# TreeMutator Refactoring Plan

## Overview

The `TreeMutator` object (~1,770 lines, ~40+ functions) has grown beyond maintainable limits and violates several SOLID principles. This document outlines a refactoring strategy to decompose it into smaller, focused components while preserving the functional/immutable nature of tree operations.

## Current Issues

### 1. Size & Complexity Violations
| Metric | Current | Detekt Limit |
|--------|---------|--------------|
| Lines of Code | ~1,770 | 600 |
| Functions | ~40+ | 11 |
| Responsibilities | 6+ distinct areas | 1 (SRP) |

### 2. SOLID Violations

| Principle | Violation |
|-----------|-----------|
| **S**ingle Responsibility | Handles push, pop, tab, pane, utility, and back operations |
| **O**pen/Closed | Adding new node types requires modifying TreeMutator |
| **I**nterface Segregation | All operations bundled in one object |
| **D**ependency Inversion | Direct coupling between operation categories |

### 3. Code Organization Issues
- Nested sealed classes (`PopResult`, `BackResult`, `PushStrategy`) are internal details exposed at module level
- Configuration enums mixed with operations
- Utility functions (`replaceNode`, `removeNode`) used by all categories but not separated
- Similar patterns repeated across different node types (StackNode, TabNode, PaneNode)

---

## Refactoring Goals

1. **Single Responsibility**: Each class handles one category of operations
2. **Extensibility**: Easy to add new node types without modifying existing code
3. **Testability**: Smaller units are easier to test in isolation
4. **Discoverability**: Clear naming and organization for API consumers
5. **Kotlin Idioms**: Leverage extension functions, sealed hierarchies, and DSLs

---

## Proposed Architecture

### Package Structure

```
com.jermey.quo.vadis.core.navigation.tree/
├── TreeMutator.kt              # Façade (delegates to specialized mutators)
├── result/                     # Result types
│   ├── PopResult.kt
│   ├── BackResult.kt
│   └── PushStrategy.kt
├── config/                     # Configuration types
│   └── PopBehavior.kt
├── operations/                 # Specialized operation classes
│   ├── TreeNodeOperations.kt   # Core tree manipulation (replaceNode, removeNode)
│   ├── PushOperations.kt       # All push-related operations
│   ├── PopOperations.kt        # All pop-related operations
│   ├── TabOperations.kt        # Tab switching operations
│   ├── PaneOperations.kt       # Pane navigation operations
│   └── BackOperations.kt       # Tree-aware back handling
└── util/                       # Utilities
    └── KeyGenerator.kt         # Key generation abstraction
```

### Component Responsibilities

#### 1. `TreeNodeOperations.kt` (~100 lines)
**Purpose**: Core tree manipulation utilities used by all other operations.

```kotlin
/**
 * Core tree manipulation utilities.
 * 
 * Provides foundational operations for modifying the NavNode tree:
 * - Node replacement (structural sharing)
 * - Node removal
 * - Node lookup helpers
 */
object TreeNodeOperations {
    fun replaceNode(root: NavNode, targetKey: String, newNode: NavNode): NavNode
    fun removeNode(root: NavNode, targetKey: String): NavNode?
}
```

**Functions to extract**:
- `replaceNode()` 
- `removeNode()`

---

#### 2. `PushOperations.kt` (~300 lines)
**Purpose**: All navigation push operations.

```kotlin
/**
 * Push operations for the navigation tree.
 * 
 * Handles all forward navigation:
 * - Simple push to active stack
 * - Push to specific stack by key
 * - Scope-aware push with tab switching
 * - Pane role routing
 * - Multi-destination push
 * - Clear and push patterns
 */
object PushOperations {
    // Simple push operations
    fun push(root: NavNode, destination: NavDestination, generateKey: () -> String): NavNode
    fun pushToStack(root: NavNode, stackKey: String, destination: NavDestination, generateKey: () -> String): NavNode
    
    // Scope-aware operations
    fun push(
        root: NavNode,
        destination: NavDestination,
        scopeRegistry: ScopeRegistry,
        paneRoleRegistry: PaneRoleRegistry,
        generateKey: () -> String
    ): NavNode
    
    // Batch operations
    fun pushAll(root: NavNode, destinations: List<NavDestination>, generateKey: () -> String): NavNode
    
    // Clear patterns
    fun clearAndPush(root: NavNode, destination: NavDestination, generateKey: () -> String): NavNode
    fun clearStackAndPush(root: NavNode, stackKey: String, destination: NavDestination, generateKey: () -> String): NavNode
    fun replaceCurrent(root: NavNode, destination: NavDestination, generateKey: () -> String): NavNode
}

// Internal helpers (private)
private fun determinePushStrategy(...): PushStrategy
private fun findTabWithDestination(...): Int?
private fun pushToActiveStack(...): NavNode
private fun pushOutOfScope(...): NavNode
private fun pushToPaneStack(...): NavNode
```

**Functions to extract**:
- `push()` (simple)
- `pushToStack()`
- `push()` (scope-aware)
- `determinePushStrategy()` (private)
- `findTabWithDestination()` (private)
- `pushToActiveStack()` (private)
- `pushOutOfScope()` (private)
- `pushToPaneStack()` (private)
- `pushAll()`
- `clearAndPush()`
- `clearStackAndPush()`
- `replaceCurrent()`

---

#### 3. `PopOperations.kt` (~200 lines)
**Purpose**: All navigation pop operations.

```kotlin
/**
 * Pop operations for the navigation tree.
 * 
 * Handles all backward navigation from stacks:
 * - Simple pop from active stack
 * - Pop to predicate/route/destination type
 * - Configurable empty stack behavior
 */
object PopOperations {
    fun pop(root: NavNode, behavior: PopBehavior = PopBehavior.PRESERVE_EMPTY): NavNode?
    fun popTo(root: NavNode, inclusive: Boolean = false, predicate: (NavNode) -> Boolean): NavNode
    fun popToRoute(root: NavNode, route: String, inclusive: Boolean = false): NavNode
    inline fun <reified D : NavDestination> popToDestination(root: NavNode, inclusive: Boolean = false): NavNode
}

// Internal helpers
private fun handleEmptyStackPop(root: NavNode, emptyStack: StackNode, behavior: PopBehavior): NavNode?
```

**Functions to extract**:
- `pop()`
- `popTo()`
- `popToRoute()`
- `popToDestination()`
- `handleEmptyStackPop()` (private)

---

#### 4. `TabOperations.kt` (~80 lines)
**Purpose**: Tab switching operations.

```kotlin
/**
 * Tab switching operations for TabNode.
 * 
 * Handles tab navigation:
 * - Switch to tab by index and key
 * - Switch active tab in path
 */
object TabOperations {
    fun switchTab(root: NavNode, tabNodeKey: String, newIndex: Int): NavNode
    fun switchActiveTab(root: NavNode, newIndex: Int): NavNode
}
```

**Functions to extract**:
- `switchTab()`
- `switchActiveTab()`

---

#### 5. `PaneOperations.kt` (~350 lines)
**Purpose**: All pane-related navigation operations.

```kotlin
/**
 * Pane navigation operations for PaneNode.
 * 
 * Handles adaptive layout navigation:
 * - Navigate to specific pane
 * - Switch active pane
 * - Pop from pane with behavior awareness
 * - Pane configuration management
 */
object PaneOperations {
    // Navigation
    fun navigateToPane(
        root: NavNode,
        nodeKey: String,
        role: PaneRole,
        destination: NavDestination,
        switchFocus: Boolean = true,
        generateKey: () -> String
    ): NavNode
    
    // Pane management
    fun switchActivePane(root: NavNode, nodeKey: String, role: PaneRole): NavNode
    fun popPane(root: NavNode, nodeKey: String, role: PaneRole): NavNode?
    
    // Behavior-aware pop
    fun popWithPaneBehavior(root: NavNode): PopResult
    fun popPaneAdaptive(root: NavNode, isCompact: Boolean): PopResult
    
    // Configuration
    fun setPaneConfiguration(root: NavNode, nodeKey: String, role: PaneRole, config: PaneConfiguration): NavNode
    fun removePaneConfiguration(root: NavNode, nodeKey: String, role: PaneRole): NavNode
}

// Internal helpers
private fun popFromActivePane(root: NavNode, paneNode: PaneNode): PopResult
private fun clearPaneStack(root: NavNode, paneNodeKey: String, role: PaneRole): NavNode
```

**Functions to extract**:
- `navigateToPane()`
- `switchActivePane()`
- `popPane()`
- `popWithPaneBehavior()`
- `popPaneAdaptive()`
- `popFromActivePane()` (private)
- `clearPaneStack()` (private)
- `setPaneConfiguration()`
- `removePaneConfiguration()`

---

#### 6. `BackOperations.kt` (~300 lines)
**Purpose**: Tree-aware back navigation handling.

```kotlin
/**
 * Tree-aware back navigation operations.
 * 
 * Handles intelligent back navigation:
 * - Tab-aware back with cascade behavior
 * - Pane-aware back with window size consideration
 * - Back capability checking
 */
object BackOperations {
    fun popWithTabBehavior(root: NavNode, isCompact: Boolean = true): BackResult
    fun canGoBack(root: NavNode): Boolean
    fun canHandleBackNavigation(root: NavNode): Boolean
    fun currentDestination(root: NavNode): NavDestination?
}

// Internal helpers (complex back handling logic)
private fun handleRootStackBack(root: NavNode, activeStack: StackNode): BackResult
private fun handleTabBack(root: NavNode, tabNode: TabNode): BackResult
private fun handleNestedStackBack(root: NavNode, parentStack: StackNode, childStack: StackNode): BackResult
private fun handlePaneBack(root: NavNode, paneNode: PaneNode, isCompact: Boolean): BackResult
private fun popEntirePaneNode(root: NavNode, paneNode: PaneNode): BackResult
```

**Functions to extract**:
- `popWithTabBehavior()`
- `canGoBack()`
- `canHandleBackNavigation()`
- `currentDestination()`
- `handleRootStackBack()` (private)
- `handleTabBack()` (private)
- `handleNestedStackBack()` (private)
- `handlePaneBack()` (private)
- `popEntirePaneNode()` (private)

---

#### 7. Result Types (Separate Files)

**`result/PopResult.kt`**:
```kotlin
/**
 * Result of a pop operation that respects PaneBackBehavior.
 */
sealed class PopResult {
    data class Popped(val newState: NavNode) : PopResult()
    data class PaneEmpty(val paneRole: PaneRole) : PopResult()
    data object CannotPop : PopResult()
    data object RequiresScaffoldChange : PopResult()
}
```

**`result/BackResult.kt`**:
```kotlin
/**
 * Result of a tree-aware back operation.
 */
sealed class BackResult {
    data class Handled(val newState: NavNode) : BackResult()
    data object DelegateToSystem : BackResult()
    data object CannotHandle : BackResult()
}
```

**`result/PushStrategy.kt`** (internal):
```kotlin
/**
 * Internal strategy for push operations with tab/pane awareness.
 */
internal sealed class PushStrategy {
    data class PushToStack(val targetStack: StackNode) : PushStrategy()
    data class SwitchToTab(val tabNode: TabNode, val tabIndex: Int) : PushStrategy()
    data class PushToPaneStack(val paneNode: PaneNode, val role: PaneRole) : PushStrategy()
    data class PushOutOfScope(val parentStack: StackNode) : PushStrategy()
}
```

---

#### 8. Configuration Types

**`config/PopBehavior.kt`**:
```kotlin
/**
 * Configures behavior when a StackNode becomes empty after pop.
 */
enum class PopBehavior {
    /** Remove the empty stack from parent (cascading pop). */
    CASCADE,
    
    /** Preserve the empty stack in place. */
    PRESERVE_EMPTY
}
```

---

#### 9. Key Generator Abstraction

**`util/KeyGenerator.kt`**:
```kotlin
/**
 * Abstraction for generating unique node keys.
 */
fun interface KeyGenerator {
    fun generate(): String
    
    companion object {
        @OptIn(ExperimentalUuidApi::class)
        val Default: KeyGenerator = KeyGenerator { Uuid.random().toString().take(8) }
    }
}
```

---

#### 10. Façade Pattern - `TreeMutator.kt` (~150 lines)

The original `TreeMutator` becomes a façade that delegates to specialized operations:

```kotlin
/**
 * Pure functional operations for manipulating the NavNode tree.
 *
 * This object provides a unified API for all tree mutations. Internally,
 * operations are delegated to specialized handlers:
 * - [PushOperations] - forward navigation
 * - [PopOperations] - backward navigation
 * - [TabOperations] - tab switching
 * - [PaneOperations] - pane navigation
 * - [BackOperations] - tree-aware back handling
 *
 * All operations are immutable - they return new tree instances rather than
 * modifying existing ones.
 *
 * @see PushOperations
 * @see PopOperations
 * @see TabOperations
 * @see PaneOperations
 * @see BackOperations
 */
object TreeMutator {
    
    // Re-export types for backward compatibility
    @Deprecated("Use PopBehavior directly", ReplaceWith("PopBehavior"))
    typealias PopBehavior = com.jermey.quo.vadis.core.navigation.tree.config.PopBehavior
    
    // =========================================================================
    // PUSH OPERATIONS (delegate to PushOperations)
    // =========================================================================
    
    fun push(
        root: NavNode,
        destination: NavDestination,
        generateKey: () -> String = KeyGenerator.Default::generate
    ): NavNode = PushOperations.push(root, destination, generateKey)
    
    fun pushToStack(
        root: NavNode,
        stackKey: String,
        destination: NavDestination,
        generateKey: () -> String = KeyGenerator.Default::generate
    ): NavNode = PushOperations.pushToStack(root, stackKey, destination, generateKey)
    
    fun push(
        root: NavNode,
        destination: NavDestination,
        scopeRegistry: ScopeRegistry,
        paneRoleRegistry: PaneRoleRegistry = PaneRoleRegistry.Empty,
        generateKey: () -> String = KeyGenerator.Default::generate
    ): NavNode = PushOperations.push(root, destination, scopeRegistry, paneRoleRegistry, generateKey)
    
    fun pushAll(
        root: NavNode,
        destinations: List<NavDestination>,
        generateKey: () -> String = KeyGenerator.Default::generate
    ): NavNode = PushOperations.pushAll(root, destinations, generateKey)
    
    fun clearAndPush(
        root: NavNode,
        destination: NavDestination,
        generateKey: () -> String = KeyGenerator.Default::generate
    ): NavNode = PushOperations.clearAndPush(root, destination, generateKey)
    
    fun clearStackAndPush(
        root: NavNode,
        stackKey: String,
        destination: NavDestination,
        generateKey: () -> String = KeyGenerator.Default::generate
    ): NavNode = PushOperations.clearStackAndPush(root, stackKey, destination, generateKey)
    
    fun replaceCurrent(
        root: NavNode,
        destination: NavDestination,
        generateKey: () -> String = KeyGenerator.Default::generate
    ): NavNode = PushOperations.replaceCurrent(root, destination, generateKey)
    
    // =========================================================================
    // POP OPERATIONS (delegate to PopOperations)
    // =========================================================================
    
    fun pop(
        root: NavNode,
        behavior: PopBehavior = PopBehavior.PRESERVE_EMPTY
    ): NavNode? = PopOperations.pop(root, behavior)
    
    fun popTo(
        root: NavNode,
        inclusive: Boolean = false,
        predicate: (NavNode) -> Boolean
    ): NavNode = PopOperations.popTo(root, inclusive, predicate)
    
    fun popToRoute(
        root: NavNode,
        route: String,
        inclusive: Boolean = false
    ): NavNode = PopOperations.popToRoute(root, route, inclusive)
    
    inline fun <reified D : NavDestination> popToDestination(
        root: NavNode,
        inclusive: Boolean = false
    ): NavNode = PopOperations.popToDestination<D>(root, inclusive)
    
    // =========================================================================
    // TAB OPERATIONS (delegate to TabOperations)
    // =========================================================================
    
    fun switchTab(
        root: NavNode,
        tabNodeKey: String,
        newIndex: Int
    ): NavNode = TabOperations.switchTab(root, tabNodeKey, newIndex)
    
    fun switchActiveTab(root: NavNode, newIndex: Int): NavNode = 
        TabOperations.switchActiveTab(root, newIndex)
    
    // =========================================================================
    // PANE OPERATIONS (delegate to PaneOperations)
    // =========================================================================
    
    fun navigateToPane(
        root: NavNode,
        nodeKey: String,
        role: PaneRole,
        destination: NavDestination,
        switchFocus: Boolean = true,
        generateKey: () -> String = KeyGenerator.Default::generate
    ): NavNode = PaneOperations.navigateToPane(root, nodeKey, role, destination, switchFocus, generateKey)
    
    fun switchActivePane(
        root: NavNode,
        nodeKey: String,
        role: PaneRole
    ): NavNode = PaneOperations.switchActivePane(root, nodeKey, role)
    
    fun popPane(
        root: NavNode,
        nodeKey: String,
        role: PaneRole
    ): NavNode? = PaneOperations.popPane(root, nodeKey, role)
    
    fun popWithPaneBehavior(root: NavNode): PopResult = 
        PaneOperations.popWithPaneBehavior(root)
    
    fun popPaneAdaptive(root: NavNode, isCompact: Boolean): PopResult = 
        PaneOperations.popPaneAdaptive(root, isCompact)
    
    fun setPaneConfiguration(
        root: NavNode,
        nodeKey: String,
        role: PaneRole,
        config: PaneConfiguration
    ): NavNode = PaneOperations.setPaneConfiguration(root, nodeKey, role, config)
    
    fun removePaneConfiguration(
        root: NavNode,
        nodeKey: String,
        role: PaneRole
    ): NavNode = PaneOperations.removePaneConfiguration(root, nodeKey, role)
    
    // =========================================================================
    // UTILITY OPERATIONS (delegate to TreeNodeOperations)
    // =========================================================================
    
    fun replaceNode(root: NavNode, targetKey: String, newNode: NavNode): NavNode = 
        TreeNodeOperations.replaceNode(root, targetKey, newNode)
    
    fun removeNode(root: NavNode, targetKey: String): NavNode? = 
        TreeNodeOperations.removeNode(root, targetKey)
    
    // =========================================================================
    // BACK OPERATIONS (delegate to BackOperations)
    // =========================================================================
    
    fun popWithTabBehavior(root: NavNode, isCompact: Boolean = true): BackResult = 
        BackOperations.popWithTabBehavior(root, isCompact)
    
    fun canGoBack(root: NavNode): Boolean = BackOperations.canGoBack(root)
    
    fun canHandleBackNavigation(root: NavNode): Boolean = 
        BackOperations.canHandleBackNavigation(root)
    
    fun currentDestination(root: NavNode): NavDestination? = 
        BackOperations.currentDestination(root)
}
```

---

## Alternative: Extension Functions Approach

For a more Kotlin-idiomatic API, operations could also be exposed as extension functions on `NavNode`:

```kotlin
// In NavNodeExtensions.kt
fun NavNode.push(destination: NavDestination): NavNode = 
    PushOperations.push(this, destination, KeyGenerator.Default::generate)

fun NavNode.pop(behavior: PopBehavior = PopBehavior.PRESERVE_EMPTY): NavNode? = 
    PopOperations.pop(this, behavior)

fun NavNode.switchTab(tabNodeKey: String, newIndex: Int): NavNode = 
    TabOperations.switchTab(this, tabNodeKey, newIndex)

// Usage becomes:
val newTree = root
    .push(HomeDestination)
    .push(DetailDestination("123"))
    ?.pop()
```

**Trade-off**: Extension functions are more discoverable but may pollute the `NavNode` namespace. The façade approach preserves the current API while enabling better organization internally.

---

## Implementation Strategy

### Phase 1: Extract Types (Low Risk)
1. Create `config/PopBehavior.kt` - move enum
2. Create `result/PopResult.kt` - move sealed class
3. Create `result/BackResult.kt` - move sealed class  
4. Create `result/PushStrategy.kt` (internal) - move sealed class
5. Create `util/KeyGenerator.kt` - extract key generation
6. Add `@Deprecated` typealiases in `TreeMutator` for backward compatibility

### Phase 2: Extract Core Operations (Medium Risk)
1. Create `TreeNodeOperations.kt` with `replaceNode()` and `removeNode()`
2. Update all usages to call `TreeNodeOperations` directly
3. Keep delegating methods in `TreeMutator` façade

### Phase 3: Extract Feature Operations (Medium Risk)
Extract in order of independence (least dependencies first):
1. `TabOperations.kt` - minimal dependencies
2. `PopOperations.kt` - depends on TreeNodeOperations
3. `PushOperations.kt` - depends on TreeNodeOperations, TabOperations
4. `PaneOperations.kt` - depends on TreeNodeOperations, PopOperations
5. `BackOperations.kt` - depends on all others

### Phase 4: Cleanup & Documentation
1. Remove implementation code from `TreeMutator.kt` (keep only delegations)
2. Update KDoc with cross-references to specialized classes
3. Update architecture documentation
4. Add unit tests for each operation class

---

## Dependency Graph

```
                    TreeMutator (Façade)
                          │
     ┌────────────────────┼────────────────────┐
     │                    │                    │
     ▼                    ▼                    ▼
PushOperations      PopOperations       TabOperations
     │                    │                    
     │                    │                    
     ├────────────────────┼────────────────────┐
     │                    │                    │
     ▼                    ▼                    ▼
PaneOperations     BackOperations      TreeNodeOperations
     │                    │                    ▲
     │                    │                    │
     └────────────────────┴────────────────────┘
```

---

## Metrics After Refactoring

| File | Estimated Lines | Functions |
|------|-----------------|-----------|
| TreeMutator.kt (façade) | ~150 | ~25 (delegates) |
| TreeNodeOperations.kt | ~100 | 2 |
| PushOperations.kt | ~300 | 12 |
| PopOperations.kt | ~200 | 5 |
| TabOperations.kt | ~80 | 2 |
| PaneOperations.kt | ~350 | 11 |
| BackOperations.kt | ~300 | 9 |
| Result types | ~50 | - |
| Config types | ~20 | - |
| **Total** | **~1,550** | **~66** |

**Benefits**:
- Each file under 400 lines ✅
- Functions per class under 12 ✅
- Clear single responsibility per file ✅
- Improved testability ✅
- Better code navigation ✅

---

## Backward Compatibility

The refactoring maintains **100% API compatibility**:

1. **TreeMutator façade** keeps all existing method signatures
2. **Type aliases** with `@Deprecated` annotations guide migration
3. **Import changes** only needed for direct type usage (`PopResult`, `BackResult`)

```kotlin
// Before (still works)
val result = TreeMutator.push(root, destination)
val canPop = TreeMutator.canGoBack(root)

// After (also works)
val result = PushOperations.push(root, destination, KeyGenerator.Default::generate)
val canPop = BackOperations.canGoBack(root)

// Kotlin extensions (optional new API)
val result = root.push(destination)
val canPop = root.canGoBack()
```

---

## Testing Strategy

### Unit Tests per Component

Each operation class should have dedicated tests:

```
test/
├── TreeNodeOperationsTest.kt
├── PushOperationsTest.kt
├── PopOperationsTest.kt
├── TabOperationsTest.kt
├── PaneOperationsTest.kt
└── BackOperationsTest.kt
```

### Integration Tests

Keep existing `TreeMutatorTest.kt` as integration tests to ensure the façade works correctly with all components.

---

## Timeline Estimate

| Phase | Effort | Risk |
|-------|--------|------|
| Phase 1: Extract Types | 2 hours | Low |
| Phase 2: Extract Core Ops | 3 hours | Medium |
| Phase 3: Extract Feature Ops | 6 hours | Medium |
| Phase 4: Cleanup & Docs | 2 hours | Low |
| **Total** | **~13 hours** | |

---

## Open Questions

1. **Extension functions vs object methods**: Should the new API favor extension functions on `NavNode`?
   - Pros: More Kotlin-idiomatic, better discoverability
   - Cons: Namespace pollution, harder to mock in tests

2. **Internal visibility**: Should specialized operations be `internal`?
   - Current proposal: Public for direct access, but façade is the recommended API
   - Alternative: Make operations internal, only expose through façade

3. **Key generation**: Should `KeyGenerator` be injectable through a context object?
   - Would allow custom key generation strategies
   - Adds complexity for minimal gain

---

## Conclusion

This refactoring plan transforms a 1,770-line monolithic object into 7+ focused components, each with a single responsibility. The façade pattern ensures backward compatibility while enabling better organization, testability, and maintainability.

The functional/immutable nature of all operations is preserved - this is purely a structural refactoring to improve code organization without changing behavior.
