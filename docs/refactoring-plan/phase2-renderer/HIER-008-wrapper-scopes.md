# HIER-008: Wrapper Scopes

## Task Metadata

| Property | Value |
|----------|-------|
| **Task ID** | HIER-008 |
| **Task Name** | Define TabWrapperScope and PaneWrapperScope |
| **Phase** | Phase 1: Core Components |
| **Complexity** | Small |
| **Estimated Time** | 0.5-1 day |
| **Dependencies** | None |
| **Blocked By** | - |
| **Blocks** | HIER-002, HIER-021, HIER-022 |

---

## Overview

The wrapper scopes provide context to user-defined wrapper composables. `TabWrapperScope` gives access to tab metadata and navigation, while `PaneWrapperScope` provides pane configuration and layout state. These scopes enable wrappers to build UIs that interact with the navigation system.

### Purpose

- Provide tab metadata (labels, icons) to tab wrappers
- Enable tab switching from wrapper UI
- Provide pane visibility and focus state
- Enable pane navigation from wrapper UI

### Design Decisions

1. **@Stable**: Allows Compose to skip when scope hasn't changed
2. **Interface-based**: Enables fake implementations for testing
3. **Immutable tab metadata**: Tab definitions don't change at runtime
4. **Reactive pane state**: Pane visibility changes with window size

---

## File Location

```
quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/scope/WrapperScopes.kt
```

---

## Implementation

```kotlin
package com.jermey.quo.vadis.core.navigation.compose.scope

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.vector.ImageVector
import com.jermey.quo.vadis.core.navigation.Navigator
import com.jermey.quo.vadis.core.navigation.core.PaneRole

/**
 * Scope provided to `@TabWrapper` annotated composables.
 *
 * Provides access to tab metadata, current selection state,
 * and navigation actions for building tab wrapper UIs.
 *
 * ## Usage
 *
 * ```kotlin
 * @TabWrapper(MainTabs::class)
 * @Composable
 * fun MainTabsWrapper(
 *     scope: TabWrapperScope,
 *     tabContent: @Composable () -> Unit
 * ) {
 *     Scaffold(
 *         bottomBar = {
 *             NavigationBar {
 *                 scope.tabs.forEachIndexed { index, tab ->
 *                     NavigationBarItem(
 *                         selected = scope.activeIndex == index,
 *                         onClick = { scope.switchTab(index) },
 *                         icon = { Icon(tab.icon, tab.label) },
 *                         label = { Text(tab.label) }
 *                     )
 *                 }
 *             }
 *         }
 *     ) { padding ->
 *         Box(Modifier.padding(padding)) {
 *             tabContent()
 *         }
 *     }
 * }
 * ```
 *
 * @see TabMetadata
 * @see WrapperRegistry
 */
@Stable
interface TabWrapperScope {
    
    /**
     * The navigator managing this tab node.
     *
     * Use for navigation operations beyond tab switching.
     */
    val navigator: Navigator
    
    /**
     * Index of the currently active tab.
     *
     * Zero-based index into [tabs] list.
     */
    val activeIndex: Int
    
    /**
     * Metadata for all tabs in this container.
     *
     * Extracted from `@TabItem` annotations on each tab's
     * sealed class/object.
     */
    val tabs: List<TabMetadata>
    
    /**
     * Switches to the tab at the given index.
     *
     * @param index Zero-based index of the tab to switch to
     * @throws IndexOutOfBoundsException if index is invalid
     */
    fun switchTab(index: Int)
    
    /**
     * The currently active tab's metadata.
     */
    val activeTab: TabMetadata
        get() = tabs[activeIndex]
}

/**
 * Metadata describing a single tab.
 *
 * Extracted from `@TabItem` annotation on the tab's destination class.
 *
 * @property label Display label for the tab
 * @property icon Icon to display (nullable for text-only tabs)
 * @property selectedIcon Optional different icon when selected
 * @property contentDescription Accessibility description
 * @property badge Optional badge content (e.g., notification count)
 */
@Immutable
data class TabMetadata(
    val label: String,
    val icon: ImageVector? = null,
    val selectedIcon: ImageVector? = null,
    val contentDescription: String = label,
    val badge: TabBadge? = null
) {
    /**
     * Returns the appropriate icon for the given selection state.
     */
    fun iconFor(selected: Boolean): ImageVector? {
        return if (selected && selectedIcon != null) selectedIcon else icon
    }
}

/**
 * Badge content for a tab.
 *
 * @property count Numeric badge (null for dot-only)
 * @property isVisible Whether badge is shown
 */
@Immutable
data class TabBadge(
    val count: Int? = null,
    val isVisible: Boolean = true
) {
    companion object {
        /**
         * Simple dot badge without count.
         */
        val Dot = TabBadge(count = null, isVisible = true)
        
        /**
         * Hidden badge.
         */
        val None = TabBadge(count = null, isVisible = false)
    }
}

/**
 * Scope provided to `@PaneWrapper` annotated composables.
 *
 * Provides access to pane configuration, visibility state,
 * and navigation actions for building adaptive pane layouts.
 *
 * ## Usage
 *
 * ```kotlin
 * @PaneWrapper(MasterDetailPanes::class)
 * @Composable
 * fun MasterDetailWrapper(
 *     scope: PaneWrapperScope,
 *     paneContent: @Composable PaneContentScope.() -> Unit
 * ) {
 *     Row {
 *         if (scope.isExpanded) {
 *             // Multi-pane layout
 *             scope.paneContents.forEach { pane ->
 *                 Box(
 *                     modifier = Modifier.weight(
 *                         if (pane.role == PaneRole.Primary) 0.4f else 0.6f
 *                     )
 *                 ) {
 *                     pane.content()
 *                 }
 *             }
 *         } else {
 *             // Single-pane layout
 *             val activePaneContent = scope.paneContents
 *                 .first { it.role == scope.activePaneRole }
 *             activePaneContent.content()
 *         }
 *     }
 * }
 * ```
 *
 * @see PaneContentSlot
 * @see PaneContentScope
 * @see WrapperRegistry
 */
@Stable
interface PaneWrapperScope {
    
    /**
     * The navigator managing this pane node.
     */
    val navigator: Navigator
    
    /**
     * Content slots for each pane.
     *
     * Each slot contains the pane's role, visibility state,
     * and composable content.
     */
    val paneContents: List<PaneContentSlot>
    
    /**
     * The currently focused pane role.
     *
     * In single-pane mode, this is the visible pane.
     * In multi-pane mode, this is the pane with focus.
     */
    val activePaneRole: PaneRole
    
    /**
     * Whether the layout is in expanded (multi-pane) mode.
     *
     * Based on window size class:
     * - true: WindowWidthSizeClass.Expanded or larger
     * - false: WindowWidthSizeClass.Compact or Medium
     */
    val isExpanded: Boolean
    
    /**
     * Navigates focus to a specific pane.
     *
     * In single-pane mode, this switches the visible pane.
     * In multi-pane mode, this updates focus state.
     *
     * @param role The pane role to navigate to
     */
    fun navigateToPane(role: PaneRole)
}

/**
 * Content slot representing a single pane.
 *
 * @property role The pane's role (Primary, Secondary, Tertiary)
 * @property isVisible Whether the pane is currently visible
 * @property isPrimary Whether this is the primary pane
 * @property content Composable content for this pane
 */
@Immutable
data class PaneContentSlot(
    val role: PaneRole,
    val isVisible: Boolean,
    val isPrimary: Boolean,
    val content: @Composable () -> Unit
) {
    /**
     * Creates a copy with different visibility.
     */
    fun withVisibility(visible: Boolean): PaneContentSlot {
        return copy(isVisible = visible)
    }
}

/**
 * Scope for rendering pane content within a wrapper.
 *
 * Provides access to visible panes for custom rendering logic.
 */
interface PaneContentScope {
    
    /**
     * List of currently visible pane slots.
     */
    val visiblePanes: List<PaneContentSlot>
}

// ============================================================================
// Implementation Classes
// ============================================================================

/**
 * Default implementation of [TabWrapperScope].
 */
@Stable
internal class TabWrapperScopeImpl(
    override val navigator: Navigator,
    override val activeIndex: Int,
    override val tabs: List<TabMetadata>,
    private val onSwitchTab: (Int) -> Unit
) : TabWrapperScope {
    
    override fun switchTab(index: Int) {
        require(index in tabs.indices) {
            "Tab index $index out of bounds for ${tabs.size} tabs"
        }
        onSwitchTab(index)
    }
}

/**
 * Default implementation of [PaneWrapperScope].
 */
@Stable
internal class PaneWrapperScopeImpl(
    override val navigator: Navigator,
    override val paneContents: List<PaneContentSlot>,
    override val activePaneRole: PaneRole,
    override val isExpanded: Boolean,
    private val onNavigateToPane: (PaneRole) -> Unit
) : PaneWrapperScope {
    
    override fun navigateToPane(role: PaneRole) {
        onNavigateToPane(role)
    }
}

/**
 * Default implementation of [PaneContentScope].
 */
internal class PaneContentScopeImpl(
    override val visiblePanes: List<PaneContentSlot>
) : PaneContentScope

// ============================================================================
// Factory Functions
// ============================================================================

/**
 * Creates a [TabWrapperScope] for a TabNode.
 *
 * @param navigator The navigator instance
 * @param activeIndex Current active tab index
 * @param tabs Tab metadata list
 * @param onSwitchTab Callback when tab is switched
 */
fun createTabWrapperScope(
    navigator: Navigator,
    activeIndex: Int,
    tabs: List<TabMetadata>,
    onSwitchTab: (Int) -> Unit
): TabWrapperScope {
    return TabWrapperScopeImpl(navigator, activeIndex, tabs, onSwitchTab)
}

/**
 * Creates a [PaneWrapperScope] for a PaneNode.
 *
 * @param navigator The navigator instance
 * @param paneContents Pane content slots
 * @param activePaneRole Currently focused pane
 * @param isExpanded Whether in expanded mode
 * @param onNavigateToPane Callback when pane navigation requested
 */
fun createPaneWrapperScope(
    navigator: Navigator,
    paneContents: List<PaneContentSlot>,
    activePaneRole: PaneRole,
    isExpanded: Boolean,
    onNavigateToPane: (PaneRole) -> Unit
): PaneWrapperScope {
    return PaneWrapperScopeImpl(
        navigator, paneContents, activePaneRole, isExpanded, onNavigateToPane
    )
}

/**
 * Creates a [PaneContentScope] from visible panes.
 */
fun createPaneContentScope(
    visiblePanes: List<PaneContentSlot>
): PaneContentScope {
    return PaneContentScopeImpl(visiblePanes)
}
```

---

## Integration Points

### Providers

- **TabRenderer** (HIER-021): Creates TabWrapperScope
- **PaneRenderer** (HIER-022): Creates PaneWrapperScope

### Consumers

- **@TabWrapper composables**: Receive TabWrapperScope
- **@PaneWrapper composables**: Receive PaneWrapperScope
- **WrapperRegistry** (HIER-002): Passes scopes to wrappers

### Related Components

| Component | Relationship |
|-----------|--------------|
| `WrapperRegistry` | Invokes wrappers with scopes (HIER-002) |
| `TabNode` | Source of tab state |
| `PaneNode` | Source of pane state |
| `@TabItem` annotation | Source of TabMetadata |

---

## Testing Requirements

### Unit Tests

```kotlin
class WrapperScopesTest {
    
    @Test
    fun `TabWrapperScope activeTab returns correct tab`() {
        val tabs = listOf(
            TabMetadata("Home"),
            TabMetadata("Profile")
        )
        val scope = createTabWrapperScope(
            navigator = FakeNavigator(),
            activeIndex = 1,
            tabs = tabs,
            onSwitchTab = {}
        )
        
        assertEquals("Profile", scope.activeTab.label)
    }
    
    @Test
    fun `TabWrapperScope switchTab validates index`() {
        val scope = createTabWrapperScope(
            navigator = FakeNavigator(),
            activeIndex = 0,
            tabs = listOf(TabMetadata("Home")),
            onSwitchTab = {}
        )
        
        assertFailsWith<IllegalArgumentException> {
            scope.switchTab(5) // Invalid index
        }
    }
    
    @Test
    fun `TabWrapperScope switchTab calls callback`() {
        var switchedTo: Int? = null
        val scope = createTabWrapperScope(
            navigator = FakeNavigator(),
            activeIndex = 0,
            tabs = listOf(TabMetadata("Home"), TabMetadata("Profile")),
            onSwitchTab = { switchedTo = it }
        )
        
        scope.switchTab(1)
        
        assertEquals(1, switchedTo)
    }
    
    @Test
    fun `TabMetadata iconFor returns selectedIcon when selected`() {
        val icon = Icons.Default.Home
        val selectedIcon = Icons.Filled.Home
        val metadata = TabMetadata(
            label = "Home",
            icon = icon,
            selectedIcon = selectedIcon
        )
        
        assertEquals(selectedIcon, metadata.iconFor(selected = true))
        assertEquals(icon, metadata.iconFor(selected = false))
    }
    
    @Test
    fun `TabMetadata iconFor returns icon when no selectedIcon`() {
        val icon = Icons.Default.Home
        val metadata = TabMetadata(label = "Home", icon = icon)
        
        assertEquals(icon, metadata.iconFor(selected = true))
        assertEquals(icon, metadata.iconFor(selected = false))
    }
    
    @Test
    fun `PaneWrapperScope navigateToPane calls callback`() {
        var navigatedTo: PaneRole? = null
        val scope = createPaneWrapperScope(
            navigator = FakeNavigator(),
            paneContents = emptyList(),
            activePaneRole = PaneRole.Primary,
            isExpanded = true,
            onNavigateToPane = { navigatedTo = it }
        )
        
        scope.navigateToPane(PaneRole.Secondary)
        
        assertEquals(PaneRole.Secondary, navigatedTo)
    }
    
    @Test
    fun `PaneContentSlot withVisibility creates correct copy`() {
        val slot = PaneContentSlot(
            role = PaneRole.Primary,
            isVisible = false,
            isPrimary = true,
            content = {}
        )
        
        val visible = slot.withVisibility(true)
        
        assertTrue(visible.isVisible)
        assertEquals(slot.role, visible.role)
        assertEquals(slot.isPrimary, visible.isPrimary)
    }
    
    @Test
    fun `TabBadge Dot has no count`() {
        assertNull(TabBadge.Dot.count)
        assertTrue(TabBadge.Dot.isVisible)
    }
    
    @Test
    fun `TabBadge None is not visible`() {
        assertFalse(TabBadge.None.isVisible)
    }
}
```

---

## Acceptance Criteria

- [ ] `TabWrapperScope` interface with navigator, activeIndex, tabs, switchTab
- [ ] `TabMetadata` data class with label, icon, selectedIcon, contentDescription, badge
- [ ] `TabBadge` data class with count and isVisible
- [ ] `PaneWrapperScope` interface with navigator, paneContents, activePaneRole, isExpanded, navigateToPane
- [ ] `PaneContentSlot` data class with role, isVisible, isPrimary, content
- [ ] `PaneContentScope` interface with visiblePanes
- [ ] Implementation classes: TabWrapperScopeImpl, PaneWrapperScopeImpl, PaneContentScopeImpl
- [ ] Factory functions: createTabWrapperScope, createPaneWrapperScope, createPaneContentScope
- [ ] `@Stable` and `@Immutable` annotations where appropriate
- [ ] Full KDoc documentation with usage examples
- [ ] Unit tests pass

---

## Notes

### Open Questions

1. Should TabMetadata support custom badge composables?
2. Should PaneWrapperScope include window size class directly?

### Design Rationale

- **Interface-based scopes**: Enables testing without real navigator
- **Immutable metadata**: Tab definitions shouldn't change at runtime
- **Separate content scope**: Gives wrapper control over rendering order
- **Factory functions**: Cleaner API than direct constructor calls
