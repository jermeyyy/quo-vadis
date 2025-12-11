# Scoped Navigation Refactoring Plan

## Executive Summary

This document outlines the refactoring plan to fix the navigation scope issue where destinations outside a tab/pane container's scope are incorrectly rendered inside the container's wrapper instead of taking the full screen.

**Issue**: When navigating from `MainTabs.HomeTab` to `MasterDetailDestination.List`, the new screen renders inside the TabWrapper content slot instead of as a full-screen destination.

**Solution**: Implement scope-aware navigation that determines whether a destination belongs to the current container's scope. If out of scope, navigation should push to the parent stack, preserving the container for predictive back gestures.

---

## Problem Analysis

### Current Behavior

When a user is on `MainTabs.HomeTab` and navigates to `MasterDetailDestination.List`:

```
Before Navigation:
StackNode (root)
  └── TabNode (MainTabs)
       ├── StackNode (HomeTab) ← ACTIVE
       │     └── ScreenNode (HomeDestination)
       └── ...other tabs

After Navigation (CURRENT - BROKEN):
StackNode (root)
  └── TabNode (MainTabs)
       ├── StackNode (HomeTab) ← still active
       │     ├── ScreenNode (HomeDestination)
       │     └── ScreenNode (MasterDetailDestination.List) ← WRONG!
       └── ...other tabs
```

The `TreeMutator.push()` function finds the deepest active stack (HomeTab's stack) and pushes there regardless of destination scope.

### Desired Behavior

```
After Navigation (DESIRED):
StackNode (root)
  ├── TabNode (MainTabs) ← cached, not active
  │    ├── StackNode (HomeTab)
  │    │     └── ScreenNode (HomeDestination)
  │    └── ...other tabs
  └── StackNode (master_detail) ← NEW, active
       └── ScreenNode (MasterDetailDestination.List)
```

The new destination creates a sibling to the TabNode, preserving the tab state for back navigation.

### Scope Determination Rules

1. **In-Scope Navigation** (within container):
   - Destination class is a member/subclass of the container's destination sealed class
   - Example: `MainTabs.SettingsTab` is in scope of `MainTabs` TabNode

2. **Out-of-Scope Navigation** (outside container):
   - Destination class is NOT a member/subclass of the container's destination sealed class
   - Example: `MasterDetailDestination.List` is out of scope of `MainTabs` TabNode

---

## Architecture Design

### New Component: ScopeRegistry

A new registry interface to determine destination scope membership:

```kotlin
// quo-vadis-core/src/commonMain/kotlin/.../core/ScopeRegistry.kt

/**
 * Registry for determining navigation scope membership.
 * 
 * Used by [TreeMutator] to decide whether a destination belongs to
 * a container's scope (TabNode/PaneNode) or should navigate outside.
 */
public interface ScopeRegistry {
    /**
     * Checks if a destination belongs to the scope identified by [scopeKey].
     * 
     * @param scopeKey The container's scope identifier (e.g., "MainTabs")
     * @param destination The destination to check
     * @return true if destination is in scope, false otherwise
     */
    fun isInScope(scopeKey: String, destination: Destination): Boolean
    
    /**
     * Gets the scope key for a destination, if it belongs to any registered scope.
     * 
     * @param destination The destination to look up
     * @return The scope key, or null if destination doesn't belong to any scope
     */
    fun getScopeKey(destination: Destination): String?
    
    companion object {
        /**
         * Empty registry that always returns false (no scope restrictions).
         * Used as default for backward compatibility.
         */
        val Empty: ScopeRegistry = object : ScopeRegistry {
            override fun isInScope(scopeKey: String, destination: Destination) = true
            override fun getScopeKey(destination: Destination): String? = null
        }
    }
}
```

### NavNode Metadata Enhancement

Add scope key to `TabNode` and `PaneNode`:

```kotlin
// TabNode enhancement
data class TabNode(
    override val key: String,
    override val parentKey: String?,
    val stacks: List<StackNode>,
    val activeStackIndex: Int = 0,
    val wrapperKey: String? = null,
    val tabMetadata: List<GeneratedTabMetadata> = emptyList(),
    val scopeKey: String? = null  // NEW: Scope identifier for scope checking
) : NavNode

// PaneNode enhancement
data class PaneNode(
    override val key: String,
    override val parentKey: String?,
    val paneConfigurations: Map<PaneRole, PaneConfiguration>,
    val activePaneRole: PaneRole,
    val backBehavior: PaneBackBehavior = PaneBackBehavior.PopUntilScaffoldValueChange,
    val scopeKey: String? = null  // NEW: Scope identifier for scope checking
) : NavNode
```

### TreeMutator Enhancement

Modify `push()` to be scope-aware:

```kotlin
// TreeMutator.kt

/**
 * Push a destination with scope awareness.
 * 
 * If the destination is out of the active container's scope,
 * pushes to the parent stack instead of the deepest active stack.
 */
fun push(
    root: NavNode,
    destination: Destination,
    scopeRegistry: ScopeRegistry = ScopeRegistry.Empty,
    generateKey: () -> String = { Uuid.random().toString().take(8) }
): NavNode {
    // Find the target stack considering scope
    val targetStack = findTargetStackForPush(root, destination, scopeRegistry)
        ?: throw IllegalStateException("No suitable stack found for navigation")
    
    // If pushing to a different stack than the deepest active,
    // we need to create a new StackNode for the destination
    val deepestActiveStack = root.activeStack()
    
    return if (targetStack == deepestActiveStack) {
        // In-scope: push directly to active stack
        pushToActiveStack(root, destination, generateKey)
    } else {
        // Out-of-scope: push to parent stack with new StackNode
        pushOutOfScope(root, targetStack, destination, generateKey)
    }
}

/**
 * Finds the appropriate target stack for a destination considering scope.
 */
private fun findTargetStackForPush(
    root: NavNode,
    destination: Destination,
    scopeRegistry: ScopeRegistry
): StackNode? {
    // Walk up the tree from deepest active stack
    val activeStack = root.activeStack() ?: return null
    var currentParentKey = activeStack.parentKey
    
    // Check if current container (TabNode/PaneNode) allows this destination
    while (currentParentKey != null) {
        val parentNode = root.findByKey(currentParentKey) ?: break
        
        when (parentNode) {
            is TabNode -> {
                val scopeKey = parentNode.scopeKey
                if (scopeKey != null && !scopeRegistry.isInScope(scopeKey, destination)) {
                    // Out of scope - find parent stack
                    return findParentStack(root, parentNode)
                }
                // In scope - continue with active stack
                return activeStack
            }
            is PaneNode -> {
                val scopeKey = parentNode.scopeKey
                if (scopeKey != null && !scopeRegistry.isInScope(scopeKey, destination)) {
                    // Out of scope - find parent stack
                    return findParentStack(root, parentNode)
                }
                return activeStack
            }
            is StackNode -> {
                // Continue up the tree
                currentParentKey = parentNode.parentKey
            }
            else -> break
        }
    }
    
    return activeStack
}

/**
 * Pushes a destination outside the current container's scope.
 * Creates a new StackNode as sibling to the container.
 */
private fun pushOutOfScope(
    root: NavNode,
    parentStack: StackNode,
    destination: Destination,
    generateKey: () -> String
): NavNode {
    val stackKey = generateKey()
    val screenKey = generateKey()
    
    // Create new stack with the destination
    val newStack = StackNode(
        key = stackKey,
        parentKey = parentStack.key,
        children = listOf(
            ScreenNode(
                key = screenKey,
                parentKey = stackKey,
                destination = destination
            )
        )
    )
    
    // Add new stack to parent stack's children
    val updatedParentStack = parentStack.copy(
        children = parentStack.children + newStack
    )
    
    return replaceNode(root, parentStack.key, updatedParentStack)
}
```

### TreeNavigator Enhancement

Update `TreeNavigator` to use `ScopeRegistry`:

```kotlin
class TreeNavigator(
    private val deepLinkHandler: DeepLinkHandler? = null,
    private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
    private val scopeRegistry: ScopeRegistry = ScopeRegistry.Empty  // NEW
) : Navigator {
    
    override fun navigate(destination: Destination, transition: NavigationTransition?) {
        // ... existing code ...
        val newState = TreeMutator.push(
            oldState, 
            destination, 
            scopeRegistry  // Pass scope registry
        ) { generateKey() }
        // ... rest of existing code ...
    }
}
```

### NavigationHost Enhancement

Pass `ScopeRegistry` to the navigation host:

```kotlin
@Composable
public fun NavigationHost(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    screenRegistry: ScreenRegistry = EmptyScreenRegistry,
    wrapperRegistry: WrapperRegistry = WrapperRegistry.Empty,
    transitionRegistry: TransitionRegistry = TransitionRegistry.Empty,
    scopeRegistry: ScopeRegistry = ScopeRegistry.Empty,  // NEW
    enablePredictiveBack: Boolean = true
) {
    // ... existing setup ...
    
    // Ensure navigator has scope registry (if TreeNavigator)
    LaunchedEffect(scopeRegistry) {
        (navigator as? TreeNavigator)?.setScopeRegistry(scopeRegistry)
    }
    
    // ... rest of existing code ...
}
```

---

## KSP Code Generation Changes

### 1. New Generator: ScopeRegistryGenerator

```kotlin
// quo-vadis-ksp/.../generators/ScopeRegistryGenerator.kt

/**
 * Generates ScopeRegistry implementation from @Tabs and @Panes annotations.
 */
class ScopeRegistryGenerator {
    fun generate(
        tabs: List<TabInfo>,
        panes: List<PaneInfo>,
        stacks: List<StackInfo>
    ): FileSpec {
        // Generate registry that maps:
        // scopeKey -> list of destination KClasses in scope
        
        return FileSpec.builder(packageName, "GeneratedScopeRegistry")
            .addType(
                TypeSpec.objectBuilder("GeneratedScopeRegistry")
                    .addSuperinterface(ScopeRegistry::class)
                    .addFunction(generateIsInScope(tabs, panes))
                    .addFunction(generateGetScopeKey(tabs, panes))
                    .addProperty(generateScopeMappings(tabs, panes))
                    .build()
            )
            .build()
    }
}
```

### 2. Generated Code Example

For `MainTabs`:

```kotlin
// Generated file: GeneratedScopeRegistry.kt

public object GeneratedScopeRegistry : ScopeRegistry {
    
    private val scopeMappings: Map<String, Set<KClass<out Destination>>> = mapOf(
        "MainTabs" to setOf(
            MainTabs.HomeTab::class,
            MainTabs.ExploreTab::class,
            MainTabs.ProfileTab::class,
            MainTabs.SettingsTab::class,
            MainTabs.SettingsTab.SettingsMain::class
        ),
        "MasterDetailDestination" to setOf(
            MasterDetailDestination.List::class,
            MasterDetailDestination.Detail::class
        )
        // ... other stacks
    )
    
    override fun isInScope(scopeKey: String, destination: Destination): Boolean {
        val scopeClasses = scopeMappings[scopeKey] ?: return true
        return scopeClasses.any { it.isInstance(destination) }
    }
    
    override fun getScopeKey(destination: Destination): String? {
        return scopeMappings.entries.find { (_, classes) ->
            classes.any { it.isInstance(destination) }
        }?.key
    }
}
```

### 3. NavNodeBuilder Updates

Update `NavNodeBuilderGenerator` to include `scopeKey` in generated TabNode/PaneNode builders:

```kotlin
// Generated buildMainTabsNode():
fun buildMainTabsNode(
    parentKey: String? = null,
    generateKey: () -> String = { ... }
): TabNode {
    val tabNodeKey = generateKey()
    return TabNode(
        key = tabNodeKey,
        parentKey = parentKey,
        stacks = listOf(
            buildHomeTabStack(tabNodeKey, generateKey),
            // ... other tabs
        ),
        activeStackIndex = 0,
        wrapperKey = "MainTabs",
        tabMetadata = getMainTabsMetadata(),
        scopeKey = "MainTabs"  // NEW: Added scope key
    )
}
```

---

## Implementation Phases

### Phase 1: Core Library Changes (3-4 days)
**Files to modify:**
- [NavNode.kt](quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/NavNode.kt)
  - Add `scopeKey` property to `TabNode`
  - Add `scopeKey` property to `PaneNode`
- NEW: [ScopeRegistry.kt](quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/ScopeRegistry.kt)
  - Create `ScopeRegistry` interface
- [TreeMutator.kt](quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/TreeMutator.kt)
  - Add scope-aware `findTargetStackForPush()` function
  - Add `pushOutOfScope()` function
  - Modify `push()` to use scope registry
- [TreeNavigator.kt](quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/TreeNavigator.kt)
  - Add `ScopeRegistry` parameter
  - Update `navigate()` to pass scope registry

**Backward Compatibility:**
- `ScopeRegistry.Empty` returns `true` for all scope checks
- Default `scopeKey = null` means no scope enforcement

### Phase 2: KSP Generator Updates (2-3 days)
**Files to modify:**
- [StackInfo.kt](quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/models/StackInfo.kt)
  - Add scope-related fields
- [TabInfo.kt](quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/models/TabInfo.kt)
  - Add scope key field
- [PaneInfo.kt](quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/models/PaneInfo.kt)
  - Add scope key field
- NEW: [ScopeRegistryGenerator.kt](quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/generators/ScopeRegistryGenerator.kt)
  - Create generator for `ScopeRegistry` implementation
- [NavNodeBuilderGenerator.kt](quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/generators/NavNodeBuilderGenerator.kt)
  - Include `scopeKey` in generated TabNode/PaneNode builders
- [QuoVadisSymbolProcessor.kt](quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/QuoVadisSymbolProcessor.kt)
  - Add ScopeRegistryGenerator to processing pipeline

### Phase 3: Compose Integration (1-2 days)
**Files to modify:**
- [HierarchicalQuoVadisHost.kt](quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/HierarchicalQuoVadisHost.kt)
  - Add `scopeRegistry` parameter to `NavigationHost`
- [GraphNavHost.kt](quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/GraphNavHost.kt)
  - Add `scopeRegistry` parameter
- [NavRenderScope.kt](quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/hierarchical/NavRenderScope.kt)
  - Add `scopeRegistry` to render scope (optional)

### Phase 4: Demo App & Testing (2-3 days)
**Tasks:**
- Update demo app to use `GeneratedScopeRegistry`
- Add unit tests for scope checking logic
- Add integration tests for out-of-scope navigation
- Test predictive back with scoped navigation
- Test shared element transitions across scope boundaries

---

## Risk Assessment

### High Risk
1. **Stack structure integrity** - Pushing to parent stack requires careful tree manipulation
   - *Mitigation*: Extensive unit tests for TreeMutator operations
   - *Mitigation*: Validate tree structure after each mutation

2. **Predictive back animation** - Need to ensure entire container animates correctly
   - *Mitigation*: Test with existing PredictiveBackCoordinator
   - *Mitigation*: Cache entries properly for container nodes

### Medium Risk
3. **KSP generation complexity** - Determining scope from class hierarchy
   - *Mitigation*: Use existing TabExtractor/StackExtractor patterns
   - *Mitigation*: Clear error messages for ambiguous cases

4. **Backward compatibility** - Existing apps should work without changes
   - *Mitigation*: Default `ScopeRegistry.Empty` preserves current behavior
   - *Mitigation*: `scopeKey = null` means no scope enforcement

### Low Risk
5. **Performance** - Scope lookup on every navigation
   - *Mitigation*: Use efficient set-based lookup
   - *Mitigation*: Scope check is O(1) map lookup + O(n) set contains

---

## Test Scenarios

### Unit Tests

1. **TreeMutator.push scope detection**
   - Push in-scope destination → pushes to active stack
   - Push out-of-scope destination → pushes to parent stack
   - Push to root (no parent) → creates new root stack

2. **ScopeRegistry implementations**
   - Empty registry always returns true
   - Generated registry correctly identifies scope membership
   - Nested sealed classes are included in scope

3. **NavNode serialization**
   - TabNode with scopeKey serializes/deserializes correctly
   - PaneNode with scopeKey serializes/deserializes correctly

### Integration Tests

1. **Tab → Out-of-scope navigation**
   - Navigate from HomeTab to MasterDetailDestination
   - Verify TabNode preserved in tree
   - Verify new StackNode is sibling to TabNode
   - Verify back navigation restores TabNode as active

2. **Tab → In-scope navigation**
   - Navigate from HomeTab to SettingsTab
   - Verify navigation stays within TabNode
   - Verify activeStackIndex updates

3. **Pane → Out-of-scope navigation**
   - Navigate from PaneNode to different destination type
   - Verify PaneNode preserved for back navigation

### UI Tests (Human testing)

1. **Predictive back from out-of-scope screen**
   - Gesture should reveal entire tab container
   - Bottom navigation should appear during gesture
   - Cancel should restore full-screen destination

2. **Shared elements across scope boundary**
   - Shared elements should animate correctly
   - No visual glitches during scope transitions

---

## Migration Guide

### For Existing Apps

**No action required for basic usage.** The default `ScopeRegistry.Empty` preserves current behavior.

### For Apps Wanting Scoped Navigation

1. **Ensure KSP is up to date** - Regenerate code after library update

2. **Pass GeneratedScopeRegistry to NavigationHost**:
```kotlin
NavigationHost(
    navigator = navigator,
    screenRegistry = GeneratedScreenRegistry,
    wrapperRegistry = GeneratedWrapperRegistry,
    scopeRegistry = GeneratedScopeRegistry  // NEW
)
```

3. **Verify tab/pane navigation behavior** - Test navigation flows

---

## Open Questions

1. **Should there be a way to force in-scope navigation?**
   - E.g., `navigator.navigate(destination, forceInScope = true)`
   - Decision: No, navigation should be safe and should not allow that

2. **How should deep links handle scoped navigation?**
   - Deep links may need to navigate directly to scoped destinations
   - Decision: Deep links should respect scope by default

3. **Should scope be configurable per-navigation?**
   - E.g., different scope behavior for modal vs push
   - Decision: No

---

## Timeline Estimate

| Phase | Duration | Dependencies |
|-------|----------|--------------|
| Phase 1: Core | 3-4 days | None |
| Phase 2: KSP | 2-3 days | Phase 1 |
| Phase 3: Compose | 1-2 days | Phase 1 |
| Phase 4: Testing | 2-3 days | Phase 1, 2, 3 |

**Total: 8-12 days**

---

## Appendix: Code Location Reference

### Core Library
- `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/`
  - `NavNode.kt` - NavNode data classes
  - `TreeMutator.kt` - Tree manipulation operations
  - `TreeNavigator.kt` - Navigator implementation
  - `ScopeRegistry.kt` - NEW

### KSP Processor
- `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/`
  - `generators/ScopeRegistryGenerator.kt` - NEW
  - `generators/NavNodeBuilderGenerator.kt` - Update
  - `models/TabInfo.kt` - Update
  - `models/StackInfo.kt` - Update

### Compose Integration
- `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/`
  - `HierarchicalQuoVadisHost.kt` - Update
  - `GraphNavHost.kt` - Update

---

## Document History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2025-12-11 | Architecture Agent | Initial plan |
