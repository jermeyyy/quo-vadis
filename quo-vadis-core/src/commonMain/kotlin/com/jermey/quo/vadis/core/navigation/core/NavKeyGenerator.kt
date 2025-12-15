package com.jermey.quo.vadis.core.navigation.core

/**
 * Utility object for generating unique navigation node keys.
 */
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