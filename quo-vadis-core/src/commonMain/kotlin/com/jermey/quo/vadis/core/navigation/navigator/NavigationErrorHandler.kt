package com.jermey.quo.vadis.core.navigation.navigator

import com.jermey.quo.vadis.core.navigation.destination.NavDestination

/**
 * Defines a strategy for handling navigation errors.
 *
 * Navigation operations can fail for various reasons (invalid state, missing registries,
 * misconfigured destinations). This interface allows the error handling strategy to be
 * configured rather than hardcoded.
 *
 * ## Built-in Strategies
 *
 * - [LogAndRecover]: Logs the error and attempts recovery (default behavior)
 * - [ThrowOnError]: Rethrows all navigation errors (useful for testing/debugging)
 */
fun interface NavigationErrorHandler {
    /**
     * Called when a navigation operation fails.
     *
     * @param error The error that occurred
     * @param destination The destination being navigated to (if applicable)
     * @param context A human-readable description of what operation failed
     */
    fun onNavigationError(error: Throwable, destination: NavDestination?, context: String)

    companion object {
        /**
         * Default handler that logs errors and silently recovers.
         */
        val LogAndRecover: NavigationErrorHandler = NavigationErrorHandler { error, destination, context ->
            println("Navigation error [$context]: ${error.message} (destination: ${destination?.let { it::class.simpleName }})")
        }

        /**
         * Handler that rethrows all navigation errors. Useful for testing.
         */
        val ThrowOnError: NavigationErrorHandler = NavigationErrorHandler { error, _, _ ->
            throw error
        }
    }
}
