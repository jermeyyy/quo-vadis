package com.jermey.quo.vadis.core.navigation.core

/**
 * Navigate to a destination and suspend until it returns a result.
 *
 * The navigation is performed immediately. The calling coroutine
 * suspends until the destination calls [navigateBackWithResult].
 *
 * ## Usage
 *
 * ```kotlin
 * // In the calling screen's container:
 * coroutineScope.launch {
 *     val result: PickedItem? = navigator.navigateForResult(
 *         ItemPickerDestination(availableItems)
 *     )
 *     if (result != null) {
 *         // User selected an item
 *         handleSelection(result)
 *     } else {
 *         // User cancelled (pressed back)
 *     }
 * }
 * ```
 *
 * ## Type Safety
 *
 * The destination must implement both [Destination] and [ReturnsResult].
 * The result type `R` is enforced at compile time.
 *
 * @param R The expected result type (enforced by [ReturnsResult])
 * @param D The destination type (must implement both [Destination] and [ReturnsResult])
 * @param destination The destination to navigate to
 * @return The result value, or null if navigation was cancelled
 *         (user pressed back without calling [navigateBackWithResult])
 */
@Suppress("UNCHECKED_CAST")
suspend fun <R : Any, D> Navigator.navigateForResult(
    destination: D
): R? where D : NavDestination, D : ReturnsResult<R> {
    println("navigateForResult: navigating to $destination")
    // Navigate to the destination
    navigate(destination)

    // Get the new screen's key (the destination we just pushed)
    val targetScreenKey = state.value.activeLeaf()?.key
        ?: throw IllegalStateException("No active screen after navigation")

    println("navigateForResult: target screen key = $targetScreenKey")

    // Request a result from the result manager
    val deferred = resultManager.requestResult(targetScreenKey)

    println("navigateForResult: awaiting result...")
    // Await and return the result (cast is safe due to generic constraints)
    val result = deferred.await() as R?
    println("navigateForResult: received result = $result")
    return result
}

/**
 * Navigate back and pass a result to the previous destination.
 *
 * Must be called from a destination that was navigated to via [navigateForResult].
 * The result will be delivered to the suspended coroutine in the calling screen.
 *
 * ## Usage
 *
 * ```kotlin
 * // In the result-returning screen's container:
 * fun onItemSelected(item: PickedItem) {
 *     navigator.navigateBackWithResult(item)
 * }
 * ```
 *
 * ## Behavior
 *
 * 1. Gets the current screen's key (the one returning the result)
 * 2. Completes the pending result with the provided value
 * 3. Navigates back to the previous screen
 *
 * If no result request is pending for the current screen, the result is
 * silently discarded and regular back navigation occurs.
 *
 * @param R The result type
 * @param result The result to pass back
 */
fun <R : Any> Navigator.navigateBackWithResult(result: R) {
    // Get the current screen's key (the one returning the result)
    val currentScreenKey = state.value.activeLeaf()?.key
        ?: throw IllegalStateException("No active screen to return result from")

    println("navigateBackWithResult: current screen key = $currentScreenKey, result = $result")
    println("navigateBackWithResult: hasPendingResult = ${resultManager.hasPendingResult(currentScreenKey)}")

    // Navigate back
    navigateBack()

    println("navigateBackWithResult: completing result...")
    // Complete the result synchronously
    resultManager.completeResultSync(currentScreenKey, result)
    println("navigateBackWithResult: result completed")
}
