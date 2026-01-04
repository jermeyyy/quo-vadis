package com.jermey.quo.vadis.core.registry

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.navigation.destination.NavDestination

/**
 * Composite screen registry that delegates to secondary first, then primary.
 *
 * **Internal API** - This is an internal implementation detail of Quo Vadis.
 * Composite registries are managed internally by the navigation system.
 *
 * Priority: secondary > primary (secondary wins for duplicates)
 */
@InternalQuoVadisApi
internal class CompositeScreenRegistry(
    private val primary: ScreenRegistry,
    private val secondary: ScreenRegistry
) : ScreenRegistry {

    @Composable
    override fun Content(
        destination: NavDestination,
        sharedTransitionScope: SharedTransitionScope?,
        animatedVisibilityScope: AnimatedVisibilityScope?
    ) {
        // Check secondary first (higher priority), then primary
        when {
            secondary.hasContent(destination) -> secondary.Content(
                destination,
                sharedTransitionScope,
                animatedVisibilityScope
            )
            primary.hasContent(destination) -> primary.Content(
                destination,
                sharedTransitionScope,
                animatedVisibilityScope
            )
            else -> throw IllegalStateException(
                "No screen registered for destination: ${destination::class.simpleName}"
            )
        }
    }

    override fun hasContent(destination: NavDestination): Boolean {
        return secondary.hasContent(destination) || primary.hasContent(destination)
    }
}
