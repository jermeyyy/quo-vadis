package com.jermey.quo.vadis.core.registry

import androidx.compose.runtime.Stable
import kotlin.reflect.KClass

/**
 * Registry for determining whether destinations or containers should be
 * presented modally.
 *
 * KSP generates implementations of this interface based on `@Modal`
 * annotations on destination classes. This enables declarative, type-safe
 * modal configuration directly on navigation destinations.
 *
 * ## Purpose
 *
 * The ModalRegistry provides a centralized lookup mechanism for
 * modal presentation semantics. When navigating to a screen, the
 * navigation system consults this registry to determine whether the
 * destination should be presented as a modal (e.g., bottom sheet,
 * dialog, or full-screen overlay).
 *
 * ## KSP Integration
 *
 * The KSP processor scans for `@Modal` annotations and generates an
 * implementation of this interface that maps destination classes to
 * their modal presentation flag.
 *
 * ## Usage Example
 *
 * ```kotlin
 * // Define a modal destination
 * @Modal
 * @Destination(route = "confirm")
 * data object ConfirmDialog : SettingsDestination()
 *
 * // The generated registry will return true for ConfirmDialog::class
 * val isModal = registry.isModalDestination(ConfirmDialog::class)
 * ```
 *
 * @see com.jermey.quo.vadis.annotations.Modal
 */
@Stable
interface ModalRegistry {

    /**
     * Checks whether a destination class is marked as modal.
     *
     * @param destinationClass The [KClass] of the destination to check
     * @return `true` if the destination should be presented modally, `false` otherwise
     */
    fun isModalDestination(destinationClass: KClass<*>): Boolean

    /**
     * Checks whether a container (identified by key) should present its
     * content modally.
     *
     * @param containerKey The unique key identifying the container
     * @return `true` if the container uses modal presentation, `false` otherwise
     */
    fun isModalContainer(containerKey: String): Boolean

    /**
     * Companion object providing default implementations and factory methods.
     */
    companion object {

        /**
         * Empty registry that returns `false` for all lookups.
         *
         * Used as a default when no modal annotations are present or when
         * modal annotations are not being processed. This ensures the system
         * gracefully falls back to standard navigation presentation.
         *
         * ## Usage
         *
         * ```kotlin
         * // Use Empty when no modal destinations are defined
         * val config = navigationConfig {
         *     // modalRegistry defaults to ModalRegistry.Empty
         * }
         * ```
         */
        val Empty: ModalRegistry = object : ModalRegistry {
            override fun isModalDestination(destinationClass: KClass<*>): Boolean = false
            override fun isModalContainer(containerKey: String): Boolean = false
        }
    }
}
