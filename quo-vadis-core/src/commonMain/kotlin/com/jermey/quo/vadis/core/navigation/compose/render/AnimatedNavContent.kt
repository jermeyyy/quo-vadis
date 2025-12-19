/*
 * Copyright 2025 Jermey
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jermey.quo.vadis.core.navigation.compose.render

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.jermey.quo.vadis.core.navigation.compose.animation.NavTransition
import com.jermey.quo.vadis.core.navigation.core.NavNode

/**
 * Custom AnimatedContent variant optimized for navigation transitions.
 *
 * This composable is the core animation component for the hierarchical rendering engine.
 * It handles transitions between navigation states with support for both standard
 * `AnimatedContent` animations and predictive back gesture-driven animations.
 *
 * ## Animation Direction Detection
 *
 * The component tracks both the currently displayed state and the previous state to
 * determine navigation direction:
 *
 * - **Forward navigation**: New target differs from displayed, and is not the previous state
 * - **Back navigation**: Target state matches the previous state (returning to where we were)
 *
 * This allows transitions to use appropriate enter/exit animations based on direction.
 *
 * ## Predictive Back Integration
 *
 * When predictive back gestures are enabled and active, this component bypasses
 * `AnimatedContent` entirely and delegates to [PredictiveBackContent], which provides
 * gesture-driven animations with proper state preservation.
 *
 * ## State Tracking
 *
 * The component maintains two internal states:
 * - **displayedState**: The state currently shown on screen (may lag during animation)
 * - **previousState**: The state that was displayed before the current one
 *
 * These are updated inside the `AnimatedContent` content lambda to ensure proper
 * animation direction detection for subsequent navigations.
 *
 * ## Usage
 *
 * ```kotlin
 * AnimatedNavContent(
 *     targetState = currentNode,
 *     transition = navTransition,
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
 * @param scope The [NavRenderScope] providing access to predictive back controller,
 *              animation scopes, and other rendering dependencies
 * @param predictiveBackEnabled Whether predictive back gesture handling should be active.
 *                              When `false`, always uses standard `AnimatedContent`
 * @param modifier Optional [Modifier] applied to the container
 * @param content The composable content to render for each state, receiving an
 *                [AnimatedVisibilityScope] for enter/exit animation coordination
 *
 * @see NavTransition
 * @see NavRenderScope
 * @see PredictiveBackContent
 * @see AnimatedContent
 */
@Composable
internal fun <T : NavNode> AnimatedNavContent(
    targetState: T,
    transition: NavTransition,
    scope: NavRenderScope,
    predictiveBackEnabled: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable AnimatedVisibilityScope.(T) -> Unit
) {
    // Track the last committed state (what was fully displayed before current animation)
    // and the state before that for back navigation detection
    var lastCommittedState by remember { mutableStateOf(targetState) }
    var stateBeforeLast by remember { mutableStateOf<T?>(null) }

    // Detect if this is back navigation BEFORE updating tracking state
    // Back navigation: we're returning to the state that was displayed before the current one
    val isBackNavigation = targetState.key != lastCommittedState.key &&
        stateBeforeLast?.key == targetState.key

    // Determine if predictive back gesture is currently active
    val isPredictiveBackActive = predictiveBackEnabled &&
        scope.predictiveBackController.isActive.value

    if (isPredictiveBackActive) {
        // For predictive back, prefer cascadeState.targetNode over local stateBeforeLast
        // This ensures correct animation target even for deep links or restored state
        val cascadeState = scope.predictiveBackController.cascadeState.value
        @Suppress("UNCHECKED_CAST")
        val backTarget = (cascadeState?.targetNode as? T) ?: stateBeforeLast
        
        // Gesture-driven animation - bypass AnimatedContent
        PredictiveBackContent(
            current = lastCommittedState,
            previous = backTarget,
            progress = scope.predictiveBackController.progress.value,
            scope = scope,
            content = content
        )
    } else {
        // Standard AnimatedContent transition
        // Use contentKey to compare nodes by their key, not object reference.
        // This prevents duplicate SaveableStateProvider keys when a node's
        // internal state changes (e.g., TabNode.activeStackIndex) but its key
        // stays the same.
        AnimatedContent(
            targetState = targetState,
            contentKey = { it.key },
            transitionSpec = {
                // Use pre-computed direction for proper animation selection
                transition.createTransitionSpec(isBack = isBackNavigation)
            },
            modifier = modifier,
            label = "AnimatedNavContent"
        ) { animatingState ->
            // Provide AnimatedVisibilityScope to content via NavRenderScope
            scope.withAnimatedVisibilityScope(this) {
                content(animatingState)
            }
        }

        // Update state tracking AFTER AnimatedContent starts
        // This ensures direction detection works for the next navigation
        if (targetState.key != lastCommittedState.key) {
            stateBeforeLast = lastCommittedState
            lastCommittedState = targetState
        }
    }
}
