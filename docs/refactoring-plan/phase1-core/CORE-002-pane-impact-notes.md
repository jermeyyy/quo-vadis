# CORE-002 Impact Notes: PaneNode Enhancement Effects

## Overview

This document describes the impact of CORE-001 `PaneNode` enhancements on CORE-002 (TreeMutator Operations).

**Related Changes:** CORE-001 `PaneNode` redesign with role-based pane configurations.

---

## Summary of PaneNode Changes

The `PaneNode` structure has been redesigned:

| Aspect | Old Design | New Design |
|--------|-----------|------------|
| Pane Storage | `List<NavNode>` | `Map<PaneRole, PaneConfiguration>` |
| Pane Identification | Index-based (`panes[0]`) | Role-based (`PaneRole.Primary`) |
| Active Pane | `activePaneIndex: Int` | `activePaneRole: PaneRole` |
| Adaptation | Not specified | `AdaptStrategy` per pane |
| Back Behavior | Generic | `PaneBackBehavior` enum |

---

## Required TreeMutator Operations

### New Pane-Specific Operations

#### 1. `navigateToPane(role: PaneRole, destination: Destination)`

Navigate within a specific pane by role.

```kotlin
/**
 * Navigates to a destination within the specified pane role.
 * 
 * If the pane contains a StackNode, pushes the destination.
 * Optionally switches [activePaneRole] to the target pane.
 *
 * @param nodeKey Key of the PaneNode to mutate
 * @param role Target pane role
 * @param destination Destination to navigate to
 * @param switchFocus Whether to also set activePaneRole to this role
 */
fun TreeMutator.navigateToPane(
    nodeKey: String,
    role: PaneRole,
    destination: Destination,
    switchFocus: Boolean = true
): NavNode
```

#### 2. `switchActivePane(role: PaneRole)`

Change the active (focused) pane without navigation.

```kotlin
/**
 * Switches the active pane role without navigating.
 * 
 * This affects which pane receives subsequent navigation operations
 * and may influence visibility on compact screens.
 *
 * @param nodeKey Key of the PaneNode to mutate
 * @param role New active pane role (must exist in paneConfigurations)
 * @throws IllegalArgumentException if role is not configured
 */
fun TreeMutator.switchActivePane(
    nodeKey: String,
    role: PaneRole
): NavNode
```

#### 3. `popPane(role: PaneRole)`

Pop from a specific pane's stack.

```kotlin
/**
 * Pops the top entry from the specified pane's stack.
 *
 * @param nodeKey Key of the PaneNode to mutate
 * @param role Pane role to pop from
 * @return Updated NavNode, or null if the pane's stack is empty
 */
fun TreeMutator.popPane(
    nodeKey: String,
    role: PaneRole
): NavNode?
```

#### 4. `setPaneConfiguration(role: PaneRole, config: PaneConfiguration)`

Add or update a pane configuration.

```kotlin
/**
 * Sets or updates the configuration for a pane role.
 *
 * @param nodeKey Key of the PaneNode to mutate
 * @param role Pane role to configure
 * @param config New pane configuration
 */
fun TreeMutator.setPaneConfiguration(
    nodeKey: String,
    role: PaneRole,
    config: PaneConfiguration
): NavNode
```

#### 5. `removePaneConfiguration(role: PaneRole)`

Remove a pane configuration (except Primary).

```kotlin
/**
 * Removes a pane configuration.
 *
 * @param nodeKey Key of the PaneNode to mutate
 * @param role Pane role to remove (cannot be Primary)
 * @throws IllegalArgumentException if trying to remove Primary pane
 */
fun TreeMutator.removePaneConfiguration(
    nodeKey: String,
    role: PaneRole
): NavNode
```

---

## Back Navigation Impact

### `pop()` Behavior in PaneNode Context

The `pop()` operation must respect `PaneBackBehavior`:

```kotlin
sealed class PopResult {
    /** Successfully popped within current pane */
    data class Popped(val newState: NavNode) : PopResult()
    
    /** Pane is empty, behavior depends on PaneBackBehavior */
    data class PaneEmpty(val paneRole: PaneRole) : PopResult()
    
    /** Back behavior requires scaffold change (renderer must handle) */
    object RequiresScaffoldChange : PopResult()
}

fun TreeMutator.popWithBehavior(
    nodeKey: String,
    behavior: PaneBackBehavior = PaneBackBehavior.PopUntilScaffoldValueChange
): PopResult
```

### Behavior Matrix

| PaneBackBehavior | When Active Pane Stack Empty |
|-----------------|------------------------------|
| `PopUntilScaffoldValueChange` | Switch to Primary pane (if not already) |
| `PopUntilCurrentDestinationChange` | Switch to different pane role |
| `PopUntilContentChange` | Pop from any pane with content |
| `PopLatest` | Simple failure (nothing to pop) |

---

## Updated Acceptance Criteria for CORE-002

Add these acceptance criteria:

- [ ] `navigateToPane(role, destination)` operation implemented
- [ ] `switchActivePane(role)` operation implemented
- [ ] `popPane(role)` operation implemented
- [ ] `setPaneConfiguration(role, config)` operation implemented
- [ ] `removePaneConfiguration(role)` operation with Primary protection
- [ ] `popWithBehavior()` respects `PaneBackBehavior` settings
- [ ] All pane operations validate role existence
- [ ] Unit tests for all pane mutation operations

---

## Testing Considerations

```kotlin
@Test
fun `navigateToPane updates correct pane stack`() {
    val initialState = PaneNode(
        key = "panes",
        parentKey = null,
        paneConfigurations = mapOf(
            PaneRole.Primary to PaneConfiguration(
                content = StackNode("list-stack", "panes", listOf(listScreen))
            ),
            PaneRole.Supporting to PaneConfiguration(
                content = StackNode("detail-stack", "panes", emptyList())
            )
        )
    )
    
    val mutator = TreeMutator(initialState)
    val newState = mutator.navigateToPane(
        nodeKey = "panes",
        role = PaneRole.Supporting,
        destination = DetailDestination("123")
    )
    
    val supportingStack = newState.paneContent(PaneRole.Supporting) as StackNode
    assertEquals(1, supportingStack.size)
}

@Test
fun `popWithBehavior respects PopUntilScaffoldValueChange`() {
    // When Supporting pane is active and empty,
    // should switch to Primary pane
}
```

---

## References

- [CORE-001](./CORE-001-navnode-hierarchy.md) - NavNode hierarchy with enhanced PaneNode
- [CORE-002](./CORE-002-tree-mutator.md) - TreeMutator operations task
- [Architecture Plan](../../Refactoring%20Quo-Vadis%20Navigation%20Architecture.md) - Original refactoring plan
