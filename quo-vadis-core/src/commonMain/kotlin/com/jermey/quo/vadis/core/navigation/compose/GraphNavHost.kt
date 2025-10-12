package com.jermey.navplayground.navigation.compose

import androidx.compose.animation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import com.jermey.navplayground.navigation.core.*

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
