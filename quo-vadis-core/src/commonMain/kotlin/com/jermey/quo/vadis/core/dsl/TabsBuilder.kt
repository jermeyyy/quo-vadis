package com.jermey.quo.vadis.core.dsl

import com.jermey.quo.vadis.core.navigation.NavDestination
import kotlin.reflect.KClass

/**
 * DSL builder for configuring tab container entries.
 *
 * Tab containers manage parallel navigation with indexed tab switching.
 * This builder allows specifying tabs with their destinations, titles,
 * icons, and optional nested stacks.
 *
 * ## Usage
 *
 * ### Simple flat tabs
 * ```kotlin
 * tabs<MainTabs>("main-tabs") {
 *     initialTab = 0
 *     tab(HomeTab, title = "Home", icon = Icons.Home)
 *     tab(SearchTab, title = "Search", icon = Icons.Search)
 *     tab(ProfileTab, title = "Profile", icon = Icons.Person)
 * }
 * ```
 *
 * ### Tabs with nested stacks
 * ```kotlin
 * tabs<MainTabs>("main-tabs") {
 *     tab(HomeTab, title = "Home", icon = Icons.Home) {
 *         screen<HomeScreen>()
 *         screen<HomeDetailScreen>()
 *     }
 *     tab(ProfileTab, title = "Profile", icon = Icons.Person) {
 *         screen<ProfileScreen>()
 *         screen<EditProfileScreen>()
 *     }
 * }
 * ```
 *
 * ### Tabs referencing existing containers
 * ```kotlin
 * tabs<MainTabs>("main-tabs") {
 *     containerTab(HomeStack::class, title = "Home", icon = Icons.Home)
 *     containerTab(ProfileStack::class, title = "Profile", icon = Icons.Person)
 * }
 * ```
 *
 * @see NavigationConfigBuilder.tabs
 * @see TabEntry
 */
@NavigationConfigDsl
class TabsBuilder {

    /**
     * List of tab entries configured for this container.
     */
    @PublishedApi
    internal val tabs: MutableList<TabEntry> = mutableListOf()

    /**
     * Index of the initially selected tab (0-based).
     *
     * Defaults to 0 (first tab).
     */
    var initialTab: Int = 0

    /**
     * Adds a flat screen tab (no nested navigation).
     *
     * The tab displays a single destination without its own navigation stack.
     *
     * ## Example
     *
     * ```kotlin
     * tab(HomeScreen, title = "Home", icon = Icons.Home)
     * ```
     *
     * @param destination The destination to display in this tab
     * @param title Optional display title for the tab
     * @param icon Optional icon for the tab (type is Any to support various icon types)
     */
    fun tab(
        destination: NavDestination,
        title: String? = null,
        icon: Any? = null
    ) {
        tabs.add(
            TabEntry.FlatScreen(
                destination = destination,
                destinationClass = destination::class,
                title = title,
                icon = icon
            )
        )
    }

    /**
     * Adds a tab with a nested navigation stack.
     *
     * The tab has its own navigation stack, allowing push/pop within the tab.
     *
     * ## Example
     *
     * ```kotlin
     * tab(HomeTab, title = "Home", icon = Icons.Home) {
     *     screen<HomeScreen>()
     *     screen<HomeDetailScreen>()
     *     screen<HomeSettingsScreen>()
     * }
     * ```
     *
     * @param destination The root destination for this tab
     * @param title Optional display title for the tab
     * @param icon Optional icon for the tab
     * @param builder Stack configuration lambda
     */
    fun tab(
        destination: NavDestination,
        title: String?,
        icon: Any?,
        builder: StackBuilder.() -> Unit
    ) {
        val stackBuilder = StackBuilder().apply(builder)
        tabs.add(
            TabEntry.NestedStack(
                rootDestination = destination,
                destinationClass = destination::class,
                screens = stackBuilder.build(),
                title = title,
                icon = icon
            )
        )
    }

    /**
     * Adds a tab referencing an existing container.
     *
     * Use this when you have a separately defined container (e.g., via [NavigationConfigBuilder.stack])
     * that should be displayed in this tab.
     *
     * ## Example
     *
     * ```kotlin
     * // Define container separately
     * stack<HomeStack>("home-stack") { ... }
     *
     * // Reference it in tabs
     * tabs<MainTabs>("main-tabs") {
     *     containerTab(HomeStack::class, title = "Home", icon = Icons.Home)
     * }
     * ```
     *
     * @param containerClass The class of the container destination
     * @param title Optional display title for the tab
     * @param icon Optional icon for the tab
     */
    fun containerTab(
        containerClass: KClass<out NavDestination>,
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

    /**
     * Adds a tab referencing an existing container using reified type.
     *
     * ## Example
     *
     * ```kotlin
     * containerTab<HomeStack>(title = "Home", icon = Icons.Home)
     * ```
     *
     * @param D The container destination type
     * @param title Optional display title for the tab
     * @param icon Optional icon for the tab
     */
    inline fun <reified D : NavDestination> containerTab(
        title: String? = null,
        icon: Any? = null
    ) {
        containerTab(D::class, title, icon)
    }

    /**
     * Builds the tabs configuration.
     *
     * @return [BuiltTabsConfig] containing all tab entries and settings
     */
    fun build(): BuiltTabsConfig = BuiltTabsConfig(
        tabs = tabs.toList(),
        initialTab = initialTab
    )
}

/**
 * Represents a single tab entry in a tab container.
 *
 * Tab entries can be one of three types:
 * - [FlatScreen]: A single destination without nested navigation
 * - [NestedStack]: A destination with its own navigation stack
 * - [ContainerReference]: A reference to an existing container
 */
sealed class TabEntry {

    /**
     * Display title for this tab.
     */
    abstract val title: String?

    /**
     * Icon for this tab (supports various icon types).
     */
    abstract val icon: Any?

    /**
     * A tab displaying a single screen without nested navigation.
     *
     * @property destination The destination instance
     * @property destinationClass The destination class
     * @property title Display title
     * @property icon Tab icon
     */
    data class FlatScreen(
        val destination: NavDestination,
        val destinationClass: KClass<out NavDestination>,
        override val title: String?,
        override val icon: Any?
    ) : TabEntry()

    /**
     * A tab with its own navigation stack.
     *
     * @property rootDestination The root destination for this tab's stack
     * @property destinationClass The destination class of the root
     * @property screens List of screens in this tab's stack
     * @property title Display title
     * @property icon Tab icon
     */
    data class NestedStack(
        val rootDestination: NavDestination,
        val destinationClass: KClass<out NavDestination>,
        val screens: List<StackScreenEntry>,
        override val title: String?,
        override val icon: Any?
    ) : TabEntry()

    /**
     * A tab referencing an existing container.
     *
     * @property containerClass The class of the referenced container
     * @property title Display title
     * @property icon Tab icon
     */
    data class ContainerReference(
        val containerClass: KClass<out NavDestination>,
        override val title: String?,
        override val icon: Any?
    ) : TabEntry()
}
