package com.jermey.quo.vadis.core.navigation.compose.registry

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import com.jermey.quo.vadis.core.navigation.core.NavDestination

/**
 * Interface for screen registries that map destinations to composable content.
 *
 * Implementations are typically KSP-generated from @Screen annotations.
 * The registry provides a central dispatch mechanism for rendering screen
 * content based on the current destination.
 *
 * @see com.jermey.quo.vadis.annotations.Screen
 */
interface ScreenRegistry {

    /**
     * Render composable content for the given destination.
     *
     * @param destination The destination to render
     * @param sharedTransitionScope Optional SharedTransitionScope for shared element transitions
     * @param animatedVisibilityScope Optional AnimatedVisibilityScope for coordinated animations
     */
    @Composable
    fun Content(
        destination: NavDestination,
        sharedTransitionScope: SharedTransitionScope? = null,
        animatedVisibilityScope: AnimatedVisibilityScope? = null
    )

    /**
     * Check if content is registered for the given destination.
     *
     * @param destination The destination to check
     * @return true if a screen is registered for this destination
     */
    fun hasContent(destination: NavDestination): Boolean
}
