package com.jermey.navplayground.navigation

/**
 * Base interface for feature entry points that return a typed result.
 *
 * Feature modules expose their entry point as a [FeatureEntry] implementation,
 * hiding concrete destination types from consuming modules. Consumers inject
 * the interface via DI and call [start] to navigate into the feature and
 * await the result.
 *
 * @param R The result type returned by this feature entry. Use [Unit] for
 *          fire-and-forget navigation (no result expected).
 */
interface FeatureEntry<R : Any> {
    /**
     * Start navigating into this feature and await the result.
     *
     * @return The result from the feature, or null if the user navigated back
     *         without providing a result.
     */
    suspend fun start(): R?
}
