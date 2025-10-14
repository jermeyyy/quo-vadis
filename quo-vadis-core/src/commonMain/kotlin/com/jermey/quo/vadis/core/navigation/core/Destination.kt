package com.jermey.quo.vadis.core.navigation.core



/**
 * Base interface for all navigation destinations.
 * Each screen/feature should implement this to define a navigation target.
 */
interface Destination {
    /**
     * Unique identifier for this destination.
     * Used for backstack management and deep linking.
     */
    val route: String

    /**
     * Optional arguments passed to this destination.
     */
    val arguments: Map<String, Any?> get() = emptyMap()
}

/**
 * Destination with default transition.
 * Implement this interface to specify a default transition for a destination.
 */
interface TransitionDestination : Destination {
    /**
     * Default transition when navigating TO this destination.
     * If null, uses NavHost's default transition.
     */
    val defaultTransition: NavigationTransition?
        get() = null
}

/**
 * Extension property to access transition from any Destination.
 */
val Destination.transition: NavigationTransition?
    get() = (this as? TransitionDestination)?.defaultTransition

/**
 * Extension property to access shared elements from any Destination.
 */
val Destination.sharedElements: List<SharedElementKey>
    get() = (this as? SharedElementDestination)?.sharedElementKeys ?: emptyList()

/**
 * Destination that participates in shared element transitions.
 * Implement this interface to enable shared element animations for a destination.
 */
interface SharedElementDestination : TransitionDestination {
    /**
     * Shared element keys for this destination.
     */
    val sharedElementKeys: List<SharedElementKey>
        get() = emptyList()

    /**
     * Default shared element transition.
     * Should typically use withSharedElements() on a base transition.
     */
    override val defaultTransition: NavigationTransition?
        get() = null
}

/**
 * Typed destination that can carry data with type safety.
 */
abstract class TypedDestination<T : Any>(
    override val route: String,
    val data: T? = null
) : Destination {
    override val arguments: Map<String, Any?>
        get() = data?.let { mapOf("data" to it) } ?: emptyMap()
}

/**
 * Simple destination without data.
 */
data class SimpleDestination(
    override val route: String,
    override val arguments: Map<String, Any?> = emptyMap()
) : Destination

/**
 * Destination builder for creating destinations with arguments.
 */
class DestinationBuilder(private val route: String) {
    private val args = mutableMapOf<String, Any?>()

    fun arg(key: String, value: Any?): DestinationBuilder {
        args[key] = value
        return this
    }

    fun build(): Destination = SimpleDestination(route, args)
}

/**
 * Extension function to create destinations with builder pattern.
 */
fun destination(route: String, block: DestinationBuilder.() -> Unit = {}): Destination {
    return DestinationBuilder(route).apply(block).build()
}

