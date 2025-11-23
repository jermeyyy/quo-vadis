package com.jermey.quo.vadis.flowmvi.savedstate

import com.jermey.quo.vadis.core.navigation.core.Destination
import com.jermey.quo.vadis.flowmvi.core.NavigationState
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Serializable wrapper for NavigationState.
 * 
 * Provides a serializable representation of navigation state that can be:
 * - Saved to persistent storage
 * - Restored after process death
 * - Transferred between platforms
 * 
 * Note: Destination data must be serializable. Complex objects should
 * implement kotlinx.serialization @Serializable annotation.
 */
@Serializable
data class SerializableNavigationState(
    /**
     * Route string of the current destination.
     * Used to recreate the destination instance.
     */
    val currentRoute: String?,
    
    /**
     * JSON-encoded destination data.
     * Null if destination has no data.
     */
    val destinationData: String?,
    
    /**
     * Back stack size at serialization time.
     */
    val backStackSize: Int,
    
    /**
     * Whether navigation back is possible.
     */
    val canGoBack: Boolean,
    
    /**
     * List of routes in the back stack (oldest to newest).
     * Used to restore full navigation history.
     */
    val backStackRoutes: List<String> = emptyList()
)

/**
 * Interface for serializing and deserializing navigation state.
 * 
 * Implement this interface to provide custom serialization logic for your destinations.
 * The default implementation uses destination class names as routes.
 * 
 * Example custom implementation:
 * ```kotlin
 * class MyNavigationStateSerializer : NavigationStateSerializer {
 *     override fun serialize(state: NavigationState): SerializableNavigationState {
 *         // Custom logic to serialize your destinations
 *     }
 *     
 *     override fun deserialize(serialized: SerializableNavigationState): NavigationState {
 *         // Custom logic to recreate destinations
 *     }
 * }
 * ```
 */
interface NavigationStateSerializer {
    /**
     * Serialize a NavigationState to a serializable format.
     * 
     * @param state The navigation state to serialize
     * @return Serializable representation
     */
    fun serialize(state: NavigationState): SerializableNavigationState
    
    /**
     * Deserialize a SerializableNavigationState back to NavigationState.
     * 
     * @param serialized The serialized state
     * @return Reconstructed navigation state, or null if deserialization fails
     */
    fun deserialize(serialized: SerializableNavigationState): NavigationState?
}

/**
 * Default implementation of NavigationStateSerializer.
 * 
 * Uses destination class simple names as routes.
 * Does NOT preserve full back stack history (only current destination).
 * 
 * For production use, consider implementing a custom serializer that:
 * - Uses stable route identifiers (not class names)
 * - Properly serializes destination data
 * - Preserves back stack history
 */
class DefaultNavigationStateSerializer : NavigationStateSerializer {
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }
    
    override fun serialize(state: NavigationState): SerializableNavigationState {
        val destination = state.currentDestination
        return SerializableNavigationState(
            currentRoute = destination?.let { it::class.simpleName },
            destinationData = destination?.data?.let { data ->
                try {
                    json.encodeToString(data)
                } catch (e: Exception) {
                    null // Serialization failed, skip data
                }
            },
            backStackSize = state.backStackSize,
            canGoBack = state.canGoBack,
            backStackRoutes = emptyList() // Basic implementation doesn't preserve history
        )
    }
    
    override fun deserialize(serialized: SerializableNavigationState): NavigationState? {
        // Basic implementation: only restore if we have a route
        return if (serialized.currentRoute != null) {
            object : NavigationState {
                override val currentDestination: Destination? = null // Can't recreate without registry
                override val backStackSize: Int = serialized.backStackSize
                override val canGoBack: Boolean = serialized.canGoBack
                
                override fun equals(other: Any?): Boolean = other is NavigationState && 
                    other.currentDestination == null &&
                    other.backStackSize == backStackSize &&
                    other.canGoBack == canGoBack
                    
                override fun hashCode(): Int {
                    var result = 0
                    result = 31 * result + backStackSize
                    result = 31 * result + canGoBack.hashCode()
                    return result
                }
            }
        } else {
            null
        }
    }
}

/**
 * Extension to serialize NavigationState to JSON string.
 * 
 * Usage:
 * ```kotlin
 * val state: NavigationState = ...
 * val json = state.toJsonString(serializer)
 * ```
 */
fun NavigationState.toJsonString(serializer: NavigationStateSerializer): String {
    val serializable = serializer.serialize(this)
    return Json.encodeToString(serializable)
}

/**
 * Extension to deserialize NavigationState from JSON string.
 * 
 * Usage:
 * ```kotlin
 * val json: String = ...
 * val state = json.toNavigationState(serializer)
 * ```
 */
fun String.toNavigationState(serializer: NavigationStateSerializer): NavigationState? {
    return try {
        val serializable = Json.decodeFromString<SerializableNavigationState>(this)
        serializer.deserialize(serializable)
    } catch (e: Exception) {
        null
    }
}
