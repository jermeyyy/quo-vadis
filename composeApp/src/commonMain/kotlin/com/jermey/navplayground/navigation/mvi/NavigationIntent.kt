package com.jermey.navplayground.navigation.mvi

import com.jermey.navplayground.navigation.core.Destination
import com.jermey.navplayground.navigation.core.NavigationTransition

/**
 * MVI integration for navigation.
 * Navigation intents that can be used in MVI architecture.
 */
sealed interface NavigationIntent {
    /**
     * Navigate to a destination.
     */
    data class Navigate(
        val destination: Destination,
        val transition: NavigationTransition? = null
    ) : NavigationIntent

    /**
     * Navigate back.
     */
    data object NavigateBack : NavigationIntent

    /**
     * Navigate up in the hierarchy.
     */
    data object NavigateUp : NavigationIntent

    /**
     * Navigate and clear backstack.
     */
    data class NavigateAndClearAll(
        val destination: Destination
    ) : NavigationIntent

    /**
     * Navigate and clear to a specific route.
     */
    data class NavigateAndClearTo(
        val destination: Destination,
        val clearRoute: String,
        val inclusive: Boolean = false
    ) : NavigationIntent

    /**
     * Replace current destination.
     */
    data class NavigateAndReplace(
        val destination: Destination,
        val transition: NavigationTransition? = null
    ) : NavigationIntent

    /**
     * Handle deep link.
     */
    data class HandleDeepLink(val uri: String) : NavigationIntent
}

/**
 * Navigation effect for MVI side effects.
 */
sealed interface NavigationEffect {
    data class NavigationFailed(val reason: String) : NavigationEffect
    data class DeepLinkHandled(val success: Boolean) : NavigationEffect
}

/**
 * Navigation state for MVI.
 */
data class NavigationState(
    val currentRoute: String? = null,
    val canGoBack: Boolean = false,
    val isNavigating: Boolean = false
)

