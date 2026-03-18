package com.jermey.quo.vadis.core.registry

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import com.jermey.quo.vadis.core.navigation.destination.NavDestination

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

    companion object {
        /**
         * Empty registry that has no screen content registered.
         *
         * All lookups return false and [Content] is a no-op.
         * This is the identity element when no screens are registered.
         */
        val Empty: ScreenRegistry = object : ScreenRegistry {
            @Composable
            override fun Content(
                destination: NavDestination,
                sharedTransitionScope: SharedTransitionScope?,
                animatedVisibilityScope: AnimatedVisibilityScope?
            ) {
                // No screens registered — nothing to render
            }

            override fun hasContent(destination: NavDestination): Boolean = false
        }
    }
}
