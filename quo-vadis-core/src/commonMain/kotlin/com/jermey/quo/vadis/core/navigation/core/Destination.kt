package com.jermey.quo.vadis.core.navigation.core

/**
 * Base interface for all navigation destinations.
 * Each screen/feature should implement this to define a navigation target.
 *
 * All destinations support optional transition animations and type-safe arguments.
 */
interface Destination {
    /**
     * Unique identifier for this destination.
     * Used for backstack management and deep linking.
     */
    val route: String

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
 * sealed class AppDestination : Destination {
 *     data class ProductDetail(val itemId: String) : 
 *         AppDestination(), TypedDestination<ProductDetails> {
 *         override val route = "product_detail"
 *         override val data = ProductDetails(itemId, "Product Name")
 *     }
 * }
 * ```
 */
interface TypedDestination<T>: Destination {
    override val route: String
    override val data: T
    override val transition: NavigationTransition?
        get() = null
}

/**
 * Internal implementation of TypedDestination for state restoration.
 * This class is used by the serialization framework to reconstruct typed destinations
 * from saved state. The actual type information is preserved as a string and
 * deserialized by the DSL layer.
 * 
 * @internal This class is internal to the library.
 */
internal data class RestoredTypedDestination<T>(
    override val route: String,
    override val data: T,
    override val transition: NavigationTransition? = null
) : TypedDestination<T>

/**
 * Basic destination implementation for simple navigation without typed data.
 * Prefer using sealed classes with specific destination objects for type safety.
 *
 * This class does not accept data in its constructor to enforce type safety.
 * Use sealed classes or TypedDestination for destinations that need to carry data.
 * 
 * @internal This class is internal to the library. Use the DSL functions to create destinations.
 */
internal data class BasicDestination(
    override val route: String,
    override val transition: NavigationTransition? = null
) : Destination


