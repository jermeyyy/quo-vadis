package com.jermey.quo.vadis.core.navigation.compose.navback

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import kotlinx.coroutines.flow.collectLatest

/**
 * Quo Vadis back handler that integrates with the NavigationEvent API.
 *
 * This composable provides predictive back gesture handling with proper
 * system integration on Android 14+ and custom gesture handling on other platforms.
 *
 * ## System Integration
 *
 * On Android 14+ (API 34+), this handler properly integrates with the system's
 * `OnBackInvokedDispatcher` to provide:
 * - System predictive back animations when closing the app
 * - In-app predictive back animations during navigation
 * - Proper priority handling for nested handlers
 *
 * On other platforms, the handler provides gesture detection without system animation.
 *
 * ## Usage
 *
 * ```kotlin
 * QuoVadisBackHandler(
 *     enabled = canGoBack,
 *     currentScreenInfo = ScreenNavigationInfo(screenId = "current"),
 *     previousScreenInfo = ScreenNavigationInfo(screenId = "previous"),
 *     onBackProgress = { event -> /* animate with event.progress */ },
 *     onBackCancelled = { /* reset animation */ },
 *     onBackCompleted = { navigator.goBack() }
 * ) {
 *     // Screen content
 * }
 * ```
 *
 * @param enabled Whether back handling is enabled. When false, gestures pass through to the system.
 * @param currentScreenInfo Info about the currently displayed screen for system animation context.
 * @param previousScreenInfo Info about the screen that will be revealed on back navigation.
 *                           Pass null if at the root (system will handle back).
 * @param onBackProgress Called with progress updates during the back gesture (0.0 to 1.0).
 *                       Use this to drive custom animations.
 * @param onBackCancelled Called when the back gesture is cancelled (user didn't complete swipe).
 *                        Reset any animation state here.
 * @param onBackCompleted Called when the back gesture is completed and navigation should occur.
 *                        Perform the actual navigation in this callback.
 * @param content The content to display.
 *
 * @see ScreenNavigationInfo
 * @see BackNavigationEvent
 */
@Composable
fun NavigateBackHandler(
    enabled: Boolean = true,
    currentScreenInfo: ScreenNavigationInfo,
    previousScreenInfo: ScreenNavigationInfo? = null,
    onBackProgress: ((BackNavigationEvent) -> Unit)? = null,
    onBackCancelled: (() -> Unit)? = null,
    onBackCompleted: () -> Unit,
    content: @Composable () -> Unit
) {
    // Create NavigationEventState with current and back screen info
    val navEventState = rememberNavigationEventState(
        currentInfo = currentScreenInfo,
        backInfo = if (previousScreenInfo != null) listOf(previousScreenInfo) else emptyList()
    )

    // Observe transition state for animation progress callbacks
    if (onBackProgress != null) {
        LaunchedEffect(navEventState) {
            snapshotFlow { navEventState.transitionState }
                .collectLatest { transitionState ->
                    when (transitionState) {
                        is NavigationEventTransitionState.InProgress -> {
                            val event = transitionState.latestEvent
                            onBackProgress(
                                BackNavigationEvent(
                                    progress = event.progress,
                                    touchX = event.touchX,
                                    touchY = event.touchY,
                                    swipeEdge = event.swipeEdge
                                )
                            )
                        }
                        is NavigationEventTransitionState.Idle -> {
                            // No action needed for idle state
                        }
                    }
                }
        }
    }

    // Register the NavigationBackHandler with the dispatcher
    NavigationBackHandler(
        state = navEventState,
        isBackEnabled = enabled,
        onBackCancelled = { onBackCancelled?.invoke() },
        onBackCompleted = onBackCompleted
    )

    // Render content
    content()
}

/**
 * Simplified Quo Vadis back handler without progress tracking.
 *
 * Use this when you don't need to animate based on gesture progress.
 * This is useful for simple back navigation without custom animations.
 *
 * @param enabled Whether back handling is enabled.
 * @param onBack Called when the back gesture is completed and navigation should occur.
 * @param content The content to display.
 */
@Composable
fun NavigateBackHandler(
    enabled: Boolean = true,
    onBack: () -> Unit,
    content: @Composable () -> Unit
) {
    NavigateBackHandler(
        enabled = enabled,
        currentScreenInfo = ScreenNavigationInfo(screenId = "current"),
        previousScreenInfo = null,
        onBackProgress = null,
        onBackCancelled = null,
        onBackCompleted = onBack,
        content = content
    )
}
