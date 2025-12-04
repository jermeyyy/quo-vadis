# RENDER-008: User Wrapper API for TabNode and PaneNode

## Task Metadata

| Property | Value |
|----------|-------|
| **Task ID** | RENDER-008 |
| **Task Name** | User Wrapper API for TabNode and PaneNode |
| **Phase** | Phase 2: Unified Renderer |
| **Complexity** | Medium |
| **Estimated Time** | 2-3 days |
| **Dependencies** | RENDER-001, RENDER-002B, RENDER-002C |
| **Blocked By** | RENDER-001 |
| **Blocks** | RENDER-004 |

---

## Overview

This task defines the API contract for user-provided wrapper composables for `TabNode` and `PaneNode`. The key principle is **inversion of control**: users control the wrapper structure (scaffold, app bar, tabs, bottom navigation), while the library provides content slots that render the actual navigation content.

### Design Philosophy

```
┌─────────────────────────────────────────────────────────────┐
│                    User's Wrapper                           │
│  ┌───────────────────────────────────────────────────────┐  │
│  │                   TopAppBar                           │  │
│  └───────────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────────┐  │
│  │                                                       │  │
│  │           Library-Provided Content Slot               │  │
│  │                  (tabContent())                       │  │
│  │                                                       │  │
│  └───────────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────────┐  │
│  │                 NavigationBar                         │  │
│  └───────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

This approach provides:

1. **Full UI Control** - Users decide exactly how tabs/panes are presented
2. **Library Consistency** - Navigation state is managed by the library
3. **Flexibility** - Support for Material 3, custom designs, or any UI framework
4. **Type Safety** - Strongly typed scope interfaces with navigation capabilities

### Relationship to RenderableSurface

The wrapper APIs defined here are used by the TreeFlattener (RENDER-002B, RENDER-002C) when creating `RenderableSurface` instances:

- `TabWrapper` creates surfaces with `renderingMode = TAB_WRAPPER`
- `PaneWrapper` creates surfaces with `renderingMode = PANE_WRAPPER`
- The `paneStructures` field in `RenderableSurface` is populated from `PaneContent` list

---

## File Location

```
quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/wrapper/
├── TabWrapperScope.kt
├── TabWrapper.kt
├── PaneWrapperScope.kt
├── PaneWrapper.kt
└── DefaultWrappers.kt
```

---

## Implementation

### Tab Wrapper API

#### TabWrapperScope Interface

```kotlin
package com.jermey.quo.vadis.core.navigation.compose.wrapper

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.vector.ImageVector
import com.jermey.quo.vadis.core.navigation.Navigator

/**
 * Metadata for a single tab in a [TabNode].
 * 
 * This class provides information about a tab that can be used
 * to render navigation UI elements like bottom navigation items
 * or tab bar items.
 * 
 * @property label Human-readable label for the tab
 * @property icon Optional icon for the tab
 * @property route The route identifier for this tab
 * @property contentDescription Accessibility content description
 * @property badge Optional badge content (e.g., notification count)
 */
data class TabMetadata(
    val label: String,
    val icon: ImageVector? = null,
    val route: String,
    val contentDescription: String? = null,
    val badge: String? = null
)

/**
 * Scope interface for tab wrapper composables.
 * 
 * This scope provides access to the tab navigation state and actions,
 * allowing user-defined wrappers to render custom tab UI while
 * delegating content rendering to the library.
 * 
 * ## Usage
 * 
 * The scope is receiver for [TabWrapper] composables:
 * 
 * ```kotlin
 * val myTabWrapper: TabWrapper = { tabContent ->
 *     Scaffold(
 *         bottomBar = {
 *             NavigationBar {
 *                 tabMetadata.forEachIndexed { index, meta ->
 *                     NavigationBarItem(
 *                         selected = activeTabIndex == index,
 *                         onClick = { switchTab(index) },
 *                         icon = { Icon(meta.icon, meta.label) },
 *                         label = { Text(meta.label) }
 *                     )
 *                 }
 *             }
 *         }
 *     ) { padding ->
 *         Box(modifier = Modifier.padding(padding)) {
 *             tabContent()
 *         }
 *     }
 * }
 * ```
 * 
 * @see TabWrapper
 * @see TabMetadata
 */
@Stable
interface TabWrapperScope {
    
    /**
     * The navigator instance for this tab container.
     * 
     * Can be used for programmatic navigation or accessing
     * navigation state beyond tab switching.
     */
    val navigator: Navigator
    
    /**
     * The currently active tab index (0-based).
     * 
     * Use this to highlight the selected tab in your UI.
     */
    val activeTabIndex: Int
    
    /**
     * Total number of tabs in this container.
     */
    val tabCount: Int
    
    /**
     * Metadata for all tabs in order.
     * 
     * Use this to render tab items with their labels, icons, and routes.
     */
    val tabMetadata: List<TabMetadata>
    
    /**
     * Whether tab switching animation is currently in progress.
     * 
     * Can be used to disable user interaction during transitions.
     */
    val isTransitioning: Boolean
    
    /**
     * Switch to the tab at the given index.
     * 
     * This triggers the tab change through the navigation system,
     * handling any necessary state updates and animations.
     * 
     * @param index The 0-based index of the tab to switch to
     * @throws IndexOutOfBoundsException if index is out of range
     */
    fun switchTab(index: Int)
    
    /**
     * Switch to the tab with the given route.
     * 
     * @param route The route identifier of the tab to switch to
     * @throws IllegalArgumentException if no tab with the given route exists
     */
    fun switchTab(route: String)
}
```

#### TabWrapper Type Alias

```kotlin
package com.jermey.quo.vadis.core.navigation.compose.wrapper

import androidx.compose.runtime.Composable

/**
 * Type alias for tab wrapper composables.
 * 
 * A [TabWrapper] is a user-provided composable that wraps the tab content,
 * providing the overall structure (scaffold, app bar, navigation bar, etc.)
 * while delegating the actual tab content rendering to the library.
 * 
 * ## Contract
 * 
 * - The wrapper receives a [TabWrapperScope] as receiver for accessing tab state
 * - The wrapper receives `tabContent` lambda that MUST be called exactly once
 * - The wrapper is responsible for positioning the content appropriately
 * - The library handles tab content switching and animations
 * 
 * ## Example
 * 
 * ```kotlin
 * val myTabWrapper: TabWrapper = { tabContent ->
 *     Scaffold(
 *         topBar = {
 *             TopAppBar(title = { Text("My App") })
 *         },
 *         bottomBar = {
 *             NavigationBar {
 *                 tabMetadata.forEachIndexed { index, meta ->
 *                     NavigationBarItem(
 *                         selected = activeTabIndex == index,
 *                         onClick = { switchTab(index) },
 *                         icon = { meta.icon?.let { Icon(it, meta.label) } },
 *                         label = { Text(meta.label) }
 *                     )
 *                 }
 *             }
 *         }
 *     ) { padding ->
 *         Box(modifier = Modifier.padding(padding)) {
 *             tabContent() // Library renders active tab content here
 *         }
 *     }
 * }
 * ```
 * 
 * ## Integration with TabNode
 * 
 * ```kotlin
 * TabNode(
 *     tabs = listOf(homeTab, searchTab, profileTab),
 *     wrapper = myTabWrapper
 * )
 * ```
 * 
 * @see TabWrapperScope
 * @see DefaultTabWrapper
 */
typealias TabWrapper = @Composable TabWrapperScope.(
    tabContent: @Composable () -> Unit
) -> Unit
```

### Pane Wrapper API

#### PaneWrapperScope Interface

```kotlin
package com.jermey.quo.vadis.core.navigation.compose.wrapper

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import com.jermey.quo.vadis.core.navigation.Navigator

/**
 * Defines the semantic role of a pane in a multi-pane layout.
 * 
 * Pane roles help determine appropriate sizing, animation, and
 * behavior for each pane in the layout.
 */
enum class PaneRole {
    /**
     * Primary list/navigation pane.
     * Typically narrower, shows list of items or navigation options.
     * Example: Email inbox list, file browser tree.
     */
    LIST,
    
    /**
     * Main detail/content pane.
     * Typically wider, shows selected item details.
     * Example: Email content, file preview.
     */
    DETAIL,
    
    /**
     * Supplementary information pane.
     * Optional pane for additional context or actions.
     * Example: Properties panel, comments sidebar.
     */
    SUPPLEMENTARY
}

/**
 * Represents the content of a single pane in a multi-pane layout.
 * 
 * Used by [PaneWrapper] to provide pane information and content
 * to user-defined wrapper composables.
 * 
 * @property role The semantic role of this pane
 * @property content The composable content to render in this pane
 * @property isVisible Whether this pane should be visible in the current layout
 */
data class PaneContent(
    val role: PaneRole,
    val content: @Composable () -> Unit,
    val isVisible: Boolean = true
)

/**
 * Scope interface for pane wrapper composables.
 * 
 * This scope provides access to the pane layout state and configuration,
 * allowing user-defined wrappers to implement custom multi-pane layouts
 * while delegating content rendering to the library.
 * 
 * ## Usage
 * 
 * The scope is receiver for [PaneWrapper] composables:
 * 
 * ```kotlin
 * val myPaneWrapper: PaneWrapper = { paneContents ->
 *     Row(modifier = Modifier.fillMaxSize()) {
 *         paneContents.filter { it.isVisible }.forEach { pane ->
 *             val weight = when (pane.role) {
 *                 PaneRole.LIST -> 0.35f
 *                 PaneRole.DETAIL -> 0.65f
 *                 PaneRole.SUPPLEMENTARY -> 0.25f
 *             }
 *             Box(modifier = Modifier.weight(weight)) {
 *                 pane.content()
 *             }
 *         }
 *     }
 * }
 * ```
 * 
 * @see PaneWrapper
 * @see PaneContent
 * @see PaneRole
 */
@Stable
interface PaneWrapperScope {
    
    /**
     * The navigator instance for this pane container.
     * 
     * Can be used for programmatic navigation or accessing
     * navigation state.
     */
    val navigator: Navigator
    
    /**
     * The currently active pane role.
     * 
     * In single-pane mode (e.g., on phones), this indicates
     * which pane is currently visible.
     */
    val activePaneRole: PaneRole
    
    /**
     * Total number of panes configured in this container.
     */
    val paneCount: Int
    
    /**
     * Number of panes currently visible.
     * 
     * This may differ from [paneCount] based on screen size
     * and adaptive layout configuration.
     */
    val visiblePaneCount: Int
    
    /**
     * Whether the layout is in expanded (multi-pane) mode.
     * 
     * - `true`: Multiple panes are visible side-by-side
     * - `false`: Single pane mode (stack-like behavior)
     */
    val isExpanded: Boolean
    
    /**
     * Whether a pane transition animation is in progress.
     */
    val isTransitioning: Boolean
    
    /**
     * Navigate to show the specified pane role.
     * 
     * In expanded mode, this may highlight or focus the pane.
     * In compact mode, this switches to show that pane.
     * 
     * @param role The pane role to navigate to
     */
    fun navigateToPane(role: PaneRole)
}
```

#### PaneWrapper Type Alias

```kotlin
package com.jermey.quo.vadis.core.navigation.compose.wrapper

import androidx.compose.runtime.Composable

/**
 * Type alias for pane wrapper composables.
 * 
 * A [PaneWrapper] is a user-provided composable that defines how multiple
 * panes are arranged in a multi-pane layout. The user controls the overall
 * structure (row arrangement, dividers, sizing) while the library provides
 * the content for each pane.
 * 
 * ## Contract
 * 
 * - The wrapper receives a [PaneWrapperScope] as receiver for accessing pane state
 * - The wrapper receives `paneContents` list that should be rendered
 * - Each pane's `content()` MUST be called exactly once if the pane is visible
 * - The wrapper is responsible for sizing and positioning panes
 * - The library handles pane content and navigation state
 * 
 * ## Example: Side-by-Side Layout
 * 
 * ```kotlin
 * val myPaneWrapper: PaneWrapper = { paneContents ->
 *     Row(modifier = Modifier.fillMaxSize()) {
 *         paneContents.filter { it.isVisible }.forEach { pane ->
 *             val weight = when (pane.role) {
 *                 PaneRole.LIST -> 0.35f
 *                 PaneRole.DETAIL -> 0.65f
 *                 PaneRole.SUPPLEMENTARY -> 0.25f
 *             }
 *             Box(
 *                 modifier = Modifier
 *                     .weight(weight)
 *                     .fillMaxHeight()
 *             ) {
 *                 pane.content()
 *             }
 *         }
 *     }
 * }
 * ```
 * 
 * ## Example: With Dividers
 * 
 * ```kotlin
 * val paneWrapperWithDividers: PaneWrapper = { paneContents ->
 *     val visiblePanes = paneContents.filter { it.isVisible }
 *     Row(modifier = Modifier.fillMaxSize()) {
 *         visiblePanes.forEachIndexed { index, pane ->
 *             if (index > 0) {
 *                 VerticalDivider()
 *             }
 *             Box(modifier = Modifier.weight(1f)) {
 *                 pane.content()
 *             }
 *         }
 *     }
 * }
 * ```
 * 
 * ## Integration with PaneNode
 * 
 * ```kotlin
 * PaneNode(
 *     listPane = listPaneConfig,
 *     detailPane = detailPaneConfig,
 *     wrapper = myPaneWrapper
 * )
 * ```
 * 
 * @see PaneWrapperScope
 * @see PaneContent
 * @see DefaultPaneWrapper
 */
typealias PaneWrapper = @Composable PaneWrapperScope.(
    paneContents: List<PaneContent>
) -> Unit
```

### Default Wrapper Implementations

```kotlin
package com.jermey.quo.vadis.core.navigation.compose.wrapper

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Default tab wrapper that renders a basic bottom navigation layout.
 * 
 * This implementation provides a Material 3 styled wrapper with:
 * - A [NavigationBar] at the bottom
 * - Tab content filling the remaining space
 * 
 * For custom layouts, users should provide their own [TabWrapper].
 * 
 * ## Usage
 * 
 * ```kotlin
 * TabNode(
 *     tabs = listOf(homeTab, searchTab, profileTab),
 *     wrapper = DefaultTabWrapper
 * )
 * ```
 */
val DefaultTabWrapper: TabWrapper = { tabContent ->
    Scaffold(
        bottomBar = {
            NavigationBar {
                tabMetadata.forEachIndexed { index, meta ->
                    NavigationBarItem(
                        selected = activeTabIndex == index,
                        onClick = { switchTab(index) },
                        icon = {
                            meta.icon?.let { icon ->
                                Icon(
                                    imageVector = icon,
                                    contentDescription = meta.contentDescription ?: meta.label
                                )
                            }
                        },
                        label = { Text(meta.label) },
                        enabled = !isTransitioning
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            tabContent()
        }
    }
}

/**
 * Tab wrapper with top tab row instead of bottom navigation.
 * 
 * Useful for tablet layouts or secondary tab navigation.
 */
val TopTabWrapper: TabWrapper = { tabContent ->
    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = activeTabIndex) {
            tabMetadata.forEachIndexed { index, meta ->
                Tab(
                    selected = activeTabIndex == index,
                    onClick = { switchTab(index) },
                    text = { Text(meta.label) },
                    icon = {
                        meta.icon?.let { icon ->
                            Icon(
                                imageVector = icon,
                                contentDescription = meta.contentDescription ?: meta.label
                            )
                        }
                    },
                    enabled = !isTransitioning
                )
            }
        }
        Box(modifier = Modifier.weight(1f)) {
            tabContent()
        }
    }
}

/**
 * Default pane wrapper that renders panes side-by-side with equal weight.
 * 
 * This implementation provides a basic multi-pane layout:
 * - Panes arranged horizontally in a [Row]
 * - Equal width distribution by default
 * - Vertical dividers between panes
 * 
 * For custom layouts (weighted widths, draggable dividers, etc.),
 * users should provide their own [PaneWrapper].
 * 
 * ## Usage
 * 
 * ```kotlin
 * PaneNode(
 *     listPane = listPaneConfig,
 *     detailPane = detailPaneConfig,
 *     wrapper = DefaultPaneWrapper
 * )
 * ```
 */
val DefaultPaneWrapper: PaneWrapper = { paneContents ->
    val visiblePanes = paneContents.filter { it.isVisible }
    Row(modifier = Modifier.fillMaxSize()) {
        visiblePanes.forEachIndexed { index, pane ->
            if (index > 0) {
                VerticalDivider(
                    modifier = Modifier.fillMaxHeight(),
                    thickness = 1.dp
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                pane.content()
            }
        }
    }
}

/**
 * Pane wrapper with weighted widths based on pane role.
 * 
 * Applies common width ratios:
 * - LIST: 35%
 * - DETAIL: 65%
 * - SUPPLEMENTARY: Additional 25%
 */
val WeightedPaneWrapper: PaneWrapper = { paneContents ->
    val visiblePanes = paneContents.filter { it.isVisible }
    Row(modifier = Modifier.fillMaxSize()) {
        visiblePanes.forEachIndexed { index, pane ->
            if (index > 0) {
                VerticalDivider(
                    modifier = Modifier.fillMaxHeight(),
                    thickness = 1.dp
                )
            }
            val weight = when (pane.role) {
                PaneRole.LIST -> 0.35f
                PaneRole.DETAIL -> 0.65f
                PaneRole.SUPPLEMENTARY -> 0.25f
            }
            Box(
                modifier = Modifier
                    .weight(weight)
                    .fillMaxHeight()
            ) {
                pane.content()
            }
        }
    }
}
```

### Internal Scope Implementations

```kotlin
package com.jermey.quo.vadis.core.navigation.compose.wrapper.internal

import com.jermey.quo.vadis.core.navigation.Navigator
import com.jermey.quo.vadis.core.navigation.compose.wrapper.*

/**
 * Internal implementation of [TabWrapperScope].
 * 
 * Created by the TreeFlattener when processing TabNode.
 */
internal class TabWrapperScopeImpl(
    override val navigator: Navigator,
    override val activeTabIndex: Int,
    override val tabCount: Int,
    override val tabMetadata: List<TabMetadata>,
    override val isTransitioning: Boolean,
    private val onSwitchTab: (Int) -> Unit
) : TabWrapperScope {
    
    override fun switchTab(index: Int) {
        require(index in 0 until tabCount) {
            "Tab index $index out of bounds [0, $tabCount)"
        }
        onSwitchTab(index)
    }
    
    override fun switchTab(route: String) {
        val index = tabMetadata.indexOfFirst { it.route == route }
        require(index >= 0) {
            "No tab found with route: $route"
        }
        switchTab(index)
    }
}

/**
 * Internal implementation of [PaneWrapperScope].
 * 
 * Created by the TreeFlattener when processing PaneNode.
 */
internal class PaneWrapperScopeImpl(
    override val navigator: Navigator,
    override val activePaneRole: PaneRole,
    override val paneCount: Int,
    override val visiblePaneCount: Int,
    override val isExpanded: Boolean,
    override val isTransitioning: Boolean,
    private val onNavigateToPane: (PaneRole) -> Unit
) : PaneWrapperScope {
    
    override fun navigateToPane(role: PaneRole) {
        onNavigateToPane(role)
    }
}
```

---

## Implementation Steps

### Step 1: Create Package Structure

1. Create `wrapper` package under `compose`
2. Set up file structure for all wrapper-related classes

### Step 2: Implement Tab Wrapper API

1. Define `TabMetadata` data class
2. Define `TabWrapperScope` interface
3. Define `TabWrapper` typealias
4. Implement `TabWrapperScopeImpl`

### Step 3: Implement Pane Wrapper API

1. Define `PaneRole` enum
2. Define `PaneContent` data class
3. Define `PaneWrapperScope` interface
4. Define `PaneWrapper` typealias
5. Implement `PaneWrapperScopeImpl`

### Step 4: Default Implementations

1. Implement `DefaultTabWrapper`
2. Implement `TopTabWrapper`
3. Implement `DefaultPaneWrapper`
4. Implement `WeightedPaneWrapper`

### Step 5: Integration Points

1. Add wrapper parameter to `TabNode`
2. Add wrapper parameter to `PaneNode`
3. Update TreeFlattener to use wrapper APIs
4. Connect scope implementations to navigation system

### Step 6: Documentation and Testing

1. Write comprehensive KDoc for all public APIs
2. Create unit tests for scope implementations
3. Add integration tests with sample wrappers

---

## Usage Examples

### Custom Tab Wrapper with Scaffold

```kotlin
val myTabWrapper: TabWrapper = { tabContent ->
    val snackbarHostState = remember { SnackbarHostState() }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My App") },
                actions = {
                    IconButton(onClick = { /* settings */ }) {
                        Icon(Icons.Default.Settings, "Settings")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                tabMetadata.forEachIndexed { index, meta ->
                    NavigationBarItem(
                        selected = activeTabIndex == index,
                        onClick = { switchTab(index) },
                        icon = {
                            if (meta.badge != null) {
                                BadgedBox(badge = { Badge { Text(meta.badge) } }) {
                                    meta.icon?.let { Icon(it, meta.label) }
                                }
                            } else {
                                meta.icon?.let { Icon(it, meta.label) }
                            }
                        },
                        label = { Text(meta.label) }
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            tabContent() // Library renders active tab content here
        }
    }
}
```

### Custom Pane Wrapper with Draggable Divider

```kotlin
val draggablePaneWrapper: PaneWrapper = { paneContents ->
    var listPaneWeight by remember { mutableFloatStateOf(0.35f) }
    val visiblePanes = paneContents.filter { it.isVisible }
    
    Row(modifier = Modifier.fillMaxSize()) {
        visiblePanes.forEachIndexed { index, pane ->
            val weight = when {
                pane.role == PaneRole.LIST -> listPaneWeight
                pane.role == PaneRole.DETAIL -> 1f - listPaneWeight
                else -> 0.25f
            }
            
            Box(
                modifier = Modifier
                    .weight(weight)
                    .fillMaxHeight()
            ) {
                pane.content()
            }
            
            if (index < visiblePanes.lastIndex) {
                DraggableDivider(
                    onDrag = { delta ->
                        listPaneWeight = (listPaneWeight + delta).coerceIn(0.2f, 0.6f)
                    }
                )
            }
        }
    }
}
```

### Integration with TabNode

```kotlin
// In NavGraph definition
val navGraph = navGraph {
    tabNode(
        key = "main_tabs",
        tabs = listOf(
            tab(
                route = "home",
                label = "Home",
                icon = Icons.Default.Home
            ) {
                screenNode("home_screen") { HomeScreen() }
            },
            tab(
                route = "search", 
                label = "Search",
                icon = Icons.Default.Search
            ) {
                screenNode("search_screen") { SearchScreen() }
            },
            tab(
                route = "profile",
                label = "Profile", 
                icon = Icons.Default.Person
            ) {
                screenNode("profile_screen") { ProfileScreen() }
            }
        ),
        wrapper = myTabWrapper
    )
}
```

### Integration with PaneNode

```kotlin
// In NavGraph definition
val navGraph = navGraph {
    paneNode(
        key = "email_panes",
        listPane = paneConfig(
            role = PaneRole.LIST,
            content = { EmailListPane() }
        ),
        detailPane = paneConfig(
            role = PaneRole.DETAIL,
            content = { EmailDetailPane() }
        ),
        wrapper = draggablePaneWrapper
    )
}
```

---

## Files Affected

| File | Action | Description |
|------|--------|-------------|
| `quo-vadis-core/.../compose/wrapper/TabWrapperScope.kt` | Create | Tab scope interface and metadata |
| `quo-vadis-core/.../compose/wrapper/TabWrapper.kt` | Create | TabWrapper typealias |
| `quo-vadis-core/.../compose/wrapper/PaneWrapperScope.kt` | Create | Pane scope interface and related types |
| `quo-vadis-core/.../compose/wrapper/PaneWrapper.kt` | Create | PaneWrapper typealias |
| `quo-vadis-core/.../compose/wrapper/DefaultWrappers.kt` | Create | Default implementations |
| `quo-vadis-core/.../compose/wrapper/internal/ScopeImpls.kt` | Create | Internal scope implementations |

---

## Dependencies

| Task ID | Dependency Type | Description |
|---------|----------------|-------------|
| RENDER-001 | Required | `RenderableSurface` with `paneStructures` field |
| RENDER-002B | Related | TreeFlattener for TabNode uses `TabWrapper` |
| RENDER-002C | Related | TreeFlattener for PaneNode uses `PaneWrapper` |

---

## Acceptance Criteria

- [ ] `TabWrapperScope` interface defined with all properties and methods
- [ ] `TabMetadata` data class defined with label, icon, route, contentDescription, badge
- [ ] `TabWrapper` typealias defined with proper documentation
- [ ] `PaneWrapperScope` interface defined with all properties and methods
- [ ] `PaneContent` data class defined with role, content, isVisible
- [ ] `PaneRole` enum defined with LIST, DETAIL, SUPPLEMENTARY values
- [ ] `PaneWrapper` typealias defined with proper documentation
- [ ] `DefaultTabWrapper` implementation provided with Material 3 styling
- [ ] `TopTabWrapper` implementation provided
- [ ] `DefaultPaneWrapper` implementation provided
- [ ] `WeightedPaneWrapper` implementation provided
- [ ] Internal `TabWrapperScopeImpl` implemented
- [ ] Internal `PaneWrapperScopeImpl` implemented
- [ ] Integration points with `TabNode` defined
- [ ] Integration points with `PaneNode` defined
- [ ] Unit tests for `TabWrapperScopeImpl` functionality
- [ ] Unit tests for `PaneWrapperScopeImpl` functionality
- [ ] Documentation with comprehensive usage examples
- [ ] Code compiles on all target platforms

---

## Testing Notes

### Unit Tests

```kotlin
class TabWrapperScopeTest {
    
    @Test
    fun `switchTab with valid index calls onSwitchTab`() {
        var switchedIndex = -1
        val scope = TabWrapperScopeImpl(
            navigator = mockNavigator,
            activeTabIndex = 0,
            tabCount = 3,
            tabMetadata = listOf(
                TabMetadata("Home", null, "home"),
                TabMetadata("Search", null, "search"),
                TabMetadata("Profile", null, "profile")
            ),
            isTransitioning = false,
            onSwitchTab = { switchedIndex = it }
        )
        
        scope.switchTab(1)
        
        assertEquals(1, switchedIndex)
    }
    
    @Test
    fun `switchTab with invalid index throws exception`() {
        val scope = TabWrapperScopeImpl(
            navigator = mockNavigator,
            activeTabIndex = 0,
            tabCount = 3,
            tabMetadata = emptyList(),
            isTransitioning = false,
            onSwitchTab = {}
        )
        
        assertFailsWith<IllegalArgumentException> {
            scope.switchTab(5)
        }
    }
    
    @Test
    fun `switchTab by route finds correct index`() {
        var switchedIndex = -1
        val scope = TabWrapperScopeImpl(
            navigator = mockNavigator,
            activeTabIndex = 0,
            tabCount = 3,
            tabMetadata = listOf(
                TabMetadata("Home", null, "home"),
                TabMetadata("Search", null, "search"),
                TabMetadata("Profile", null, "profile")
            ),
            isTransitioning = false,
            onSwitchTab = { switchedIndex = it }
        )
        
        scope.switchTab("search")
        
        assertEquals(1, switchedIndex)
    }
}

class PaneWrapperScopeTest {
    
    @Test
    fun `navigateToPane calls onNavigateToPane`() {
        var navigatedRole: PaneRole? = null
        val scope = PaneWrapperScopeImpl(
            navigator = mockNavigator,
            activePaneRole = PaneRole.LIST,
            paneCount = 2,
            visiblePaneCount = 2,
            isExpanded = true,
            isTransitioning = false,
            onNavigateToPane = { navigatedRole = it }
        )
        
        scope.navigateToPane(PaneRole.DETAIL)
        
        assertEquals(PaneRole.DETAIL, navigatedRole)
    }
    
    @Test
    fun `isExpanded reflects multi-pane state`() {
        val expandedScope = PaneWrapperScopeImpl(
            navigator = mockNavigator,
            activePaneRole = PaneRole.LIST,
            paneCount = 2,
            visiblePaneCount = 2,
            isExpanded = true,
            isTransitioning = false,
            onNavigateToPane = {}
        )
        
        val compactScope = PaneWrapperScopeImpl(
            navigator = mockNavigator,
            activePaneRole = PaneRole.LIST,
            paneCount = 2,
            visiblePaneCount = 1,
            isExpanded = false,
            isTransitioning = false,
            onNavigateToPane = {}
        )
        
        assertTrue(expandedScope.isExpanded)
        assertFalse(compactScope.isExpanded)
    }
}
```

### Compose UI Tests

```kotlin
class WrapperComposableTest {
    
    @Test
    fun `DefaultTabWrapper renders NavigationBar with all tabs`() {
        composeTestRule.setContent {
            val scope = remember {
                TabWrapperScopeImpl(
                    navigator = mockNavigator,
                    activeTabIndex = 0,
                    tabCount = 3,
                    tabMetadata = listOf(
                        TabMetadata("Home", Icons.Default.Home, "home"),
                        TabMetadata("Search", Icons.Default.Search, "search"),
                        TabMetadata("Profile", Icons.Default.Person, "profile")
                    ),
                    isTransitioning = false,
                    onSwitchTab = {}
                )
            }
            
            with(scope) {
                DefaultTabWrapper { Text("Content") }
            }
        }
        
        composeTestRule.onNodeWithText("Home").assertExists()
        composeTestRule.onNodeWithText("Search").assertExists()
        composeTestRule.onNodeWithText("Profile").assertExists()
    }
    
    @Test
    fun `DefaultPaneWrapper renders all visible panes`() {
        composeTestRule.setContent {
            val scope = remember {
                PaneWrapperScopeImpl(
                    navigator = mockNavigator,
                    activePaneRole = PaneRole.LIST,
                    paneCount = 2,
                    visiblePaneCount = 2,
                    isExpanded = true,
                    isTransitioning = false,
                    onNavigateToPane = {}
                )
            }
            
            with(scope) {
                DefaultPaneWrapper(
                    listOf(
                        PaneContent(PaneRole.LIST, { Text("List Pane") }),
                        PaneContent(PaneRole.DETAIL, { Text("Detail Pane") })
                    )
                )
            }
        }
        
        composeTestRule.onNodeWithText("List Pane").assertExists()
        composeTestRule.onNodeWithText("Detail Pane").assertExists()
    }
}
```

---

## References

- [INDEX](../INDEX.md) - Phase 2 Overview
- [RENDER-001](./RENDER-001-renderable-surface.md) - RenderableSurface with paneStructures
- [RENDER-002B](./RENDER-002B-tabnode-flattener.md) - TabNode TreeFlattener
- [RENDER-002C](./RENDER-002C-panenode-flattener.md) - PaneNode TreeFlattener
- [RENDER-004](./RENDER-004-quovadis-host.md) - QuoVadisHost that renders wrappers
- [Original Architecture Plan](../../Refactoring%20Quo-Vadis%20Navigation%20Architecture.md) - Section on wrapper APIs
