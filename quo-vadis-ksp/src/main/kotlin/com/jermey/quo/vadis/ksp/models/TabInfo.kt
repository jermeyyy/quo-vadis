package com.jermey.quo.vadis.ksp.models

import com.google.devtools.ksp.symbol.KSClassDeclaration

/**
 * Classifies the type of a `@TabItem`-annotated class.
 *
 * Used to determine how a tab should be processed and what code to generate:
 * - [DESTINATION]: Tab is a single screen (`@TabItem` + `@Destination`)
 * - [STACK]: Tab has its own navigation stack (`@TabItem` + `@Stack`)
 * - [TABS]: Tab is a nested tab container (`@TabItem` + `@Tabs`)
 */
enum class TabItemType {
    /**
     * Tab is a single screen destination.
     *
     * Pattern: `@TabItem` + `@Destination` on a class/object
     * ```kotlin
     * @TabItem(parent = MainTabs::class, ordinal = 2)
     * @Destination(route = "settings")
     * data object SettingsTab
     * ```
     */
    DESTINATION,

    /**
     * Tab has its own navigation stack.
     *
     * Pattern: `@TabItem` + `@Stack` on a sealed class
     * ```kotlin
     * @TabItem(parent = MainTabs::class, ordinal = 0)
     * @Stack(name = "homeStack", startDestination = HomeTab.Feed::class)
     * sealed class HomeTab : NavDestination {
     *     @Destination(route = "home/feed")
     *     data object Feed : HomeTab()
     * }
     * ```
     */
    STACK,

    /**
     * Tab is a nested tab container.
     *
     * Pattern: `@TabItem` + `@Tabs` on a class/object
     * ```kotlin
     * @TabItem(parent = MainTabs::class, ordinal = 1)
     * @Tabs(name = "subTabs")
     * object SubTabs
     * ```
     */
    TABS,
}

/**
 * Extracted metadata from a `@Tabs`-annotated class.
 *
 * Tabs are collected by scanning for `@TabItem(parent = ...)` annotations that
 * reference this container. The [tabs] list is sorted by ordinal.
 *
 * ```kotlin
 * @Tabs(name = "mainTabs")
 * object MainTabs
 *
 * @TabItem(parent = MainTabs::class, ordinal = 0)
 * @Stack(name = "homeStack", startDestination = HomeTab.Feed::class)
 * sealed class HomeTab : NavDestination { ... }
 *
 * @TabItem(parent = MainTabs::class, ordinal = 1)
 * @Stack(name = "exploreStack", startDestination = ExploreTab.ExploreRoot::class)
 * sealed class ExploreTab : NavDestination { ... }
 * ```
 *
 * @property classDeclaration The KSP class declaration for this tab container
 * @property name Tab container identifier from annotation (e.g., "mainTabs")
 * @property className Simple class name (e.g., "MainTabs")
 * @property packageName Package containing this class
 * @property tabs List of all tab items sorted by ordinal
 * @property isCrossModule True if this @Tabs was resolved from a compiled dependency (classpath),
 *   meaning not all tab items may be visible in this compilation unit
 */
data class TabInfo(
    val classDeclaration: KSClassDeclaration,
    val name: String,
    val className: String,
    val packageName: String,
    val tabs: List<TabItemInfo>,
    val isCrossModule: Boolean = false,
)

/**
 * Extracted metadata from a `@TabItem`-annotated class.
 *
 * ## DESTINATION
 * ```kotlin
 * @TabItem(parent = MainTabs::class, ordinal = 2)
 * @Destination(route = "settings")
 * data object SettingsTab
 * ```
 * - [tabType] = [TabItemType.DESTINATION], [destinationInfo] set, [stackInfo] = null
 *
 * ## STACK
 * ```kotlin
 * @TabItem(parent = MainTabs::class, ordinal = 0)
 * @Stack(name = "homeStack", startDestination = HomeTab.Feed::class)
 * sealed class HomeTab : NavDestination { ... }
 * ```
 * - [tabType] = [TabItemType.STACK], [destinationInfo] = null, [stackInfo] set
 *
 * ## TABS
 * ```kotlin
 * @TabItem(parent = MainTabs::class, ordinal = 1)
 * @Tabs(name = "subTabs")
 * object SubTabs
 * ```
 * - [tabType] = [TabItemType.TABS], [destinationInfo] = null, [stackInfo] = null
 *
 * @property classDeclaration The KSP class declaration for this tab item
 * @property tabType Type of tab: [TabItemType.DESTINATION], [TabItemType.STACK], or [TabItemType.TABS]
 * @property ordinal Position of this tab within the parent container (from `@TabItem(ordinal = ...)`)
 * @property destinationInfo Destination info for DESTINATION tabs. Null for others.
 * @property stackInfo Stack info for STACK tabs. Null for others.
 */
data class TabItemInfo(
    val classDeclaration: KSClassDeclaration,
    val tabType: TabItemType,
    val ordinal: Int,
    val destinationInfo: DestinationInfo? = null,
    val stackInfo: StackInfo? = null,
)
