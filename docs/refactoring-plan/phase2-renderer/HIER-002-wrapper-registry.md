# HIER-002: WrapperRegistry Interface

## Task Metadata

| Property | Value |
|----------|-------|
| **Task ID** | HIER-002 |
| **Task Name** | Define WrapperRegistry Interface |
| **Phase** | Phase 1: Core Components |
| **Complexity** | Small |
| **Estimated Time** | 0.5-1 day |
| **Dependencies** | HIER-008 (WrapperScopes) |
| **Blocked By** | - |
| **Blocks** | HIER-013, HIER-021, HIER-022 |

---

## Overview

The `WrapperRegistry` interface defines the contract for KSP-generated wrapper resolution. It maps tab and pane node keys to their corresponding wrapper composables, enabling the hierarchical rendering system to invoke user-defined wrappers.

### Purpose

- Define the interface that KSP will implement
- Enable runtime resolution of wrapper composables
- Provide default fallback for nodes without custom wrappers
- Support testing via fake implementations

### Design Decisions

1. **Key-based lookup**: Uses node keys rather than class types for flexibility
2. **Default registry**: Provides passthrough behavior when no wrapper defined
3. **Composable slots**: Wrapper receives content as a composable lambda

---

## File Location

```
quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/registry/WrapperRegistry.kt
```

---

## Implementation

```kotlin
package com.jermey.quo.vadis.core.navigation.compose.registry

import androidx.compose.runtime.Composable
import com.jermey.quo.vadis.core.navigation.compose.scope.PaneContentScope
import com.jermey.quo.vadis.core.navigation.compose.scope.PaneWrapperScope
import com.jermey.quo.vadis.core.navigation.compose.scope.TabWrapperScope

/**
 * Registry for resolving tab and pane wrappers at runtime.
 *
 * This interface is implemented by KSP-generated code that maps
 * `@TabWrapper` and `@PaneWrapper` annotated composables to their
 * corresponding navigation nodes.
 *
 * ## How It Works
 *
 * 1. User annotates composables with `@TabWrapper(MyTabs::class)` or `@PaneWrapper(MyPanes::class)`
 * 2. KSP generates `GeneratedWrapperRegistry` implementing this interface
 * 3. [TabRenderer] and [PaneRenderer] invoke wrappers via this registry
 * 4. If no wrapper is defined, the default behavior renders content directly
 *
 * ## Example Generated Code
 *
 * ```kotlin
 * object GeneratedWrapperRegistry : WrapperRegistry {
 *     @Composable
 *     override fun TabWrapper(
 *         wrapperScope: TabWrapperScope,
 *         tabNodeKey: String,
 *         content: @Composable () -> Unit
 *     ) {
 *         when {
 *             tabNodeKey.contains("mainTabs") -> MainTabsWrapper(wrapperScope, content)
 *             else -> content() // Default: no wrapper
 *         }
 *     }
 * }
 * ```
 *
 * @see TabWrapperScope
 * @see PaneWrapperScope
 * @see DefaultWrapperRegistry
 */
interface WrapperRegistry {
    
    /**
     * Renders a tab wrapper for the given tab node.
     *
     * The wrapper composable receives a [TabWrapperScope] for accessing
     * tab metadata and navigation, plus a [content] slot for rendering
     * the active tab content.
     *
     * @param wrapperScope Scope providing tab state and navigation actions
     * @param tabNodeKey The key of the TabNode being rendered
     * @param content The tab content to render inside the wrapper
     */
    @Composable
    fun TabWrapper(
        wrapperScope: TabWrapperScope,
        tabNodeKey: String,
        content: @Composable () -> Unit
    )
    
    /**
     * Renders a pane wrapper for the given pane node.
     *
     * The wrapper composable receives a [PaneWrapperScope] for accessing
     * pane configuration and layout state, plus a [content] slot for
     * rendering pane contents.
     *
     * @param wrapperScope Scope providing pane state and configuration
     * @param paneNodeKey The key of the PaneNode being rendered
     * @param content The pane content to render, with access to [PaneContentScope]
     */
    @Composable
    fun PaneWrapper(
        wrapperScope: PaneWrapperScope,
        paneNodeKey: String,
        content: @Composable PaneContentScope.() -> Unit
    )
    
    /**
     * Checks if a custom tab wrapper exists for the given node.
     *
     * @param tabNodeKey The key of the TabNode to check
     * @return true if a custom wrapper is registered, false otherwise
     */
    fun hasTabWrapper(tabNodeKey: String): Boolean
    
    /**
     * Checks if a custom pane wrapper exists for the given node.
     *
     * @param paneNodeKey The key of the PaneNode to check
     * @return true if a custom wrapper is registered, false otherwise
     */
    fun hasPaneWrapper(paneNodeKey: String): Boolean
}

/**
 * Default wrapper registry that provides passthrough behavior.
 *
 * Used when:
 * - No custom wrappers are defined in the project
 * - Testing without KSP-generated code
 * - Fallback for nodes without matching wrappers
 *
 * ## Behavior
 *
 * - Tab wrapper: Renders content directly without any wrapper UI
 * - Pane wrapper: Renders primary pane content in a basic layout
 *
 * @see WrapperRegistry
 */
object DefaultWrapperRegistry : WrapperRegistry {
    
    @Composable
    override fun TabWrapper(
        wrapperScope: TabWrapperScope,
        tabNodeKey: String,
        content: @Composable () -> Unit
    ) {
        // No wrapper UI - just render content
        content()
    }
    
    @Composable
    override fun PaneWrapper(
        wrapperScope: PaneWrapperScope,
        paneNodeKey: String,
        content: @Composable PaneContentScope.() -> Unit
    ) {
        // Basic layout - render visible panes
        DefaultPaneLayout(wrapperScope, content)
    }
    
    override fun hasTabWrapper(tabNodeKey: String): Boolean = false
    
    override fun hasPaneWrapper(paneNodeKey: String): Boolean = false
}

/**
 * Default pane layout when no custom wrapper is provided.
 *
 * Renders visible panes in a row (expanded) or shows only
 * the primary pane (compact).
 */
@Composable
private fun DefaultPaneLayout(
    wrapperScope: PaneWrapperScope,
    content: @Composable PaneContentScope.() -> Unit
) {
    val scope = object : PaneContentScope {
        override val visiblePanes = wrapperScope.paneContents.filter { it.isVisible }
    }
    
    if (wrapperScope.isExpanded) {
        // Multi-pane: render side by side
        androidx.compose.foundation.layout.Row {
            scope.visiblePanes.forEach { pane ->
                androidx.compose.foundation.layout.Box(
                    modifier = androidx.compose.ui.Modifier.weight(
                        if (pane.isPrimary) 0.6f else 0.4f
                    )
                ) {
                    pane.content()
                }
            }
        }
    } else {
        // Single-pane: render primary only
        scope.visiblePanes
            .firstOrNull { it.isPrimary }
            ?.content()
    }
    
    // Invoke content for any additional rendering
    scope.content()
}
```

---

## Integration Points

### Providers

- **KSP Generator** (HIER-013): Generates `GeneratedWrapperRegistry`
- **HierarchicalQuoVadisHost** (HIER-024): Provides registry to NavRenderScope

### Consumers

- **TabRenderer** (HIER-021): Invokes `TabWrapper()` for TabNodes
- **PaneRenderer** (HIER-022): Invokes `PaneWrapper()` for PaneNodes

### Related Components

| Component | Relationship |
|-----------|--------------|
| `TabWrapperScope` | Parameter to TabWrapper (HIER-008) |
| `PaneWrapperScope` | Parameter to PaneWrapper (HIER-008) |
| `@TabWrapper` annotation | Marks wrapper composables (HIER-009) |
| `@PaneWrapper` annotation | Marks wrapper composables (HIER-010) |

---

## Testing Requirements

### Unit Tests

```kotlin
class WrapperRegistryTest {
    
    @Test
    fun `DefaultWrapperRegistry hasTabWrapper returns false`() {
        assertFalse(DefaultWrapperRegistry.hasTabWrapper("any-key"))
    }
    
    @Test
    fun `DefaultWrapperRegistry hasPaneWrapper returns false`() {
        assertFalse(DefaultWrapperRegistry.hasPaneWrapper("any-key"))
    }
    
    @Test
    fun `DefaultWrapperRegistry TabWrapper renders content`() = runComposeTest {
        var contentRendered = false
        
        setContent {
            val scope = FakeTabWrapperScope()
            DefaultWrapperRegistry.TabWrapper(scope, "test-tabs") {
                contentRendered = true
                Text("Tab Content")
            }
        }
        
        assertTrue(contentRendered)
    }
    
    @Test
    fun `DefaultWrapperRegistry PaneWrapper renders visible panes`() = runComposeTest {
        val scope = FakePaneWrapperScope(
            isExpanded = true,
            paneContents = listOf(
                PaneContentSlot(role = PaneRole.Primary, isVisible = true, isPrimary = true) { Text("Primary") },
                PaneContentSlot(role = PaneRole.Secondary, isVisible = true, isPrimary = false) { Text("Secondary") }
            )
        )
        
        setContent {
            DefaultWrapperRegistry.PaneWrapper(scope, "test-panes") {
                // Additional content
            }
        }
        
        onNodeWithText("Primary").assertExists()
        onNodeWithText("Secondary").assertExists()
    }
}
```

### Integration Tests

- Verify KSP-generated registry correctly dispatches to annotated wrappers
- Verify fallback to default when no wrapper matches

---

## Acceptance Criteria

- [ ] `WrapperRegistry` interface with `TabWrapper()` and `PaneWrapper()` composables
- [ ] `hasTabWrapper()` and `hasPaneWrapper()` query methods
- [ ] `DefaultWrapperRegistry` object with passthrough implementations
- [ ] Default pane layout for expanded/compact modes
- [ ] Full KDoc documentation
- [ ] Unit tests pass

---

## Notes

### Open Questions

1. Should `hasWrapper` methods be used for optimization (skip wrapper invocation)?
2. Should we support wrapper composition (multiple wrappers on same node)?

### Design Rationale

- **Key-based matching**: More flexible than class-based, handles nested/generated keys
- **Default registry**: Ensures system works without custom wrappers
- **Separate has* methods**: Enables optimization and debugging
