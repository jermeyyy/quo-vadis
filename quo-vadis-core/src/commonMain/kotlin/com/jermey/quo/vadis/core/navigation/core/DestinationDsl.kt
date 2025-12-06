package com.jermey.quo.vadis.core.navigation.core

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.runtime.Composable
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

/**
 * DSL extension for registering destinations with type-safe serializable data.
 *
 * This function allows you to register a destination that carries typed, serializable data.
 * The data is automatically serialized/deserialized using kotlinx.serialization.
 *
 * Example:
 * ```
 * @Serializable
 * data class ProductDetails(val id: String, val name: String)
 *
 * navigationGraph("myGraph") {
 *     typedDestination<ProductDetails>("product_details") { data, navigator ->
 *         ProductScreen(id = data.id, name = data.name)
 *     }
 * }
 *
 * // Navigate with:
 * navigator.navigate(
 *     TypedDestination(
 *         route = "product_details",
 *         data = ProductDetails("123", "Widget")
 *     )
 * )
 * ```
 *
 * @param DESTINATION The non-nullable serializable data type (must be annotated with @Serializable).
 *          Use a regular destination() for routes without data.
 * @param route The unique route identifier for this destination
 * @param transition Optional default transition animation
 * @param content Composable content that receives the typed data and navigator
 */
@Suppress("UNCHECKED_CAST")
@OptIn(InternalSerializationApi::class)
inline fun <reified DESTINATION: TypedDestination<TYPE>, reified TYPE: Any> NavigationGraphBuilder.typedDestination(
    route: String,
    transition: NavigationTransition? = null,
    noinline content: @Composable (TYPE, Navigator) -> Unit
) {
    typedDestinationImpl(
        route = route,
        transition = transition,
        dataSerializer = serializer<TYPE>(),
        content = content
    )
}

/**
 * Internal implementation for typedDestination to avoid inline function accessing internal constructors.
 */
@PublishedApi
internal fun <T : Any> NavigationGraphBuilder.typedDestinationImpl(
    route: String,
    transition: NavigationTransition?,
    dataSerializer: KSerializer<T>,
    content: @Composable (T, Navigator) -> Unit
) {
    destination(
        destination = BasicDestination(route, transition),
        transition = transition
    ) { dest, navigator ->
        // Extract the data from the current destination in state tree
        val currentDest = navigator.currentDestination.value

        // TypedDestination now requires non-null data, so it must be present
        val data: T = when (val destData = currentDest?.data) {
            null -> error("Error: TypedDestination requires non-null data for route '$route'")
            is String -> {
                // Deserialize from JSON string (e.g., from state restoration)
                @Suppress("TooGenericExceptionCaught", "SwallowedException")
                try {
                    val json = Json { ignoreUnknownKeys = true }
                    json.decodeFromString(dataSerializer, destData)
                } catch (e: Exception) {
                    error("Error: Failed to deserialize data for destination '$route': ${e.message}")
                }
            }

            else -> {
                // Direct cast (in-memory navigation with typed data)
                val expectedType = dataSerializer.descriptor.serialName
                @Suppress("UNCHECKED_CAST")
                (destData as? T)
                    ?: error("Error: Type mismatch for destination '$route'. Expected $expectedType")
            }
        }

        content(data, navigator)
    }
}

/**
 * DSL extension for registering typed destinations with shared element transition support.
 *
 * This variant provides access to TransitionScope for shared element animations.
 * Use this when you need shared elements with typed, serializable data destinations.
 *
 * @param DESTINATION The destination type implementing TypedDestination<TYPE>
 * @param TYPE The non-nullable serializable data type (must be annotated with @Serializable)
 * @param route The unique route identifier for this destination
 * @param transition Optional default transition animation
 * @param content Composable content that receives the typed data, navigator, and transition scope
 */
@Suppress("UNCHECKED_CAST")
@OptIn(InternalSerializationApi::class, ExperimentalSharedTransitionApi::class)
inline fun <reified DESTINATION: TypedDestination<TYPE>, reified TYPE: Any>
NavigationGraphBuilder.typedDestinationWithScopes(
    route: String,
    transition: NavigationTransition? = null,
    noinline content: @Composable (
        TYPE,
        Navigator,
        com.jermey.quo.vadis.core.navigation.compose.TransitionScope?
    ) -> Unit
) {
    typedDestinationWithScopesImpl(
        route = route,
        transition = transition,
        dataSerializer = serializer<TYPE>(),
        content = content
    )
}

/**
 * Internal implementation for typedDestinationWithScopes.
 */
@Suppress("UNCHECKED_CAST")
@PublishedApi
@OptIn(ExperimentalSharedTransitionApi::class)
internal fun <T : Any> NavigationGraphBuilder.typedDestinationWithScopesImpl(
    route: String,
    transition: NavigationTransition?,
    dataSerializer: KSerializer<T>,
    content: @Composable (T, Navigator, com.jermey.quo.vadis.core.navigation.compose.TransitionScope?) -> Unit
) {
    destinationWithScopes(
        destination = BasicDestination(route, transition),
        transition = transition
    ) { dest, navigator, transitionScope ->
        // Extract the data from the current destination in state tree
        val currentDest = navigator.currentDestination.value

        println("DEBUG: typedDestinationWithScopesImpl - route=$route")
        println("DEBUG: currentDest=${currentDest}")
        println("DEBUG: currentDest.data=${currentDest?.data}")
        println("DEBUG: currentDest.route=${currentDest?.route}")

        // TypedDestination now requires non-null data, so it must be present
        val data: T = when (val destData = currentDest?.data) {
            null -> {
                println("DEBUG: ERROR - No data found!")
                error("Error: TypedDestination requires non-null data for route '$route'")
            }
            is String -> {
                println("DEBUG: Data is String, deserializing...")
                // Deserialize from JSON string (e.g., from state restoration)
                @Suppress("TooGenericExceptionCaught", "SwallowedException")
                try {
                    val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                    json.decodeFromString(dataSerializer, destData)
                } catch (e: Exception) {
                    error("Error: Failed to deserialize data for destination '$route': ${e.message}")
                }
            }

            else -> {
                println("DEBUG: Data is object, casting... type=${destData::class}")
                // Direct cast (in-memory navigation with typed data)
                @Suppress("UNCHECKED_CAST")
                val expectedType = dataSerializer.descriptor.serialName
                val actualType = destData::class
                (destData as? T)
                    ?: error(
                        "Error: Type mismatch for destination '$route'. " +
                        "Expected $expectedType, got $actualType"
                    )
            }
        }

        println("DEBUG: Successfully extracted data: $data")
        content(data, navigator, transitionScope)
    }
}


