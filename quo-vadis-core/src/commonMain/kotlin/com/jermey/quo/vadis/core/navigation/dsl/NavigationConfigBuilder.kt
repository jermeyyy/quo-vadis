package com.jermey.quo.vadis.core.navigation.dsl

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import com.jermey.quo.vadis.core.navigation.DslNavigationConfig
import com.jermey.quo.vadis.core.navigation.NavigationConfig
import com.jermey.quo.vadis.core.navigation.compose.animation.NavTransition
import com.jermey.quo.vadis.core.navigation.compose.wrapper.PaneWrapperScope
import com.jermey.quo.vadis.core.navigation.compose.wrapper.TabWrapperScope
import com.jermey.quo.vadis.core.navigation.core.Destination
import com.jermey.quo.vadis.core.navigation.core.Navigator
import kotlin.reflect.KClass

/**
 * Main DSL builder for creating [NavigationConfig] instances.
 *
 * This builder provides a type-safe, declarative API for defining navigation
 * configuration including screens, containers (stacks, tabs, panes), scopes,
 * transitions, and wrappers.
 *
 * ## Usage
 *
 * ```kotlin
 * val config = navigationConfig {
 *     // Register screens
 *     screen<HomeScreen> { destination, navigator, _, _ ->
 *         HomeScreenContent(destination, navigator)
 *     }
 *
 *     // Register stack container
 *     stack<MainStack>("main-scope") {
 *         screen<HomeScreen>()
 *         screen<DetailScreen>()
 *     }
 *
 *     // Register tab container
 *     tabs<MainTabs>("tabs-scope") {
 *         initialTab = 0
 *         tab(HomeTab, title = "Home", icon = Icons.Home)
 *         tab(SettingsTab, title = "Settings", icon = Icons.Settings)
 *     }
 *
 *     // Register custom transitions
 *     transition<DetailScreen>(NavTransition.SlideHorizontal)
 *
 *     // Register wrappers
 *     tabWrapper("main-tabs") { content ->
 *         MyCustomTabBar(content)
 *     }
 * }
 * ```
 *
 * @see navigationConfig
 * @see NavigationConfig
 */
@NavigationConfigDsl
@OptIn(ExperimentalSharedTransitionApi::class)
public class NavigationConfigBuilder {

    /**
     * Registered screen content composables indexed by destination class.
     */
    @PublishedApi
    internal val screens: MutableMap<KClass<out Destination>, ScreenEntry> = mutableMapOf()

    /**
     * Registered container builders indexed by destination class.
     */
    @PublishedApi
    internal val containers: MutableMap<KClass<out Destination>, ContainerBuilder> = mutableMapOf()

    /**
     * Scope membership definitions mapping scope keys to sets of destination classes.
     */
    @PublishedApi
    internal val scopes: MutableMap<String, MutableSet<KClass<out Destination>>> = mutableMapOf()

    /**
     * Custom transitions indexed by destination class.
     */
    @PublishedApi
    internal val transitions: MutableMap<KClass<out Destination>, NavTransition> = mutableMapOf()

    /**
     * Custom tab wrapper composables indexed by wrapper key.
     */
    internal val tabWrappers: MutableMap<
        String,
        @Composable TabWrapperScope.(@Composable () -> Unit) -> Unit
        > = mutableMapOf()

    /**
     * Custom pane wrapper composables indexed by wrapper key.
     */
    internal val paneWrappers: MutableMap<
        String,
        @Composable PaneWrapperScope.(@Composable () -> Unit) -> Unit
        > = mutableMapOf()

    /**
     * Registers a screen with its composable content.
     *
     * The content lambda receives the destination instance, navigator, and optional
     * animation scopes for shared element transitions.
     *
     * ## Example
     *
     * ```kotlin
     * screen<ProfileScreen> { destination, navigator, sharedScope, animScope ->
     *     ProfileContent(
     *         userId = destination.userId,
     *         onBack = { navigator.navigateBack() },
     *         sharedTransitionScope = sharedScope,
     *         animatedVisibilityScope = animScope
     *     )
     * }
     * ```
     *
     * @param D The destination type (must extend [Destination])
     * @param content Composable lambda that renders the screen content
     */
    public inline fun <reified D : Destination> screen(
        noinline content: @Composable (
            destination: D,
            navigator: Navigator,
            sharedTransitionScope: SharedTransitionScope?,
            animatedVisibilityScope: AnimatedVisibilityScope?
        ) -> Unit
    ) {
        @Suppress("UNCHECKED_CAST")
        screens[D::class] = ScreenEntry(
            destinationClass = D::class,
            content = content as @Composable (
                Destination,
                Navigator,
                SharedTransitionScope?,
                AnimatedVisibilityScope?
            ) -> Unit
        )
    }

    /**
     * Registers a stack container with its scope configuration.
     *
     * Stack containers manage linear navigation history with push/pop semantics.
     *
     * ## Example
     *
     * ```kotlin
     * stack<MainStack>("main") {
     *     screen<HomeScreen>()
     *     screen<DetailScreen>()
     *     screen<SettingsScreen>()
     * }
     * ```
     *
     * @param D The container destination type
     * @param scopeKey Unique identifier for this container's scope
     * @param builder Configuration lambda for the stack's screens
     */
    public inline fun <reified D : Destination> stack(
        scopeKey: String = D::class.simpleName ?: "stack",
        builder: StackBuilder.() -> Unit = {}
    ) {
        val stackBuilder = StackBuilder().apply(builder)
        containers[D::class] = ContainerBuilder.Stack(
            destinationClass = D::class,
            scopeKey = scopeKey,
            screens = stackBuilder.build()
        )

        // Register scope membership for all screens in this stack
        val scopeMembers = scopes.getOrPut(scopeKey) { mutableSetOf() }
        stackBuilder.screens.forEach { entry ->
            entry.destinationClass?.let { scopeMembers.add(it) }
        }
    }

    /**
     * Registers a tab container with its configuration.
     *
     * Tab containers manage parallel navigation stacks with indexed tab switching.
     *
     * ## Example
     *
     * ```kotlin
     * tabs<MainTabs>("main-tabs") {
     *     initialTab = 0
     *     tab(HomeTab, title = "Home", icon = R.drawable.ic_home)
     *     tab(SearchTab, title = "Search", icon = R.drawable.ic_search)
     *     tab(ProfileTab, title = "Profile", icon = R.drawable.ic_profile)
     * }
     * ```
     *
     * @param D The container destination type
     * @param scopeKey Unique identifier for this container's scope
     * @param builder Configuration lambda for the tabs
     */
    public inline fun <reified D : Destination> tabs(
        scopeKey: String = D::class.simpleName ?: "tabs",
        builder: TabsBuilder.() -> Unit = {}
    ) {
        val tabsBuilder = TabsBuilder().apply(builder)
        containers[D::class] = ContainerBuilder.Tabs(
            destinationClass = D::class,
            scopeKey = scopeKey,
            config = tabsBuilder.build()
        )

        // Register scope membership for tabs
        val scopeMembers = scopes.getOrPut(scopeKey) { mutableSetOf() }
        tabsBuilder.tabs.forEach { entry ->
            when (entry) {
                is TabEntry.FlatScreen -> entry.destinationClass?.let { scopeMembers.add(it) }
                is TabEntry.NestedStack -> entry.screens.forEach { screen ->
                    screen.destinationClass?.let { scopeMembers.add(it) }
                }
                is TabEntry.ContainerReference -> scopeMembers.add(entry.containerClass)
            }
        }
    }

    /**
     * Registers a pane container for adaptive multi-pane layouts.
     *
     * Pane containers support master-detail and three-column layouts that adapt
     * to screen size.
     *
     * ## Example
     *
     * ```kotlin
     * panes<ListDetailPanes>("list-detail") {
     *     initialPane = PaneRole.Primary
     *     backBehavior = PaneBackBehavior.PopUntilScaffoldValueChange
     *
     *     primary(weight = 0.4f, minWidth = 300.dp) {
     *         root(ListScreen)
     *     }
     *
     *     secondary(weight = 0.6f) {
     *         root(DetailPlaceholder)
     *     }
     * }
     * ```
     *
     * @param D The container destination type
     * @param scopeKey Unique identifier for this container's scope
     * @param builder Configuration lambda for the panes
     */
    public inline fun <reified D : Destination> panes(
        scopeKey: String = D::class.simpleName ?: "panes",
        builder: PanesBuilder.() -> Unit = {}
    ) {
        val panesBuilder = PanesBuilder().apply(builder)
        containers[D::class] = ContainerBuilder.Panes(
            destinationClass = D::class,
            scopeKey = scopeKey,
            config = panesBuilder.build()
        )
    }

    /**
     * Defines scope membership for destinations.
     *
     * Scopes determine which destinations belong to which containers,
     * enabling scope-aware navigation behavior.
     *
     * ## Example
     *
     * ```kotlin
     * scope("main-stack") {
     *     +HomeScreen::class
     *     +DetailScreen::class
     *     +SettingsScreen::class
     * }
     * ```
     *
     * @param scopeKey Unique identifier for the scope
     * @param builder Lambda to configure scope membership
     */
    public fun scope(
        scopeKey: String,
        builder: ScopeBuilder.() -> Unit
    ) {
        val scopeBuilder = ScopeBuilder().apply(builder)
        val members = scopes.getOrPut(scopeKey) { mutableSetOf() }
        members.addAll(scopeBuilder.members)
    }

    /**
     * Registers a custom transition for a destination type.
     *
     * The transition will be used when navigating to this destination type.
     *
     * ## Example
     *
     * ```kotlin
     * transition<DetailScreen>(NavTransition.SlideHorizontal)
     * transition<ModalScreen>(NavTransition.SlideVertical)
     * transition<SettingsScreen>(NavTransition.Fade)
     * ```
     *
     * @param D The destination type
     * @param transition The [NavTransition] to use
     */
    public inline fun <reified D : Destination> transition(transition: NavTransition) {
        transitions[D::class] = transition
    }

    /**
     * Registers a custom tab wrapper composable.
     *
     * Tab wrappers provide custom chrome/UI around tab content, such as
     * bottom navigation bars, navigation rails, or custom tab indicators.
     *
     * ## Example
     *
     * ```kotlin
     * tabWrapper("main-tabs") { content ->
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
     *         Box(Modifier.padding(padding)) {
     *             content()
     *         }
     *     }
     * }
     * ```
     *
     * @param key Unique identifier for the wrapper (typically matches container scope key)
     * @param wrapper Composable lambda that wraps tab content
     */
    public fun tabWrapper(
        key: String,
        wrapper: @Composable TabWrapperScope.(@Composable () -> Unit) -> Unit
    ) {
        tabWrappers[key] = wrapper
    }

    /**
     * Registers a custom pane wrapper composable.
     *
     * Pane wrappers provide custom layouts for multi-pane navigation,
     * such as master-detail or three-column layouts.
     *
     * ## Example
     *
     * ```kotlin
     * paneWrapper("list-detail") { content ->
     *     Row(Modifier.fillMaxSize()) {
     *         // Custom pane arrangement
     *         content()
     *     }
     * }
     * ```
     *
     * @param key Unique identifier for the wrapper (typically matches container scope key)
     * @param wrapper Composable lambda that wraps pane content
     */
    public fun paneWrapper(
        key: String,
        wrapper: @Composable PaneWrapperScope.(@Composable () -> Unit) -> Unit
    ) {
        paneWrappers[key] = wrapper
    }

    /**
     * Builds the final [NavigationConfig] from this builder's configuration.
     *
     * Creates a [DslNavigationConfig] that converts all collected builder data
     * into runtime-usable navigation registries.
     *
     * @return The constructed [NavigationConfig]
     */
    public fun build(): NavigationConfig {
        return DslNavigationConfig(
            screens = screens.toMap(),
            containers = containers.toMap(),
            scopes = scopes.mapValues { it.value.toSet() },
            transitions = transitions.toMap(),
            tabWrappers = tabWrappers.toMap(),
            paneWrappers = paneWrappers.toMap()
        )
    }
}

/**
 * Creates a [NavigationConfig] using the DSL builder.
 *
 * This is the entry point for declaratively defining navigation configuration.
 *
 * ## Example
 *
 * ```kotlin
 * val config = navigationConfig {
 *     screen<HomeScreen> { dest, nav, _, _ ->
 *         HomeContent(dest, nav)
 *     }
 *
 *     stack<MainStack>("main") {
 *         screen<HomeScreen>()
 *         screen<DetailScreen>()
 *     }
 *
 *     transition<DetailScreen>(NavTransition.SlideHorizontal)
 * }
 *
 * NavigationHost(
 *     config = config,
 *     navigator = navigator
 * )
 * ```
 *
 * @param builder Configuration lambda applied to [NavigationConfigBuilder]
 * @return The constructed [NavigationConfig]
 */
public fun navigationConfig(builder: NavigationConfigBuilder.() -> Unit): NavigationConfig {
    return NavigationConfigBuilder().apply(builder).build()
}

/**
 * Builder for defining scope membership.
 *
 * Used within [NavigationConfigBuilder.scope] to specify which destination
 * classes belong to a navigation scope.
 */
@NavigationConfigDsl
public class ScopeBuilder {
    @PublishedApi
    internal val members: MutableSet<KClass<out Destination>> = mutableSetOf()

    /**
     * Adds a destination class to this scope.
     *
     * @param destinationClass The class to add
     */
    public operator fun KClass<out Destination>.unaryPlus() {
        addMember(this)
    }

    /**
     * Adds a destination class to this scope using reified type.
     */
    public inline fun <reified D : Destination> include() {
        addMember(D::class)
    }

    /**
     * Internal helper to add a member to the scope.
     */
    @PublishedApi
    internal fun addMember(klass: KClass<out Destination>) {
        members.add(klass)
    }
}
