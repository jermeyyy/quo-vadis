package com.jermey.quo.vadis.core.navigation.result

/**
 * Marker interface for destinations that can return a result.
 *
 * Combined with [Destination] to create type-safe result-returning destinations.
 * Use with [navigateForResult] extension function to navigate to a destination
 * and await its result.
 *
 * ## Usage
 *
 * ```kotlin
 * @Destination
 * data class ItemPickerDestination(
 *     val items: List<String>
 * ) : NavDestination, ReturnsResult<PickedItem>
 *
 * data class PickedItem(val id: String, val name: String)
 *
 * // In calling code:
 * val result: PickedItem? = navigator.navigateForResult(
 *     ItemPickerDestination(listOf("A", "B", "C"))
 * )
 * ```
 *
 * @param R The type of result this destination returns. Must be non-null.
 *          The actual return type from [navigateForResult] is `R?` - null indicates
 *          the user navigated back without providing a result.
 */
interface ReturnsResult<R : Any>
