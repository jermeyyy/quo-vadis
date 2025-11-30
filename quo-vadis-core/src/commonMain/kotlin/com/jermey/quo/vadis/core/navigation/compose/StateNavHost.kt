package com.jermey.quo.vadis.core.navigation.compose

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import com.jermey.quo.vadis.core.navigation.core.BackStackEntry
import com.jermey.quo.vadis.core.navigation.core.Destination
import com.jermey.quo.vadis.core.navigation.core.StateBackStack

/**
 * Default transition specification for StateNavHost using simple fade animations.
 *
 * This provides a smooth, unobtrusive transition between destinations.
 */
private val DefaultTransitionSpec: AnimatedContentTransitionScope<BackStackEntry>.() -> ContentTransform = {
    fadeIn() togetherWith fadeOut()
}

/**
 * A state-driven navigation host that renders destinations based on the current [StateBackStack] state.
 *
 * Unlike the graph-based [NavHost], this composable provides a simpler, more declarative approach
 * to navigation where the UI is directly driven by the backstack state. This follows the
 * Navigation 3-style pattern where developers have direct control over the backstack.
 *
 * Key features:
 * - **State-driven rendering**: Automatically observes [StateBackStack.current] and renders
 *   the appropriate destination
 * - **Animated transitions**: Uses [AnimatedContent] for smooth transitions between destinations
 * - **Customizable animations**: Supports custom transition specifications via [transitionSpec]
 * - **State preservation**: Uses [SaveableStateHolder] to preserve composable state across
 *   configuration changes
 * - **Proper animation targeting**: Uses entry ID as key for correct animation behavior
 *
 * Example usage:
 * ```kotlin
 * val backStack = rememberStateBackStack(HomeDestination)
 *
 * StateNavHost(
 *     stateBackStack = backStack,
 *     modifier = Modifier.fillMaxSize()
 * ) { destination ->
 *     when (destination) {
 *         is HomeDestination -> HomeScreen(
 *             onNavigateToDetail = { backStack.push(DetailDestination(it)) }
 *         )
 *         is DetailDestination -> DetailScreen(
 *             id = destination.id,
 *             onBack = { backStack.pop() }
 *         )
 *     }
 * }
 * ```
 *
 * @param stateBackStack The [StateBackStack] that drives the navigation state.
 *   The host observes [StateBackStack.current] and renders the corresponding destination.
 * @param modifier Optional [Modifier] to be applied to the host container.
 * @param transitionSpec A lambda that defines how transitions should animate.
 *   Receives the target [BackStackEntry] and returns a [ContentTransform].
 *   Defaults to a simple fade in/out animation.
 * @param entryProvider A composable lambda that renders the UI for a given [Destination].
 *   Called within an [AnimatedContentScope] to enable animated visibility modifiers.
 *
 * @see StateBackStack for the underlying navigation state container
 * @see rememberStateBackStack for creating a remembered StateBackStack
 * @see rememberStateNavigator for a higher-level navigation API
 */
@Composable
fun StateNavHost(
    stateBackStack: StateBackStack,
    modifier: Modifier = Modifier,
    transitionSpec: AnimatedContentTransitionScope<BackStackEntry>.() -> ContentTransform = DefaultTransitionSpec,
    entryProvider: @Composable AnimatedContentScope.(Destination) -> Unit
) {
    val saveableStateHolder = rememberSaveableStateHolder()
    val currentEntry = stateBackStack.current

    if (currentEntry != null) {
        StateNavHostContent(
            currentEntry = currentEntry,
            saveableStateHolder = saveableStateHolder,
            modifier = modifier,
            transitionSpec = transitionSpec,
            entryProvider = entryProvider
        )
    }
}

/**
 * Internal content composable that handles the animated rendering of destinations.
 *
 * This is separated from [StateNavHost] to ensure proper recomposition behavior
 * when the current entry changes.
 *
 * @param currentEntry The current [BackStackEntry] to display
 * @param saveableStateHolder Holder for preserving composable state
 * @param modifier Modifier for the container
 * @param transitionSpec Transition animation specification
 * @param entryProvider Content provider for destinations
 */
@Composable
private fun StateNavHostContent(
    currentEntry: BackStackEntry,
    saveableStateHolder: SaveableStateHolder,
    modifier: Modifier,
    transitionSpec: AnimatedContentTransitionScope<BackStackEntry>.() -> ContentTransform,
    entryProvider: @Composable AnimatedContentScope.(Destination) -> Unit
) {
    AnimatedContent(
        targetState = currentEntry,
        modifier = modifier,
        transitionSpec = transitionSpec,
        contentKey = { it.id }
    ) { entry ->
        // Use key to ensure proper state management for each entry
        key(entry.id) {
            saveableStateHolder.SaveableStateProvider(entry.id) {
                entryProvider(entry.destination)
            }
        }
    }
}
