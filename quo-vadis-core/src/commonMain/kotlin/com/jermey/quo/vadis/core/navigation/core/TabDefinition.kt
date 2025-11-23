package com.jermey.quo.vadis.core.navigation.core

/**
 * Defines a tab in a tabbed navigation interface.
 *
 * Implementations should be sealed classes or enums to ensure type safety.
 * Each tab represents an independent navigation stack with its own root destination.
 *
 * Example:
 * ```kotlin
 * sealed class MainTabs : TabDefinition {
 *     data object Home : MainTabs() {
 *         override val route = "home"
 *         override val label = "Home"
 *         override val icon = "home"
 *         override val rootDestination = HomeDestination
 *     }
 *
 *     data object Profile : MainTabs() {
 *         override val route = "profile"
 *         override val label = "Profile"
 *         override val icon = "person"
 *         override val rootDestination = ProfileDestination
 *     }
 * }
 * ```
 */
interface TabDefinition {
    /**
     * Unique identifier for this tab. Used for navigation and state management.
     * Must be unique within a tab container.
     */
    val route: String

    /**
     * Display label for the tab (optional, for UI layer).
     * Can be used by tab bars or navigation drawers.
     */
    val label: String? get() = null

    /**
     * Icon identifier for the tab (optional, for UI layer).
     * Can be a resource name, icon name, or any identifier your UI system uses.
     */
    val icon: String? get() = null

    /**
     * Root destination for this tab's navigation stack.
     * This destination will be automatically navigated to when the tab is first selected.
     */
    val rootDestination: Destination
}

/**
 * Configuration for a tab navigation container.
 *
 * Defines which tabs are available, which tab is selected initially,
 * and which tab should be considered the "primary" tab for back press behavior.
 *
 * @property allTabs All tabs available in this container (order matters for UI display).
 * @property initialTab Tab to display when the container is first shown.
 * @property primaryTab Primary tab for back navigation (defaults to initialTab).
 *                       When back is pressed and not on this tab, navigation switches to this tab.
 */
data class TabNavigatorConfig(
    val allTabs: List<TabDefinition>,
    val initialTab: TabDefinition,
    val primaryTab: TabDefinition = initialTab
) {
    init {
        require(allTabs.isNotEmpty()) { "allTabs must contain at least one tab" }
        require(initialTab in allTabs) { "initialTab must be in allTabs" }
        require(primaryTab in allTabs) { "primaryTab must be in allTabs" }
        
        // Ensure all tab routes are unique
        val routes = allTabs.map { it.route }
        require(routes.size == routes.distinct().size) {
            val duplicates = routes.groupingBy { it }.eachCount().filter { it.value > 1 }.keys
            "All tab routes must be unique. Found duplicates: $duplicates"
        }
    }
}
