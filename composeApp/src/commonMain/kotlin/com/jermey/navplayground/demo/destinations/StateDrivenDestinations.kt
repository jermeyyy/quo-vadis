package com.jermey.navplayground.demo.destinations

import com.jermey.quo.vadis.core.navigation.core.Destination

/**
 * Destinations for the State-Driven Navigation Demo.
 *
 * This sealed class demonstrates simple destinations for use with [StateBackStack]
 * and [StateNavigator]. Unlike graph-based navigation, these destinations don't
 * require @Route annotations since they're used directly with the state-driven API.
 *
 * The demo showcases:
 * - Object destinations (Home, Settings) - no parameters
 * - Data class destinations (Profile, Detail) - with parameters
 */
sealed class StateDrivenDestination : Destination {

    /**
     * Home destination - the starting point of the demo.
     */
    data object Home : StateDrivenDestination() {
        override fun toString(): String = "Home"
    }

    /**
     * Profile destination with a user ID parameter.
     *
     * @property userId The ID of the user to display
     */
    data class Profile(val userId: String) : StateDrivenDestination() {
        override fun toString(): String = "Profile($userId)"
    }

    /**
     * Settings destination - no parameters.
     */
    data object Settings : StateDrivenDestination() {
        override fun toString(): String = "Settings"
    }

    /**
     * Detail destination with an item ID parameter.
     *
     * @property itemId The ID of the item to display
     */
    data class Detail(val itemId: String) : StateDrivenDestination() {
        override fun toString(): String = "Detail($itemId)"
    }

    companion object {
        /**
         * Returns a display name for the destination type (for UI).
         */
        fun getDisplayName(destination: StateDrivenDestination): String = when (destination) {
            is Home -> "Home"
            is Profile -> "Profile"
            is Settings -> "Settings"
            is Detail -> "Detail"
        }

        /**
         * Returns all available destination types for the picker.
         */
        val allTypes: List<String> = listOf("Home", "Profile", "Settings", "Detail")
    }
}
