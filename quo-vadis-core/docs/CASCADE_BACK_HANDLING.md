# Cascade Back Handling

This document explains how Quo Vadis handles back navigation when it needs to "cascade" through multiple container levels.

## Overview

Back navigation in Quo Vadis follows intelligent cascading rules. When a stack contains only one item and cannot pop normally, the system cascades up to parent containers, potentially popping entire stacks or tab nodes.

## Cascade Rules

### Stack Back Handling

When back is pressed on a stack:

1. **Stack has multiple children** → Pop the last child (normal pop)
2. **Stack has 1 child AND parent is StackNode with multiple children** → Pop this stack from parent
3. **Stack has 1 child AND parent is StackNode with 1 child** → Cascade up (recurse)
4. **Stack has 1 child AND is root** → Delegate to system (close app)

### Tab Back Handling

When back is pressed inside a TabNode:

1. **Active tab's stack has items to pop** → Normal pop within tab
2. **Not on initial tab (index 0)** → Switch to initial tab
3. **On initial tab with 1 item AND TabNode can be popped** → Pop entire TabNode
4. **On initial tab with 1 item AND TabNode is at root** → Delegate to system

### Flow Diagram

```
┌────────────────────────────────────────────────────────────────┐
│                    STACK BACK HANDLING FLOW                     │
├────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Stack has > 1 children?                                        │
│       │                                                         │
│       ├── YES → Pop last child (normal pop)                    │
│       │                                                         │
│       └── NO → Check parent type                               │
│                   │                                             │
│                   ├── null (root) → DelegateToSystem           │
│                   │                                             │
│                   ├── StackNode → Try to remove this stack     │
│                   │                  from parent, if parent     │
│                   │                  has 1 child → cascade up   │
│                   │                                             │
│                   ├── TabNode → If initial tab, try to pop     │
│                   │             TabNode from its parent         │
│                   │                                             │
│                   └── PaneNode → Handle per pane behavior      │
│                                                                  │
└────────────────────────────────────────────────────────────────┘
```

## Cascade Examples

### Example 1: Nested Stack Cascade

```
RootStack
├── Screen A
└── ChildStack           ← Back pressed here
    └── Screen B (only child)
```

**Result**: ChildStack is popped from RootStack, Screen A becomes active.

### Example 2: Tab Container Pop

```
RootStack
├── Screen A
└── TabNode              ← Entire TabNode popped
    └── Tab 0 (initial, active)
        └── Screen B (only child)
```

**Result**: Back cascades through Tab 0's single-item stack, pops entire TabNode.

### Example 3: Deep Cascade

```
RootStack
├── Screen A
└── MiddleStack (only child = TabNode)
    └── TabNode (initial tab)
        └── TabStack (only child = Screen B)
            └── Screen B
```

**Result**: Back cascades through TabStack → TabNode → MiddleStack, all the way to pop MiddleStack from RootStack.

### Example 4: Tab Switch Before Cascade

```
RootStack
├── Screen A
└── TabNode
    ├── Tab 0 (initial)
    │   └── Screen B (only child)
    └── Tab 1 (active)       ← Back pressed here
        └── Screen C (only child)
```

**Result**: Instead of cascading, the system switches to Tab 0 (initial tab). Another back press would then cascade to pop the entire TabNode.

## Predictive Back Integration

### PredictiveBackMode

Control how predictive back gestures are handled:

```kotlin
NavigationHost(
    navigator = navigator,
    predictiveBackMode = PredictiveBackMode.ROOT_ONLY // default
)
```

| Mode | Behavior |
|------|----------|
| `ROOT_ONLY` | Only root stack shows predictive back animations. Cascade pops happen instantly after gesture completion. |
| `FULL_CASCADE` | All stacks show predictive back. Containers animate away during cascade. Provides richer visual feedback. |

### When to Use Each Mode

**Use `ROOT_ONLY` when:**
- Simple navigation structures (most navigation in root stack)
- Performance-constrained devices
- You want consistent animation behavior

**Use `FULL_CASCADE` when:**
- Complex nested navigation (tabs within tabs, nested stacks)
- Visual consistency across all back actions is important
- Users benefit from seeing what will be revealed during cascade

### CascadeBackState

When using `FULL_CASCADE` mode, the system pre-calculates what will happen on back at gesture start:

```kotlin
val cascadeState = calculateCascadeBackState(rootNode)

// What's exiting (screen, stack, or container)
cascadeState.exitingNode

// What will be revealed
cascadeState.targetNode

// How deep the cascade goes (0 = normal pop)
cascadeState.cascadeDepth

// Whether back would close the app
cascadeState.delegatesToSystem
```

## API Reference

### TreeMutator.popWithTabBehavior()

Performs back navigation with cascade support:

```kotlin
val result = TreeMutator.popWithTabBehavior(rootNode)

when (result) {
    is BackResult.Handled -> updateState(result.newState)
    is BackResult.DelegateToSystem -> finishActivity()
    is BackResult.CannotHandle -> // Unexpected
}
```

### TreeMutator.canHandleBackNavigation()

Checks if back can be handled without delegating to system:

```kotlin
if (TreeMutator.canHandleBackNavigation(rootNode)) {
    // Back will pop something or switch tabs
} else {
    // Back will close app
}
```

Considers:
- Root stack must keep at least one item
- Tab switching as an alternative to popping
- Nested stack cascade opportunities

### TreeMutator.wouldCascade() / wouldCascade()

Checks if back would result in a cascade pop:

```kotlin
if (TreeMutator.wouldCascade(rootNode)) {
    // Container will be popped (stack/tab)
} else {
    // Normal screen pop
}

// Also available as standalone function
if (wouldCascade(rootNode)) {
    // ...
}
```

### calculateCascadeBackState()

Pre-calculates the full cascade state for predictive back animations:

```kotlin
val state = calculateCascadeBackState(rootNode)

when {
    state.delegatesToSystem -> {
        // Will close app, show system animation
    }
    state.cascadeDepth == 0 -> {
        // Normal pop, animate single screen
    }
    else -> {
        // Cascade pop, animate container(s)
        // exitingNode tells you what to animate out
        // targetNode tells you what to reveal
    }
}
```

## Migration

If you're upgrading from a version without cascade support:

### Step 1: No Code Changes Required

Basic functionality works out of the box. Back navigation with cascade support is the default behavior.

### Step 2: Optional - Enable FULL_CASCADE Mode

For richer predictive back animations:

```kotlin
// Before (implicit)
NavigationHost(
    navigator = navigator
)

// After (explicit cascade animations)
NavigationHost(
    navigator = navigator,
    predictiveBackMode = PredictiveBackMode.FULL_CASCADE
)
```

### Step 3: Testing

Verify back behavior in these scenarios:
- [ ] Nested stack with single item cascades to parent
- [ ] Tab container pops when on initial tab with single item
- [ ] Non-initial tabs switch to initial tab before cascade
- [ ] Deep nesting cascades correctly

## Troubleshooting

### Back doesn't pop nested container

**Symptom**: Back returns `CannotHandle` instead of cascading.

**Check**:
- Ensure parent containers are correctly linked via `parentKey`
- Verify the navigation tree structure using logging or debugging

```kotlin
// Debug the navigation tree
println(rootNode.debugPrint())
```

### Predictive back animation jumps

**Symptom**: Animation is not smooth during cascade.

**Solution**: Enable `FULL_CASCADE` mode for smooth cascade animations.

```kotlin
NavigationHost(
    navigator = navigator,
    predictiveBackMode = PredictiveBackMode.FULL_CASCADE
)
```

### canHandleBackNavigation returns unexpected results

**Symptom**: Method returns `true` when expecting `false` or vice versa.

**Check**:
- Tab switching is considered a valid back action (not delegating to system)
- Root stack with 1 item returns `false` (delegates to system)
- Verify `activeStackIndex` for tab nodes

### Back goes to wrong screen

**Symptom**: After cascade, the wrong screen is displayed.

**Check**:
- Tab node's `activeStackIndex` is set correctly
- Stack's `activeChildKey` points to the correct child
- Parent-child relationships are properly maintained

## Performance Considerations

### ROOT_ONLY Mode
- Minimal overhead
- Cascade state calculated only on gesture completion
- Recommended for most apps

### FULL_CASCADE Mode
- `CascadeBackState` calculated at gesture start
- Additional tree traversal for cascade depth calculation
- Slight memory overhead for caching preview content
- Worth it for complex navigation with frequent cascade scenarios

## See Also

- [API_REFERENCE.md](API_REFERENCE.md#predictive-back-configuration) - PredictiveBackMode API
- [MULTIPLATFORM_PREDICTIVE_BACK.md](MULTIPLATFORM_PREDICTIVE_BACK.md) - Platform-specific predictive back details
