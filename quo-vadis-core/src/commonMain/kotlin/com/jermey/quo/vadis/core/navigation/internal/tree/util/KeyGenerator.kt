package com.jermey.quo.vadis.core.navigation.internal.tree.util

import com.jermey.quo.vadis.core.navigation.node.NodeKey
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Abstraction for generating unique node keys.
 *
 * This functional interface allows for custom key generation strategies,
 * while providing a sensible default using UUIDs.
 *
 * ## Usage
 *
 * ```kotlin
 * // Use default UUID-based generator
 * val key = KeyGenerator.Default.generate()
 *
 * // Custom generator
 * val counter = AtomicInteger(0)
 * val customGenerator = KeyGenerator { NodeKey("node-${counter.incrementAndGet()}") }
 * ```
 */
fun interface KeyGenerator {
    /**
     * Generates a unique key for a navigation node.
     *
     * @return A unique [NodeKey]
     */
    fun generate(): NodeKey

    companion object {
        /**
         * Default key generator using UUID.
         *
         * Generates 8-character keys from random UUIDs for compact representation
         * while maintaining uniqueness.
         */
        @OptIn(ExperimentalUuidApi::class)
        val Default: KeyGenerator = KeyGenerator { NodeKey(Uuid.random().toString().take(8)) }
    }
}
