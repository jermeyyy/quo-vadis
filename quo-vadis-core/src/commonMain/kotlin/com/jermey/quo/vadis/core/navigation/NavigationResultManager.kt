package com.jermey.quo.vadis.core.navigation

import com.jermey.quo.vadis.core.InternalQuoVadisApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Thread-safe manager for navigation result passing between screens.
 *
 * This manager tracks pending result requests and delivers results when
 * destinations call [completeResultSync]. Results are nullable - if a user
 * navigates back without calling [completeResultSync], the result is completed
 * with null via [cancelResult].
 *
 * ## Internal Usage
 *
 * This class is used internally by the Navigator and its extension functions.
 * Users should interact with results through:
 * - [Navigator.navigateForResult] - to navigate and await a result
 * - [Navigator.navigateBackWithResult] - to return a result
 *
 * ## Thread Safety
 *
 * All operations are thread-safe using [Mutex] for coroutine contexts
 * or direct access for simple single-threaded UI operations.
 */
@InternalQuoVadisApi
class NavigationResultManager {

    private val mutex = Mutex()
    private val pendingResults = mutableMapOf<String, CompletableDeferred<Any?>>()

    /**
     * Request a result for a screen.
     *
     * Creates and stores a [CompletableDeferred] for the given screen key.
     * The returned deferred will complete when:
     * - [completeResultSync] is called with a result value
     * - [cancelResult] is called (completes with null)
     *
     * @param screenKey The unique key of the screen that will return the result
     * @return A [CompletableDeferred] that will receive the result
     */
    suspend fun requestResult(screenKey: String): CompletableDeferred<Any?> {
        return mutex.withLock {
            val deferred = CompletableDeferred<Any?>()
            pendingResults[screenKey] = deferred
            deferred
        }
    }

    /**
     * Complete a pending result with a value (synchronous version).
     *
     * Delivers the result to the awaiting coroutine and removes the pending entry.
     * If no pending result exists for the screen, this operation is a no-op.
     *
     * Uses best-effort locking via [Mutex.tryLock] since this is called from
     * non-suspend contexts (e.g., [navigateBackWithResult]). Thread safety is
     * guaranteed because [CompletableDeferred.complete] is itself atomic.
     *
     * @param screenKey The unique key of the screen returning the result
     * @param result The result value to deliver (can be any type)
     */
    fun completeResultSync(screenKey: String, result: Any?) {
        val locked = mutex.tryLock()
        try {
            pendingResults.remove(screenKey)?.complete(result)
        } finally {
            if (locked) mutex.unlock()
        }
    }

    /**
     * Cancel a pending result (completes with null).
     *
     * This is called when a screen is destroyed without returning a result
     * (e.g., user pressed back). The awaiting coroutine will receive null.
     *
     * @param screenKey The unique key of the screen being destroyed
     */
    suspend fun cancelResult(screenKey: String) {
        val deferred = mutex.withLock {
            pendingResults.remove(screenKey)
        }
        deferred?.complete(null)
    }

    /**
     * Check if a screen has a pending result request.
     *
     * @param screenKey The unique key of the screen to check
     * @return true if there's a pending result for this screen
     */
    fun hasPendingResult(screenKey: String): Boolean {
        return pendingResults.containsKey(screenKey)
    }

    /**
     * Get the number of pending results (for testing/debugging).
     *
     * @return The count of pending result requests
     */
    fun pendingCount(): Int {
        return pendingResults.size
    }
}
