# Phase 1: Core Library - Nested Navigator Foundation

## Phase Overview

**Objective**: Implement the foundational core library components for hierarchical tabbed navigation in `quo-vadis-core`.

**Scope**: Platform-agnostic Kotlin code in `commonMain` that establishes:
- Tab navigation state management
- Nested navigator architecture
- Hierarchical back press delegation
- Type-safe tab definitions

**Timeline**: 3-4 days

**Dependencies**: None (foundational phase)

## Architectural Principles

### 1. Separation of Concerns
- **TabNavigatorState**: Pure state holder (no Compose dependencies)
- **TabDefinition**: Type-safe tab configuration
- **BackPressHandler**: Interface for hierarchical delegation
- **Navigator Enhancement**: Support for child navigators

### 2. Reactive State Management
- All state via `StateFlow` (no mutable collections exposed)
- Tab switching via state updates, not callbacks
- Observable stack for each tab independently

### 3. Type Safety
- Sealed classes for tab definitions
- Compile-time enforcement of tab routes
- No string-based tab identification

## Detailed Implementation Plan

### Step 1: Core Interfaces & Data Classes

**File**: `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/TabDefinition.kt`

**Purpose**: Define type-safe tab configuration

```kotlin
package com.jermey.quo.vadis.core.navigation.core

/**
 * Defines a tab in a tabbed navigation interface.
 * 
 * Each tab represents an independent navigation stack with its own backstack.
 * Tabs should be implemented as sealed class hierarchies for type safety.
 * 
 * @sample
 * ```kotlin
 * sealed class MainTab : TabDefinition {
 *     abstract val rootDestination: Destination
 *     
 *     data object Home : MainTab() {
 *         override val id = "home"
 *         override val rootDestination = HomeDestination.Root
 *     }
 *     
 *     data object Profile : MainTab() {
 *         override val id = "profile"
 *         override val rootDestination = ProfileDestination.Root
 *     }
 * }
 * ```
 */
interface TabDefinition {
    /**
     * Unique identifier for this tab.
     * Used for serialization and equality checks.
     */
    val id: String
    
    /**
     * The root destination that this tab starts with.
     * When a tab is selected for the first time, this destination is pushed to its stack.
     */
    val rootDestination: Destination
    
    /**
     * Optional label for the tab (for UI display).
     * Can be overridden by UI components.
     */
    val label: String? get() = null
    
    /**
     * Optional icon identifier for the tab (for UI display).
     * Can be overridden by UI components.
     */
    val icon: String? get() = null
}

/**
 * Configuration for a tab navigation container.
 * 
 * @property allTabs All tabs in this container, in display order
 * @property initialTab The tab to show when container is first created
 * @property primaryTab The "home" tab for back press behavior (defaults to initial)
 */
data class TabNavigatorConfig(
    val allTabs: List<TabDefinition>,
    val initialTab: TabDefinition,
    val primaryTab: TabDefinition = initialTab
) {
    init {
        require(allTabs.isNotEmpty()) { "allTabs must not be empty" }
        require(initialTab in allTabs) { "initialTab must be in allTabs" }
        require(primaryTab in allTabs) { "primaryTab must be in allTabs" }
        
        // Verify unique IDs
        val ids = allTabs.map { it.id }
        require(ids.size == ids.distinct().size) { "Tab IDs must be unique" }
    }
}
```

**Key Design Decisions**:
- Interface (not sealed class) for flexibility
- `rootDestination` ensures each tab has a starting point
- Validation in `TabNavigatorConfig` ensures correctness
- Optional UI metadata (`label`, `icon`) for Compose layer

---

### Step 2: Back Press Handling Interface

**File**: `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/BackPressHandler.kt`

**Purpose**: Define hierarchical back press delegation protocol

```kotlin
package com.jermey.quo.vadis.core.navigation.core

/**
 * Interface for components that can handle back press events.
 * 
 * Used to implement hierarchical back press delegation, where parent
 * navigators can delegate back press handling to child navigators.
 * 
 * @sample Delegation pattern
 * ```kotlin
 * class ParentNavigator : BackPressHandler {
 *     private val childNavigator: BackPressHandler? = ...
 *     
 *     override fun onBack(): Boolean {
 *         // First try child
 *         if (childNavigator?.onBack() == true) {
 *             return true // Child consumed
 *         }
 *         
 *         // Child didn't consume, handle ourselves
 *         return if (canGoBack) {
 *             navigateBack()
 *             true
 *         } else {
 *             false // Pass to parent
 *         }
 *     }
 * }
 * ```
 */
interface BackPressHandler {
    /**
     * Handles a back press event.
     * 
     * @return `true` if the event was consumed (handled), `false` if it should
     *         be passed to the parent handler or system.
     */
    fun onBack(): Boolean
}

/**
 * Interface for navigators that can contain child navigators.
 * 
 * Parent navigators delegate back press to active children before
 * handling themselves.
 */
interface ParentNavigator : BackPressHandler {
    /**
     * The currently active child navigator, if any.
     * When non-null, back press is delegated to this child first.
     */
    val activeChild: BackPressHandler?
    
    /**
     * Default back press implementation with child delegation.
     */
    override fun onBack(): Boolean {
        // Try child first
        if (activeChild?.onBack() == true) {
            return true
        }
        
        // Child didn't consume, use our own logic
        return handleBackInternal()
    }
    
    /**
     * Internal back press handling for this navigator.
     * Only called if no child consumed the event.
     */
    fun handleBackInternal(): Boolean
}
```

**Key Design Decisions**:
- Simple boolean return (consumed vs not consumed)
- Default implementation in `ParentNavigator` for consistency
- `activeChild` abstracts the child lookup logic

---

### Step 3: Tab Navigator State Management

**File**: `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/TabNavigatorState.kt`

**Purpose**: Core state management for tab navigation (NO Compose dependencies)

```kotlin
package com.jermey.quo.vadis.core.navigation.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Manages the state for tabbed navigation.
 * 
 * Maintains independent navigation stacks for each tab, allowing parallel
 * navigation states that are preserved when switching between tabs.
 * 
 * This is a platform-agnostic state holder with no Compose dependencies.
 * 
 * @property config Configuration for this tab navigator
 */
class TabNavigatorState(
    val config: TabNavigatorConfig
) : BackPressHandler {
    
    /**
     * The currently selected (visible) tab.
     */
    private val _selectedTab = MutableStateFlow(config.initialTab)
    val selectedTab: StateFlow<TabDefinition> = _selectedTab.asStateFlow()
    
    /**
     * Navigation stacks for each tab.
     * Key = TabDefinition, Value = List of destinations (stack)
     */
    private val _tabStacks = MutableStateFlow<Map<TabDefinition, List<Destination>>>(
        config.allTabs.associateWith { listOf(it.rootDestination) }
    )
    val tabStacks: StateFlow<Map<TabDefinition, List<Destination>>> = _tabStacks.asStateFlow()
    
    /**
     * Get the current stack for the selected tab.
     */
    val currentTabStack: StateFlow<List<Destination>>
        get() = MutableStateFlow(getTabStack(selectedTab.value)).asStateFlow()
    
    /**
     * Get the stack for a specific tab.
     */
    fun getTabStack(tab: TabDefinition): List<Destination> {
        return _tabStacks.value[tab] ?: listOf(tab.rootDestination)
    }
    
    /**
     * Get the current destination for a specific tab.
     */
    fun getCurrentDestination(tab: TabDefinition): Destination? {
        return getTabStack(tab).lastOrNull()
    }
    
    /**
     * Select (switch to) a different tab.
     * 
     * @param tab The tab to select
     * @return `true` if tab changed, `false` if already on this tab
     */
    fun selectTab(tab: TabDefinition): Boolean {
        require(tab in config.allTabs) { "Tab not in configured tabs" }
        
        if (_selectedTab.value == tab) {
            return false
        }
        
        _selectedTab.value = tab
        return true
    }
    
    /**
     * Navigate to a destination within the currently selected tab.
     * 
     * @param destination The destination to navigate to
     */
    fun navigateInTab(destination: Destination) {
        val currentTab = _selectedTab.value
        val currentStack = getTabStack(currentTab)
        
        _tabStacks.update { stacks ->
            stacks + (currentTab to (currentStack + destination))
        }
    }
    
    /**
     * Navigate back within the currently selected tab.
     * 
     * @return `true` if navigated back, `false` if at tab root
     */
    fun navigateBackInTab(): Boolean {
        val currentTab = _selectedTab.value
        val currentStack = getTabStack(currentTab)
        
        if (currentStack.size <= 1) {
            return false // At root of tab
        }
        
        _tabStacks.update { stacks ->
            stacks + (currentTab to currentStack.dropLast(1))
        }
        return true
    }
    
    /**
     * Clear the stack for a specific tab back to its root.
     * 
     * @param tab The tab to clear (defaults to current tab)
     */
    fun clearTabToRoot(tab: TabDefinition = _selectedTab.value) {
        _tabStacks.update { stacks ->
            stacks + (tab to listOf(tab.rootDestination))
        }
    }
    
    /**
     * Reset a tab's stack to a specific destination.
     * 
     * @param tab The tab to reset
     * @param destination The new root destination for this tab
     */
    fun resetTabTo(tab: TabDefinition, destination: Destination) {
        _tabStacks.update { stacks ->
            stacks + (tab to listOf(destination))
        }
    }
    
    /**
     * Handles back press with intelligent delegation.
     * 
     * Logic:
     * 1. If current tab stack > 1, pop from tab stack (CONSUMED)
     * 2. If not on primary tab, switch to primary tab (CONSUMED)
     * 3. If at primary tab root, do nothing (NOT CONSUMED)
     * 
     * @return `true` if consumed, `false` if should be passed to parent
     */
    override fun onBack(): Boolean {
        val currentTab = _selectedTab.value
        val currentStack = getTabStack(currentTab)
        
        // Case 1: Can pop within current tab
        if (currentStack.size > 1) {
            navigateBackInTab()
            return true
        }
        
        // Case 2: Not on primary tab, switch to it
        if (currentTab != config.primaryTab) {
            selectTab(config.primaryTab)
            return true
        }
        
        // Case 3: At primary tab root, don't consume
        return false
    }
    
    /**
     * Check if back press can be handled by this navigator.
     */
    fun canHandleBack(): Boolean {
        val currentTab = _selectedTab.value
        val currentStack = getTabStack(currentTab)
        
        return currentStack.size > 1 || currentTab != config.primaryTab
    }
}
```

**Key Design Decisions**:
- All state via `StateFlow` (reactive)
- Immutable state updates (thread-safe)
- No Compose dependencies (pure Kotlin)
- Implements `BackPressHandler` for hierarchy
- Intelligent back press logic encapsulated

**Testing Strategy**:
```kotlin
class TabNavigatorStateTest {
    @Test
    fun `selectTab switches current tab`() {
        val state = TabNavigatorState(config)
        state.selectTab(ProfileTab)
        assertEquals(ProfileTab, state.selectedTab.value)
    }
    
    @Test
    fun `navigateInTab adds to current tab stack`() {
        val state = TabNavigatorState(config)
        state.navigateInTab(HomeDetail)
        assertEquals(2, state.getTabStack(HomeTab).size)
    }
    
    @Test
    fun `onBack pops from tab stack first`() {
        val state = TabNavigatorState(config)
        state.navigateInTab(HomeDetail)
        assertTrue(state.onBack())
        assertEquals(1, state.getTabStack(HomeTab).size)
    }
    
    @Test
    fun `onBack switches to primary tab when at root`() {
        val state = TabNavigatorState(config)
        state.selectTab(ProfileTab)
        assertTrue(state.onBack())
        assertEquals(config.primaryTab, state.selectedTab.value)
    }
    
    @Test
    fun `onBack returns false at primary tab root`() {
        val state = TabNavigatorState(config)
        assertFalse(state.onBack())
    }
}
```

---

### Step 4: Navigator Enhancement for Child Support

**File**: `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/Navigator.kt`

**Purpose**: Extend existing `Navigator` interface to support child navigators

**Changes Required**:

```kotlin
/**
 * Central navigation controller with support for hierarchical navigation.
 * 
 * Existing interface - ADD THESE MEMBERS:
 */
interface Navigator : BackPressHandler {
    // ... existing members ...
    
    /**
     * The active child navigator, if any.
     * 
     * When a child navigator is active, back press events are delegated
     * to it before being handled by this navigator.
     */
    val activeChild: BackPressHandler?
        get() = null // Default: no child
    
    /**
     * Register a child navigator for back press delegation.
     * 
     * @param child The child navigator, or null to clear
     */
    fun setActiveChild(child: BackPressHandler?)
}

/**
 * Default implementation - MODIFY onBack():
 */
internal class DefaultNavigator : Navigator {
    // ... existing implementation ...
    
    private var _activeChild: BackPressHandler? = null
    override val activeChild: BackPressHandler?
        get() = _activeChild
    
    override fun setActiveChild(child: BackPressHandler?) {
        _activeChild = child
    }
    
    override fun onBack(): Boolean {
        // NEW: Delegate to child first
        if (_activeChild?.onBack() == true) {
            return true
        }
        
        // Existing logic
        return if (backStack.canGoBack) {
            navigateBack()
            true
        } else {
            false
        }
    }
}
```

**Key Design Decisions**:
- Non-breaking change (default implementation returns `null`)
- Opt-in child delegation via `setActiveChild()`
- Follows `ParentNavigator` pattern from Step 2

---

### Step 5: Testing Utilities

**File**: `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/testing/FakeTabNavigator.kt`

**Purpose**: Testing utility for tab navigation

```kotlin
package com.jermey.quo.vadis.core.navigation.testing

import com.jermey.quo.vadis.core.navigation.core.*

/**
 * Fake implementation of tab navigation for testing.
 */
class FakeTabNavigator(
    config: TabNavigatorConfig
) {
    private val state = TabNavigatorState(config)
    
    val currentTab: TabDefinition
        get() = state.selectedTab.value
    
    val allTabs: List<TabDefinition>
        get() = state.config.allTabs
    
    fun selectTab(tab: TabDefinition): Boolean {
        return state.selectTab(tab)
    }
    
    fun navigateInTab(destination: Destination) {
        state.navigateInTab(destination)
    }
    
    fun navigateBackInTab(): Boolean {
        return state.navigateBackInTab()
    }
    
    fun getTabStack(tab: TabDefinition): List<Destination> {
        return state.getTabStack(tab)
    }
    
    fun onBack(): Boolean {
        return state.onBack()
    }
    
    // Test helpers
    fun verifyTabSelected(tab: TabDefinition): Boolean {
        return currentTab == tab
    }
    
    fun verifyTabStackSize(tab: TabDefinition, size: Int): Boolean {
        return getTabStack(tab).size == size
    }
    
    fun verifyAtRoot(tab: TabDefinition): Boolean {
        return getTabStack(tab).size == 1
    }
}
```

---

### Step 6: Unit Tests

**File**: `quo-vadis-core/src/commonTest/kotlin/com/jermey/quo/vadis/core/navigation/core/TabNavigatorStateTest.kt`

**Test Coverage** (minimum 85%):

```kotlin
class TabNavigatorStateTest {
    private lateinit var config: TabNavigatorConfig
    private lateinit var state: TabNavigatorState
    
    @BeforeTest
    fun setup() {
        config = TabNavigatorConfig(
            allTabs = listOf(HomeTab, ExploreTab, ProfileTab, SettingsTab),
            initialTab = HomeTab,
            primaryTab = HomeTab
        )
        state = TabNavigatorState(config)
    }
    
    // Basic functionality
    @Test fun `initial state is correct`()
    @Test fun `selectTab switches current tab`()
    @Test fun `selectTab on same tab returns false`()
    @Test fun `navigateInTab adds to current tab stack`()
    @Test fun `navigateBackInTab pops from stack`()
    @Test fun `navigateBackInTab at root returns false`()
    @Test fun `clearTabToRoot resets to root destination`()
    @Test fun `resetTabTo sets new root`()
    
    // Tab independence
    @Test fun `navigating in one tab does not affect others`()
    @Test fun `switching tabs preserves stack state`()
    @Test fun `each tab maintains independent stack`()
    
    // Back press logic
    @Test fun `onBack pops from current tab when stack size greater than 1`()
    @Test fun `onBack switches to primary tab when at non-primary root`()
    @Test fun `onBack returns false at primary tab root`()
    @Test fun `canHandleBack returns true when tab has stack`()
    @Test fun `canHandleBack returns true when not on primary`()
    @Test fun `canHandleBack returns false at primary root`()
    
    // StateFlow reactivity
    @Test fun `selectedTab StateFlow emits on tab change`()
    @Test fun `tabStacks StateFlow emits on navigation`()
    @Test fun `currentTabStack reflects current tab`()
    
    // Edge cases
    @Test fun `throws when selecting tab not in config`()
    @Test fun `handles rapid tab switches`()
    @Test fun `handles deep navigation stacks`()
    
    // Configuration validation
    @Test fun `config requires non-empty tabs`()
    @Test fun `config requires initialTab in allTabs`()
    @Test fun `config requires primaryTab in allTabs`()
    @Test fun `config requires unique tab IDs`()
}
```

**File**: `quo-vadis-core/src/commonTest/kotlin/com/jermey/quo/vadis/core/navigation/core/NavigatorChildDelegationTest.kt`

```kotlin
class NavigatorChildDelegationTest {
    @Test fun `navigator delegates back to child when present`()
    @Test fun `navigator handles back when child returns false`()
    @Test fun `navigator handles back when no child`()
    @Test fun `setActiveChild updates delegation`()
    @Test fun `setActiveChild null clears delegation`()
}
```

---

## File Structure Summary

New files to create:

```
quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/
├── TabDefinition.kt          (NEW - ~150 lines)
├── BackPressHandler.kt       (NEW - ~80 lines)
├── TabNavigatorState.kt      (NEW - ~250 lines)
└── Navigator.kt              (MODIFIED - add ~30 lines)

quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/testing/
└── FakeTabNavigator.kt       (NEW - ~100 lines)

quo-vadis-core/src/commonTest/kotlin/com/jermey/quo/vadis/core/navigation/core/
├── TabNavigatorStateTest.kt  (NEW - ~400 lines)
└── NavigatorChildDelegationTest.kt  (NEW - ~150 lines)
```

**Total**: ~1,160 lines of new/modified code

---

## Quality Checklist

Before completing Phase 1:

### Code Quality
- [ ] All code follows Kotlin official style guide
- [ ] All public APIs have comprehensive KDoc
- [ ] No platform-specific code in `commonMain`
- [ ] Proper null safety (`?`, `!!` only where justified)
- [ ] Thread-safe (immutable state, `StateFlow` usage)

### Testing
- [ ] Unit tests pass on all platforms
- [ ] Test coverage ≥ 85%
- [ ] Edge cases covered
- [ ] StateFlow reactivity tested
- [ ] Back press logic thoroughly tested

### Architecture
- [ ] No Compose dependencies in core layer
- [ ] Clean separation of concerns
- [ ] Type-safe APIs (no strings for identification)
- [ ] Follows existing quo-vadis patterns
- [ ] Reactive state via `StateFlow`

### Documentation
- [ ] All public APIs documented
- [ ] Code samples in KDoc
- [ ] Architecture decisions documented
- [ ] Testing utilities documented

### Integration
- [ ] No breaking changes to existing `Navigator` API
- [ ] Backward compatible
- [ ] Follows existing naming conventions
- [ ] Consistent with quo-vadis principles

---

## Dependencies for Next Phases

**Phase 2 (Compose Integration)** will depend on:
- `TabNavigatorState` for state management
- `TabDefinition` for type-safe tabs
- `BackPressHandler` for gesture integration

**Phase 3 (KSP)** will depend on:
- `TabDefinition` interface for code generation target
- `TabNavigatorConfig` for configuration generation

---

## Risks & Mitigation

### Risk: Complex Back Press Logic
**Mitigation**: 
- Extensive unit tests for all scenarios
- Clear state machine documentation
- Test with `FakeTabNavigator` in demo app

### Risk: StateFlow Performance
**Mitigation**:
- Benchmark tab switching (<16ms target)
- Use `update {}` for batched changes
- Profile memory usage with 8+ tabs

### Risk: API Design Friction
**Mitigation**:
- Follow existing quo-vadis patterns closely
- Review with existing `Navigator` design
- Get user feedback on API before Phase 2

---

## Verification Steps

After implementation:

1. **Build**: `./gradlew :quo-vadis-core:build`
2. **Test**: `./gradlew :quo-vadis-core:test`
3. **Verify**: All tests pass on all platforms
4. **Coverage**: Check test coverage ≥ 85%
5. **Review**: Ensure no Compose dependencies in core

---

**Status**: 🔴 Not Started

**Next Phase**: Phase 2 - Compose UI Integration

**Depends On**: None (foundational)
