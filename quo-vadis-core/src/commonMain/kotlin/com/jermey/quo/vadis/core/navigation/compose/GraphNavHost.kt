package com.jermey.quo.vadis.core.navigation.compose

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
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
import androidx.compose.runtime.MutableFloatState
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
import androidx.compose.runtime.withFrameNanos
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
private const val INITIAL_GESTURE_PROGRESS = 0.001f // Small non-zero value to trigger immediate rendering

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
@OptIn(ExperimentalComposeUiApi::class, ExperimentalSharedTransitionApi::class)
@Composable
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
        GraphNavHostContent(
            graph = graph,
            navigator = navigator,
            defaultTransition = defaultTransition,
            enableComposableCache = enableComposableCache,
            enablePredictiveBack = enablePredictiveBack,
            maxCacheSize = maxCacheSize,
            sharedTransitionScope = this,
            modifier = Modifier // Use Modifier inside SharedTransitionLayout
        )
    }
}

/**
 * Renders content with predictive gesture animation.
 */
@Composable
private fun PredictiveGestureContent(
    entry: BackStackEntry,
    gestureProgress: Float,
    exitAnimProgress: Animatable<Float, *>,
    graph: NavigationGraph,
    navigator: Navigator,
    composableCache: ComposableCache,
    saveableStateHolder: SaveableStateHolder,
    enableCache: Boolean
) {
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
            enableCache = enableCache
        )
    }
}

/**
 * Renders content with animated transitions.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun AnimatedNavigationContent(
    entry: BackStackEntry,
    isBackNavigation: Boolean,
    defaultTransition: NavigationTransition,
    sharedTransitionScope: SharedTransitionScope,
    graph: NavigationGraph,
    navigator: Navigator,
    composableCache: ComposableCache,
    saveableStateHolder: SaveableStateHolder,
    enableCache: Boolean
) {
    AnimatedContent(
        targetState = entry,
        transitionSpec = {
            // Select transition source based on navigation direction
            val transition = if (isBackNavigation) {
                // For back navigation, prefer the exiting screen's transition (initialState)
                // This ensures the leaving screen's exit animation is shown
                initialState.transition ?: targetState.transition ?: defaultTransition
            } else {
                // For forward navigation, use the entering screen's transition (targetState)
                targetState.transition ?: defaultTransition
            }
            
            // Use pop animations for back navigation, regular animations for forward
            if (isBackNavigation) {
                transition.popEnter togetherWith transition.popExit
            } else {
                transition.enter togetherWith transition.exit
            }
        },
        label = "navigation_animation"
    ) { animatingEntry ->
        val transitionScope = TransitionScope(sharedTransitionScope, this@AnimatedContent)
        
        CompositionLocalProvider(
            LocalTransitionScope provides transitionScope
        ) {
            ScreenContent(
                entry = animatingEntry,
                graph = graph,
                navigator = navigator,
                composableCache = composableCache,
                saveableStateHolder = saveableStateHolder,
                enableCache = enableCache
            )
        }
    }
}

/**
 * Renders navigation content with appropriate animations.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun NavigationContentRenderer(
    displayedCurrent: BackStackEntry?,
    displayedPrevious: BackStackEntry?,
    isPredictiveGesture: Boolean,
    justCompletedGesture: Boolean,
    isBackNavigation: Boolean,
    gestureProgress: Float,
    exitAnimProgress: Animatable<Float, *>,
    graph: NavigationGraph,
    navigator: Navigator,
    composableCache: ComposableCache,
    saveableStateHolder: SaveableStateHolder,
    enableComposableCache: Boolean,
    defaultTransition: NavigationTransition,
    sharedTransitionScope: SharedTransitionScope
) {
    // Render previous screen during predictive gesture
    if (isPredictiveGesture && displayedPrevious != null && displayedPrevious.id != displayedCurrent?.id) {
        ScreenContent(
            entry = displayedPrevious,
            graph = graph,
            navigator = navigator,
            composableCache = composableCache,
            saveableStateHolder = saveableStateHolder,
            enableCache = enableComposableCache
        )
    }
    
    // Render current screen
    displayedCurrent?.let { entry ->
        when {
            isPredictiveGesture -> {
                PredictiveGestureContent(
                    entry = entry,
                    gestureProgress = gestureProgress,
                    exitAnimProgress = exitAnimProgress,
                    graph = graph,
                    navigator = navigator,
                    composableCache = composableCache,
                    saveableStateHolder = saveableStateHolder,
                    enableCache = enableComposableCache
                )
            }
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
            else -> {
                AnimatedNavigationContent(
                    entry = entry,
                    isBackNavigation = isBackNavigation,
                    defaultTransition = defaultTransition,
                    sharedTransitionScope = sharedTransitionScope,
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

/**
 * Handles forward navigation or replacement with animation.
 */
private suspend fun handleAnimatedNavigation(
    currentEntry: BackStackEntry?,
    previousEntry: BackStackEntry?,
    composableCache: ComposableCache,
    onNavigatingChange: (Boolean) -> Unit
) {
    onNavigatingChange(true)
    
    currentEntry?.let { composableCache.lockEntry(it.id) }
    previousEntry?.let { composableCache.lockEntry(it.id) }
    
    kotlinx.coroutines.delay(NavigationTransitions.ANIMATION_DURATION.toLong())
    onNavigatingChange(false)
    
    currentEntry?.let { composableCache.unlockEntry(it.id) }
    previousEntry?.let { composableCache.unlockEntry(it.id) }
}

/**
 * Animates gesture completion and completes navigation.
 */
private suspend fun animateGestureCompletion(
    gestureProgress: MutableFloatState,
    exitAnimProgress: Animatable<Float, *>,
    navigator: Navigator,
    capturedCurrent: BackStackEntry?,
    capturedPrevious: BackStackEntry?,
    composableCache: ComposableCache,
    getCurrentEntry: () -> BackStackEntry?,
    onStateUpdate: (
        current: BackStackEntry?,
        previous: BackStackEntry?,
        isPredictive: Boolean,
        progress: Float,
        justCompleted: Boolean
    ) -> Unit
) {
    exitAnimProgress.snapTo(gestureProgress.floatValue)
    gestureProgress.floatValue = 0f
    exitAnimProgress.animateTo(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )
    
    navigator.navigateBack()
    withFrameNanos { }
    
    onStateUpdate(getCurrentEntry(), null, false, 0f, true)
    
    exitAnimProgress.snapTo(0f)
    capturedCurrent?.let { composableCache.unlockEntry(it.id) }
    capturedPrevious?.let { composableCache.unlockEntry(it.id) }
    
    withFrameNanos { }
    onStateUpdate(getCurrentEntry(), null, false, 0f, false)
}

/**
 * Animates gesture cancellation and resets state.
 */
private suspend fun animateGestureCancellation(
    gestureProgress: MutableFloatState,
    exitAnimProgress: Animatable<Float, *>,
    capturedCurrent: BackStackEntry?,
    capturedPrevious: BackStackEntry?,
    composableCache: ComposableCache,
    onStateUpdate: (
        current: BackStackEntry?,
        previous: BackStackEntry?,
        isPredictive: Boolean,
        progress: Float,
        justCompleted: Boolean
    ) -> Unit
) {
    exitAnimProgress.snapTo(gestureProgress.floatValue)
    gestureProgress.floatValue = 0f
    exitAnimProgress.animateTo(
        targetValue = 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )
    
    // Restore the current screen (gesture was cancelled, so we stay on the same screen)
    onStateUpdate(capturedCurrent, null, false, 0f, false)
    
    exitAnimProgress.snapTo(0f)
    capturedCurrent?.let { composableCache.unlockEntry(it.id) }
    capturedPrevious?.let { composableCache.unlockEntry(it.id) }
}

/**
 * Internal content of GraphNavHost, extracted to allow wrapping with SharedTransitionLayout.
 * 
 * Note: LaunchedEffect and PredictiveBackHandler must remain inline to maintain reactive access
 * to state for immediate updates. Complex logic is extracted to separate suspend functions.
 */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalSharedTransitionApi::class)
@Composable
@Suppress("LongMethod")
private fun GraphNavHostContent(
    graph: NavigationGraph,
    navigator: Navigator,
    defaultTransition: NavigationTransition,
    enableComposableCache: Boolean,
    enablePredictiveBack: Boolean,
    maxCacheSize: Int,
    sharedTransitionScope: SharedTransitionScope,
    modifier: Modifier = Modifier
) {
    val backStackEntries by navigator.backStack.stack.collectAsState()
    val currentEntry by navigator.backStack.current.collectAsState()
    val previousEntry by navigator.backStack.previous.collectAsState()
    val canGoBack by remember { derivedStateOf { backStackEntries.size > 1 } }
    val saveableStateHolder = rememberSaveableStateHolder()
    val composableCache = remember { ComposableCache(maxCacheSize) }

    var isNavigating by remember { mutableStateOf(false) }
    var isPredictiveGesture by remember { mutableStateOf(false) }
    var justCompletedGesture by remember { mutableStateOf(false) }
    var isBackNavigation by remember { mutableStateOf(false) }
    val gestureProgress = remember { mutableFloatStateOf(0f) }
    val exitAnimProgress = remember { Animatable(0f) }
    var displayedCurrent by remember { mutableStateOf<BackStackEntry?>(null) }
    var displayedPrevious by remember { mutableStateOf<BackStackEntry?>(null) }

    // Track stack changes for animation direction detection
    var lastStackSize by remember { mutableIntStateOf(backStackEntries.size) }
    var lastEntryId by remember { mutableStateOf<String?>(null) }

    // Update displayed entries and animation state - MUST be inline for immediate state updates
    LaunchedEffect(backStackEntries.size, currentEntry?.id) {
        val stackSize = backStackEntries.size
        val currentId = currentEntry?.id
        
        // Skip processing if predictive gesture is active OR just completed
        if (isPredictiveGesture || justCompletedGesture) {
            lastStackSize = stackSize
            lastEntryId = currentId
            return@LaunchedEffect
        }

        when {
            // Forward navigation
            stackSize > lastStackSize -> {
                displayedCurrent = currentEntry
                displayedPrevious = previousEntry
                isBackNavigation = false
                handleAnimatedNavigation(currentEntry, previousEntry, composableCache) { isNavigating = it }
            }
            // Back navigation (not predictive gesture)
            stackSize < lastStackSize -> {
                displayedCurrent = currentEntry
                displayedPrevious = previousEntry
                isBackNavigation = true
                isNavigating = false
            }
            // Replace (same size, different entry)
            stackSize == lastStackSize && currentId != lastEntryId && currentId != null -> {
                displayedCurrent = currentEntry
                displayedPrevious = previousEntry
                isBackNavigation = false
                handleAnimatedNavigation(currentEntry, previousEntry, composableCache) { isNavigating = it }
            }
            // No animation
            else -> {
                displayedCurrent = currentEntry
                displayedPrevious = null
                isBackNavigation = false
            }
        }

        lastStackSize = stackSize
        lastEntryId = currentId
    }

    // Predictive back gesture handler - MUST be inline to access reactive state
    val scope = rememberCoroutineScope()
    PredictiveBackHandler(enabled = enablePredictiveBack && canGoBack && !isNavigating) { backEvent ->
        val capturedCurrent = currentEntry
        val capturedPrevious = previousEntry
        
        displayedCurrent = capturedCurrent
        displayedPrevious = capturedPrevious
        isPredictiveGesture = true
        gestureProgress.floatValue = INITIAL_GESTURE_PROGRESS
        
        capturedCurrent?.let { composableCache.lockEntry(it.id) }
        capturedPrevious?.let { composableCache.lockEntry(it.id) }

        try {
            backEvent.collect { event ->
                val clampedProgress = event.progress.coerceAtMost(MAX_GESTURE_PROGRESS)
                gestureProgress.floatValue = clampedProgress
            }
            
            scope.launch {
                animateGestureCompletion(
                    gestureProgress,
                    exitAnimProgress,
                    navigator,
                    capturedCurrent,
                    capturedPrevious,
                    composableCache,
                    { currentEntry }
                ) { current, previous, isPredictive, progress, justCompleted ->
                    displayedCurrent = current
                    displayedPrevious = previous
                    isPredictiveGesture = isPredictive
                    gestureProgress.floatValue = progress
                    justCompletedGesture = justCompleted
                }
            }
        } catch (_: Exception) {
            scope.launch {
                animateGestureCancellation(
                    gestureProgress,
                    exitAnimProgress,
                    capturedCurrent,
                    capturedPrevious,
                    composableCache
                ) { current, previous, isPredictive, progress, justCompleted ->
                    displayedCurrent = current
                    displayedPrevious = previous
                    isPredictiveGesture = isPredictive
                    gestureProgress.floatValue = progress
                    justCompletedGesture = justCompleted
                }
            }
        }
    }

    NavigationContainer(
        displayedCurrent = displayedCurrent,
        displayedPrevious = displayedPrevious,
        isPredictiveGesture = isPredictiveGesture,
        justCompletedGesture = justCompletedGesture,
        isBackNavigation = isBackNavigation,
        gestureProgress = gestureProgress.floatValue,
        exitAnimProgress = exitAnimProgress,
        graph = graph,
        navigator = navigator,
        composableCache = composableCache,
        saveableStateHolder = saveableStateHolder,
        enableComposableCache = enableComposableCache,
        defaultTransition = defaultTransition,
        sharedTransitionScope = sharedTransitionScope,
        modifier = modifier
    )
}

/**
 * Container for navigation content with background.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun NavigationContainer(
    displayedCurrent: BackStackEntry?,
    displayedPrevious: BackStackEntry?,
    isPredictiveGesture: Boolean,
    justCompletedGesture: Boolean,
    isBackNavigation: Boolean,
    gestureProgress: Float,
    exitAnimProgress: Animatable<Float, *>,
    graph: NavigationGraph,
    navigator: Navigator,
    composableCache: ComposableCache,
    saveableStateHolder: SaveableStateHolder,
    enableComposableCache: Boolean,
    defaultTransition: NavigationTransition,
    sharedTransitionScope: SharedTransitionScope,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        NavigationContentRenderer(
            displayedCurrent = displayedCurrent,
            displayedPrevious = displayedPrevious,
            isPredictiveGesture = isPredictiveGesture,
            justCompletedGesture = justCompletedGesture,
            isBackNavigation = isBackNavigation,
            gestureProgress = gestureProgress,
            exitAnimProgress = exitAnimProgress,
            graph = graph,
            navigator = navigator,
            composableCache = composableCache,
            saveableStateHolder = saveableStateHolder,
            enableComposableCache = enableComposableCache,
            defaultTransition = defaultTransition,
            sharedTransitionScope = sharedTransitionScope
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
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

    // Get transition scope from composition local (will be null if shared elements disabled)
    val transitionScope = currentTransitionScope()

    destConfig?.let { config ->
        // Prefer contentWithTransitionScope if available (for shared element support)
        val renderContent: @Composable (BackStackEntry) -> Unit = if (config.contentWithTransitionScope != null) {
            { stackEntry ->
                config.contentWithTransitionScope.invoke(
                    stackEntry.destination,
                    navigator,
                    transitionScope
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
