package com.jermey.navplayground.navigation.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.PredictiveBackHandler
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.jermey.navplayground.navigation.core.NavigationGraph
import com.jermey.navplayground.navigation.core.Navigator

/**
 * Multiplatform predictive back gesture handler with automatic screen caching.
 * Provides visual feedback during back gestures on both Android 13+ and iOS.
 *
 * This composable integrates with the multiplatform predictive back gesture system
 * to show preview animations when users swipe back. It automatically renders both
 * the current and previous screens during the gesture by leveraging composable caching.
 *
 * @param navigator The navigation controller to handle back navigation
 * @param graph The navigation graph containing screen definitions
 * @param enabled Whether predictive back is enabled
 * @param modifier Modifier to apply to the container
 * @param animationType Type of animation to use during the back gesture
 * @param sensitivity Multiplier for gesture progress (default 1.0)
 * @param customAnimation Optional custom animation function
 * @param content The content to display (current screen with NavHost or GraphNavHost)
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PredictiveBackNavigation(
    navigator: Navigator,
    graph: NavigationGraph,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    animationType: PredictiveBackAnimationType = PredictiveBackAnimationType.Material3,
    sensitivity: Float = 1f,
    maxCacheSize: Int = 3,
    customAnimation: (@Composable (progress: Float, content: @Composable () -> Unit) -> Unit)? = null,
    content: @Composable () -> Unit
) {
    var backProgress by remember { mutableStateOf(0f) }
    var isBackGestureInProgress by remember { mutableStateOf(false) }
    val backStackState by navigator.backStack.stack.collectAsState()
    val currentEntry by navigator.backStack.current.collectAsState()
    val previousEntry by navigator.backStack.previous.collectAsState()
    val canGoBack = backStackState.size > 1

    // Composable cache for keeping screens alive
    val composableCache = remember { ComposableCache(maxCacheSize) }
    val saveableStateHolder = rememberSaveableStateHolder()

    // Handle predictive back gesture using multiplatform API
    // Only intercept if we can go back, otherwise let the system handle it
    PredictiveBackHandler(enabled = enabled && canGoBack) { backEvent ->
        isBackGestureInProgress = true

        try {
            // Collect back events and update progress
            backEvent.collect { event ->
                backProgress = event.progress * sensitivity
            }

            // Gesture completed - perform actual navigation
            navigator.navigateBack()
        } finally {
            isBackGestureInProgress = false
            backProgress = 0f
        }
    }

    // Use custom animation if provided
    if (customAnimation != null) {
        customAnimation(backProgress, content)
        return
    }

    // Default animation with previous screen peeking from behind
    Box(modifier = modifier.fillMaxSize()) {
        // Capture entries locally to avoid smart cast issues
        val currentEntryValue = currentEntry
        val previousEntryValue = previousEntry

        // Previous screen (if available) - rendered beneath with scale animation
        if (isBackGestureInProgress && previousEntryValue != null) {
            val destConfig = graph.destinations.find {
                it.destination.route == previousEntryValue.destination.route
            }

            destConfig?.let { config ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(0f) // Bottom layer
                ) {
                    // Render cached previous screen
                    val cachedPreviousComposable = composableCache.getOrCreate(
                        entry = previousEntryValue,
                        saveableStateHolder = saveableStateHolder
                    ) { entry ->
                        config.content(entry.destination, navigator)
                    }
                    cachedPreviousComposable()
                }
            }
        }

        // Scrim layer between current and previous screens
        // Animates from semi-opaque to transparent as gesture progresses
        if (isBackGestureInProgress && backProgress > 0f && previousEntryValue != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(0.5f) // Between previous and current screens
                    .background(Color.Black.copy(alpha = 0.3f * (1f - backProgress)))
            )
        }

        // Current screen with predictive animation on top
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(1f) // Top layer
                .then(
                    if (isBackGestureInProgress) {
                        when (animationType) {
                            PredictiveBackAnimationType.Material3 -> Modifier.material3BackAnimation(backProgress)
                            PredictiveBackAnimationType.Scale -> Modifier.scaleBackAnimation(backProgress)
                            PredictiveBackAnimationType.Slide -> Modifier.slideBackAnimation(backProgress)
                        }
                    } else {
                        Modifier
                    }
                )
        ) {
            // Render cached current screen or provided content
            if (currentEntryValue != null) {
                val destConfig = graph.destinations.find {
                    it.destination.route == currentEntryValue.destination.route
                }

                destConfig?.let { config ->
                    val cachedCurrentComposable = composableCache.getOrCreate(
                        entry = currentEntryValue,
                        saveableStateHolder = saveableStateHolder
                    ) { entry ->
                        config.content(entry.destination, navigator)
                    }
                    cachedCurrentComposable()
                } ?: content()
            } else {
                content()
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
private fun Modifier.material3BackAnimation(progress: Float): Modifier {
    val scale = lerp(1f, 0.9f, progress)
    val offsetX = lerp(0f, 80f, progress)
    val cornerRadius = lerp(0f, 16f, progress)

    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
        translationX = offsetX
        shape = androidx.compose.foundation.shape.RoundedCornerShape(cornerRadius.dp)
        clip = true
        shadowElevation = lerp(0f, 16f, progress)
    }
}

/**
 * Scale-based back animation.
 * Simple scale down with fade.
 */
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
private fun Modifier.slideBackAnimation(progress: Float): Modifier {
    val offsetX = lerp(0f, 100f, progress)
    return this.graphicsLayer {
        translationX = offsetX
        alpha = lerp(1f, 0.7f, progress)
    }
}

/**
 * Linear interpolation between two values.
 */
private fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return start + fraction * (stop - start)
}

