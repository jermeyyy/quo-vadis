package com.jermey.quo.vadis.core.registry.internal

import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.registry.ModalRegistry
import kotlin.reflect.KClass

/**
 * Composite modal registry that unions primary and secondary lookups.
 *
 * If either the primary or secondary registry reports a destination or
 * container as modal, the composite registry returns `true`.
 */
@InternalQuoVadisApi
internal class CompositeModalRegistry(
    private val primary: ModalRegistry,
    private val secondary: ModalRegistry
) : ModalRegistry {

    override fun isModalDestination(destinationClass: KClass<*>): Boolean {
        return secondary.isModalDestination(destinationClass) ||
            primary.isModalDestination(destinationClass)
    }

    override fun isModalContainer(containerKey: String): Boolean {
        return secondary.isModalContainer(containerKey) ||
            primary.isModalContainer(containerKey)
    }
}
