package com.jermey.quo.vadis.core.dsl.registry

import com.jermey.quo.vadis.core.navigation.compose.animation.NavTransition
import kotlin.reflect.KClass

/**
 * Composite transition registry that delegates to secondary first, then primary.
 */
internal class CompositeTransitionRegistry(
    private val primary: TransitionRegistry,
    private val secondary: TransitionRegistry
) : TransitionRegistry {

    override fun getTransition(destinationClass: KClass<*>): NavTransition? {
        return secondary.getTransition(destinationClass) ?: primary.getTransition(destinationClass)
    }
}
