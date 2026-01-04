package com.jermey.quo.vadis.core.navigation.internal

import com.jermey.quo.vadis.core.InternalQuoVadisApi

/**
 * Utility object for generating unique navigation node keys.
 *
 * **Internal API** - This is an internal implementation detail of Quo Vadis.
 * The navigation key generator is managed internally by the navigation system.
 */
@InternalQuoVadisApi
object NavKeyGenerator {
    private var counter = 0L

    /**
     * Generates a unique key with an optional debug label.
     *
     * @param debugLabel Optional label for debugging (e.g., "profile", "home")
     * @return A unique key string
     */
    fun generate(debugLabel: String? = null): String {
        val id = counter++
        return debugLabel?.let { "$it-$id" } ?: "node-$id"
    }

    /**
     * Resets the counter (useful for testing).
     */
    fun reset() {
        counter = 0L
    }
}
