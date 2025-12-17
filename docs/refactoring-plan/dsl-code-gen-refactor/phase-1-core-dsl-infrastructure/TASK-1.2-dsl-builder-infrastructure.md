# Task 1.2: Create DSL Builder Infrastructure

> **Task Status**: ⬜ Not Started  
> **Estimated Effort**: 4-5 days  
> **Dependencies**: Task 1.1 (NavigationConfig Interface)  
> **Blocks**: Task 1.3 (DslNavigationConfig Implementation)

---

## Objective

Create the type-safe DSL builder infrastructure that enables declarative navigation configuration. The builders will allow developers (and the KSP generator) to define screens, containers, scopes, and transitions using an idiomatic Kotlin DSL syntax.

**Target DSL Usage**:
```kotlin
val config = navigationConfig {
    // Screens
    screen<HomeDestination> { dest -> HomeScreen(dest, navigator) }
    screen<ProfileDestination> { dest -> ProfileScreen(dest.userId, navigator) }
    
    // Tab container
    tabs<MainTabs>(scopeKey = "main") {
        initialTab = 0
        tab(MainTabs.Home, title = "Home", icon = Icons.Home)
        tab(MainTabs.Search, title = "Search") {
            screen(SearchDestination.List)
        }
    }
    
    // Stack container
    stack<ProfileStack>(scopeKey = "profile") {
        screen(ProfileDestination.Main)
    }
    
    // Pane container
    panes<MasterDetail>(scopeKey = "detail") {
        primary(weight = 0.4f) { root(ListDestination) }
        secondary(weight = 0.6f) { root(DetailDestination) }
    }
    
    // Scopes
    scope("main", MainTabs.Home::class, MainTabs.Search::class)
    
    // Transitions
    transition<DetailDestination>(NavTransitions.SharedElement)
    
    // Wrappers
    tabsContainer("main") { CustomTabBar { content() } }
}
```

---

## Files to Create

### DSL Package Structure

**Base Path**: `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/dsl/`

| File | Description |
|------|-------------|
| `NavigationConfigDsl.kt` | DSL marker annotation to prevent scope leakage |
| `NavigationConfigBuilder.kt` | Main entry point builder class |
| `StackBuilder.kt` | Builder for stack containers |
| `TabsBuilder.kt` | Builder for tab containers |
| `PanesBuilder.kt` | Builder for pane containers |
| `PaneContentBuilder.kt` | Builder for individual pane content |
| `ContainerBuilder.kt` | Sealed class hierarchy for container types |
| `BuilderDataClasses.kt` | Data classes for intermediate structures |

---

## Code Structure and Examples

### NavigationConfigDsl.kt

```kotlin
package com.jermey.quo.vadis.core.navigation.dsl

/**
 * DSL marker annotation for navigation configuration builders.
 * 
 * Prevents implicit access to outer builder scopes, ensuring
 * that DSL methods are called in the correct context.
 * 
 * ## Why This Matters
 * Without this marker, nested builders could accidentally call
 * methods from parent scopes:
 * 
 * ```kotlin
 * // Without @DslMarker - WRONG behavior:
 * tabs<MainTabs> {
 *     tab(HomeTab)
 *     screen(SomeDestination) // Oops! Called outer screen() instead of being an error
 * }
 * ```
 * 
 * With `@NavigationConfigDsl`, the compiler will prevent such mistakes.
 */
@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class NavigationConfigDsl
```

### NavigationConfigBuilder.kt

```kotlin
package com.jermey.quo.vadis.core.navigation.dsl

import androidx.compose.runtime.Composable
import com.jermey.quo.vadis.core.navigation.NavigationConfig
import com.jermey.quo.vadis.core.navigation.compose.animation.NavTransition
import com.jermey.quo.vadis.core.navigation.compose.registry.*
import com.jermey.quo.vadis.core.navigation.compose.wrapper.*
import com.jermey.quo.vadis.core.navigation.core.*
import kotlin.reflect.KClass

/**
 * Main DSL builder for constructing [NavigationConfig] instances.
 * 
 * This builder provides a type-safe, declarative way to configure
 * all aspects of navigation: screens, containers, scopes, transitions,
 * and wrappers.
 * 
 * ## Usage
 * ```kotlin
 * val config = navigationConfig {
 *     screen<HomeDestination> { HomeScreen(navigator) }
 *     
 *     tabs<MainTabs>(scopeKey = "main") {
 *         tab(MainTabs.Home, title = "Home")
 *         tab(MainTabs.Profile, title = "Profile")
 *     }
 *     
 *     transition<DetailScreen>(NavTransitions.Slide)
 * }
 * ```
 * 
 * @see navigationConfig Top-level entry point function
 */
@NavigationConfigDsl
class NavigationConfigBuilder {
    
    // ========================================
    // Screen Registrations
    // ========================================
    
    internal val screens = mutableMapOf<KClass<out Destination>, ScreenEntry>()
    
    /**
     * Register a screen for a destination type.
     * 
     * @param D The destination type this screen handles
     * @param content Composable content for the screen
     * 
     * ## Example
     * ```kotlin
     * screen<ProfileDestination> { dest ->
     *     ProfileScreen(userId = dest.userId, navigator = navigator)
     * }
     * ```
     */
    inline fun <reified D : Destination> screen(
        noinline content: @Composable ScreenScope.(D) -> Unit
    ) {
        screens[D::class] = ScreenEntry(
            destinationClass = D::class,
            content = { destination, scope ->
                @Suppress("UNCHECKED_CAST")
                content(scope, destination as D)
            }
        )
    }
    
    // ========================================
    // Container Registrations
    // ========================================
    
    internal val containers = mutableMapOf<KClass<out Destination>, ContainerBuilder>()
    
    /**
     * Register a stack container.
     * 
     * Stack containers manage a single back stack of screens.
     * 
     * @param D The container destination type
     * @param scopeKey Scope key for this container (defaults to class simple name)
     * @param builder DSL builder for stack configuration
     * 
     * ## Example
     * ```kotlin
     * stack<ProfileStack>(scopeKey = "profile") {
     *     screen(ProfileDestination.Main)
     *     screen(ProfileDestination.Settings)
     * }
     * ```
     */
    inline fun <reified D : Destination> stack(
        scopeKey: String? = null,
        noinline builder: StackBuilder.() -> Unit = {}
    ) {
        containers[D::class] = ContainerBuilder.Stack(
            destinationClass = D::class,
            scopeKey = scopeKey ?: D::class.simpleName ?: "stack",
            builder = builder
        )
    }
    
    /**
     * Register a tab container.
     * 
     * Tab containers manage multiple parallel stacks, typically
     * displayed with a tab bar UI.
     * 
     * @param D The container destination type
     * @param scopeKey Scope key for this container
     * @param wrapperKey Key for tab wrapper lookup (optional)
     * @param builder DSL builder for tabs configuration
     * 
     * ## Example
     * ```kotlin
     * tabs<MainTabs>(scopeKey = "main", wrapperKey = "mainTabBar") {
     *     initialTab = 0
     *     
     *     tab(MainTabs.Home, title = "Home", icon = Icons.Home)
     *     
     *     tab(MainTabs.Search, title = "Search") {
     *         screen(SearchDestination.Root)
     *     }
     * }
     * ```
     */
    inline fun <reified D : Destination> tabs(
        scopeKey: String? = null,
        wrapperKey: String? = null,
        noinline builder: TabsBuilder.() -> Unit
    ) {
        containers[D::class] = ContainerBuilder.Tabs(
            destinationClass = D::class,
            scopeKey = scopeKey ?: D::class.simpleName ?: "tabs",
            wrapperKey = wrapperKey,
            builder = builder
        )
    }
    
    /**
     * Register a pane container.
     * 
     * Pane containers manage multiple panes for adaptive layouts
     * (e.g., list-detail on large screens, single pane on phones).
     * 
     * @param D The container destination type
     * @param scopeKey Scope key for this container
     * @param wrapperKey Key for pane wrapper lookup (optional)
     * @param builder DSL builder for panes configuration
     * 
     * ## Example
     * ```kotlin
     * panes<MasterDetail>(scopeKey = "masterDetail") {
     *     initialPane = PaneRole.Primary
     *     backBehavior = PaneBackBehavior.PopOrSwitchPane
     *     
     *     primary(weight = 0.4f, minWidth = 240.dp) {
     *         root(ListDestination)
     *     }
     *     
     *     secondary(weight = 0.6f, minWidth = 320.dp) {
     *         root(DetailDestination)
     *     }
     * }
     * ```
     */
    inline fun <reified D : Destination> panes(
        scopeKey: String? = null,
        wrapperKey: String? = null,
        noinline builder: PanesBuilder.() -> Unit
    ) {
        containers[D::class] = ContainerBuilder.Panes(
            destinationClass = D::class,
            scopeKey = scopeKey ?: D::class.simpleName ?: "panes",
            wrapperKey = wrapperKey,
            builder = builder
        )
    }
    
    // ========================================
    // Scope Definitions
    // ========================================
    
    internal val scopes = mutableMapOf<String, MutableSet<KClass<out Destination>>>()
    
    /**
     * Define explicit scope membership for destinations.
     * 
     * Scopes are automatically inferred from container definitions,
     * but this method allows explicit overrides or additional groupings.
     * 
     * @param key The scope key
     * @param destinations Classes that belong to this scope
     * 
     * ## Example
     * ```kotlin
     * scope("auth", 
     *     LoginDestination::class, 
     *     RegisterDestination::class,
     *     ForgotPasswordDestination::class
     * )
     * ```
     */
    fun scope(key: String, vararg destinations: KClass<out Destination>) {
        scopes.getOrPut(key) { mutableSetOf() }.addAll(destinations)
    }
    
    // ========================================
    // Transition Registrations
    // ========================================
    
    internal val transitions = mutableMapOf<KClass<out Destination>, NavTransition>()
    
    /**
     * Register a transition for a destination type.
     * 
     * @param D The destination type
     * @param transition The transition to use for this destination
     * 
     * ## Example
     * ```kotlin
     * transition<DetailDestination>(NavTransitions.SharedElement)
     * transition<ModalDestination>(NavTransitions.BottomSheet)
     * ```
     */
    inline fun <reified D : Destination> transition(transition: NavTransition) {
        transitions[D::class] = transition
    }
    
    // ========================================
    // Wrapper Registrations
    // ========================================
    
    internal val tabsContainers = mutableMapOf<String, @Composable TabsContainerScope.() -> Unit>()
    internal val paneContainers = mutableMapOf<String, @Composable PaneContainerScope.() -> Unit>()
    
    /**
     * Register a tabs container.
     * 
     * Tabs containers provide custom UI around tab content,
     * typically implementing the tab bar.
     * 
     * @param key Wrapper key (referenced in tabs() builder)
     * @param wrapper Composable wrapper content
     * 
     * ## Example
     * ```kotlin
     * tabsContainer("main") {
     *     CustomTabBar(
     *         tabs = tabs,
     *         selectedIndex = activeTabIndex,
     *         onTabSelected = { switchToTab(it) }
     *     ) {
     *         content()
     *     }
     * }
     * ```
     */
    fun tabsContainer(key: String, wrapper: @Composable TabsContainerScope.() -> Unit) {
        tabsContainers[key] = wrapper
    }
    
    /**
     * Register a pane container.
     * 
     * Pane containers provide custom UI around pane content,
     * typically implementing adaptive layouts.
     * 
     * @param key Wrapper key (referenced in panes() builder)
     * @param wrapper Composable wrapper content
     * 
     * ## Example
     * ```kotlin
     * paneContainer("masterDetail") {
     *     TwoPaneLayout(
     *         primaryContent = { primaryPaneContent() },
     *         secondaryContent = { secondaryPaneContent() }
     *     )
     * }
     * ```
     */
    fun paneContainer(key: String, wrapper: @Composable PaneContainerScope.() -> Unit) {
        paneContainers[key] = wrapper
    }
    
    // ========================================
    // Build Method
    // ========================================
    
    /**
     * Builds the final [NavigationConfig] from this builder's state.
     * 
     * This method is called internally by [navigationConfig].
     * 
     * @return The constructed NavigationConfig
     */
    fun build(): NavigationConfig {
        return DslNavigationConfig(
            screens = screens.toMap(),
            containers = containers.toMap(),
            scopes = scopes.mapValues { it.value.toSet() },
            transitions = transitions.toMap(),
            tabsContainers = tabsContainers.toMap(),
            paneContainers = paneContainers.toMap()
        )
    }
}

/**
 * Top-level DSL entry point for creating a [NavigationConfig].
 * 
 * ## Example
 * ```kotlin
 * val config = navigationConfig {
 *     screen<HomeDestination> { HomeScreen(navigator) }
 *     
 *     tabs<MainTabs>(scopeKey = "main") {
 *         tab(MainTabs.Home, title = "Home")
 *     }
 * }
 * ```
 * 
 * @param builder Configuration block
 * @return The constructed NavigationConfig
 */
fun navigationConfig(builder: NavigationConfigBuilder.() -> Unit): NavigationConfig {
    return NavigationConfigBuilder().apply(builder).build()
}

/**
 * Scope interface for screen composable content.
 * 
 * Provides access to navigation context within screen content.
 */
interface ScreenScope {
    val navigator: Navigator
    // Add other contextual properties as needed
}
```

### StackBuilder.kt

```kotlin
package com.jermey.quo.vadis.core.navigation.dsl

import com.jermey.quo.vadis.core.navigation.core.Destination

/**
 * DSL builder for stack container configuration.
 * 
 * Defines the initial screens in a stack container.
 * 
 * ## Example
 * ```kotlin
 * stack<ProfileStack> {
 *     screen(ProfileDestination.Main)
 *     screen(ProfileDestination.Settings, key = "settings")
 * }
 * ```
 */
@NavigationConfigDsl
class StackBuilder {
    
    internal val screens = mutableListOf<StackScreenEntry>()
    
    /**
     * Add a screen to the stack.
     * 
     * @param destination The destination instance for this screen
     * @param key Optional custom key (defaults to destination class name)
     */
    fun screen(
        destination: Destination,
        key: String = destination::class.simpleName ?: "screen"
    ) {
        screens.add(StackScreenEntry(destination = destination, key = key))
    }
    
    /**
     * Add a screen to the stack using destination class.
     * Destination instance will be created from class.
     * 
     * @param D The destination type
     * @param key Optional custom key
     */
    inline fun <reified D : Destination> screen(key: String? = null) {
        // Note: This requires D to have a no-arg constructor
        // or use KSP to generate the default instance
        screens.add(
            StackScreenEntry(
                destinationClass = D::class,
                key = key ?: D::class.simpleName ?: "screen"
            )
        )
    }
}

/**
 * Entry representing a screen in a stack.
 */
data class StackScreenEntry(
    val destination: Destination? = null,
    val destinationClass: kotlin.reflect.KClass<out Destination>? = null,
    val key: String
) {
    init {
        require(destination != null || destinationClass != null) {
            "Either destination instance or destinationClass must be provided"
        }
    }
}
```

### TabsBuilder.kt

```kotlin
package com.jermey.quo.vadis.core.navigation.dsl

import com.jermey.quo.vadis.core.navigation.core.Destination
import kotlin.reflect.KClass

/**
 * DSL builder for tab container configuration.
 * 
 * Defines tabs, their content, and tab bar behavior.
 * 
 * ## Example
 * ```kotlin
 * tabs<MainTabs>(scopeKey = "main") {
 *     initialTab = 0
 *     
 *     // Simple flat tab
 *     tab(MainTabs.Home, title = "Home", icon = Icons.Home)
 *     
 *     // Tab with nested stack
 *     tab(MainTabs.Search, title = "Search") {
 *         screen(SearchDestination.Root)
 *     }
 *     
 *     // Tab referencing another container
 *     containerTab(
 *         containerClass = ProfileStack::class,
 *         title = "Profile"
 *     )
 * }
 * ```
 */
@NavigationConfigDsl
class TabsBuilder {
    
    internal val tabs = mutableListOf<TabEntry>()
    
    /**
     * Index of the initially selected tab.
     * Defaults to 0 (first tab).
     */
    var initialTab: Int = 0
    
    /**
     * Add a flat screen tab (no nested navigation).
     * 
     * @param destination The tab's root destination
     * @param title Display title for the tab
     * @param icon Icon for the tab (type depends on UI framework)
     */
    fun tab(
        destination: Destination,
        title: String? = null,
        icon: Any? = null
    ) {
        tabs.add(
            TabEntry.FlatScreen(
                destination = destination,
                title = title,
                icon = icon
            )
        )
    }
    
    /**
     * Add a tab with nested stack navigation.
     * 
     * @param destination The tab's root destination
     * @param title Display title for the tab
     * @param icon Icon for the tab
     * @param builder Stack builder for nested screens
     */
    fun tab(
        destination: Destination,
        title: String? = null,
        icon: Any? = null,
        builder: StackBuilder.() -> Unit
    ) {
        tabs.add(
            TabEntry.NestedStack(
                destination = destination,
                title = title,
                icon = icon,
                stackBuilder = builder
            )
        )
    }
    
    /**
     * Add a tab that references another container definition.
     * 
     * Use this when a tab should host a container defined elsewhere.
     * 
     * @param containerClass The container class to reference
     * @param title Display title for the tab
     * @param icon Icon for the tab
     */
    fun containerTab(
        containerClass: KClass<out Destination>,
        title: String? = null,
        icon: Any? = null
    ) {
        tabs.add(
            TabEntry.ContainerReference(
                containerClass = containerClass,
                title = title,
                icon = icon
            )
        )
    }
}

/**
 * Sealed hierarchy representing different tab types.
 */
sealed class TabEntry {
    abstract val title: String?
    abstract val icon: Any?
    
    /**
     * A tab with a single flat screen (no nested stack).
     */
    data class FlatScreen(
        val destination: Destination,
        override val title: String?,
        override val icon: Any?
    ) : TabEntry()
    
    /**
     * A tab with nested stack navigation.
     */
    data class NestedStack(
        val destination: Destination,
        override val title: String?,
        override val icon: Any?,
        val stackBuilder: StackBuilder.() -> Unit
    ) : TabEntry()
    
    /**
     * A tab that references another container.
     */
    data class ContainerReference(
        val containerClass: KClass<out Destination>,
        override val title: String?,
        override val icon: Any?
    ) : TabEntry()
}
```

### PanesBuilder.kt

```kotlin
package com.jermey.quo.vadis.core.navigation.dsl

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jermey.quo.vadis.core.navigation.core.Destination
import com.jermey.quo.vadis.core.navigation.core.PaneBackBehavior
import com.jermey.quo.vadis.core.navigation.core.PaneRole

/**
 * DSL builder for pane container configuration.
 * 
 * Defines panes for adaptive multi-pane layouts.
 * 
 * ## Example
 * ```kotlin
 * panes<MasterDetail>(scopeKey = "masterDetail") {
 *     initialPane = PaneRole.Primary
 *     backBehavior = PaneBackBehavior.PopOrSwitchPane
 *     
 *     primary(weight = 0.4f, minWidth = 240.dp) {
 *         root(ListDestination)
 *         alwaysVisible()
 *     }
 *     
 *     secondary(weight = 0.6f, minWidth = 320.dp) {
 *         root(DetailDestination)
 *     }
 *     
 *     tertiary(weight = 0.3f) {
 *         root(PreviewDestination)
 *     }
 * }
 * ```
 */
@NavigationConfigDsl
class PanesBuilder {
    
    internal val panes = mutableMapOf<PaneRole, PaneEntry>()
    
    /**
     * Which pane is initially active/focused.
     * Defaults to Primary.
     */
    var initialPane: PaneRole = PaneRole.Primary
    
    /**
     * Back navigation behavior for the pane container.
     * Defaults to PopOrSwitchPane.
     */
    var backBehavior: PaneBackBehavior = PaneBackBehavior.PopOrSwitchPane
    
    /**
     * Configure the primary pane.
     * 
     * @param weight Relative weight for width distribution (default 1f)
     * @param minWidth Minimum width before collapsing (default 0.dp)
     * @param builder Pane content configuration
     */
    fun primary(
        weight: Float = 1f,
        minWidth: Dp = 0.dp,
        builder: PaneContentBuilder.() -> Unit
    ) {
        panes[PaneRole.Primary] = PaneEntry(
            role = PaneRole.Primary,
            weight = weight,
            minWidth = minWidth,
            builder = builder
        )
    }
    
    /**
     * Configure the secondary pane.
     * 
     * @param weight Relative weight for width distribution
     * @param minWidth Minimum width before collapsing
     * @param builder Pane content configuration
     */
    fun secondary(
        weight: Float = 1f,
        minWidth: Dp = 0.dp,
        builder: PaneContentBuilder.() -> Unit
    ) {
        panes[PaneRole.Secondary] = PaneEntry(
            role = PaneRole.Secondary,
            weight = weight,
            minWidth = minWidth,
            builder = builder
        )
    }
    
    /**
     * Configure the tertiary pane (optional third pane).
     * 
     * @param weight Relative weight for width distribution
     * @param minWidth Minimum width before collapsing
     * @param builder Pane content configuration
     */
    fun tertiary(
        weight: Float = 1f,
        minWidth: Dp = 0.dp,
        builder: PaneContentBuilder.() -> Unit
    ) {
        panes[PaneRole.Tertiary] = PaneEntry(
            role = PaneRole.Tertiary,
            weight = weight,
            minWidth = minWidth,
            builder = builder
        )
    }
}

/**
 * Entry representing a pane configuration.
 */
data class PaneEntry(
    val role: PaneRole,
    val weight: Float,
    val minWidth: Dp,
    val builder: PaneContentBuilder.() -> Unit
)

/**
 * DSL builder for individual pane content.
 */
@NavigationConfigDsl
class PaneContentBuilder {
    
    internal var rootDestination: Destination? = null
    internal var isAlwaysVisible: Boolean = false
    
    /**
     * Set the root destination for this pane.
     * 
     * @param destination The destination to display in this pane
     */
    fun root(destination: Destination) {
        rootDestination = destination
    }
    
    /**
     * Mark this pane as always visible.
     * 
     * Always-visible panes don't collapse on narrow screens,
     * useful for navigation rails or persistent panels.
     */
    fun alwaysVisible() {
        isAlwaysVisible = true
    }
}
```

### ContainerBuilder.kt

```kotlin
package com.jermey.quo.vadis.core.navigation.dsl

import com.jermey.quo.vadis.core.navigation.core.Destination
import kotlin.reflect.KClass

/**
 * Sealed hierarchy representing container type definitions.
 * 
 * Used internally to store container configurations before
 * they are converted to runtime structures.
 */
sealed class ContainerBuilder {
    abstract val destinationClass: KClass<out Destination>
    abstract val scopeKey: String
    
    /**
     * Stack container configuration.
     */
    data class Stack(
        override val destinationClass: KClass<out Destination>,
        override val scopeKey: String,
        val builder: StackBuilder.() -> Unit
    ) : ContainerBuilder()
    
    /**
     * Tabs container configuration.
     */
    data class Tabs(
        override val destinationClass: KClass<out Destination>,
        override val scopeKey: String,
        val wrapperKey: String?,
        val builder: TabsBuilder.() -> Unit
    ) : ContainerBuilder()
    
    /**
     * Panes container configuration.
     */
    data class Panes(
        override val destinationClass: KClass<out Destination>,
        override val scopeKey: String,
        val wrapperKey: String?,
        val builder: PanesBuilder.() -> Unit
    ) : ContainerBuilder()
}
```

### BuilderDataClasses.kt

```kotlin
package com.jermey.quo.vadis.core.navigation.dsl

import androidx.compose.runtime.Composable
import com.jermey.quo.vadis.core.navigation.core.Destination
import kotlin.reflect.KClass

/**
 * Entry representing a screen registration.
 * 
 * @property destinationClass The destination type this entry handles
 * @property content The composable content factory
 */
data class ScreenEntry(
    val destinationClass: KClass<out Destination>,
    val content: @Composable (Destination, ScreenScope) -> Unit
)

/**
 * Result of building a pane configuration.
 */
data class BuiltPaneContent(
    val rootDestination: Destination?,
    val isAlwaysVisible: Boolean
)

/**
 * Extension to build PaneContentBuilder into BuiltPaneContent.
 */
fun PaneContentBuilder.build(): BuiltPaneContent {
    return BuiltPaneContent(
        rootDestination = rootDestination,
        isAlwaysVisible = isAlwaysVisible
    )
}

/**
 * Extension to build StackBuilder into list of entries.
 */
fun StackBuilder.build(): List<StackScreenEntry> {
    return screens.toList()
}

/**
 * Extension to build TabsBuilder into configuration.
 */
data class BuiltTabsConfig(
    val tabs: List<TabEntry>,
    val initialTab: Int
)

fun TabsBuilder.build(): BuiltTabsConfig {
    return BuiltTabsConfig(
        tabs = tabs.toList(),
        initialTab = initialTab
    )
}

/**
 * Extension to build PanesBuilder into configuration.
 */
data class BuiltPanesConfig(
    val panes: Map<com.jermey.quo.vadis.core.navigation.core.PaneRole, PaneEntry>,
    val initialPane: com.jermey.quo.vadis.core.navigation.core.PaneRole,
    val backBehavior: com.jermey.quo.vadis.core.navigation.core.PaneBackBehavior
)

fun PanesBuilder.build(): BuiltPanesConfig {
    return BuiltPanesConfig(
        panes = panes.toMap(),
        initialPane = initialPane,
        backBehavior = backBehavior
    )
}
```

---

## Dependencies on Other Tasks

| Task | Dependency Type | Description |
|------|-----------------|-------------|
| Task 1.1 | **Hard** | `NavigationConfig` interface is the return type of `build()` |

**This Task Blocks**:
- Task 1.3 (DslNavigationConfig) - uses all builder classes to construct config

---

## Acceptance Criteria Checklist

### DSL Marker Annotation
- [ ] `@NavigationConfigDsl` annotation defined
- [ ] Annotation targets CLASS and TYPE
- [ ] Properly prevents scope leakage in nested builders

### NavigationConfigBuilder
- [ ] `screen<D>()` method registers screen content
- [ ] `stack<D>()` method registers stack containers
- [ ] `tabs<D>()` method registers tab containers
- [ ] `panes<D>()` method registers pane containers
- [ ] `scope()` method defines explicit scope membership
- [ ] `transition<D>()` method registers transitions
- [ ] `tabsContainer()` method registers tab containers
- [ ] `paneContainer()` method registers pane containers
- [ ] `build()` method returns `NavigationConfig`
- [ ] All methods have KDoc documentation
- [ ] Reified type parameters used where appropriate

### StackBuilder
- [ ] `screen()` method with destination instance
- [ ] `screen<D>()` method with destination class
- [ ] Optional custom key parameter
- [ ] `StackScreenEntry` data class defined

### TabsBuilder
- [ ] `tab()` method for flat screen tabs
- [ ] `tab()` method for tabs with nested stacks
- [ ] `containerTab()` method for container references
- [ ] `initialTab` property
- [ ] `TabEntry` sealed class hierarchy defined
- [ ] Title and icon parameters on all tab methods

### PanesBuilder
- [ ] `primary()`, `secondary()`, `tertiary()` methods
- [ ] `initialPane` property
- [ ] `backBehavior` property
- [ ] Weight and minWidth parameters
- [ ] `PaneEntry` data class defined

### PaneContentBuilder
- [ ] `root()` method for destination
- [ ] `alwaysVisible()` method
- [ ] `BuiltPaneContent` data class defined

### ContainerBuilder
- [ ] Sealed class with Stack, Tabs, Panes subtypes
- [ ] Common properties: destinationClass, scopeKey
- [ ] Tabs/Panes have optional wrapperKey

### Testing
- [ ] Unit test: screen registration
- [ ] Unit test: stack builder
- [ ] Unit test: tabs builder with all tab types
- [ ] Unit test: panes builder
- [ ] Unit test: scope definition
- [ ] Unit test: transition registration
- [ ] Unit test: wrapper registration
- [ ] Unit test: DSL marker prevents scope leakage (compile-time check)

### Code Quality
- [ ] All classes annotated with `@NavigationConfigDsl`
- [ ] No compiler warnings
- [ ] Consistent naming conventions
- [ ] Proper internal vs public visibility

---

## Estimated Effort Breakdown

| Activity | Time |
|----------|------|
| DSL marker annotation | 0.25 days |
| NavigationConfigBuilder | 1 day |
| StackBuilder | 0.5 days |
| TabsBuilder | 0.75 days |
| PanesBuilder + PaneContentBuilder | 0.75 days |
| ContainerBuilder sealed class | 0.25 days |
| Data classes | 0.5 days |
| Unit tests | 1 day |
| Documentation & review | 0.5 days |
| **Total** | **4-5 days** |

---

## Implementation Notes

### Design Decisions

1. **Inline + Reified for Type Safety**
   - Use `inline` + `reified` for methods like `screen<D>()` to capture type info
   - Enables compile-time type checking without runtime reflection

2. **Builder Mutability**
   - Internal mutable collections for registration
   - Immutable copies created in `build()`
   - Thread-safety not required (single-threaded DSL execution)

3. **Icon Type as Any?**
   - Using `Any?` for icon to support different UI frameworks
   - Could be ImageVector, Painter, Int resource, etc.
   - Type safety handled by wrapper implementation

4. **Optional Keys**
   - Keys default to destination class simple name
   - Explicit keys available for disambiguation

### Scope Leakage Prevention

The `@DslMarker` annotation prevents this problematic pattern:

```kotlin
// WITHOUT @DslMarker - This would compile but is wrong:
navigationConfig {
    tabs<MainTabs> {
        screen<SomeDestination> { } // Accidentally calls outer screen()!
    }
}

// WITH @DslMarker - Compiler error:
navigationConfig {
    tabs<MainTabs> {
        screen<SomeDestination> { } // Error: Can't call 'screen' in this context
        // Must use tab()
    }
}
```

### Integration with DslNavigationConfig

The `build()` method creates `DslNavigationConfig` (Task 1.3):

```kotlin
fun build(): NavigationConfig {
    return DslNavigationConfig(
        screens = screens.toMap(),
        containers = containers.toMap(),
        scopes = scopes.mapValues { it.value.toSet() },
        transitions = transitions.toMap(),
        tabsContainers = tabsContainers.toMap(),
        paneContainers = paneContainers.toMap()
    )
}
```

---

## Related Files

- [Phase 1 Summary](./SUMMARY.md)
- [Task 1.1 - NavigationConfig Interface](./TASK-1.1-navigation-config-interface.md)
- [Task 1.3 - DslNavigationConfig Implementation](./TASK-1.3-dsl-navigation-config-impl.md)
- [Full Refactoring Plan](../../DSL_CODE_GENERATION_REFACTORING.md)
