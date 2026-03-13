package com.jermey.quo.vadis.ksp.models

import com.google.devtools.ksp.symbol.KSClassDeclaration

/**
 * Distinguishes between flat screen tabs, nested stack tabs, and container references.
 *
 * Used to determine how a @TabItem should be processed and rendered:
 * - [FLAT_SCREEN]: Tab is a single screen destination (data object with @Destination)
 * - [NESTED_STACK]: Tab has its own navigation stack (local sealed class with @Stack)
 * - [CONTAINER_REFERENCE]: Tab references a @Stack or @Tabs container defined elsewhere
 */
enum class TabItemType {
    /**
     * Tab is a single screen destination.
     *
     * Pattern: `@TabItem` + `@Destination` on data object
     * ```kotlin
     * @TabItem(label = "Settings", icon = "settings")
     * @Destination(route = "settings")
     * data object SettingsTab : MainTabs
     * ```
     */
    FLAT_SCREEN,

    /**
     * Tab has its own navigation stack defined as a local sealed member.
     *
     * Pattern: `@TabItem` + `@Stack` on sealed class that is a direct member of the @Tabs container
     * ```kotlin
     * @TabItem(label = "Home", icon = "home")
     * @Stack(name = "homeStack", startDestination = HomeDestination.Feed::class)
     * sealed class HomeTab : MainTabs {
     *     @Destination(route = "home/feed")
     *     data object Feed : HomeTab()
     * }
     * ```
     */
    NESTED_STACK,

    /**
     * Tab references a @Stack or @Tabs container defined in another module or as a standalone class.
     *
     * The referenced container is opaque — its internals are not extracted by the tab extractor.
     * It generates its own container block independently.
     *
     * Pattern: `@TabItem` + `@Stack` (cross-module) or `@TabItem` + `@Tabs` (nested tabs)
     * ```kotlin
     * // Cross-module stack reference
     * @TabItem(label = "Feature", icon = "feature")
     * @Stack(name = "feature", startDestination = ...)
     * sealed class FeatureDestination : MainTabs, NavDestination { ... }
     *
     * // Nested tabs reference
     * @TabItem(label = "Sub", icon = "sub")
     * @Tabs(items = [...])
     * sealed interface SubTabs : MainTabs
     * ```
     */
    CONTAINER_REFERENCE
}

/**
 * Extracted metadata from a @Tab annotation.
 *
 * Supports two patterns:
 *
 * ## New Pattern (Type-Safe)
 * ```kotlin
 * @Tab(initialTab = HomeTab::class, items = [HomeTab::class, ExploreTab::class])
 * sealed interface MainTabs
 *
 * @TabItem(label = "Home", icon = "home")
 * data object HomeTab : MainTabs
 * ```
 * - [isNewPattern] = true
 * - [initialTabClass] = reference to initial tab class (or null to use first item)
 *
 * ## Legacy Pattern (Nested Subclasses)
 * ```kotlin
 * @Tab(name = "main")
 * sealed class MainTabs {
 *     @TabItem(label = "Home", icon = "home")
 *     data object Home : MainTabs(), Destination
 * }
 * ```
 * - [isNewPattern] = false
 * - [initialTabClass] = null (determined from first tab or legacy string matching)
 *
 * @property classDeclaration The KSP class declaration for this tab container
 * @property name Tab container identifier from annotation (e.g., "main")
 * @property className Simple class name (e.g., "MainTabs")
 * @property packageName Package containing this class
 * @property initialTabClass Type-safe class reference to initial tab.
 *                           Null means use first tab in items/tabs list.
 * @property isNewPattern True if using new items array pattern, false for legacy nested subclasses
 * @property tabs List of all tab items (from items array or @TabItem subclasses)
 */
data class TabInfo(
    val classDeclaration: KSClassDeclaration,
    val name: String,
    val className: String,
    val packageName: String,
    val initialTabClass: KSClassDeclaration?,
    val isNewPattern: Boolean,
    val tabs: List<TabItemInfo>,
)

/**
 * Extracted metadata from a @TabItem annotation.
 *
 * Supports three tab type patterns:
 *
 * ## FLAT_SCREEN Pattern
 * Tab is a single screen destination (data object with @Destination):
 * ```kotlin
 * @TabItem
 * @Destination(route = "settings")
 * data object SettingsTab : MainTabs
 * ```
 * - [tabType] = [TabItemType.FLAT_SCREEN]
 * - [destinationInfo] = DestinationInfo for this tab
 * - [stackInfo] = null
 *
 * ## NESTED_STACK Pattern
 * Tab has its own navigation stack (local sealed class with @Stack):
 * ```kotlin
 * @TabItem
 * @Stack(name = "homeStack", startDestination = HomeDestination.Feed::class)
 * sealed class HomeTab : MainTabs {
 *     @Destination(route = "home/feed")
 *     data object Feed : HomeTab()
 * }
 * ```
 * - [tabType] = [TabItemType.NESTED_STACK]
 * - [destinationInfo] = null
 * - [stackInfo] = StackInfo for this tab's stack
 *
 * ## CONTAINER_REFERENCE Pattern
 * Tab references a @Stack or @Tabs container defined elsewhere (cross-module or standalone):
 * ```kotlin
 * @TabItem
 * @Stack(name = "feature", startDestination = ...)
 * sealed class FeatureDestination : MainTabs, NavDestination { ... }
 * ```
 * - [tabType] = [TabItemType.CONTAINER_REFERENCE]
 * - [destinationInfo] = null (opaque reference)
 * - [stackInfo] = null (opaque reference)
 *
 * @property classDeclaration The KSP class declaration for this tab item
 * @property tabType Type of tab: [TabItemType.FLAT_SCREEN], [TabItemType.NESTED_STACK],
 *           or [TabItemType.CONTAINER_REFERENCE]
 * @property destinationInfo Destination info for FLAT_SCREEN tabs. Null for others.
 * @property stackInfo Stack info for NESTED_STACK tabs. Null for others.
 */
data class TabItemInfo(
    val classDeclaration: KSClassDeclaration,
    val tabType: TabItemType = TabItemType.FLAT_SCREEN,
    val destinationInfo: DestinationInfo? = null,
    val stackInfo: StackInfo? = null,
)
