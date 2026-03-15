package com.jermey.quo.vadis.annotations

import kotlin.reflect.KClass

/**
 * Declares a tabbed navigation container.
 *
 * `@Tabs` is a pure declaration — it only defines the tab container's identity.
 * Child tabs discover their parent via `@TabItem(parent = ...)`, reversing the
 * traditional parent-lists-children dependency.
 *
 * ## Features
 *
 * Tabs support:
 * - Independent back stacks per tab
 * - State preservation when switching tabs
 * - Deep linking to specific tabs
 * - Type-safe tab references
 * - Cross-module tab discovery via `@TabItem(parent)`
 *
 * ## Usage
 *
 * Declare the tab container, then annotate each child with `@TabItem`:
 * ```kotlin
 * @Tabs(name = "mainTabs")
 * object MainTabs
 *
 * @TabItem(parent = MainTabs::class, ordinal = 0)
 * @Stack(name = "homeStack", startDestination = HomeTab.Feed::class)
 * sealed class HomeTab : NavDestination {
 *     @Destination(route = "home/feed")
 *     data object Feed : HomeTab()
 * }
 *
 * @TabItem(parent = MainTabs::class, ordinal = 1)
 * @Stack(name = "exploreStack", startDestination = ExploreTab.ExploreRoot::class)
 * sealed class ExploreTab : NavDestination {
 *     @Destination(route = "explore/root")
 *     data object ExploreRoot : ExploreTab()
 * }
 * ```
 *
 * ## Initial Tab
 *
 * The tab with `ordinal = 0` is the initial tab. Ordinals define tab order.
 *
 * ## NavNode Mapping
 *
 * This annotation maps to `TabNode` in the NavNode hierarchy:
 * ```
 * @Tabs → TabNode(
 *     key = "{name}",
 *     stacks = [StackNode for each @TabItem sorted by ordinal],
 *     activeStackIndex = 0
 * )
 * ```
 *
 * @property name Unique identifier for this tab container. Used in route
 *   generation, key construction, and navigation tree identification
 *   (e.g., "mainTabs", "settingsTabs").
 *
 * @see TabItem
 * @see Stack
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class Tabs(
    val name: String,
)

/**
 * Marks a class as a tab within a [Tabs] navigation container.
 *
 * Each `@TabItem` points to its parent `@Tabs` container via the [parent] parameter,
 * reversing the traditional parent-lists-children dependency. This enables cross-module
 * tab discovery — tabs can be defined in separate modules and still be collected
 * under the same parent container at compile time.
 *
 * The [ordinal] parameter defines the tab's position; `ordinal = 0` is the initial tab.
 *
 * ## Usage
 *
 * ```kotlin
 * @Tabs(name = "mainTabs")
 * object MainTabs
 *
 * @TabItem(parent = MainTabs::class, ordinal = 0)
 * @Stack(name = "homeStack", startDestination = HomeTab.Feed::class)
 * sealed class HomeTab : NavDestination {
 *     @Destination(route = "home/feed")
 *     data object Feed : HomeTab()
 *
 *     @Destination(route = "home/article/{id}")
 *     data class Article(val id: String) : HomeTab()
 * }
 *
 * @TabItem(parent = MainTabs::class, ordinal = 1)
 * @Stack(name = "exploreStack", startDestination = ExploreTab.ExploreRoot::class)
 * sealed class ExploreTab : NavDestination {
 *     @Destination(route = "explore/root")
 *     data object ExploreRoot : ExploreTab()
 * }
 * ```
 *
 * ## Tab Types
 *
 * `@TabItem` can be combined with:
 * - `@Destination` — tab is a single screen (flat destination)
 * - `@Stack` — tab has its own navigation stack
 * - `@Tabs` — tab is a nested tab container
 *
 * ## Tab UI Customization
 *
 * Customize tab labels and icons in your `@TabsContainer` wrapper using pattern matching.
 *
 * @property parent Reference to the `@Tabs`-annotated class this tab belongs to.
 * @property ordinal Position of this tab within the parent container.
 *   `0` = initial (first) tab. Tabs are sorted by ordinal.
 *
 * @see Tabs
 * @see Stack
 * @see Destination
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class TabItem(
    val parent: KClass<*>,
    val ordinal: Int,
)
