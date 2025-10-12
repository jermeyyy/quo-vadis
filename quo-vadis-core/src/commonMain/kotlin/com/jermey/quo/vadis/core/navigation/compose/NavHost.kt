package com.jermey.navplayground.navigation.compose

import androidx.compose.animation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import com.jermey.navplayground.navigation.core.*

/**
 * Main navigation host composable that renders the current destination.
 * Supports animations and manages the navigation state.
 */
@Composable
fun NavHost(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    defaultTransition: NavigationTransition = NavigationTransitions.Fade
) {
    val backStackEntry by navigator.backStack.current.collectAsState()
    val previousEntry = remember { mutableStateOf<BackStackEntry?>(null) }

    // Track navigation direction for proper animations
    val isNavigatingForward = remember { mutableStateOf(true) }

    LaunchedEffect(backStackEntry) {
        val current = backStackEntry
        val previous = previousEntry.value

        if (current != null && previous != null) {
            isNavigatingForward.value = current != previous
        }

        previousEntry.value = current
    }

    Box(modifier = modifier) {
        backStackEntry?.let { entry ->
            AnimatedContent(
                targetState = entry,
                transitionSpec = {
                    if (isNavigatingForward.value) {
                        defaultTransition.enter togetherWith defaultTransition.exit
                    } else {
                        defaultTransition.popEnter togetherWith defaultTransition.popExit
                    }
                },
                label = "navigation_animation"
            ) { currentEntry ->
                // Find and render the appropriate content
                RenderDestination(
                    destination = currentEntry.destination,
                    navigator = navigator
                )
            }
        }
    }
}

/**
 * Navigation host that works with a specific navigation graph.
 * Uses composable caching to keep screens alive for smooth transitions and predictive back gestures.
 */
@Composable
fun GraphNavHost(
    graph: NavigationGraph,
    navigator: Navigator,
    modifier: Modifier = Modifier,
    defaultTransition: NavigationTransition = NavigationTransitions.Fade,
    enableComposableCache: Boolean = true,
    maxCacheSize: Int = 3
) {
    val backStackEntry by navigator.backStack.current.collectAsState()
    val saveableStateHolder = rememberSaveableStateHolder()
    val composableCache = remember { ComposableCache(maxCacheSize) }

    Box(modifier = modifier) {
        backStackEntry?.let { entry ->
            val destConfig = graph.destinations.find {
                it.destination.route == entry.destination.route
            }

            destConfig?.let { config ->
                AnimatedContent(
                    targetState = entry,
                    transitionSpec = {
                        defaultTransition.enter togetherWith defaultTransition.exit
                    },
                    label = "graph_navigation"
                ) { currentEntry ->
                    if (enableComposableCache) {
                        // Use cached composable for better performance
                        key(currentEntry.id) {
                            composableCache.Entry(
                                entry = currentEntry,
                                saveableStateHolder = saveableStateHolder
                            ) { stackEntry ->
                                config.content(stackEntry.destination, navigator)
                            }
                        }
                    } else {
                        // Direct rendering without cache
                        config.content(currentEntry.destination, navigator)
                    }
                }
            }
        }
    }
}

/**
 * Renders the content for a given destination.
 */
@Composable
private fun RenderDestination(
    destination: Destination,
    navigator: Navigator
) {
    // This will look up the destination in registered graphs
    // For now, provide a placeholder
    Box {
        // Content will be provided by the registered navigation graphs
    }
}

/**
 * Remember a Navigator instance with DI support.
 */
@Composable
fun rememberNavigator(
    deepLinkHandler: DeepLinkHandler = DefaultDeepLinkHandler()
): Navigator {
    return remember {
        DefaultNavigator(deepLinkHandler)
    }
}
