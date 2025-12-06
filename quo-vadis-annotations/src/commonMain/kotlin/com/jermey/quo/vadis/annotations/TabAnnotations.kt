package com.jermey.quo.vadis.annotations

import kotlin.reflect.KClass

/**
 * Marks a sealed class or interface as a tabbed navigation container.
 *
 * Each sealed subclass represents a tab with its own independent navigation
 * stack. The container is transformed into a [TabNode] in the navigation tree,
 * with each tab maintaining its own [StackNode] for back navigation.
 *
 * ## Features
 *
 * Tabs support:
 * - Independent back stacks per tab
 * - State preservation when switching tabs
 * - Deep linking to specific tabs
 * - Configurable initial tab selection
 *
 * ## Usage
 *
 * Apply to a sealed class where each subclass represents a tab:
 * ```kotlin
 * @Tab(name = "mainTabs", initialTab = "Home")
 * sealed class MainTabs : Destination {
 *
 *     @TabItem(label = "Home", icon = "home", rootGraph = HomeDestination::class)
 *     @Destination(route = "tabs/home")
 *     data object Home : MainTabs()
 *
 *     @TabItem(label = "Search", icon = "search", rootGraph = SearchDestination::class)
 *     @Destination(route = "tabs/search")
 *     data object Search : MainTabs()
 *
 *     @TabItem(label = "Profile", icon = "person", rootGraph = ProfileDestination::class)
 *     @Destination(route = "tabs/profile")
 *     data object Profile : MainTabs()
 * }
 * ```
 *
 * ## Sealed Class Requirements
 *
 * - Must be a `sealed class` or `sealed interface`
 * - Each direct subclass must be annotated with [@TabItem] and [@Destination]
 * - Subclasses should be `data object` (tabs typically have no parameters)
 *
 * ## NavNode Mapping
 *
 * This annotation maps to [TabNode] in the NavNode hierarchy:
 * ```
 * @Tab → TabNode(
 *     key = "{name}",
 *     stacks = [StackNode for each @TabItem subclass],
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
 *         StackNode(key = "mainTabs/home", parentKey = "mainTabs", children = [...]),
 *         StackNode(key = "mainTabs/search", parentKey = "mainTabs", children = [...]),
 *         StackNode(key = "mainTabs/profile", parentKey = "mainTabs", children = [...])
 *     ),
 *     activeStackIndex = 0  // Home is initial
 * )
 * ```
 *
 * ## Initial Tab Resolution
 *
 * The [initialTab] is resolved by matching against sealed subclass simple names:
 * - `"Home"` matches `data object Home : MainTabs()`
 * - If [initialTab] is empty, the first tab in declaration order is selected
 *
 * ## Examples
 *
 * ### Basic Tab Container
 * ```kotlin
 * @Tab(name = "mainTabs", initialTab = "Home")
 * sealed class MainTabs : Destination {
 *
 *     @TabItem(label = "Home", icon = "home", rootGraph = HomeDestination::class)
 *     @Destination(route = "tabs/home")
 *     data object Home : MainTabs()
 *
 *     @TabItem(label = "Search", icon = "search", rootGraph = SearchDestination::class)
 *     @Destination(route = "tabs/search")
 *     data object Search : MainTabs()
 *
 *     @TabItem(label = "Profile", icon = "person", rootGraph = ProfileDestination::class)
 *     @Destination(route = "tabs/profile")
 *     data object Profile : MainTabs()
 * }
 * ```
 *
 * ### Tab Container with Default Initial Tab
 * ```kotlin
 * // First tab (Dashboard) is selected when initialTab is empty
 * @Tab(name = "admin")
 * sealed class AdminTabs : Destination {
 *
 *     @TabItem(label = "Dashboard", icon = "dashboard", rootGraph = DashboardGraph::class)
 *     @Destination(route = "admin/dashboard")
 *     data object Dashboard : AdminTabs()  // ← Default initial tab
 *
 *     @TabItem(label = "Users", icon = "people", rootGraph = UsersGraph::class)
 *     @Destination(route = "admin/users")
 *     data object Users : AdminTabs()
 * }
 * ```
 *
 * @property name Unique identifier for this tab container. Used in route
 *   generation, key construction, and navigation tree identification
 *   (e.g., "mainTabs", "settingsTabs").
 * @property initialTab Simple name of the initially selected tab. Must match
 *   one of the sealed subclass names exactly. If empty, the first tab in
 *   declaration order is selected.
 *
 * @see TabItem
 * @see Destination
 * @see Stack
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Tab(
    val name: String,
    val initialTab: String = ""
)

/**
 * Provides metadata for a tab within a [@Tab] container.
 *
 * Applied to sealed subclasses of a [@Tab]-annotated class to configure
 * the tab's display properties and navigation content. Each tab item
 * references a root graph (a [@Stack]-annotated sealed class) that defines
 * the navigation destinations available within that tab.
 *
 * ## Usage
 *
 * Apply to each sealed subclass within a [@Tab] container:
 * ```kotlin
 * @Tab(name = "main", initialTab = "Home")
 * sealed class MainTabs : Destination {
 *
 *     @TabItem(label = "Home", icon = "home", rootGraph = HomeDestination::class)
 *     @Destination(route = "tab/home")
 *     data object Home : MainTabs()
 *
 *     @TabItem(label = "Favorites", icon = "favorite", rootGraph = FavoritesDestination::class)
 *     @Destination(route = "tab/favorites")
 *     data object Favorites : MainTabs()
 *
 *     @TabItem(label = "Settings", icon = "settings", rootGraph = SettingsDestination::class)
 *     @Destination(route = "tab/settings")
 *     data object Settings : MainTabs()
 * }
 * ```
 *
 * ## Root Graph Connection
 *
 * Each tab references a root graph that defines the navigation destinations
 * available within that tab. The root graph must be a sealed class annotated
 * with [@Stack]:
 * ```kotlin
 * // Referenced stack graph
 * @Stack(name = "home", startDestination = "Feed")
 * sealed class HomeDestination : Destination {
 *
 *     @Destination(route = "home/feed")
 *     data object Feed : HomeDestination()
 *
 *     @Destination(route = "home/article/{id}")
 *     data class Article(val id: String) : HomeDestination()
 * }
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
 * @Tab(name = "dashboard")
 * sealed class DashboardTabs : Destination {
 *
 *     @TabItem(label = "Overview", icon = "dashboard", rootGraph = OverviewGraph::class)
 *     @Destination(route = "dashboard/overview")
 *     data object Overview : DashboardTabs()
 *
 *     @TabItem(label = "Analytics", icon = "analytics", rootGraph = AnalyticsGraph::class)
 *     @Destination(route = "dashboard/analytics")
 *     data object Analytics : DashboardTabs()
 * }
 *
 * // Deep link: app://dashboard/analytics/report/123
 * // Opens Analytics tab with ReportDetail screen
 * ```
 *
 * ## Generated Metadata
 *
 * KSP generates [TabMetadata] for each tab item, used for UI rendering:
 * ```kotlin
 * data class TabMetadata(
 *     val label: String,
 *     val icon: String
 * )
 * ```
 *
 * @property label Display label for the tab shown in the tab bar UI.
 *   Should be localized for internationalization support.
 * @property icon Icon identifier for the tab. Platform-specific interpretation:
 *   - Android: Material icon name or resource reference
 *   - iOS: SF Symbol name
 *   - Desktop/Web: Icon library identifier
 *   Empty string means no icon.
 * @property rootGraph The root navigation graph class for this tab's content.
 *   Must be a sealed class annotated with [@Stack]. The stack's start
 *   destination becomes the tab's initial screen.
 *
 * @see Tab
 * @see Stack
 * @see Destination
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class TabItem(
    val label: String,
    val icon: String = "",
    val rootGraph: KClass<*>
)
