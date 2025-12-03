# CORE-003 Impact Notes: PaneNode Enhancement Effects

## Overview

This document describes the impact of CORE-001 `PaneNode` enhancements on CORE-003 (Navigator Refactor to StateFlow<NavNode>).

**Related Changes:** CORE-001 `PaneNode` redesign with role-based pane configurations.

---

## Summary of Required Navigator API Additions

The Navigator interface must expose pane-aware operations for intuitive master-detail and supporting pane patterns.

---

## New Navigator Interface Methods

### Pane Navigation Operations

```kotlin
interface Navigator {
    // ═══════════════════════════════════════════════════════════════════
    // EXISTING OPERATIONS (keep as-is)
    // ═══════════════════════════════════════════════════════════════════
    
    /** Current navigation state as immutable tree */
    val state: StateFlow<NavNode>
    
    /** Navigate to a destination (affects deepest active stack) */
    fun navigate(destination: Destination, transition: NavigationTransition? = null)
    
    /** Navigate back (pops from deepest active stack) */
    fun navigateBack(): Boolean
    
    // ═══════════════════════════════════════════════════════════════════
    // NEW: PANE-SPECIFIC OPERATIONS
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Navigate to a destination within a specific pane.
     *
     * This is the primary API for master-detail and supporting pane patterns.
     * The destination is pushed onto the target pane's stack.
     *
     * @param role Target pane role (Primary, Supporting, Extra)
     * @param destination Destination to navigate to
     * @param switchFocus If true, also changes activePaneRole to target role
     * @param transition Optional transition animation
     *
     * @throws IllegalStateException if no PaneNode found in current state
     * @throws IllegalArgumentException if role is not configured in the PaneNode
     *
     * Example usage:
     * ```kotlin
     * // Master-detail: show item detail in supporting pane
     * navigator.navigateToPane(PaneRole.Supporting, ItemDetailDestination(itemId))
     * 
     * // Keep focus on list while loading detail
     * navigator.navigateToPane(PaneRole.Supporting, ItemDetailDestination(itemId), switchFocus = false)
     * ```
     */
    fun navigateToPane(
        role: PaneRole,
        destination: Destination,
        switchFocus: Boolean = true,
        transition: NavigationTransition? = null
    )
    
    /**
     * Switch the active (focused) pane without navigation.
     *
     * Changes which pane receives navigation focus. On compact screens,
     * this determines which pane is visible.
     *
     * @param role Pane role to activate
     * @throws IllegalArgumentException if role is not configured
     *
     * Example usage:
     * ```kotlin
     * // Return focus to list pane
     * navigator.switchPane(PaneRole.Primary)
     * ```
     */
    fun switchPane(role: PaneRole)
    
    /**
     * Check if a pane role is available in the current state.
     *
     * @param role Pane role to check
     * @return true if the role is configured in the current PaneNode
     *
     * Example usage:
     * ```kotlin
     * if (navigator.isPaneAvailable(PaneRole.Extra)) {
     *     navigator.navigateToPane(PaneRole.Extra, SettingsDestination)
     * }
     * ```
     */
    fun isPaneAvailable(role: PaneRole): Boolean
    
    /**
     * Get the current content of a specific pane.
     *
     * @param role Pane role to query
     * @return The NavNode content of the pane, or null if role not configured
     */
    fun paneContent(role: PaneRole): NavNode?
    
    /**
     * Navigate back within a specific pane.
     *
     * Pops from the specified pane's stack regardless of which pane is active.
     *
     * @param role Pane role to pop from
     * @return true if navigation occurred, false if pane stack was empty
     *
     * Example usage:
     * ```kotlin
     * // Clear detail pane when closing detail view
     * navigator.navigateBackInPane(PaneRole.Supporting)
     * ```
     */
    fun navigateBackInPane(role: PaneRole): Boolean
    
    /**
     * Clear a pane's navigation stack back to its root.
     *
     * @param role Pane role to clear
     *
     * Example usage:
     * ```kotlin
     * // Reset detail pane when selecting new list item
     * navigator.clearPane(PaneRole.Supporting)
     * navigator.navigateToPane(PaneRole.Supporting, newDetail)
     * ```
     */
    fun clearPane(role: PaneRole)
}
```

### Convenience Extension Functions

```kotlin
/**
 * Navigate to a pane and switch focus in one call.
 * This is the most common pattern for master-detail navigation.
 */
fun Navigator.showInPane(role: PaneRole, destination: Destination) {
    navigateToPane(role, destination, switchFocus = true)
}

/**
 * Show detail content while keeping focus on list.
 * Useful for "peek" scenarios or preloading.
 */
fun Navigator.preloadPane(role: PaneRole, destination: Destination) {
    navigateToPane(role, destination, switchFocus = false)
}

/**
 * Typed extension for common master-detail pattern.
 */
fun Navigator.showDetail(destination: Destination) {
    navigateToPane(PaneRole.Supporting, destination, switchFocus = true)
}

/**
 * Return to primary pane (usually list).
 */
fun Navigator.showPrimary() {
    switchPane(PaneRole.Primary)
}
```

---

## State Observation for Pane Awareness

### Observable Pane State

```kotlin
/**
 * Extensions for observing pane-specific state.
 */

/** Current active pane role, or null if no PaneNode in state */
val Navigator.activePaneRole: PaneRole?
    get() = (state.value.findFirst<PaneNode>())?.activePaneRole

/** Flow of active pane role changes */
val Navigator.activePaneRoleFlow: Flow<PaneRole?>
    get() = state.map { it.findFirst<PaneNode>()?.activePaneRole }.distinctUntilChanged()

/** Whether the current state contains a PaneNode */
val Navigator.hasPaneLayout: Boolean
    get() = state.value.findFirst<PaneNode>() != null
```

---

## Back Navigation Integration

### Enhanced Back Handling

The `navigateBack()` method must consider `PaneBackBehavior`:

```kotlin
/**
 * Navigate back with explicit behavior override.
 *
 * @param behavior Override the PaneNode's default back behavior
 * @return true if navigation occurred
 */
fun Navigator.navigateBack(behavior: PaneBackBehavior): Boolean
```

### Back Behavior Decision Flow

```
navigateBack() called
        │
        ▼
┌──────────────────────┐
│ Is state a PaneNode? │
└──────────────────────┘
        │
    Yes │                  No
        ▼                   ▼
┌──────────────────┐   ┌───────────────────┐
│ Check active pane│   │ Standard stack pop│
│ stack depth      │   └───────────────────┘
└──────────────────┘
        │
        ▼
┌──────────────────────────┐
│ depth > 1? → Pop stack   │
│ depth == 1 → Apply       │
│   PaneBackBehavior       │
└──────────────────────────┘
```

---

## Updated Acceptance Criteria for CORE-003

Add these acceptance criteria:

- [ ] `navigateToPane(role, destination, switchFocus, transition)` implemented
- [ ] `switchPane(role)` implemented
- [ ] `isPaneAvailable(role)` implemented
- [ ] `paneContent(role)` implemented
- [ ] `navigateBackInPane(role)` implemented
- [ ] `clearPane(role)` implemented
- [ ] `activePaneRole` property/flow exposed
- [ ] `navigateBack(behavior)` overload with explicit behavior
- [ ] All pane operations validate role existence
- [ ] Extension functions: `showInPane`, `preloadPane`, `showDetail`, `showPrimary`
- [ ] Unit tests for all pane navigation operations
- [ ] Integration test: master-detail flow on compact vs expanded screens

---

## Usage Examples

### Master-Detail Pattern

```kotlin
// In ListScreen
fun onItemClick(item: Item) {
    navigator.showDetail(ItemDetailDestination(item.id))
}

// In DetailScreen
fun onBackPressed() {
    navigator.showPrimary()
}

// Or let system handle it
// navigateBack() will respect PaneBackBehavior
```

### Supporting Pane Pattern

```kotlin
// Main content with supporting context
@Composable
fun DocumentEditor(navigator: Navigator) {
    // Show comments panel
    Button(onClick = {
        navigator.navigateToPane(PaneRole.Supporting, CommentsDestination(docId))
    }) {
        Text("Show Comments")
    }
    
    // Hide comments panel
    Button(onClick = {
        navigator.clearPane(PaneRole.Supporting)
        navigator.switchPane(PaneRole.Primary)
    }) {
        Text("Hide Comments")
    }
}
```

### Conditional Pane Usage

```kotlin
// Only show extra pane on large screens
if (navigator.isPaneAvailable(PaneRole.Extra)) {
    navigator.navigateToPane(PaneRole.Extra, PropertiesDestination)
} else {
    // Fall back to modal or bottom sheet
    showModal(PropertiesDestination)
}
```

---

## Testing Considerations

```kotlin
@Test
fun `navigateToPane adds destination to correct pane stack`() {
    val navigator = createNavigator(initialPaneState)
    
    navigator.navigateToPane(PaneRole.Supporting, DetailDestination("1"))
    
    val paneNode = navigator.state.value as PaneNode
    val supportingStack = paneNode.paneContent(PaneRole.Supporting) as StackNode
    assertEquals(1, supportingStack.size)
    assertEquals("1", (supportingStack.activeChild as ScreenNode).destination.arguments["id"])
}

@Test
fun `navigateToPane with switchFocus=false keeps active pane unchanged`() {
    val navigator = createNavigator(initialPaneState) // activePaneRole = Primary
    
    navigator.navigateToPane(PaneRole.Supporting, DetailDestination("1"), switchFocus = false)
    
    val paneNode = navigator.state.value as PaneNode
    assertEquals(PaneRole.Primary, paneNode.activePaneRole)
}

@Test
fun `navigateBack respects PopUntilScaffoldValueChange`() {
    // Start with Supporting pane active, empty stack
    val navigator = createNavigator(paneStateWithEmptySupportingActive)
    
    val result = navigator.navigateBack()
    
    assertTrue(result)
    assertEquals(PaneRole.Primary, navigator.activePaneRole)
}
```

---

## References

- [CORE-001](./CORE-001-navnode-hierarchy.md) - NavNode hierarchy with enhanced PaneNode
- [CORE-003](./CORE-003-navigator-refactor.md) - Navigator refactor task
- [Architecture Plan](../../Refactoring%20Quo-Vadis%20Navigation%20Architecture.md) - Original refactoring plan
