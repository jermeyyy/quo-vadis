# HIER-016: NavTreeRenderer

## Task Metadata

| Property | Value |
|----------|-------|
| **Task ID** | HIER-016 |
| **Task Name** | Create NavTreeRenderer |
| **Phase** | Phase 3: Renderer Implementation |
| **Complexity** | Medium |
| **Estimated Time** | 1-2 days |
| **Dependencies** | HIER-001 (NavRenderScope) |
| **Blocked By** | HIER-001 |
| **Blocks** | HIER-024 |

---

## Overview

The `NavTreeRenderer` is the core recursive composable that renders the navigation tree hierarchically. It dispatches to specific renderers based on the `NavNode` type, preserving the composable hierarchy.

---

## File Location

```
quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/hierarchical/NavTreeRenderer.kt
```

---

## Implementation

```kotlin
package com.jermey.quo.vadis.core.navigation.compose.hierarchical

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jermey.quo.vadis.core.navigation.core.NavNode
import com.jermey.quo.vadis.core.navigation.core.PaneNode
import com.jermey.quo.vadis.core.navigation.core.ScreenNode
import com.jermey.quo.vadis.core.navigation.core.StackNode
import com.jermey.quo.vadis.core.navigation.core.TabNode

/**
 * Core recursive renderer for the navigation tree.
 *
 * Dispatches to specific renderers based on [NavNode] type,
 * maintaining the composable hierarchy for proper animations.
 *
 * @param node The current node to render
 * @param previousNode Previous node for animation pairing (nullable)
 * @param scope Rendering context with navigator, cache, etc.
 * @param modifier Modifier to apply to the rendered content
 */
@Composable
internal fun NavTreeRenderer(
    node: NavNode,
    previousNode: NavNode?,
    scope: NavRenderScope,
    modifier: Modifier = Modifier
) {
    when (node) {
        is ScreenNode -> ScreenRenderer(
            node = node,
            scope = scope,
            modifier = modifier
        )
        
        is StackNode -> StackRenderer(
            node = node,
            previousNode = previousNode as? StackNode,
            scope = scope,
            modifier = modifier
        )
        
        is TabNode -> TabRenderer(
            node = node,
            previousNode = previousNode as? TabNode,
            scope = scope,
            modifier = modifier
        )
        
        is PaneNode -> PaneRenderer(
            node = node,
            previousNode = previousNode as? PaneNode,
            scope = scope,
            modifier = modifier
        )
    }
}
```

---

## Integration Points

- **HierarchicalQuoVadisHost**: Entry point, calls NavTreeRenderer with root node
- **StackRenderer**: Recursively calls NavTreeRenderer for active child
- **TabRenderer**: Calls NavTreeRenderer for active tab stack
- **PaneRenderer**: Calls NavTreeRenderer for pane contents

---

## Acceptance Criteria

- [ ] `NavTreeRenderer` composable with node, previousNode, scope, modifier parameters
- [ ] When expression dispatching to ScreenRenderer, StackRenderer, TabRenderer, PaneRenderer
- [ ] Handles null previousNode gracefully
- [ ] Type-safe casting of previousNode to matching type
- [ ] KDoc documentation
