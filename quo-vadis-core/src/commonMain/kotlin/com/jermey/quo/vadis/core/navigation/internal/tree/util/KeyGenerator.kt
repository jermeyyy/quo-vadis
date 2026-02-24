package com.jermey.quo.vadis.core.navigation.internal.tree.util

import com.jermey.quo.vadis.core.navigation.node.NodeKey
import kotlin.random.Random

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
         * Default key generator using random Long values.
         *
         * Generates compact base-36 keys from random Longs for efficient
         * key generation while maintaining uniqueness (64 bits of entropy).
         */
        val Default: KeyGenerator = KeyGenerator { NodeKey(Random.nextLong().toULong().toString(36)) }
    }
}
