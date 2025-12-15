# Container-Aware Navigation Specification

**Status**: 📋 Proposed  
**Priority**: Medium  
**Created**: 2025-12-15

## Problem Statement

When navigating to a destination that belongs to a `@Tabs` or `@Pane` container, the current implementation creates a `ScreenNode` and attempts to render it via `ScreenRegistry`. However, container destinations (like `DemoTabs`) don't have screens - they have `@TabWrapper` or `@PaneWrapper` composables.

### Current Behavior

```kotlin
// From HomeScreen
navigator.navigate(DemoTabs.MusicTab.List)

// Results in:
// 1. TreeNavigator creates ScreenNode(destination = MusicTab.List)
// 2. Pushes onto HomeTab's stack
// 3. ScreenRegistry renders MusicTabListScreen
// 4. NO tab wrapper, NO tab bar, NO tab switching capability
```

### Expected Behavior

Navigation to any destination within a `@Tabs` container should:
1. Detect that the destination belongs to a container
2. Create the full container structure (TabNode with all stacks)
3. Navigate to the specific tab/destination within that container
4. Render with the appropriate wrapper (tab bar, etc.)

## Proposed Solution: Container-Aware Navigation

### Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    TreeNavigator.navigate()                  │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│  1. Check ScopeRegistry - Is destination in current scope?  │
│     YES → Navigate within current container                  │
│     NO  → Continue to step 2                                 │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│  2. Check ContainerRegistry - Does destination need         │
│     a container?                                             │
│     YES → Get builder, create container, push to stack      │
│     NO  → Continue to step 3                                 │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│  3. Default behavior - Create ScreenNode, push to stack     │
└─────────────────────────────────────────────────────────────┘
```

### New Components

#### 1. ContainerRegistry Interface

```kotlin
// quo-vadis-core/src/commonMain/.../core/ContainerRegistry.kt

/**
 * Registry that maps destinations to their container structures.
 * 
 * When navigating to a destination that belongs to a @Tabs or @Pane
 * container, this registry provides the builder function to create
 * the appropriate container node structure.
 */
interface ContainerRegistry {
    
    /**
     * Check if a destination requires a container structure.
     * 
     * @param destination The destination being navigated to
     * @return ContainerInfo if destination needs a container, null otherwise
     */
    fun getContainerInfo(destination: Destination): ContainerInfo?
    
    /**
     * Empty registry that never creates containers.
     */
    companion object {
        val Empty: ContainerRegistry = object : ContainerRegistry {
            override fun getContainerInfo(destination: Destination): ContainerInfo? = null
        }
    }
}

/**
 * Information about a container and how to create it.
 */
sealed class ContainerInfo {
    
    /**
     * Tab container information.
     * 
     * @property builder Function to build the TabNode
     * @property initialTabIndex Which tab should be active (based on destination)
     * @property scopeKey The scope key for this container
     */
    data class TabContainer(
        val builder: (key: String, parentKey: String?, initialTabIndex: Int) -> TabNode,
        val initialTabIndex: Int,
        val scopeKey: String
    ) : ContainerInfo()
    
    /**
     * Pane container information.
     * 
     * @property builder Function to build the PaneNode
     * @property initialPane Which pane should be active
     * @property scopeKey The scope key for this container
     */
    data class PaneContainer(
        val builder: (key: String, parentKey: String?) -> PaneNode,
        val initialPane: PaneRole,
        val scopeKey: String
    ) : ContainerInfo()
}
```

#### 2. KSP Generator: ContainerRegistryGenerator

Generate `GeneratedContainerRegistry` from `@Tabs` and `@Pane` annotations:

```kotlin
// Generated output example:
object GeneratedContainerRegistry : ContainerRegistry {
    
    override fun getContainerInfo(destination: Destination): ContainerInfo? {
        return when (destination) {
            // DemoTabs destinations
            is DemoTabs.MusicTab -> ContainerInfo.TabContainer(
                builder = ::buildDemoTabsNavNode,
                initialTabIndex = 0, // MusicTab is index 0
                scopeKey = "DemoTabs"
            )
            is DemoTabs.MoviesTab -> ContainerInfo.TabContainer(
                builder = ::buildDemoTabsNavNode,
                initialTabIndex = 1, // MoviesTab is index 1
                scopeKey = "DemoTabs"
            )
            is DemoTabs.BooksTab -> ContainerInfo.TabContainer(
                builder = ::buildDemoTabsNavNode,
                initialTabIndex = 2, // BooksTab is index 2
                scopeKey = "DemoTabs"
            )
            
            // MainTabs would NOT be here - it's used as initial state
            // (or could be included if we want to support navigating to MainTabs)
            
            else -> null
        }
    }
}
```

#### 3. TreeNavigator Changes

Modify `navigate()` to check ContainerRegistry:

```kotlin
class TreeNavigator(
    initialState: NavNode,
    private val scopeRegistry: ScopeRegistry = ScopeRegistry.Empty,
    private val containerRegistry: ContainerRegistry = ContainerRegistry.Empty, // NEW
    // ...
) : Navigator {
    
    override fun navigate(destination: Destination, transition: NavigationTransition?) {
        val effectiveTransition = transition ?: destination.transition
        val oldState = _state.value
        val fromKey = oldState.activeLeaf()?.key
        
        // Step 1: Check if already inside a container that owns this destination
        if (scopeRegistry.isInScope(oldState, destination)) {
            // Navigate within current container (existing behavior)
            val newState = TreeMutator.push(oldState, destination, scopeRegistry) { generateKey() }
            // ... update state
            return
        }
        
        // Step 2: Check if destination needs a container
        val containerInfo = containerRegistry.getContainerInfo(destination)
        if (containerInfo != null) {
            val newState = pushContainer(oldState, containerInfo, destination)
            // ... update state
            return
        }
        
        // Step 3: Default - push as ScreenNode (existing behavior)
        val newState = TreeMutator.push(oldState, destination, scopeRegistry) { generateKey() }
        // ... update state
    }
    
    private fun pushContainer(
        root: NavNode,
        containerInfo: ContainerInfo,
        destination: Destination
    ): NavNode {
        val targetStack = root.activeStack()
            ?: throw IllegalStateException("No active stack found")
        
        return when (containerInfo) {
            is ContainerInfo.TabContainer -> {
                val containerKey = generateKey()
                val tabNode = containerInfo.builder(
                    containerKey,
                    targetStack.key,
                    containerInfo.initialTabIndex
                )
                
                // Push the TabNode onto the current stack
                val newStack = targetStack.copy(
                    children = targetStack.children + tabNode
                )
                TreeMutator.replaceNode(root, targetStack.key, newStack)
            }
            
            is ContainerInfo.PaneContainer -> {
                // Similar logic for PaneNode
                // ...
            }
        }
    }
}
```

#### 4. NavigationHost Changes

Accept ContainerRegistry and pass to TreeNavigator:

```kotlin
@Composable
fun NavigationHost(
    navigator: Navigator,
    screenRegistry: ScreenRegistry,
    wrapperRegistry: WrapperRegistry = WrapperRegistry.Empty,
    scopeRegistry: ScopeRegistry = ScopeRegistry.Empty,
    containerRegistry: ContainerRegistry = ContainerRegistry.Empty, // NEW
    // ...
)
```

### Deep Link Handling

Deep links like `demo/tabs` should automatically:
1. Resolve to `DemoTabs.MusicTab.List` (initial tab destination)
2. ContainerRegistry detects it needs a TabNode
3. Create TabNode with correct initial tab

The existing `GeneratedDeepLinkHandler` needs minor updates to resolve container routes to their initial destinations.

### Back Navigation

When backing out of a container:
1. First, pop within the active tab's stack
2. When tab stack is at root, switch to initial tab (if not already)
3. When at initial tab's root, pop the entire TabNode from parent stack

This already works correctly with `TreeMutator.pop()` and cascade behavior.

## Implementation Plan

### Phase 1: Core Infrastructure
1. Create `ContainerRegistry` interface in `quo-vadis-core`
2. Create `ContainerInfo` sealed class hierarchy
3. Add `containerRegistry` parameter to `TreeNavigator`

### Phase 2: KSP Generation
1. Create `ContainerRegistryGenerator` in `quo-vadis-ksp`
2. Generate mappings from `@Tabs` destinations to builders
3. Generate mappings from `@Pane` destinations to builders

### Phase 3: Integration
1. Update `NavigationHost` to accept ContainerRegistry
2. Modify `TreeNavigator.navigate()` to check registry
3. Update deep link handling for container routes

### Phase 4: Testing
1. Unit tests for ContainerRegistry
2. Unit tests for modified TreeNavigator
3. Integration tests for DemoTabs navigation
4. Deep link tests for container destinations

## Alternatives Considered

### Alternative A: Explicit Container Navigation API

```kotlin
// Separate method for container navigation
navigator.navigateToTabs(buildDemoTabsNavNode, initialTab = 0)
```

**Pros**: Clear intent, no magic  
**Cons**: Different API for containers vs screens, breaks navigation abstraction

### Alternative B: Manual State Updates

```kotlin
// App manually pushes TabNode
val tabNode = buildDemoTabsNavNode(parentKey = currentStackKey)
navigator.updateState { TreeMutator.replaceNode(it, currentStackKey, newStack) }
```

**Pros**: Full control  
**Cons**: Boilerplate, error-prone, breaks encapsulation

### Alternative C: Screen Wrapper (Nested NavigationHost)

```kotlin
@Screen(DemoTabs::class)
@Composable
fun DemoTabsScreen(navigator: Navigator) {
    val innerNavigator = remember { TreeNavigator(buildDemoTabsNavNode()) }
    NavigationHost(navigator = innerNavigator, ...)
}
```

**Pros**: Works with current architecture  
**Cons**: Nested navigation hosts, state management complexity, predictive back issues

## Open Questions

1. **Should MainTabs be navigable?** If the app starts with MainTabs, can you navigate TO it from somewhere else?

2. **Transition animations**: How should enter/exit animations work when pushing a TabNode?

3. **State restoration**: How does the ContainerRegistry interact with state serialization?

4. **Destination collision**: What if a destination is valid both as a standalone screen AND as part of a container?

## References

- [NavNode Architecture](./phase1-core/1.1-navnode-types.md)
- [TreeMutator Operations](./phase1-core/1.2-tree-mutator.md)
- [Scope-Aware Navigation](./phase2-navigator/2.2-scope-aware-navigation.md)
