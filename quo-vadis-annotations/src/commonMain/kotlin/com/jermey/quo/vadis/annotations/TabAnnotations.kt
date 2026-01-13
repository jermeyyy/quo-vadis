package com.jermey.quo.vadis.annotations

import kotlin.reflect.KClass

/**
 * Marks an object or class as a tabbed navigation container.
 *
 * This annotation defines a collection of tabs, each represented by a
 * `@TabItem`-annotated `@Stack` class. The container manages tab state,
 * including the active tab and per-tab back stacks.
 *
 * ## Features
 *
 * Tabs support:
 * - Independent back stacks per tab
 * - State preservation when switching tabs
 * - Deep linking to specific tabs
 * - Configurable initial tab selection
 * - Type-safe tab references
 *
 * ## New Pattern (Recommended)
 *
 * Define each tab as a separate `@TabItem` + `@Stack` class at the top level:
 * ```kotlin
 * @TabItem
 * @Stack(name = "homeStack", startDestination = HomeTab.Feed::class)
 * sealed class HomeTab : NavDestination {
 *     @Destination(route = "home/feed")
 *     data object Feed : HomeTab()
 *
 *     @Destination(route = "home/article/{articleId}")
 *     data class Article(val articleId: String) : HomeTab()
 * }
 *
 * @TabItem
 * @Stack(name = "exploreStack", startDestination = ExploreTab.ExploreRoot::class)
 * sealed class ExploreTab : NavDestination {
 *     @Destination(route = "explore/root")
 *     data object ExploreRoot : ExploreTab()
 * }
 *
 * @Tabs(
 *     name = "mainTabs",
 *     initialTab = HomeTab::class,
 *     items = [HomeTab::class, ExploreTab::class]
 * )
 * object MainTabs
 * ```
 *
 * ## NavNode Mapping
 *
 * This annotation maps to `TabNode` in the NavNode hierarchy:
 * ```
 * @Tab → TabNode(
 *     key = "{name}",
 *     stacks = [StackNode for each @TabItem in items],
 *     activeStackIndex = <index of initialTab>
 * )
 * ```
 *
 * ## Generated Code
 *
 * KSP generates a builder function for the TabNode tree:
 * ```kotlin
 * // KSP generates: buildMainTabsNavNode()
 * TabNode(
 *     key = "mainTabs",
 *     parentKey = null,
 *     stacks = listOf(
 *         buildHomeTabNavNode().copy(parentKey = "mainTabs"),
 *         buildExploreTabNavNode().copy(parentKey = "mainTabs")
 *     ),
 *     activeStackIndex = 0  // HomeTab is initial
 * )
 * ```
 *
 * ## Initial Tab Resolution
 *
 * The [initialTab] is resolved as follows:
 * - If a `KClass<*>` is provided, it must match one of the classes in [items]
 * - If `Unit::class` (default), the first tab in [items] order is selected
 *
 * ## Legacy Pattern (Deprecated)
 *
 * The old pattern with nested sealed subclasses is deprecated due to
 * KSP limitations in KMP metadata compilation:
 * ```kotlin
 * // DEPRECATED - Do not use this pattern
 * @Tabs(name = "mainTabs", initialTabLegacy = "Home")
 * sealed class MainTabs : NavDestination {
 *     @TabItem
 *     @Destination(route = "tabs/home")
 *     data object Home : MainTabs()
 * }
 * ```
 *
 * @property name Unique identifier for this tab container. Used in route
 *   generation, key construction, and navigation tree identification
 *   (e.g., "mainTabs", "settingsTabs").
 * @property initialTab Type-safe class reference to the initially selected tab.
 *   Must be one of the classes in [items]. Use `Unit::class` (default) to
 *   select the first tab in declaration order.
 * @property items Array of `@TabItem`/`@Stack` classes that comprise this
 *   tab container. The order determines tab order in the UI.
 * @property initialTabLegacy **Deprecated**: Use [initialTab] with `KClass<*>`
 *   instead. This string-based parameter is kept for backward compatibility
 *   with the legacy nested subclass pattern.
 *
 * @see TabItem
 * @see Destination
 * @see Stack
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Tabs(
    val name: String,
    val initialTab: KClass<*> = Unit::class,
    val items: Array<KClass<*>> = [],
)

/**
 * Marks a `@Stack`-annotated class as a tab within a [@Tabs] navigation container.
 *
 * Apply this annotation alongside `@Stack` on a top-level sealed class to make it
 * available as a tab item. Tab UI customization (labels, icons) is done in the
 * `@TabsContainer` wrapper composable using type-safe pattern matching.
 *
 * ## Usage
 *
 * Define each tab as a separate `@TabItem` + `@Stack` class:
 * ```kotlin
 * @TabItem
 * @Stack(name = "homeStack", startDestination = HomeTab.Feed::class)
 * sealed class HomeTab : NavDestination {
 *     @Destination(route = "home/feed")
 *     data object Feed : HomeTab()
 *
 *     @Destination(route = "home/article/{id}")
 *     data class Article(val id: String) : HomeTab()
 * }
 *
 * @TabItem
 * @Stack(name = "exploreStack", startDestination = ExploreTab.ExploreRoot::class)
 * sealed class ExploreTab : NavDestination {
 *     @Destination(route = "explore/root")
 *     data object ExploreRoot : ExploreTab()
 * }
 *
 * // Reference in @Tabs container:
 * @Tabs(
 *     name = "mainTabs",
 *     initialTab = HomeTab::class,
 *     items = [HomeTab::class, ExploreTab::class]
 * )
 * object MainTabs
 * ```
 *
 * ## Tab UI Customization
 *
 * Customize tab labels and icons in your `@TabsContainer` wrapper using pattern matching:
 * ```kotlin
 * @TabsContainer(MainTabs::class)
 * @Composable
 * fun MainTabsContainer(scope: TabsContainerScope, content: @Composable () -> Unit) {
 *     NavigationBar {
 *         scope.tabs.forEachIndexed { index, tab ->
 *             val (label, icon) = when (tab) {
 *                 is HomeTab -> "Home" to Icons.Default.Home
 *                 is ExploreTab -> "Explore" to Icons.Default.Explore
 *                 else -> "Tab" to Icons.Default.Circle
 *             }
 *             NavigationBarItem(
 *                 selected = index == scope.activeTabIndex,
 *                 onClick = { scope.switchTab(index) },
 *                 icon = { Icon(icon, contentDescription = label) },
 *                 label = { Text(label) }
 *             )
 *         }
 *     }
 * }
 * ```
 *
 * ## Deep Linking
 *
 * Tabs support deep linking to specific tabs and their content:
 * ```kotlin
 * // Deep link: app://mainTabs/home/article/123
 * // Opens Home tab with Article screen
 * ```
 *
 * @see Tabs
 * @see Stack
 * @see Destination
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class TabItem
