package com.jermey.quo.vadis.core.navigation.compose

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.PredictiveBackHandler
import androidx.compose.ui.graphics.graphicsLayer
import com.jermey.quo.vadis.core.navigation.core.BackStackEntry
import com.jermey.quo.vadis.core.navigation.core.DefaultNavigator
import com.jermey.quo.vadis.core.navigation.core.NavigationGraph
import com.jermey.quo.vadis.core.navigation.core.Navigator
import com.jermey.quo.vadis.core.navigation.core.DeepLinkHandler
import com.jermey.quo.vadis.core.navigation.core.DefaultDeepLinkHandler
import com.jermey.quo.vadis.core.navigation.core.NavigationTransition
import com.jermey.quo.vadis.core.navigation.core.NavigationTransitions
import kotlinx.coroutines.launch

// Animation constants
private const val PREDICTIVE_BACK_SCALE_FACTOR = 0.1f
private const val MAX_GESTURE_PROGRESS = 0.25f // Maximum drag distance (25% of screen width)
private const val FRAME_DELAY_MS = 16L // One frame at 60fps

/**
 * Unified navigation host with support for:
 * - Animated forward/back navigation with correct directional transitions
 * - Predictive back gesture animations
 * - Composable caching for smooth transitions
 * - Shared element transitions (automatically enabled per-destination)
 *
 * This is the primary NavHost for all navigation scenarios.
 * Shared element transitions are automatically available to destinations that use `destinationWithScopes`.
 *
 * @param graph The navigation graph containing all destinations
 * @param navigator The navigator instance for controlling navigation
 * @param modifier Modifier to be applied to the navigation host
 * @param defaultTransition Default transition to use when destination doesn't specify one
 * @param enableComposableCache Whether to enable composable caching for performance
 * @param enablePredictiveBack Whether to enable predictive back gesture animations
 * @param maxCacheSize Maximum number of composables to keep in cache
 */
@OptIn(ExperimentalComposeUiApi::class, androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@Composable
@Suppress("LongMethod", "CyclomaticComplexMethod")
fun GraphNavHost(
    graph: NavigationGraph,
    navigator: Navigator,
    modifier: Modifier = Modifier,
    defaultTransition: NavigationTransition = NavigationTransitions.Fade,
    enableComposableCache: Boolean = true,
    enablePredictiveBack: Boolean = true,
    maxCacheSize: Int = 3
) {
    // Always wrap in SharedTransitionLayout - it's lightweight and enables per-destination shared elements
    SharedTransitionLayout(modifier = modifier) {
        CompositionLocalProvider(
            LocalSharedTransitionScope provides this
        ) {
            GraphNavHostContent(
                graph = graph,
                navigator = navigator,
                defaultTransition = defaultTransition,
                enableComposableCache = enableComposableCache,
                enablePredictiveBack = enablePredictiveBack,
                maxCacheSize = maxCacheSize,
                modifier = Modifier // Use Modifier inside SharedTransitionLayout
            )
        }
    }
}

/**
 * Internal content of GraphNavHost, extracted to allow wrapping with SharedTransitionLayout.
 */
@OptIn(ExperimentalComposeUiApi::class, androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@Composable
@Suppress("LongMethod", "CyclomaticComplexity", "ComplexMethod")
private fun GraphNavHostContent(
    graph: NavigationGraph,
    navigator: Navigator,
    defaultTransition: NavigationTransition,
    enableComposableCache: Boolean,
    enablePredictiveBack: Boolean,
    maxCacheSize: Int,
    modifier: Modifier = Modifier
) {
    val backStackEntries by navigator.backStack.stack.collectAsState()
    val currentEntry by navigator.backStack.current.collectAsState()
    val previousEntry by navigator.backStack.previous.collectAsState()
    val canGoBack by remember { derivedStateOf { backStackEntries.size > 1 } }

    val saveableStateHolder = rememberSaveableStateHolder()
    val composableCache = remember { ComposableCache(maxCacheSize) }
    val scope = rememberCoroutineScope()

    // Animation state
    var isNavigating by remember { mutableStateOf(false) }
    var isBackNavigation by remember { mutableStateOf(false) }
    var isPredictiveGesture by remember { mutableStateOf(false) }
    var justCompletedGesture by remember { mutableStateOf(false) }
    var gestureProgress by remember { mutableFloatStateOf(0f) }
    val exitAnimProgress = remember { Animatable(0f) }

    // Track stack changes for animation direction detection
    var lastStackSize by remember { mutableIntStateOf(backStackEntries.size) }
    var lastEntryId by remember { mutableStateOf<String?>(null) }
    var lastCurrentEntry by remember { mutableStateOf<BackStackEntry?>(null) }

    // Stable rendering state during animations
    var displayedCurrent by remember { mutableStateOf<BackStackEntry?>(null) }
    var displayedPrevious by remember { mutableStateOf<BackStackEntry?>(null) }

    // Update displayed entries and animation state
    LaunchedEffect(backStackEntries.size, currentEntry?.id) {
        val stackSize = backStackEntries.size
        val currentId = currentEntry?.id
        
        // Skip processing if predictive gesture is active OR just completed
        // The gesture handler manages its own state
        if (isPredictiveGesture || justCompletedGesture) {
            // Still update tracking to stay in sync
            lastStackSize = stackSize
            lastEntryId = currentId
            lastCurrentEntry = currentEntry
            return@LaunchedEffect
        }

        when {
            // Forward navigation
            stackSize > lastStackSize -> {
                displayedCurrent = currentEntry
                displayedPrevious = previousEntry
                isBackNavigation = false
                isNavigating = true
                
                // Lock entries to prevent cache eviction during animation
                currentEntry?.let { 
                    composableCache.lockEntry(it.id)
                }
                previousEntry?.let { 
                    composableCache.lockEntry(it.id)
                }
                
                // Auto-complete animation after duration
                kotlinx.coroutines.delay(NavigationTransitions.ANIMATION_DURATION.toLong())
                isNavigating = false
                
                // Unlock entries
                currentEntry?.let { 
                    composableCache.unlockEntry(it.id)
                }
                previousEntry?.let { 
                    composableCache.unlockEntry(it.id)
                }
            }
            // Navigation change (forward or back, not predictive gesture)
            // AnimatedContent will handle the animation automatically
            stackSize != lastStackSize && !isPredictiveGesture -> {
                displayedCurrent = currentEntry
                displayedPrevious = previousEntry
                isBackNavigation = stackSize < lastStackSize
                isNavigating = false  // Let AnimatedContent handle animation
            }
            // Replace (same size, different entry)
            stackSize == lastStackSize && currentId != lastEntryId && currentId != null -> {
                displayedCurrent = currentEntry
                displayedPrevious = previousEntry
                isBackNavigation = false
                isNavigating = true
                
                // Lock entries to prevent cache eviction during animation
                currentEntry?.let { 
                    composableCache.lockEntry(it.id)
                }
                previousEntry?.let { 
                    composableCache.lockEntry(it.id)
                }
                
                // Auto-complete animation after duration
                kotlinx.coroutines.delay(NavigationTransitions.ANIMATION_DURATION.toLong())
                isNavigating = false
                
                // Unlock entries
                currentEntry?.let { 
                    composableCache.unlockEntry(it.id)
                }
                previousEntry?.let { 
                    composableCache.unlockEntry(it.id)
                }
            }
            // No animation (idle or predictive gesture completed)
            else -> {
                displayedCurrent = currentEntry
                displayedPrevious = null
            }
        }

        lastStackSize = stackSize
        lastEntryId = currentId
        lastCurrentEntry = currentEntry
    }

    // Predictive back gesture handler
    PredictiveBackHandler(enabled = enablePredictiveBack && canGoBack && !isNavigating) { backEvent ->
        // Capture entries BEFORE navigation
        val capturedCurrent = currentEntry
        val capturedPrevious = previousEntry
        
        displayedCurrent = capturedCurrent
        displayedPrevious = capturedPrevious
        isPredictiveGesture = true
        isBackNavigation = true
        
        // Set initial gesture progress immediately to start animation without delay
        gestureProgress = 0.001f  // Small non-zero value to trigger immediate rendering
        
        // Lock entries to prevent cache eviction during gesture
        capturedCurrent?.let { 
            composableCache.lockEntry(it.id)
        }
        capturedPrevious?.let { 
            composableCache.lockEntry(it.id)
        }

        try {
            backEvent.collect { event ->
                // Clamp gesture progress to maximum (25%)
                val clampedProgress = event.progress.coerceAtMost(MAX_GESTURE_PROGRESS)
                gestureProgress = clampedProgress
            }

            // Gesture completed - animate exit from current position
            scope.launch {
                exitAnimProgress.snapTo(gestureProgress)
                gestureProgress = 0f  // Clear gesture progress so animation takes over
                exitAnimProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )

                // Complete navigation - but keep isPredictiveGesture flag set
                navigator.navigateBack()
                
                // Wait a frame for stack change to be processed
                kotlinx.coroutines.delay(FRAME_DELAY_MS)
                
                // Update final state BEFORE resetting isPredictiveGesture
                displayedCurrent = currentEntry
                displayedPrevious = null
                isBackNavigation = false
                justCompletedGesture = true  // Flag to skip AnimatedContent
                
                // Reset gesture state - this will trigger LaunchedEffect but it will now see correct state
                isPredictiveGesture = false
                exitAnimProgress.snapTo(0f)
                
                // Unlock entries
                capturedCurrent?.let { 
                    composableCache.unlockEntry(it.id)
                }
                capturedPrevious?.let { 
                    composableCache.unlockEntry(it.id)
                }
                
                // Wait a frame, then clear the flag to allow normal animations again
                kotlinx.coroutines.delay(FRAME_DELAY_MS)
                justCompletedGesture = false
            }
        } catch (_: Exception) {
            // Gesture cancelled - animate back to original position
            scope.launch {
                exitAnimProgress.snapTo(gestureProgress)
                gestureProgress = 0f  // Clear gesture progress so animation takes over
                exitAnimProgress.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
                
                // Reset state - no navigation happened
                isPredictiveGesture = false
                exitAnimProgress.snapTo(0f)
                displayedPrevious = null
                
                // Unlock entries
                capturedCurrent?.let { 
                    composableCache.unlockEntry(it.id)
                }
                capturedPrevious?.let { 
                    composableCache.unlockEntry(it.id)
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Render previous screen during predictive gesture (static underneath)
        if (isPredictiveGesture && displayedPrevious != null && displayedPrevious?.id != displayedCurrent?.id) {
            ScreenContent(
                entry = displayedPrevious!!,
                graph = graph,
                navigator = navigator,
                composableCache = composableCache,
                saveableStateHolder = saveableStateHolder,
                enableCache = enableComposableCache
            )
        }
        
        // Render current screen with animations
        displayedCurrent?.let { entry ->
            when {
                // Predictive gesture active - manual transform
                isPredictiveGesture -> {
                    val progress = if (gestureProgress > 0) gestureProgress else exitAnimProgress.value
                    
                    Box(
                        modifier = Modifier.graphicsLayer {
                            translationX = size.width * progress
                            scaleX = 1f - (progress * PREDICTIVE_BACK_SCALE_FACTOR)
                            scaleY = 1f - (progress * PREDICTIVE_BACK_SCALE_FACTOR)
                        }
                    ) {
                        ScreenContent(
                            entry = entry,
                            graph = graph,
                            navigator = navigator,
                            composableCache = composableCache,
                            saveableStateHolder = saveableStateHolder,
                            enableCache = enableComposableCache
                        )
                    }
                }
                // Just completed gesture - render without animation
                justCompletedGesture -> {
                    ScreenContent(
                        entry = entry,
                        graph = graph,
                        navigator = navigator,
                        composableCache = composableCache,
                        saveableStateHolder = saveableStateHolder,
                        enableCache = enableComposableCache
                    )
                }
                // Regular navigation (forward or back) - use AnimatedContent with proper transitions
                // This enables AnimatedVisibilityScope for shared elements in BOTH directions
                else -> {
                    AnimatedContent(
                        targetState = entry,
                        transitionSpec = {
                            val transition = entry.transition ?: defaultTransition
                            // Use the transition as-is - AnimatedContent handles direction automatically
                            transition.enter togetherWith transition.exit
                        },
                        label = "navigation_animation"
                    ) { animatingEntry ->
                        CompositionLocalProvider(
                            LocalNavAnimatedVisibilityScope provides this@AnimatedContent
                        ) {
                            ScreenContent(
                                entry = animatingEntry,
                                graph = graph,
                                navigator = navigator,
                                composableCache = composableCache,
                                saveableStateHolder = saveableStateHolder,
                                enableCache = enableComposableCache
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@Composable
private fun ScreenContent(
    entry: BackStackEntry,
    graph: NavigationGraph,
    navigator: Navigator,
    composableCache: ComposableCache,
    saveableStateHolder: SaveableStateHolder,
    enableCache: Boolean
) {
    val destConfig = remember(entry.destination.route) {
        graph.destinations.find { it.destination.route == entry.destination.route }
    }

    // Get scopes from composition locals (will be null if shared elements disabled)
    val sharedTransitionScope = currentSharedTransitionScope()
    val animatedVisibilityScope = currentNavAnimatedVisibilityScope()

    destConfig?.let { config ->
        // Prefer contentWithScopes if available (for shared element support)
        val renderContent: @Composable (BackStackEntry) -> Unit = if (config.contentWithScopes != null) {
            { stackEntry ->
                config.contentWithScopes.invoke(
                    stackEntry.destination,
                    navigator,
                    sharedTransitionScope,
                    animatedVisibilityScope
                )
            }
        } else {
            { stackEntry ->
                config.content(stackEntry.destination, navigator)
            }
        }

        if (enableCache) {
            key(entry.id) {
                composableCache.Entry(
                    entry = entry,
                    saveableStateHolder = saveableStateHolder,
                    content = renderContent
                )
            }
        } else {
            renderContent(entry)
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
