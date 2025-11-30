package com.jermey.quo.vadis.core.navigation.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.jermey.quo.vadis.core.navigation.core.Destination
import com.jermey.quo.vadis.core.navigation.core.StateBackStack
import com.jermey.quo.vadis.core.navigation.core.StateNavigator

/**
 * Creates and remembers a [StateBackStack] instance that survives recomposition.
 *
 * This function is the primary way to create a [StateBackStack] in a Compose context.
 * The backstack will be preserved across recompositions, maintaining the navigation
 * state as long as the composable remains in the composition.
 *
 * If an [initialDestination] is provided, it will be pushed onto the backstack
 * during creation, establishing the initial navigation state.
 *
 * Example usage:
 * ```kotlin
 * @Composable
 * fun App() {
 *     val backStack = rememberStateBackStack(HomeDestination)
 *
 *     StateNavHost(stateBackStack = backStack) { destination ->
 *         when (destination) {
 *             is HomeDestination -> HomeScreen(
 *                 onNavigate = { backStack.push(it) }
 *             )
 *             // ... other destinations
 *         }
 *     }
 * }
 * ```
 *
 * Note: The backstack state will be lost on configuration changes unless
 * additional state restoration is implemented. For state preservation across
 * process death, consider using saveable state mechanisms.
 *
 * @param initialDestination Optional initial destination to push onto the backstack.
 *   If `null`, the backstack will be created empty.
 * @return A [StateBackStack] instance that is remembered across recompositions.
 *
 * @see StateBackStack for the underlying backstack implementation
 * @see rememberStateNavigator for a higher-level navigation API
 * @see StateNavHost for rendering destinations from the backstack
 */
@Composable
fun rememberStateBackStack(initialDestination: Destination? = null): StateBackStack {
    return remember {
        StateBackStack().apply {
            if (initialDestination != null) {
                push(initialDestination)
            }
        }
    }
}

/**
 * Creates and remembers a [StateNavigator] instance that survives recomposition.
 *
 * This function provides a convenient way to create a [StateNavigator] with
 * high-level navigation methods in a Compose context. The navigator will be
 * preserved across recompositions.
 *
 * If an [initialDestination] is provided, it will be set as the initial
 * destination when the navigator is created.
 *
 * The [StateNavigator] provides a richer API compared to [StateBackStack],
 * including methods like:
 * - [StateNavigator.navigate] - Push a new destination
 * - [StateNavigator.navigateBack] - Pop the current destination
 * - [StateNavigator.navigateAndReplace] - Replace the current destination
 * - [StateNavigator.navigateAndClearAll] - Clear stack and navigate
 *
 * Example usage:
 * ```kotlin
 * @Composable
 * fun App() {
 *     val navigator = rememberStateNavigator(HomeDestination)
 *
 *     StateNavHost(stateBackStack = navigator.getBackStack()) { destination ->
 *         when (destination) {
 *             is HomeDestination -> HomeScreen(
 *                 onNavigateToDetail = { id ->
 *                     navigator.navigate(DetailDestination(id))
 *                 }
 *             )
 *             is DetailDestination -> DetailScreen(
 *                 id = destination.id,
 *                 onBack = { navigator.navigateBack() }
 *             )
 *         }
 *     }
 * }
 * ```
 *
 * Note: Like [rememberStateBackStack], the navigator state will be lost on
 * configuration changes unless additional state restoration is implemented.
 *
 * @param initialDestination Optional initial destination to navigate to.
 *   If `null`, the navigator will be created with an empty backstack.
 * @return A [StateNavigator] instance that is remembered across recompositions.
 *
 * @see StateNavigator for the underlying navigator implementation
 * @see rememberStateBackStack for direct backstack access without navigator wrapper
 * @see StateNavHost for rendering destinations from the navigator's backstack
 */
@Composable
fun rememberStateNavigator(initialDestination: Destination? = null): StateNavigator {
    return remember {
        StateNavigator().apply {
            if (initialDestination != null) {
                navigate(initialDestination)
            }
        }
    }
}
