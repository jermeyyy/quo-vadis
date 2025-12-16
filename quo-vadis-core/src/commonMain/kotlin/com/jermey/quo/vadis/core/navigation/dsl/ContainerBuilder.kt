package com.jermey.quo.vadis.core.navigation.dsl

import com.jermey.quo.vadis.core.navigation.core.Destination
import kotlin.reflect.KClass

/**
 * Sealed class hierarchy representing container configurations.
 *
 * Container builders define the structure and scope of navigation containers
 * (stack, tabs, panes). They are used by the DSL to collect container
 * configuration before building the final [NavigationConfig].
 *
 * ## Container Types
 *
 * - [Stack]: Linear navigation with push/pop semantics
 * - [Tabs]: Parallel navigation with indexed tab switching
 * - [Panes]: Adaptive multi-pane layouts
 *
 * @see NavigationConfigBuilder
 */
public sealed class ContainerBuilder {

    /**
     * The destination class that identifies this container.
     */
    public abstract val destinationClass: KClass<out Destination>

    /**
     * The scope key for this container.
     *
     * Used to determine which destinations belong to this container's scope.
     */
    public abstract val scopeKey: String

    /**
     * Stack container configuration.
     *
     * Represents a container with linear navigation history. Screens are
     * pushed onto and popped from the stack.
     *
     * @property destinationClass The container's destination class
     * @property scopeKey The scope key for membership determination
     * @property screens List of screens belonging to this stack
     */
    public data class Stack(
        override val destinationClass: KClass<out Destination>,
        override val scopeKey: String,
        val screens: List<StackScreenEntry>
    ) : ContainerBuilder()

    /**
     * Tab container configuration.
     *
     * Represents a container with parallel navigation tabs. Each tab can
     * have its own navigation stack or reference another container.
     *
     * @property destinationClass The container's destination class
     * @property scopeKey The scope key for membership determination
     * @property config The built tabs configuration
     */
    public data class Tabs(
        override val destinationClass: KClass<out Destination>,
        override val scopeKey: String,
        val config: BuiltTabsConfig
    ) : ContainerBuilder()

    /**
     * Pane container configuration.
     *
     * Represents an adaptive multi-pane container. Panes can show/hide
     * based on screen size and each can have its own navigation stack.
     *
     * @property destinationClass The container's destination class
     * @property scopeKey The scope key for membership determination
     * @property config The built panes configuration
     */
    public data class Panes(
        override val destinationClass: KClass<out Destination>,
        override val scopeKey: String,
        val config: BuiltPanesConfig
    ) : ContainerBuilder()
}
