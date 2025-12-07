package com.jermey.quo.vadis.core.navigation.core

/**
 * Marker interface for navigation destinations.
 * 
 * Use this interface with @Route annotations to define navigation targets.
 * The actual route resolution is handled internally by the library via RouteRegistry.
 * 
 * KSP generates the necessary code to register routes, so destinations remain simple POJOs.
 */
interface Destination {
    /**
     * Optional data passed to this destination.
     * Should be a serializable type when using TypedDestination.
     * Use null for destinations without data.
     */
    val data: Any? get() = null

    /**
     * Default transition when navigating TO this destination.
     * If null, uses NavHost's default transition.
     */
    val transition: NavigationTransition? get() = null
}

/**
 * Extension to get the route for any Destination.
 * For BasicDestination, returns the stored route directly.
 * For other destinations, routes are resolved from RouteRegistry (populated by KSP-generated code).
 */
val Destination.route: String
    get() {
        // BasicDestination holds its route directly
        if (this is BasicDestination) {
            return this.routeString
        }
        
        // For user-defined destinations, use RouteRegistry
        val kClass = this::class
        val route = RouteRegistry.getRoute(kClass)
        println("DEBUG: Destination.route - class=$kClass, simpleName=${kClass.simpleName}, route=$route")
        return route ?: error("Route not registered for ${kClass.simpleName}. Ensure @Route annotation is present.")
    }

/**
 * Typed destination that can carry serializable data with type safety.
 *
 * @param T The type of data this destination carries (must be non-nullable).
 * Type should be serializable for state persistence.
 * 
 * This interface can be implemented to create custom typed destinations using sealed classes.
 * 
 * Example:
 * ```
 * @Serializable
 * data class ProductDetails(val id: String, val name: String)
 * 
 * @Graph("app")
 * sealed class AppDestination : Destination {
 *     @Route("product_detail")
 *     @Argument(ProductDetails::class)
 *     data class ProductDetail(val itemId: String) : 
 *         AppDestination(), TypedDestination<ProductDetails> {
 *         override val data = ProductDetails(itemId, "Product Name")
 *     }
 * }
 * ```
 */
@Deprecated(
    message = "TypedDestination<T> is replaced by route templates. Use @Destination(route = \"path/{param}\") with data class properties. Access params directly on the destination instance.",
    level = DeprecationLevel.WARNING
)
interface TypedDestination<T>: Destination {
    override val data: T
    override val transition: NavigationTransition?
        get() = null
}

/**
 * Internal implementation of TypedDestination for state restoration.
 * This class is used by the serialization framework to reconstruct typed destinations
 * from saved state. Routes are resolved via RouteRegistry.
 * 
 * @internal This class is internal to the library.
 */
@Deprecated(
    message = "RestoredTypedDestination is replaced by NavNode state serialization. Destination restoration is handled automatically by the NavNode tree.",
    level = DeprecationLevel.WARNING
)
internal data class RestoredTypedDestination<T>(
    private val routeString: String,
    override val data: T,
    override val transition: NavigationTransition? = null
) : TypedDestination<T> {
    init {
        // Register the route for this restored destination
        RouteRegistry.register(this::class, routeString)
    }
}

/**
 * Basic destination implementation for simple navigation without typed data.
 * Holds the route string directly, bypassing RouteRegistry to avoid conflicts.
 * 
 * @internal This class is internal to the library. Use the DSL functions to create destinations.
 */
@Deprecated(
    message = "BasicDestination is replaced by sealed class members.",
    level = DeprecationLevel.WARNING
)
internal data class BasicDestination(
    internal val routeString: String,
    override val transition: NavigationTransition? = null
) : Destination


