package com.jermey.quo.vadis.core.navigation.dsl

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import com.jermey.quo.vadis.core.navigation.DslNavigationConfig
import com.jermey.quo.vadis.core.navigation.NavigationConfig
import com.jermey.quo.vadis.core.navigation.compose.animation.NavTransition
import com.jermey.quo.vadis.core.navigation.compose.wrapper.PaneContainerScope
import com.jermey.quo.vadis.core.navigation.compose.wrapper.TabsContainerScope
import com.jermey.quo.vadis.core.navigation.core.NavDestination
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
 *     screen<HomeScreen> { destination, _, _ ->
 *         HomeScreenContent(destination)
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
 *     tabsContainer("main-tabs") { content ->
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
class NavigationConfigBuilder {

    /**
     * Registered screen content composables indexed by destination class.
     */
    @PublishedApi
    internal val screens: MutableMap<KClass<out NavDestination>, ScreenEntry> = mutableMapOf()

    /**
     * Registered container builders indexed by destination class.
     */
    @PublishedApi
    internal val containers: MutableMap<KClass<out NavDestination>, ContainerBuilder> = mutableMapOf()

    /**
     * Scope membership definitions mapping scope keys to sets of destination classes.
     */
    @PublishedApi
    internal val scopes: MutableMap<String, MutableSet<KClass<out NavDestination>>> = mutableMapOf()

    /**
     * Custom transitions indexed by destination class.
     */
    @PublishedApi
    internal val transitions: MutableMap<KClass<out NavDestination>, NavTransition> = mutableMapOf()

    /**
     * Custom tabs container wrapper composables indexed by wrapper key.
     */
    internal val tabsContainers: MutableMap<
        String,
        @Composable TabsContainerScope.(@Composable () -> Unit) -> Unit
        > = mutableMapOf()

    /**
     * Custom pane container composables indexed by wrapper key.
     */
    internal val paneContainers: MutableMap<
        String,
        @Composable PaneContainerScope.(@Composable () -> Unit) -> Unit
        > = mutableMapOf()

    /**
     * Registers a screen with its composable content.
     *
     * The content lambda receives the destination instance and optional
     * animation scopes for shared element transitions.
     *
     * ## Example
     *
     * ```kotlin
     * screen<ProfileScreen> { destination, sharedScope, animScope ->
     *     ProfileContent(
     *         userId = destination.userId,
     *         sharedTransitionScope = sharedScope,
     *         animatedVisibilityScope = animScope
     *     )
     * }
     * ```
     *
     * @param D The destination type (must extend [Destination])
     * @param content Composable lambda that renders the screen content
     */
    inline fun <reified D : NavDestination> screen(
        noinline content: @Composable (
            destination: D,
            sharedTransitionScope: SharedTransitionScope?,
            animatedVisibilityScope: AnimatedVisibilityScope?
        ) -> @Composable () -> Unit
    ) {
        @Suppress("UNCHECKED_CAST")
        screens[D::class] = ScreenEntry(
            destinationClass = D::class,
            content = content as @Composable (
                NavDestination,
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
    inline fun <reified D : NavDestination> stack(
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
    inline fun <reified D : NavDestination> tabs(
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
    inline fun <reified D : NavDestination> panes(
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
    fun scope(
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
    inline fun <reified D : NavDestination> transition(transition: NavTransition) {
        transitions[D::class] = transition
    }

    /**
     * Registers a custom tabs container wrapper composable.
     *
     * Tabs container wrappers provide custom chrome/UI around tab content, such as
     * bottom navigation bars, navigation rails, or custom tab indicators.
     *
     * ## Example
     *
     * ```kotlin
     * tabsContainer("main-tabs") { content ->
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
    fun tabsContainer(
        key: String,
        wrapper: @Composable TabsContainerScope.(@Composable () -> Unit) -> Unit
    ) {
        tabsContainers[key] = wrapper
    }

    /**
     * Registers a custom pane container composable.
     *
     * Pane containers provide custom layouts for multi-pane navigation,
     * such as master-detail or three-column layouts.
     *
     * ## Example
     *
     * ```kotlin
     * paneContainer("list-detail") { content ->
     *     Row(Modifier.fillMaxSize()) {
     *         // Custom pane arrangement
     *         content()
     *     }
     * }
     * ```
     *
     * @param key Unique identifier for the container (typically matches container scope key)
     * @param wrapper Composable lambda that wraps pane content
     */
    fun paneContainer(
        key: String,
        wrapper: @Composable PaneContainerScope.(@Composable () -> Unit) -> Unit
    ) {
        paneContainers[key] = wrapper
    }

    /**
     * Builds the final [NavigationConfig] from this builder's configuration.
     *
     * Creates a [DslNavigationConfig] that converts all collected builder data
     * into runtime-usable navigation registries.
     *
     * @return The constructed [NavigationConfig]
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
 * Creates a [NavigationConfig] using the DSL builder.
 *
 * This is the entry point for declaratively defining navigation configuration.
 *
 * ## Example
 *
 * ```kotlin
 * val config = navigationConfig {
 *     screen<HomeScreen> { dest, _, _ ->
 *         HomeContent(dest)
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
 * val navigator = rememberQuoVadisNavigator(MainStack::class, config)
 * NavigationHost(navigator)  // Config is read from navigator
 * ```
 *
 * @param builder Configuration lambda applied to [NavigationConfigBuilder]
 * @return The constructed [NavigationConfig]
 */
fun navigationConfig(builder: NavigationConfigBuilder.() -> Unit): NavigationConfig {
    return NavigationConfigBuilder().apply(builder).build()
}

/**
 * Builder for defining scope membership.
 *
 * Used within [NavigationConfigBuilder.scope] to specify which destination
 * classes belong to a navigation scope.
 */
@NavigationConfigDsl
class ScopeBuilder {
    @PublishedApi
    internal val members: MutableSet<KClass<out NavDestination>> = mutableSetOf()

    /**
     * Adds a destination class to this scope.
     *
     * @param destinationClass The class to add
     */
    operator fun KClass<out NavDestination>.unaryPlus() {
        addMember(this)
    }

    /**
     * Adds a destination class to this scope using reified type.
     */
    inline fun <reified D : NavDestination> include() {
        addMember(D::class)
    }

    /**
     * Internal helper to add a member to the scope.
     */
    @PublishedApi
    internal fun addMember(klass: KClass<out NavDestination>) {
        members.add(klass)
    }
}
