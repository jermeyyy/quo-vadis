package com.jermey.quo.vadis.core.dsl.internal

import androidx.compose.runtime.Stable
import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.registry.ModalRegistry
import kotlin.reflect.KClass

/**
 * DSL-based implementation of [ModalRegistry] that tracks which destinations
 * and containers should be presented modally.
 *
 * This registry is created by [DslNavigationConfig] from modal registrations
 * collected by [com.jermey.quo.vadis.core.dsl.NavigationConfigBuilder].
 *
 * ## Usage
 *
 * Modal destinations and containers are registered via the DSL:
 *
 * ```kotlin
 * val config = navigationConfig {
 *     modal<ConfirmDialog>()
 *     modalContainer("bottom-sheet")
 * }
 * ```
 *
 * The registry is then consulted during navigation to determine the
 * appropriate presentation style.
 *
 * @param modalDestinations Set of destination classes marked as modal
 * @param modalContainers Set of container keys marked as modal
 *
 * @see ModalRegistry
 * @see com.jermey.quo.vadis.core.dsl.NavigationConfigBuilder.modal
 */
@InternalQuoVadisApi
@Stable
internal class DslModalRegistry(
    private val modalDestinations: Set<KClass<out NavDestination>>,
    private val modalContainers: Set<String>
) : ModalRegistry {

    /**
     * Checks whether a destination class is marked as modal.
     *
     * @param destinationClass The [KClass] of the destination to check
     * @return `true` if the destination is in the modal destinations set
     */
    override fun isModalDestination(destinationClass: KClass<*>): Boolean {
        return destinationClass in modalDestinations
    }

    /**
     * Checks whether a container is marked as modal.
     *
     * @param containerKey The unique key identifying the container
     * @return `true` if the container key is in the modal containers set
     */
    override fun isModalContainer(containerKey: String): Boolean {
        return containerKey in modalContainers
    }
}
