package com.jermey.quo.vadis.ksp.models

import com.google.devtools.ksp.symbol.KSClassDeclaration

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
 *     @TabItem(label = "Home", icon = "home", rootGraph = HomeGraph::class)
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
 * Supports two patterns:
 *
 * ## New Pattern
 * ```kotlin
 * @TabItem(label = "Home", icon = "home")
 * data object HomeTab : MainTabs  // Class IS the navigation stack
 * ```
 * - [classDeclaration] = HomeTab class
 * - [rootGraphClass] = null (class itself is the stack)
 * - [destination] = null (not required)
 *
 * ## Legacy Pattern
 * ```kotlin
 * @TabItem(label = "Home", icon = "home", rootGraph = HomeGraph::class)
 * data object Home : MainTabs(), Destination  // Separate rootGraph
 * ```
 * - [classDeclaration] = Home class
 * - [rootGraphClass] = HomeGraph class reference
 * - [destination] = DestinationInfo for this tab item
 *
 * @property label Display label for the tab (e.g., "Home")
 * @property icon Icon identifier for the tab (e.g., "home")
 * @property classDeclaration The KSP class declaration for this tab item
 * @property rootGraphClass Class declaration for the root graph of this tab.
 *                          Null for new pattern where the class itself is the stack.
 * @property destination Destination info for this tab item.
 *                       Null for new pattern (doesn't require nested @Destination).
 */
data class TabItemInfo(
    val label: String,
    val icon: String,
    val classDeclaration: KSClassDeclaration,
    val rootGraphClass: KSClassDeclaration?,
    val destination: DestinationInfo?,
)
