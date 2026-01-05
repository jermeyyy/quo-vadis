package com.jermey.quo.vadis.core.registry

import androidx.compose.runtime.Stable
import com.jermey.quo.vadis.core.compose.transition.NavTransition
import kotlin.reflect.KClass

/**
 * Registry for annotation-based transition definitions.
 *
 * KSP generates implementations of this interface based on `@Transition`
 * annotations on destination classes. This enables declarative, type-safe
 * transition configuration directly on navigation destinations.
 *
 * ## Purpose
 *
 * The TransitionRegistry provides a centralized lookup mechanism for
 * destination-specific transitions. When navigating to a screen, the
 * [AnimationCoordinator][com.jermey.quo.vadis.core.navigation.compose.animation.AnimationCoordinator]
 * consults this registry to find any custom transition defined via annotations.
 *
 * ## KSP Integration
 *
 * The KSP processor scans for `@Transition` annotations and generates an
 * implementation of this interface that maps destination classes to their
 * configured transitions.
 *
 * ## Usage Example
 *
 * ```kotlin
 * // Define a destination with a custom transition
 * @NavigationDestination("details")
 * @Transition(NavTransition.SlideVertical)
 * data class DetailsScreen(val id: String) : Destination
 *
 * // The generated registry will return SlideVertical for DetailsScreen::class
 * val transition = registry.getTransition(DetailsScreen::class)
 * ```
 *
 * ## Fallback Behavior
 *
 * When no transition is registered for a destination (returns `null`),
 * the [com.jermey.quo.vadis.core.navigation.compose.animation.AnimationCoordinator] falls back to default transitions based on
 * the navigation context (stack, tab, or pane navigation).
 *
 * @see NavTransition
 * @see com.jermey.quo.vadis.core.navigation.compose.animation.AnimationCoordinator
 */
@Stable
interface TransitionRegistry {

    /**
     * Gets the transition configuration for a destination class.
     *
     * Looks up any custom transition defined via `@Transition` annotation
     * on the given destination class.
     *
     * @param destinationClass The [KClass] of the destination to look up
     * @return The [NavTransition] for this destination, or `null` if not registered
     *
     * @see NavTransition
     */
    fun getTransition(destinationClass: KClass<*>): NavTransition?

    /**
     * Companion object providing default implementations and factory methods.
     */
    companion object {

        /**
         * Empty registry that returns `null` for all lookups.
         *
         * Used as a default when no annotations are present or when
         * transition annotations are not being processed. This ensures
         * the system gracefully falls back to default transitions.
         *
         * ## Usage
         *
         * ```kotlin
         * // Use Empty when no custom transitions are defined
         * val coordinator = AnimationCoordinator(
         *     transitionRegistry = TransitionRegistry.Empty
         * )
         * ```
         */
        val Empty: TransitionRegistry = object : TransitionRegistry {
            override fun getTransition(destinationClass: KClass<*>): NavTransition? = null
        }
    }
}
