# Task MIG-001: BackStack-to-NavNode Converter

## Metadata

| Field | Value |
|-------|-------|
| **Task ID** | MIG-001 |
| **Name** | BackStack Migration Utility |
| **Phase** | 5 - Migration Utilities |
| **Complexity** | Medium |
| **Estimated Time** | 2 days |
| **Dependencies** | CORE-001 (NavNode types) |

## Overview

Create utility functions to convert existing `List<BackStackEntry>` to `NavNode` tree structure, enabling gradual migration from the old linear model.

## Implementation

```kotlin
// quo-vadis-core/src/commonMain/kotlin/.../migration/BackStackMigration.kt

package com.jermey.quo.vadis.core.navigation.migration

/**
 * Converts a list of BackStackEntry to a StackNode tree.
 */
fun List<BackStackEntry>.toStackNode(
    stackKey: String = "migrated_stack",
    parentKey: String? = null
): StackNode {
    return StackNode(
        key = stackKey,
        parentKey = parentKey,
        children = mapIndexed { index, entry ->
            ScreenNode(
                key = "${stackKey}_screen_$index",
                parentKey = stackKey,
                destination = entry.destination
            )
        }
    )
}

/**
 * Converts a BackStackEntry to a ScreenNode.
 */
fun BackStackEntry.toScreenNode(
    parentKey: String
): ScreenNode = ScreenNode(
    key = "screen_${id}",
    parentKey = parentKey,
    destination = destination
)

/**
 * Migration helper for Navigator instances.
 */
fun Navigator.migrateToNavNode(): NavNode {
    @Suppress("DEPRECATION")
    return backStack.toStackNode()
}
```

## Files Affected

| File | Change Type |
|------|-------------|
| `quo-vadis-core/src/commonMain/kotlin/.../migration/BackStackMigration.kt` | New |

## Acceptance Criteria

- [ ] `List<BackStackEntry>.toStackNode()` converts correctly
- [ ] Empty lists produce empty StackNode
- [ ] Destination metadata preserved
- [ ] Works across all platforms
