package com.jermey.quo.vadis.core.registry.internal

import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.compose.transition.NavTransition
import com.jermey.quo.vadis.core.registry.TransitionRegistry
import kotlin.reflect.KClass

/**
 * Composite transition registry that delegates to secondary first, then primary.
 */
@InternalQuoVadisApi
internal class CompositeTransitionRegistry(
    private val primary: TransitionRegistry,
    private val secondary: TransitionRegistry
) : TransitionRegistry {

    override fun getTransition(destinationClass: KClass<*>): NavTransition? {
        return secondary.getTransition(destinationClass) ?: primary.getTransition(destinationClass)
    }
}
