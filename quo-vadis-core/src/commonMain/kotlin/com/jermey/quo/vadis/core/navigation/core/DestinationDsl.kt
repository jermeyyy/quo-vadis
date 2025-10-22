package com.jermey.quo.vadis.core.navigation.core

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
        // Extract the data from the current destination in backstack
        val currentDest = navigator.backStack.current.value

        // TypedDestination now requires non-null data, so it must be present
        val data: T = when (val destData = currentDest?.destination?.data) {
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
                @Suppress("UNCHECKED_CAST")
                (destData as? T)
                    ?: error("Error: Type mismatch for destination '$route'. Expected ${dataSerializer.descriptor.serialName}")
            }
        }

        content(data, navigator)
    }
}

