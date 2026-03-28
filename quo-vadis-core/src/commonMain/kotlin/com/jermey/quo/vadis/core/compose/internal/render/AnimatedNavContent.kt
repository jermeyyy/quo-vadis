@file:OptIn(InternalQuoVadisApi::class)

package com.jermey.quo.vadis.core.compose.internal.render

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.compose.internal.ComposableCache
import com.jermey.quo.vadis.core.compose.scope.NavRenderScope
import com.jermey.quo.vadis.core.compose.transition.NavTransition
import com.jermey.quo.vadis.core.navigation.node.NavNode
import com.jermey.quo.vadis.core.navigation.node.forEachNode

/**
 * Custom AnimatedContent variant optimized for navigation transitions.
 *
 * This composable is the core animation component for the hierarchical rendering engine.
 * It handles transitions between navigation states with support for both standard
 * `AnimatedContent` animations and predictive back gesture-driven animations.
 *
 * ## Animation Direction
 *
 * The direction of navigation is determined by the caller (typically the parent renderer
 * like [StackRenderer]) and passed via the [isBackNavigation] parameter. This ensures
 * consistent direction detection across different rendering contexts.
 *
 * ## Predictive Back Integration
 *
 * When predictive back gestures are enabled and active, the current screen remains at its
 * stable composition tree position inside `AnimatedContent`. Gesture-driven visual transforms
 * (slide + scale) are applied via `graphicsLayer` inside the content lambda, which is GPU-only
 * and causes zero recomposition. The previous screen is rendered as a sibling underlay Box
 * **before** the main content block, with a parallax `graphicsLayer` effect.
 *
 * During the gesture, `AnimatedContent` transitions are suppressed
 * (`EnterTransition.None togetherWith ExitTransition.None`) so that the framework does not
 * run its own enter/exit animations. A one-frame `recentlyCompletedGesture` flag ensures
 * the suppression extends through the completion boundary frame.
 *
 * ## Cache Locking
 *
 * When the predictive back gesture is active, both the current screen and the back target
 * screen are locked in the [ComposableCache][ComposableCache]
 * via `lock`/`unlock`. This prevents cache eviction during the gesture, ensuring both
 * screens remain renderable.
 *
 * ## State Tracking
 *
 * The component maintains internal state for predictive back:
 * - **lastCommittedState**: The state currently shown on screen
 * - **stateBeforeLast**: The previous state for gesture target resolution
 *
 * ## Shared Element Scope Provision
 *
 * The [AnimatedVisibilityScope] is ALWAYS provided to all children during navigation
 * transitions. This ensures that shared element transitions work correctly because:
 * - Both entering and exiting content use the SAME AnimatedVisibilityScope object
 * - Exit content doesn't recompose to read updated composition locals, so passing
 *   the scope directly via parameter is essential
 * - Nested structures (screens inside tabs/panes) can access the correct scope
 *   even during exit animations
 *
 * ## Modal Support
 *
 * When [isTargetModal] is `true`, this component renders the target content directly
 * with [StaticAnimatedVisibilityScope] instead of `AnimatedContent`. This ensures that
 * when navigating to or from a modal destination, the background content beneath the
 * modal is not removed by exit animations. When a predictive back gesture is active on
 * a modal, slide + scale transforms are applied via `graphicsLayer`.
 *
 * Modal transitions with background layers are primarily handled by
 * [StackRenderer], which renders modal nodes as sibling overlays while
 * keeping the background in this [AnimatedNavContent]. This parameter
 * supports the edge case where a modal is the only child in a stack
 * (no background layer needed) or when future animated modal transitions
 * are added.
 *
 * ## Usage
 *
 * ```kotlin
 * AnimatedNavContent(
 *     targetState = currentNode,
 *     transition = navTransition,
 *     isBackNavigation = isBack,  // Determined by caller
 *     scope = renderScope,
 *     predictiveBackEnabled = true
 * ) { node ->
 *     // Content is provided an AnimatedVisibilityScope
 *     ScreenContent(node)
 * }
 * ```
 *
 * @param T The type of navigation node being animated (must extend [NavNode])
 * @param targetState The current target state to animate to
 * @param transition The [NavTransition] defining enter/exit animations for both directions
 * @param isBackNavigation Whether this is a back navigation (pop). Used to select
 *                         the appropriate animation direction (enter/exit vs popEnter/popExit)
 * @param scope The [NavRenderScope] providing access to predictive back controller,
 *              animation scopes, and other rendering dependencies
 * @param predictiveBackEnabled Whether predictive back gesture handling should be active.
 *                              When `false`, always uses standard `AnimatedContent`
 * @param isTargetModal Whether the target state represents a modal destination.
 *                      When `true`, bypasses `AnimatedContent` and renders
 *                      content directly to avoid removing background content.
 * @param modifier Optional [Modifier] applied to the container
 * @param content The composable content to render for each state, receiving an
 *                [AnimatedVisibilityScope] for enter/exit animation coordination
 *
 * @see NavTransition
 * @see NavRenderScope
 * @see AnimatedContent
 */
@Composable
internal fun <T : NavNode> AnimatedNavContent(
    targetState: T,
    transition: NavTransition,
    isBackNavigation: Boolean,
    scope: NavRenderScope,
    predictiveBackEnabled: Boolean,
    isTargetModal: Boolean = false,
    modifier: Modifier = Modifier,
    content: @Composable AnimatedVisibilityScope.(T) -> Unit
) {
    var lastCommittedState by remember { mutableStateOf(targetState) }
    var stateBeforeLast by remember { mutableStateOf<T?>(null) }

    val isPredictiveBackActive = predictiveBackEnabled &&
        scope.predictiveBackController.isActive.value
    val progress = scope.predictiveBackController.progress.value

    val recentlyCompletedGesture = rememberGestureCompletionFlag(isPredictiveBackActive)
    val backTarget = resolveBackTarget<T>(isPredictiveBackActive, scope, stateBeforeLast)

    PredictiveBackCacheLock(isPredictiveBackActive, backTarget, targetState, scope.cache)
    PredictiveBackUnderlay(isPredictiveBackActive, backTarget, targetState, progress, content)

    if (isTargetModal) {
        // Modal has a single content slot — transform only while gesture is active.
        // On completion, targetState has changed to the new screen, so no transform needed.
        Box(modifier = gestureSlideScaleModifier(isPredictiveBackActive, progress)) {
            StaticAnimatedVisibilityScope { content(targetState) }
        }
    } else {
        AnimatedContent(
            targetState = targetState,
            contentKey = { it.key },
            transitionSpec = {
                if (isPredictiveBackActive || recentlyCompletedGesture) {
                    EnterTransition.None togetherWith ExitTransition.None
                } else {
                    transition.createTransitionSpec(isBack = isBackNavigation)
                }
            },
            modifier = modifier,
            label = "AnimatedNavContent"
        ) { animatingState ->
            // During the gesture, animatingState == targetState (same screen) — apply transform.
            // On the completion frame, AnimatedContent may compose both old + new content.
            // Only the OLD (exiting) content should stay off-screen; the NEW (entering)
            // content must be visible at its natural position immediately.
            val isExitingDuringCompletion = recentlyCompletedGesture &&
                animatingState.key != targetState.key
            val applyTransform = isPredictiveBackActive || isExitingDuringCompletion
            val transformProgress = if (isPredictiveBackActive) progress else 1f
            Box(modifier = gestureSlideScaleModifier(applyTransform, transformProgress)) {
                scope.WithAnimatedVisibilityScope(this@AnimatedContent) {
                    content(animatingState)
                }
            }
        }
    }

    // State tracking — detect navigation changes for predictive back target resolution
    if (targetState != lastCommittedState) {
        if (targetState.key != lastCommittedState.key) {
            stateBeforeLast = lastCommittedState
        }
        lastCommittedState = targetState
    }
}

// ── Private helpers ──

/**
 * Tracks gesture completion across frames. Returns `true` for exactly one composition
 * frame after the predictive back gesture ends, preventing AnimatedContent from running
 * its standard transition on the completion boundary.
 */
@Composable
internal fun rememberGestureCompletionFlag(isPredictiveBackActive: Boolean): Boolean {
    var recentlyCompleted by remember { mutableStateOf(false) }
    val wasActive = remember { mutableStateOf(false) }
    if (isPredictiveBackActive && !wasActive.value) {
        wasActive.value = true
    }
    if (!isPredictiveBackActive && wasActive.value) {
        recentlyCompleted = true
        wasActive.value = false
    }
    SideEffect {
        if (recentlyCompleted) recentlyCompleted = false
    }
    return recentlyCompleted
}

/**
 * Resolves the back target node from the predictive back cascade state or the
 * previously committed state.
 */
private fun <T : NavNode> resolveBackTarget(
    isPredictiveBackActive: Boolean,
    scope: NavRenderScope,
    stateBeforeLast: T?
): T? {
    if (!isPredictiveBackActive) return null
    val cascadeState = scope.predictiveBackController.cascadeState.value
    @Suppress("UNCHECKED_CAST")
    return (cascadeState?.targetNode as? T) ?: stateBeforeLast
}

/**
 * Locks both the back target (and all its descendant nodes) and the current screen
 * in the composable cache while the predictive back gesture is active, preventing
 * eviction during the gesture. This ensures that [SaveableStateProvider][androidx.compose.runtime.saveable.SaveableStateHolder.SaveableStateProvider]
 * can restore saved state for all screens visible in the underlay.
 *
 * When the back target is a container (e.g., [TabNode][com.jermey.quo.vadis.core.navigation.node.TabNode]),
 * its descendant screen keys are also locked so the individual screens within
 * the container retain their saved state throughout the gesture.
 */
@Composable
private fun <T : NavNode> PredictiveBackCacheLock(
    isPredictiveBackActive: Boolean,
    backTarget: T?,
    currentTarget: T,
    cache: ComposableCache
) {
    if (isPredictiveBackActive && backTarget != null) {
        DisposableEffect(backTarget.key) {
            val keysToLock = mutableSetOf(currentTarget.key.value)
            backTarget.forEachNode { keysToLock.add(it.key.value) }
            keysToLock.forEach { cache.lock(it) }
            onDispose {
                keysToLock.forEach { cache.unlock(it) }
            }
        }
    }
}

/**
 * Renders the previous screen as an underlay with parallax translation during
 * the predictive back gesture.
 */
@Composable
private fun <T : NavNode> PredictiveBackUnderlay(
    isPredictiveBackActive: Boolean,
    backTarget: T?,
    currentTarget: T,
    progress: Float,
    content: @Composable AnimatedVisibilityScope.(T) -> Unit
) {
    if (isPredictiveBackActive && backTarget != null &&
        backTarget.key != currentTarget.key
    ) {
        Box(
            modifier = Modifier.fillMaxSize().graphicsLayer {
                translationX = -size.width * PARALLAX_FACTOR * (1f - progress)
            }
        ) {
            StaticAnimatedVisibilityScope { content(backTarget) }
        }
    }
}

/**
 * Creates a gesture-driven slide + scale modifier when predictive back is active,
 * or returns an empty modifier at rest.
 */
private fun gestureSlideScaleModifier(
    isPredictiveBackActive: Boolean,
    progress: Float
): Modifier = if (isPredictiveBackActive) {
    Modifier.fillMaxSize().graphicsLayer {
        translationX = size.width * progress
        val scale = 1f - (progress * SCALE_FACTOR)
        scaleX = scale
        scaleY = scale
    }
} else {
    Modifier
}

// ── Constants ──

private const val PARALLAX_FACTOR = 0.15f
private const val SCALE_FACTOR = 0.15f
