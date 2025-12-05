package com.jermey.quo.vadis.core.navigation.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.PredictiveBackHandler
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.jermey.quo.vadis.core.navigation.core.NavigationGraph
import com.jermey.quo.vadis.core.navigation.core.Navigator
import com.jermey.quo.vadis.core.navigation.core.BackStackEntry
import com.jermey.quo.vadis.core.navigation.core.route
import com.jermey.quo.vadis.core.navigation.core.backStack
import kotlinx.coroutines.launch

/**
 * CompositionLocal that indicates whether a predictive back gesture is currently in progress.
 *
 * Child composables can check this value to skip animations during the predictive back gesture,
 * preventing visual glitches like blinking when content is being rendered as part of the
 * back animation.
 */
val LocalPredictiveBackInProgress = staticCompositionLocalOf { false }

/**
 * Coordinates animation state and display entries during predictive back gestures.
 * Separates logical backstack state from visual rendering state to prevent
 * premature composable destruction during exit animations.
 */
@Stable
private class PredictiveBackAnimationCoordinator {
    /**
     * Stable entries for rendering during animation.
     * These remain fixed while animation is in progress.
     */
    var displayedCurrentEntry by mutableStateOf<BackStackEntry?>(null)
        private set
    var displayedPreviousEntry by mutableStateOf<BackStackEntry?>(null)
        private set

    /**
     * Whether animation is currently in progress.
     */
    var isAnimating by mutableStateOf(false)
        private set

    /**
     * Start animation with captured entries.
     * Freezes the displayed entries until animation completes.
     */
    fun startAnimation(current: BackStackEntry?, previous: BackStackEntry?) {
        displayedCurrentEntry = current
        displayedPreviousEntry = previous
        isAnimating = true
    }

    /**
     * Finish animation and allow normal rendering to resume.
     */
    fun finishAnimation() {
        isAnimating = false
        // Entries will be updated on next frame from backstack state
    }

    /**
     * Cancel animation and reset state.
     */
    fun cancelAnimation() {
        isAnimating = false
        displayedCurrentEntry = null
        displayedPreviousEntry = null
    }
}

/**
 * Multiplatform predictive back gesture handler with automatic screen caching.
 *
 * Provides visual feedback during back gestures on both Android 13+ and iOS.
 * Automatically renders both the current and previous screens during the gesture
 * using composable caching for smooth animations.
 *
 * @param navigator The navigation controller
 * @param graph The navigation graph containing screen definitions
 * @param enabled Whether predictive back is enabled
 * @param modifier Modifier to apply to the container
 * @param animationType Type of animation during the back gesture
 * @param sensitivity Multiplier for gesture progress (default 1.0)
 * @param maxCacheSize Maximum number of cached screens (default 3)
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PredictiveBackNavigation(
    navigator: Navigator,
    graph: NavigationGraph,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    animationType: PredictiveBackAnimationType = PredictiveBackAnimationType.Slide,
    sensitivity: Float = 1f,
    maxCacheSize: Int = 3
) {
    // State management
    var gestureProgress by remember { mutableFloatStateOf(0f) }
    val exitAnimProgress = remember { Animatable(0f) }
    var isGesturing by remember { mutableStateOf(false) }
    var isExitAnimating by remember { mutableStateOf(false) }

    // Animation coordinator for stable rendering during animations
    val coordinator = remember { PredictiveBackAnimationCoordinator() }

    // Navigation state
    val backStackState by navigator.backStack.stack.collectAsState()
    val currentEntry by navigator.backStack.current.collectAsState()
    val previousEntry by navigator.backStack.previous.collectAsState()
    val canGoBack by remember { derivedStateOf { backStackState.size > 1 } }

    // Determine what to display
    // Current screen always uses live entry for proper animation updates
    val displayedCurrent = currentEntry

    // Previous screen uses coordinator entry during animation to keep it rendered
    val displayedPrevious = if (coordinator.isAnimating) {
        coordinator.displayedPreviousEntry
    } else {
        null // Don't render previous screen when not animating
    }

    // Resources
    val composableCache = rememberComposableCache(maxCacheSize)
    val saveableStateHolder = rememberSaveableStateHolder()
    val scope = rememberCoroutineScope()

    // Lock cache entries during animation to prevent premature eviction
    LaunchedEffect(
        coordinator.isAnimating,
        currentEntry?.id,
        coordinator.displayedPreviousEntry?.id
    ) {
        if (coordinator.isAnimating) {
            currentEntry?.let { composableCache.lockEntry(it.id) }
            coordinator.displayedPreviousEntry?.let { composableCache.lockEntry(it.id) }
        } else {
            currentEntry?.let { composableCache.unlockEntry(it.id) }
            coordinator.displayedPreviousEntry?.let { composableCache.unlockEntry(it.id) }
        }
    }

    // Predictive back gesture handler
    PredictiveBackHandler(enabled = enabled && canGoBack && !isExitAnimating) { backEvent ->
        // Capture entries BEFORE any state changes
        coordinator.startAnimation(currentEntry, previousEntry)
        isGesturing = true

        try {
            backEvent.collect { event ->
                gestureProgress = event.progress * sensitivity
            }

            // Gesture completed - animate exit
            isGesturing = false
            isExitAnimating = true

            scope.launch {
                exitAnimProgress.snapTo(gestureProgress)
                exitAnimProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )

                // Animation complete - NOW navigate and cleanup
                navigator.navigateBack()

                // End animation immediately - the new screen will render
                isExitAnimating = false
                coordinator.finishAnimation()
                exitAnimProgress.snapTo(0f)
                gestureProgress = 0f
            }
        } catch (_: Exception) {
            // Gesture cancelled
            isGesturing = false
            scope.launch {
                exitAnimProgress.snapTo(gestureProgress)
                exitAnimProgress.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessHigh
                    )
                )

                // Reset state after cancellation animation
                coordinator.cancelAnimation()
                gestureProgress = 0f
            }
        }
    }

    // Rendering
    PredictiveBackContent(
        modifier = modifier,
        isGesturing = isGesturing,
        isExitAnimating = isExitAnimating,
        gestureProgress = gestureProgress,
        exitProgress = exitAnimProgress.value,
        animationType = animationType,
        currentEntry = displayedCurrent,
        previousEntry = displayedPrevious,
        graph = graph,
        navigator = navigator,
        composableCache = composableCache,
        saveableStateHolder = saveableStateHolder
    )
}

/**
 * Renders the content for predictive back animation.
 */
@Composable
private fun PredictiveBackContent(
    modifier: Modifier,
    isGesturing: Boolean,
    isExitAnimating: Boolean,
    gestureProgress: Float,
    exitProgress: Float,
    animationType: PredictiveBackAnimationType,
    currentEntry: BackStackEntry?,
    previousEntry: BackStackEntry?,
    graph: NavigationGraph,
    navigator: Navigator,
    composableCache: ComposableCache,
    saveableStateHolder: SaveableStateHolder
) {
    val isAnimating = isGesturing || isExitAnimating

    // Provide predictive back state to child composables so they can skip animations
    CompositionLocalProvider(
        LocalPredictiveBackInProgress provides isAnimating
    ) {
        Box(modifier = modifier.fillMaxSize()) {
            // Previous screen layer
            if (isAnimating && previousEntry != null) {
                PreviousScreenLayer(
                    entry = previousEntry,
                    graph = graph,
                    navigator = navigator,
                    composableCache = composableCache,
                    saveableStateHolder = saveableStateHolder
                )
            }

            // Scrim layer - only during gesture, not during exit
            if (isGesturing && gestureProgress > 0f && previousEntry != null) {
                ScrimLayer(gestureProgress)
            }

            // Current screen layer
            if (currentEntry != null) {
                CurrentScreenLayer(
                    entry = currentEntry,
                    isGesturing = isGesturing,
                    isExitAnimating = isExitAnimating,
                    gestureProgress = gestureProgress,
                    exitProgress = exitProgress,
                    animationType = animationType,
                    graph = graph,
                    navigator = navigator,
                    composableCache = composableCache,
                    saveableStateHolder = saveableStateHolder
                )
            }
        }
    }
}

/**
 * Renders the previous screen layer.
 */
@Composable
private fun PreviousScreenLayer(
    entry: BackStackEntry,
    graph: NavigationGraph,
    navigator: Navigator,
    composableCache: ComposableCache,
    saveableStateHolder: SaveableStateHolder
) {
    val destConfig = remember(entry.destination.route) {
        graph.destinations.find { it.destination.route == entry.destination.route }
    }

    destConfig?.let { config ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(0f)
        ) {
            key(entry.id) {
                composableCache.Entry(
                    entry = entry,
                    saveableStateHolder = saveableStateHolder
                ) { stackEntry ->
                    config.content(stackEntry.destination, navigator)
                }
            }
        }
    }
}

/**
 * Renders the scrim layer.
 */
@Suppress("MagicNumber")
@Composable
private fun ScrimLayer(progress: Float) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(0.5f)
            .background(Color.Black.copy(alpha = 0.3f * (1f - progress)))
    )
}

/**
 * Renders the current screen layer with animation.
 */
@Composable
private fun CurrentScreenLayer(
    entry: BackStackEntry,
    isGesturing: Boolean,
    isExitAnimating: Boolean,
    gestureProgress: Float,
    exitProgress: Float,
    animationType: PredictiveBackAnimationType,
    graph: NavigationGraph,
    navigator: Navigator,
    composableCache: ComposableCache,
    saveableStateHolder: SaveableStateHolder
) {
    val destConfig = remember(entry.destination.route) {
        graph.destinations.find { it.destination.route == entry.destination.route }
    }

    destConfig?.let { config ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(1f)
                .then(
                    when {
                        isGesturing -> {
                            // Gesture animation: user is dragging back
                            when (animationType) {
                                PredictiveBackAnimationType.Material3 ->
                                    Modifier.material3BackAnimation(gestureProgress)

                                PredictiveBackAnimationType.Scale ->
                                    Modifier.scaleBackAnimation(gestureProgress)

                                PredictiveBackAnimationType.Slide ->
                                    Modifier.slideBackAnimation(gestureProgress)
                            }
                        }

                        isExitAnimating -> {
                            // Exit animation: screen is leaving after gesture completed
                            // Use same animation type as gesture for consistency
                            when (animationType) {
                                PredictiveBackAnimationType.Material3 ->
                                    Modifier.material3ExitAnimation(exitProgress)

                                PredictiveBackAnimationType.Scale ->
                                    Modifier.scaleExitAnimation(exitProgress)

                                PredictiveBackAnimationType.Slide ->
                                    Modifier.slideExitAnimation(exitProgress)
                            }
                        }

                        else -> Modifier
                    }
                )
        ) {
            key(entry.id) {
                composableCache.Entry(
                    entry = entry,
                    saveableStateHolder = saveableStateHolder
                ) { stackEntry ->
                    config.content(stackEntry.destination, navigator)
                }
            }
        }
    }
}

/**
 * Types of predictive back animations.
 */
enum class PredictiveBackAnimationType {
    /**
     * Material 3 style animation with scale, translation, and rounded corners.
     */
    Material3,

    /**
     * Simple scale animation.
     */
    Scale,

    /**
     * Slide animation.
     */
    Slide
}

/**
 * Material 3 style back animation.
 * Scales down, translates right, adds rounded corners and shadow.
 */
@Suppress("MagicNumber")
private fun Modifier.material3BackAnimation(progress: Float): Modifier {
    val scale = lerp(1f, 0.9f, progress)
    val offsetX = lerp(0f, 80f, progress)
    val cornerRadius = lerp(0f, 16f, progress)

    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
        translationX = offsetX
        shape = RoundedCornerShape(cornerRadius.dp)
        clip = true
        shadowElevation = lerp(0f, 16f, progress)
    }
}

/**
 * Scale-based back animation.
 * Simple scale down with fade.
 */
@Suppress("MagicNumber")
private fun Modifier.scaleBackAnimation(progress: Float): Modifier {
    val scale = lerp(1f, 0.9f, progress)
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
        alpha = lerp(1f, 0.8f, progress)
    }
}

/**
 * Slide-based back animation.
 * Slides right with fade.
 */
@Suppress("MagicNumber")
private fun Modifier.slideBackAnimation(progress: Float): Modifier {
    val offsetX = lerp(0f, 100f, progress)
    return this.graphicsLayer {
        translationX = offsetX
    }
}

/**
 * Material 3 style exit animation.
 * Continues from gesture end state with scale, translation, rounded corners, and fade out.
 */
@Suppress("MagicNumber")
private fun Modifier.material3ExitAnimation(progress: Float): Modifier {
    // Continue from gesture end state (scale 0.9, offset 80px, corners 16dp)
    val scale = lerp(0.9f, 0.7f, progress)
    val offsetX = lerp(80f, 250f, progress)
    val cornerRadius = lerp(16f, 24f, progress)
    val alpha = lerp(1f, 0f, progress)

    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
        translationX = offsetX
        this.alpha = alpha
        shape = androidx.compose.foundation.shape.RoundedCornerShape(cornerRadius.dp)
        clip = true
        shadowElevation = lerp(16f, 0f, progress)
    }
}

/**
 * Scale-based exit animation.
 * Continues scaling down with fade out.
 */
@Suppress("MagicNumber")
private fun Modifier.scaleExitAnimation(progress: Float): Modifier {
    // Continue from gesture end state (scale 0.9, alpha 0.8)
    val scale = lerp(0.9f, 0.6f, progress)
    val alpha = lerp(0.8f, 0f, progress)

    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
        this.alpha = alpha
    }
}

/**
 * Slide-based exit animation.
 * Continues sliding right with fade out.
 */
@Suppress("MagicNumber")
private fun Modifier.slideExitAnimation(progress: Float): Modifier {
    // Continue from gesture end state (offset 100px)
    val offsetX = lerp(100f, 300f, progress)
    val alpha = lerp(1f, 0f, progress)

    return this.graphicsLayer {
        translationX = offsetX
        this.alpha = alpha
    }
}

/**
 * Linear interpolation between two values.
 */
private fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return start + fraction * (stop - start)
}

