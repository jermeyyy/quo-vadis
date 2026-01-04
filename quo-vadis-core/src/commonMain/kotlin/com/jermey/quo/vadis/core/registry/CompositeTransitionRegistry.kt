package com.jermey.quo.vadis.core.dsl.registry

import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.compose.animation.NavTransition
import com.jermey.quo.vadis.core.registry.TransitionRegistry
import kotlin.reflect.KClass

/**
 * Composite transition registry that delegates to secondary first, then primary.
 *
 * **Internal API** - This is an internal implementation detail of Quo Vadis.
 * Composite registries are managed internally by the navigation system.
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
