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
 * @TabItem(label = "Home", icon = "home")
 * @Stack(name = "homeStack", startDestination = HomeTab.Feed::class)
 * sealed class HomeTab : Destination {
 *     @Destination(route = "home/feed")
 *     data object Feed : HomeTab()
 *
 *     @Destination(route = "home/article/{articleId}")
 *     data class Article(val articleId: String) : HomeTab()
 * }
 *
 * @TabItem(label = "Explore", icon = "explore")
 * @Stack(name = "exploreStack", startDestination = ExploreTab.ExploreRoot::class)
 * sealed class ExploreTab : Destination {
 *     @Destination(route = "explore/root")
 *     data object ExploreRoot : ExploreTab()
 * }
 *
 * @Tab(
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
 *     activeStackIndex = 0,  // HomeTab is initial
 *     tabMetadata = listOf(
 *         TabMetadata(label = "Home", icon = "home"),
 *         TabMetadata(label = "Explore", icon = "explore")
 *     )
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
 * @Tab(name = "mainTabs", initialTabLegacy = "Home")
 * sealed class MainTabs : Destination {
 *     @TabItem(label = "Home", icon = "home")
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
 * Provides UI metadata for a tab within a [@Tab] navigation container.
 *
 * Apply this annotation to a `@Stack`-annotated class to make it available
 * as a tab. The annotated class serves dual purposes:
 * 1. Defines the tab's UI properties (label, icon)
 * 2. Defines the tab's navigation content (as a `@Stack`)
 *
 * ## New Pattern (Recommended)
 *
 * Apply `@TabItem` alongside `@Stack` on the same top-level class:
 * ```kotlin
 * @TabItem(label = "Home", icon = "home")
 * @Stack(name = "homeStack", startDestination = HomeTab.Feed::class)
 * sealed class HomeTab : Destination {
 *     @Destination(route = "home/feed")
 *     data object Feed : HomeTab()
 *
 *     @Destination(route = "home/article/{id}")
 *     data class Article(val id: String) : HomeTab()
 * }
 *
 * @TabItem(label = "Explore", icon = "explore")
 * @Stack(name = "exploreStack", startDestination = ExploreTab.ExploreRoot::class)
 * sealed class ExploreTab : Destination {
 *     @Destination(route = "explore/root")
 *     data object ExploreRoot : ExploreTab()
 * }
 *
 * // Then reference in @Tab container:
 * @Tab(
 *     name = "mainTabs",
 *     initialTab = HomeTab::class,
 *     items = [HomeTab::class, ExploreTab::class]
 * )
 * object MainTabs
 * ```
 *
 * ## Icon Platforms
 *
 * The [icon] parameter has platform-specific interpretation:
 * - **Android**: Material icon name (e.g., "home", "search", "person")
 *   or drawable resource reference
 * - **iOS**: SF Symbol name
 * - **Desktop/Web**: Icon library identifier
 *
 * ## Deep Linking
 *
 * Tabs support deep linking to specific tabs and their content:
 * ```kotlin
 * // Deep link: app://mainTabs/home/article/123
 * // Opens Home tab with Article screen
 * ```
 *
 * ## Generated Metadata
 *
 * KSP generates `TabMetadata` for each tab item, used for UI rendering:
 * ```kotlin
 * data class TabMetadata(
 *     val label: String,
 *     val icon: String
 * )
 * ```
 *
 * ## Legacy Pattern (Deprecated)
 *
 * The old pattern applied `@TabItem` to nested sealed subclasses.
 * This is deprecated due to KSP limitations in KMP metadata compilation:
 * ```kotlin
 * // DEPRECATED - Do not use this pattern
 * @Tab(name = "mainTabs")
 * sealed class MainTabs : Destination {
 *     @TabItem(label = "Home", icon = "home")
 *     @Destination(route = "tabs/home")
 *     data object Home : MainTabs()
 * }
 * ```
 *
 * @property label Display label for the tab shown in the tab bar UI.
 *   Should be localized for internationalization support.
 * @property icon Icon identifier for the tab. Platform-specific interpretation:
 *   - Android: Material icon name or resource reference
 *   - iOS: SF Symbol name
 *   - Desktop/Web: Icon library identifier
 *   Empty string means no icon.
 *
 * @see Tabs
 * @see Stack
 * @see Destination
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class TabItem(
    val label: String,
    val icon: String = "",
)
