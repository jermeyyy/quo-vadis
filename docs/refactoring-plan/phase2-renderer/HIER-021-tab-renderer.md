# HIER-021: TabRenderer

## Task Metadata

| Property | Value |
|----------|-------|
| **Task ID** | HIER-021 |
| **Task Name** | Create TabRenderer |
| **Phase** | Phase 3: Renderer Implementation |
| **Complexity** | Large |
| **Estimated Time** | 3-4 days |
| **Dependencies** | HIER-016, HIER-008 (WrapperScopes), HIER-002 (WrapperRegistry), HIER-019 |
| **Blocked By** | HIER-002, HIER-008 |
| **Blocks** | HIER-024 |

---

## Overview

The `TabRenderer` renders a `TabNode` with its wrapper composing the tab content. It creates the `TabWrapperScope`, caches the ENTIRE tab structure (wrapper + content as a unit), and invokes the `WrapperRegistry` to render the user-defined wrapper.

**Critical**: The wrapper CONTAINS the content as a slot, ensuring they animate together.

---

## File Location

```
quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/hierarchical/TabRenderer.kt
```

---

## Implementation

```kotlin
package com.jermey.quo.vadis.core.navigation.compose.hierarchical

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.jermey.quo.vadis.core.navigation.compose.scope.TabMetadata
import com.jermey.quo.vadis.core.navigation.compose.scope.createTabWrapperScope
import com.jermey.quo.vadis.core.navigation.core.TabNode

/**
 * Renders a [TabNode] with wrapper containing tab content.
 *
 * ## Key Behavior
 * - Wrapper composable contains content as a slot (parent-child)
 * - Entire TabNode is cached as a unit
 * - Tab switching animates within the wrapper
 * - Predictive back transforms entire tab structure
 *
 * @param node The tab node to render
 * @param previousNode Previous tab node for animation
 * @param scope Rendering context
 * @param modifier Modifier to apply
 */
@Composable
internal fun TabRenderer(
    node: TabNode,
    previousNode: TabNode?,
    scope: NavRenderScope,
    modifier: Modifier = Modifier
) {
    val activeStack = node.stacks.getOrNull(node.activeStackIndex) ?: return
    val previousActiveStack = previousNode?.stacks?.getOrNull(previousNode.activeStackIndex)
    
    // Extract tab metadata from node
    val tabMetadata = remember(node.key) {
        extractTabMetadata(node)
    }
    
    // Create wrapper scope
    val tabWrapperScope = remember(node.activeStackIndex, tabMetadata) {
        createTabWrapperScope(
            navigator = scope.navigator,
            activeIndex = node.activeStackIndex,
            tabs = tabMetadata,
            onSwitchTab = { index ->
                scope.navigator.switchTab(node.key, index)
            }
        )
    }
    
    // Cache ENTIRE tab node (wrapper + all content)
    scope.cache.CachedEntry(key = node.key) {
        // Invoke wrapper from registry - content is a slot INSIDE wrapper
        scope.wrapperRegistry.TabWrapper(
            wrapperScope = tabWrapperScope,
            tabNodeKey = node.key
        ) {
            // Content slot: animate between tabs
            AnimatedNavContent(
                targetState = activeStack,
                transition = scope.animationCoordinator.getTabTransition(
                    fromIndex = previousNode?.activeStackIndex,
                    toIndex = node.activeStackIndex
                ),
                scope = scope,
                predictiveBackEnabled = false, // Tab switching not via predictive back
                modifier = Modifier
            ) { stack ->
                // Render active tab's stack
                NavTreeRenderer(
                    node = stack,
                    previousNode = previousActiveStack,
                    scope = scope
                )
            }
        }
    }
}

/**
 * Extracts [TabMetadata] from a [TabNode].
 *
 * Tab metadata comes from @TabItem annotations on the tab destinations.
 */
private fun extractTabMetadata(node: TabNode): List<TabMetadata> {
    return node.stacks.map { stack ->
        // Get metadata from first screen in stack (tab root)
        val tabRoot = stack.children.firstOrNull()
        TabMetadata(
            label = tabRoot?.getTabLabel() ?: "Tab",
            icon = tabRoot?.getTabIcon(),
            selectedIcon = tabRoot?.getTabSelectedIcon()
        )
    }
}

// Extension functions to get tab metadata from nodes
private fun NavNode.getTabLabel(): String? = (this as? ScreenNode)?.destination?.tabLabel
private fun NavNode.getTabIcon(): ImageVector? = (this as? ScreenNode)?.destination?.tabIcon
private fun NavNode.getTabSelectedIcon(): ImageVector? = (this as? ScreenNode)?.destination?.tabSelectedIcon
```

---

## Integration Points

- **NavTreeRenderer**: Dispatches TabNode to TabRenderer
- **WrapperRegistry**: Provides user-defined wrapper composable
- **AnimatedNavContent**: Animates between tabs within wrapper
- **ComposableCache**: Caches entire tab structure
- **TabWrapperScope**: Provides state and actions to wrapper

---

## Acceptance Criteria

- [ ] `TabRenderer` composable with node, previousNode, scope, modifier
- [ ] Creates `TabWrapperScope` with correct state
- [ ] Caches ENTIRE TabNode (wrapper + content)
- [ ] Invokes `WrapperRegistry.TabWrapper` with content slot
- [ ] Content slot uses `AnimatedNavContent` for tab switching
- [ ] Recurses via `NavTreeRenderer` for active stack
- [ ] `extractTabMetadata` extracts labels/icons
- [ ] `predictiveBackEnabled = false` for tab content
- [ ] KDoc documentation
