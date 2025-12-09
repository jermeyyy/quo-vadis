# HIER-022: PaneRenderer

## Task Metadata

| Property | Value |
|----------|-------|
| **Task ID** | HIER-022 |
| **Task Name** | Create PaneRenderer |
| **Phase** | Phase 3: Renderer Implementation |
| **Complexity** | Large |
| **Estimated Time** | 3-4 days |
| **Dependencies** | HIER-016, HIER-008 (WrapperScopes), HIER-002 (WrapperRegistry) |
| **Blocked By** | HIER-002, HIER-008 |
| **Blocks** | HIER-024 |

---

## Overview

The `PaneRenderer` renders a `PaneNode` with adaptive layout based on window size class. In expanded mode, it shows multiple panes via the wrapper. In compact mode, it behaves like a stack showing only the focused pane.

---

## File Location

```
quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/hierarchical/PaneRenderer.kt
```

---

## Implementation

```kotlin
package com.jermey.quo.vadis.core.navigation.compose.hierarchical

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.jermey.quo.vadis.core.navigation.compose.scope.PaneContentSlot
import com.jermey.quo.vadis.core.navigation.compose.scope.createPaneContentScope
import com.jermey.quo.vadis.core.navigation.compose.scope.createPaneWrapperScope
import com.jermey.quo.vadis.core.navigation.core.PaneNode
import com.jermey.quo.vadis.core.navigation.core.PaneRole

/**
 * Renders a [PaneNode] with adaptive layout wrapper.
 *
 * ## Layout Modes
 * - **Expanded**: Multiple panes visible, wrapper determines layout
 * - **Compact**: Single pane visible, behaves like stack
 *
 * @param node The pane node to render
 * @param previousNode Previous pane node for animation
 * @param scope Rendering context
 * @param modifier Modifier to apply
 */
@Composable
internal fun PaneRenderer(
    node: PaneNode,
    previousNode: PaneNode?,
    scope: NavRenderScope,
    modifier: Modifier = Modifier
) {
    // Detect window size class
    val windowSizeClass = calculateWindowSizeClass()
    val isExpanded = windowSizeClass.widthSizeClass >= WindowWidthSizeClass.Expanded
    
    // Build pane content slots
    val paneContentSlots = remember(node, isExpanded) {
        buildPaneContentSlots(node, isExpanded, scope)
    }
    
    // Create wrapper scope
    val paneWrapperScope = remember(node.activePaneRole, isExpanded, paneContentSlots) {
        createPaneWrapperScope(
            navigator = scope.navigator,
            paneContents = paneContentSlots,
            activePaneRole = node.activePaneRole,
            isExpanded = isExpanded,
            onNavigateToPane = { role ->
                scope.navigator.focusPane(node.key, role)
            }
        )
    }
    
    // Cache entire pane configuration
    scope.cache.CachedEntry(key = node.key) {
        if (isExpanded) {
            // Multi-pane: invoke wrapper with content scope
            scope.wrapperRegistry.PaneWrapper(
                wrapperScope = paneWrapperScope,
                paneNodeKey = node.key
            ) {
                // PaneContentScope available here
                // Wrapper decides how to render visiblePanes
            }
        } else {
            // Single-pane: animate between panes like stack
            val activeContent = node.getContent(node.activePaneRole)
            val previousContent = previousNode?.getContent(previousNode.activePaneRole)
            
            if (activeContent != null) {
                AnimatedNavContent(
                    targetState = activeContent,
                    transition = scope.animationCoordinator.getPaneTransition(
                        fromRole = previousNode?.activePaneRole,
                        toRole = node.activePaneRole
                    ),
                    scope = scope,
                    predictiveBackEnabled = node.parentKey == null,
                    modifier = modifier
                ) { content ->
                    NavTreeRenderer(
                        node = content,
                        previousNode = previousContent,
                        scope = scope
                    )
                }
            }
        }
    }
}

/**
 * Builds pane content slots from a PaneNode.
 */
private fun buildPaneContentSlots(
    node: PaneNode,
    isExpanded: Boolean,
    scope: NavRenderScope
): List<PaneContentSlot> {
    return node.paneConfigurations.map { (role, config) ->
        val isVisible = when {
            isExpanded -> true // All panes visible in expanded
            else -> role == node.activePaneRole // Only active in compact
        }
        
        PaneContentSlot(
            role = role,
            isVisible = isVisible,
            isPrimary = role == PaneRole.Primary,
            content = {
                config.content?.let { content ->
                    NavTreeRenderer(
                        node = content,
                        previousNode = null,
                        scope = scope
                    )
                }
            }
        )
    }
}

/**
 * Extension to get content for a pane role.
 */
private fun PaneNode.getContent(role: PaneRole): NavNode? {
    return paneConfigurations[role]?.content
}
```

---

## Integration Points

- **NavTreeRenderer**: Dispatches PaneNode to PaneRenderer
- **WrapperRegistry**: Provides user-defined pane wrapper
- **AnimatedNavContent**: Animates pane switches in compact mode
- **ComposableCache**: Caches entire pane structure
- **WindowSizeClass**: Determines expanded vs compact mode

---

## Acceptance Criteria

- [ ] `PaneRenderer` composable with node, previousNode, scope, modifier
- [ ] Detects window size class for expanded/compact
- [ ] Creates `PaneWrapperScope` with correct state
- [ ] Caches ENTIRE PaneNode
- [ ] Expanded: invokes `WrapperRegistry.PaneWrapper` with content scope
- [ ] Compact: uses `AnimatedNavContent` for pane switching
- [ ] `buildPaneContentSlots` creates correct visibility states
- [ ] `predictiveBackEnabled` for root panes in compact mode
- [ ] KDoc documentation
