package com.jermey.quo.vadis.core.navigation.serialization

import com.jermey.quo.vadis.core.navigation.core.Destination
import com.jermey.quo.vadis.core.navigation.core.NavNode
import com.jermey.quo.vadis.core.navigation.core.PaneNode
import com.jermey.quo.vadis.core.navigation.core.ScreenNode
import com.jermey.quo.vadis.core.navigation.core.StackNode
import com.jermey.quo.vadis.core.navigation.core.TabNode
import com.jermey.quo.vadis.core.navigation.core.navNodeSerializersModule
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.plus
import kotlinx.serialization.modules.polymorphic

/**
 * Pre-configured Json instance for NavNode serialization.
 *
 * Features:
 * - Polymorphic support for NavNode hierarchy via class discriminator
 * - Lenient parsing for forward compatibility (ignores unknown keys)
 * - Encodes default values for complete state representation
 * - Pretty printing disabled for efficiency
 *
 * ## Usage
 *
 * ```kotlin
 * val serialized = navNodeJson.encodeToString(NavNode.serializer(), rootNode)
 * val restored = navNodeJson.decodeFromString<NavNode>(serialized)
 * ```
 *
 * @see NavNodeSerializer for convenience wrapper functions
 */
val navNodeJson: Json = Json {
    serializersModule = navNodeSerializersModule
    classDiscriminator = "_type"
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
    prettyPrint = false
}

/**
 * Serializer utilities for NavNode trees.
 *
 * Provides convenient methods for serializing and deserializing NavNode trees
 * to/from JSON strings. Uses kotlinx.serialization with polymorphic support
 * to handle the sealed class hierarchy.
 *
 * ## Basic Usage
 *
 * ```kotlin
 * // Serialize a NavNode tree
 * val json = NavNodeSerializer.toJson(rootNode)
 *
 * // Deserialize with error handling
 * val restored = NavNodeSerializer.fromJsonOrNull(json)
 *     ?: error("Failed to restore navigation state")
 * ```
 *
 * ## Custom Destination Serializers
 *
 * If your Destination implementations require custom serialization,
 * create a Json instance with additional serializers:
 *
 * ```kotlin
 * val customJson = NavNodeSerializer.createJson(
 *     SerializersModule {
 *         polymorphic(Destination::class) {
 *             subclass(MyDestination::class)
 *         }
 *     }
 * )
 * val json = customJson.encodeToString(NavNode.serializer(), rootNode)
 * ```
 *
 * @see navNodeJson for the pre-configured Json instance
 * @see StateRestoration for platform-specific persistence
 */
object NavNodeSerializer {

    /**
     * Serialize a NavNode tree to JSON string.
     *
     * @param node The root node of the navigation tree to serialize
     * @return JSON string representation of the tree
     * @throws kotlinx.serialization.SerializationException if serialization fails
     */
    fun toJson(node: NavNode): String {
        return navNodeJson.encodeToString(NavNode.serializer(), node)
    }

    /**
     * Deserialize a NavNode tree from JSON string.
     *
     * @param json The JSON string to deserialize
     * @return The deserialized NavNode tree
     * @throws kotlinx.serialization.SerializationException if deserialization fails
     * @throws IllegalArgumentException if the JSON structure is invalid
     */
    fun fromJson(json: String): NavNode {
        return navNodeJson.decodeFromString(NavNode.serializer(), json)
    }

    /**
     * Safely deserialize a NavNode tree, returning null on failure.
     *
     * Use this method when restoring state where failure is expected
     * (e.g., schema migration, corrupted state, first launch).
     *
     * @param json The JSON string to deserialize, or null
     * @return The deserialized NavNode, or null if parsing fails or input is null/blank
     */
    fun fromJsonOrNull(json: String?): NavNode? {
        if (json.isNullOrBlank()) return null
        return try {
            fromJson(json)
        } catch (e: Exception) {
            // Log for debugging in development
            println("NavNodeSerializer: Failed to deserialize JSON - ${e.message}")
            null
        }
    }

    /**
     * Create a Json instance with custom Destination serializers.
     *
     * Use this when your Destination implementations have custom serialization
     * requirements beyond the default polymorphic handling.
     *
     * ## Example
     *
     * ```kotlin
     * val myModule = SerializersModule {
     *     polymorphic(Destination::class) {
     *         subclass(HomeDestination::class)
     *         subclass(ProfileDestination::class)
     *     }
     * }
     *
     * val json = NavNodeSerializer.createJson(myModule)
     * val serialized = json.encodeToString(NavNode.serializer(), rootNode)
     * ```
     *
     * @param additionalModule SerializersModule with custom Destination serializers
     * @return Configured Json instance with combined serializers modules
     */
    fun createJson(additionalModule: SerializersModule): Json {
        return Json {
            serializersModule = navNodeSerializersModule + additionalModule
            classDiscriminator = "_type"
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
            prettyPrint = false
        }
    }

    /**
     * Create a Json instance with pretty printing enabled for debugging.
     *
     * @return Configured Json instance with pretty printing
     */
    fun createPrettyJson(): Json {
        return Json {
            serializersModule = navNodeSerializersModule
            classDiscriminator = "_type"
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
            prettyPrint = true
        }
    }
}

/**
 * Registry for custom Destination serializers.
 *
 * Apps using custom Destination implementations should register their
 * serializers with this registry before serialization occurs:
 *
 * ```kotlin
 * // In app initialization
 * DestinationSerializerRegistry.register(
 *     SerializersModule {
 *         polymorphic(Destination::class) {
 *             subclass(AppDestination.Home::class)
 *             subclass(AppDestination.Profile::class)
 *             subclass(AppDestination.Settings::class)
 *         }
 *     }
 * )
 *
 * // Then use the combined module
 * val json = NavNodeSerializer.createJson(
 *     DestinationSerializerRegistry.combinedModule
 * )
 * ```
 *
 * @see NavNodeSerializer.createJson
 */
object DestinationSerializerRegistry {
    private var customModules: MutableList<SerializersModule> = mutableListOf()

    /**
     * The combined serializers module including all registered custom modules.
     */
    val combinedModule: SerializersModule
        get() = customModules.fold(navNodeSerializersModule) { acc, module -> acc + module }

    /**
     * Register a custom serializers module for Destination types.
     *
     * @param module SerializersModule containing polymorphic Destination serializers
     */
    fun register(module: SerializersModule) {
        customModules.add(module)
    }

    /**
     * Clear all registered custom modules.
     * Primarily for testing purposes.
     */
    fun clear() {
        customModules.clear()
    }
}
