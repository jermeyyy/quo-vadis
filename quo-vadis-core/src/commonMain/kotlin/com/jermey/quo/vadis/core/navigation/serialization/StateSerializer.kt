package com.jermey.quo.vadis.core.navigation.serialization

import com.jermey.quo.vadis.core.navigation.core.BackStack
import com.jermey.quo.vadis.core.navigation.core.BackStackEntry
import com.jermey.quo.vadis.core.navigation.core.Destination
import com.jermey.quo.vadis.core.navigation.core.MutableBackStack
import com.jermey.quo.vadis.core.navigation.core.RestoredTypedDestination
import com.jermey.quo.vadis.core.navigation.core.TypedDestination
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Serializable wrapper for Destination.
 * Used to convert Destination interface to a serializable data class.
 * 
 * The data field stores the serialized JSON representation of the destination's data.
 * The hasTypedData flag indicates whether this destination has typed data or is a simple destination.
 * This allows proper type-safe deserialization using kotlinx.serialization while preserving type information.
 */
@Serializable
data class SerializableDestination(
    val route: String,
    val data: String = "",
    val hasTypedData: Boolean = false
)

/**
 * Serializable wrapper for BackStackEntry.
 * Used to convert BackStackEntry to a fully serializable structure.
 */
@Serializable
data class SerializableBackStackEntry(
    val id: String,
    val destination: SerializableDestination,
    val savedState: Map<String, String> = emptyMap()
)

/**
 * State restoration support for navigation.
 * Allows saving and restoring backstack state across process death.
 *
 * Now powered by kotlinx.serialization for robust, type-safe serialization.
 */
interface NavigationStateSerializer {
    /**
     * Serialize backstack to a string representation.
     */
    fun serializeBackStack(entries: List<BackStackEntry>): String

    /**
     * Deserialize backstack from a string representation.
     */
    fun deserializeBackStack(serialized: String): List<BackStackEntry>

    /**
     * Serialize a single destination.
     */
    fun serializeDestination(destination: Destination): String

    /**
     * Deserialize a single destination.
     */
    fun deserializeDestination(serialized: String): Destination?
}

/**
 * Kotlinx.serialization-based serializer for navigation state.
 * Provides robust JSON serialization with proper type safety.
 */
class KotlinxNavigationStateSerializer(
    private val json: Json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
        encodeDefaults = true
    }
) : NavigationStateSerializer {

    override fun serializeBackStack(entries: List<BackStackEntry>): String {
        val serializableEntries = entries.map { it.toSerializable() }
        return json.encodeToString(serializableEntries)
    }

    override fun deserializeBackStack(serialized: String): List<BackStackEntry> {
        if (serialized.isBlank()) return emptyList()

        return try {
            val serializableEntries =
                json.decodeFromString<List<SerializableBackStackEntry>>(serialized)
            serializableEntries.map { it.toBackStackEntry() }
        } catch (e: SerializationException) {
            // Log or handle serialization errors if needed
            println("Failed to deserialize backstack: ${e.message}")
            emptyList()
        }
    }

    override fun serializeDestination(destination: Destination): String {
        val serializableDestination = destination.toSerializable()
        return json.encodeToString(serializableDestination)
    }

    override fun deserializeDestination(serialized: String): Destination? {
        return try {
            val serializableDestination = json.decodeFromString<SerializableDestination>(serialized)
            serializableDestination.toDestination()
        } catch (e: SerializationException) {
            // Log or handle serialization errors if needed
            println("Failed to deserialize destination: ${e.message}")
            null
        }
    }

    private fun Destination.toSerializable(): SerializableDestination {
        // Check if this is a TypedDestination
        val isTypedDestination = this is TypedDestination<*>
        
        val serializedData = when (val destData = data) {
            null -> "" // Empty string for destinations without data
            is String -> destData // Already a string (from previous serialization)
            else -> {
                // Convert to string representation
                // For proper serialization, the destination should use TypedDestination
                // with a serializable type and handle serialization in the DSL
                destData.toString()
            }
        }
        
        return SerializableDestination(
            route = route,
            data = serializedData,
            hasTypedData = isTypedDestination && serializedData.isNotEmpty()
        )
    }

    private fun SerializableDestination.toDestination(): Destination {
        // Use the type discriminator to determine what kind of destination to restore
        return when {
            hasTypedData && data.isNotEmpty() -> {
                // This was a TypedDestination - restore it with the internal implementation
                // The actual typed deserialization happens in typedDestination DSL
                RestoredTypedDestination(
                    route = route,
                    data = data
                )
            }
            else -> {
                // This was a simple destination without typed data
                val serializedData = this.data
                object : Destination {
                    override val route = this@toDestination.route
                    override val data: Any? = if (serializedData.isEmpty()) null else serializedData
                }
            }
        }
    }

    private fun BackStackEntry.toSerializable(): SerializableBackStackEntry {
        return SerializableBackStackEntry(
            id = id,
            destination = destination.toSerializable(),
            savedState = savedState.mapValues { (_, value) -> value.toString() }
        )
    }

    private fun SerializableBackStackEntry.toBackStackEntry(): BackStackEntry {
        return BackStackEntry(
            id = id,
            destination = destination.toDestination(),
            savedState = savedState
        )
    }
}

/**
 * Extension function to save navigation state.
 * Now uses KotlinxNavigationStateSerializer by default for better type safety.
 */
fun BackStack.saveState(
    serializer: NavigationStateSerializer = KotlinxNavigationStateSerializer()
): String {
    return serializer.serializeBackStack(stack.value)
}

/**
 * Extension function to restore navigation state.
 * Now uses KotlinxNavigationStateSerializer by default for better type safety.
 */
fun MutableBackStack.restoreState(
    savedState: String,
    serializer: NavigationStateSerializer = KotlinxNavigationStateSerializer()
) {
    val entries = serializer.deserializeBackStack(savedState)
    if (entries.isNotEmpty()) {
        replaceAll(entries.map { it.destination })
    }
}
