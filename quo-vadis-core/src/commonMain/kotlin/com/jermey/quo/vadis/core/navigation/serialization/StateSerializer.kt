package com.jermey.quo.vadis.core.navigation.serialization

import com.jermey.quo.vadis.core.navigation.core.BackStackEntry
import com.jermey.quo.vadis.core.navigation.core.Destination
import com.jermey.quo.vadis.core.navigation.core.BackStack
import com.jermey.quo.vadis.core.navigation.core.MutableBackStack

/**
 * State restoration support for navigation.
 * Allows saving and restoring backstack state across process death.
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
 * Simple JSON-like serializer for navigation state.
 * For production, consider using kotlinx.serialization.
 */
class SimpleNavigationStateSerializer : NavigationStateSerializer {

    override fun serializeBackStack(entries: List<BackStackEntry>): String {
        return entries.joinToString("|") { entry ->
            serializeEntry(entry)
        }
    }

    override fun deserializeBackStack(serialized: String): List<BackStackEntry> {
        if (serialized.isBlank()) return emptyList()

        return serialized.split("|").mapNotNull { entryStr ->
            deserializeEntry(entryStr)
        }
    }

    override fun serializeDestination(destination: Destination): String {
        val args = destination.arguments.entries.joinToString(",") { (key, value) ->
            "$key=$value"
        }
        return "${destination.route}?$args"
    }

    override fun deserializeDestination(serialized: String): Destination? {
        val parts = serialized.split("?")
        val route = parts[0]
        val args = if (parts.size > 1) {
            parts[1].split(",").mapNotNull { pair ->
                val keyValue = pair.split("=")
                if (keyValue.size == 2) {
                    keyValue[0] to keyValue[1]
                } else null
            }.toMap()
        } else {
            emptyMap()
        }

        return object : Destination {
            override val route = route
            override val arguments = args
        }
    }

    private fun serializeEntry(entry: BackStackEntry): String {
        return "${entry.id}:${serializeDestination(entry.destination)}"
    }

    private fun deserializeEntry(serialized: String): BackStackEntry? {
        val parts = serialized.split(":", limit = 2)
        if (parts.size != 2) return null

        val destination = deserializeDestination(parts[1]) ?: return null

        return BackStackEntry(
            id = parts[0],
            destination = destination
        )
    }
}

/**
 * Extension function to save navigation state.
 */
fun BackStack.saveState(
    serializer: NavigationStateSerializer = SimpleNavigationStateSerializer()
): String {
    return serializer.serializeBackStack(stack.value)
}

/**
 * Extension function to restore navigation state.
 */
fun MutableBackStack.restoreState(
    savedState: String,
    serializer: NavigationStateSerializer = SimpleNavigationStateSerializer()
) {
    val entries = serializer.deserializeBackStack(savedState)
    if (entries.isNotEmpty()) {
        replaceAll(entries.map { it.destination })
    }
}

