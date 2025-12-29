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

package com.jermey.quo.vadis.core.compose.render

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.rememberTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import com.jermey.quo.vadis.core.compose.LocalNavRenderScope
import com.jermey.quo.vadis.core.compose.animation.LocalTransitionScope
import com.jermey.quo.vadis.core.compose.animation.TransitionScope

/**
 * Provides a static [AnimatedVisibilityScope] for content rendered
 * outside of `AnimatedContent`.
 *
 * Used during predictive back gestures when both current and previous
 * content need an [AnimatedVisibilityScope] but aren't actually animating
 * via `AnimatedContent`.
 *
 * ## Behavior
 * - Transition state is always [EnterExitState.Visible]
 * - No actual animation occurs
 * - Safe to use with shared element modifiers
 *
 * ## Usage
 * ```kotlin
 * StaticAnimatedVisibilityScope {
 *     // Content that needs AnimatedVisibilityScope
 *     content(previous)
 * }
 * ```
 *
 * @param content The composable content requiring an [AnimatedVisibilityScope]
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun StaticAnimatedVisibilityScope(
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    val scope = rememberStaticAnimatedVisibilityScope()

    // Get SharedTransitionScope from NavRenderScope for TransitionScope creation
    val navRenderScope = LocalNavRenderScope.current
    val sharedTransitionScope = navRenderScope?.sharedTransitionScope

    // Create TransitionScope if SharedTransitionScope is available
    val transitionScope = sharedTransitionScope?.let {
        TransitionScope(it, scope)
    }

    CompositionLocalProvider(
        LocalAnimatedVisibilityScope provides scope,
        LocalTransitionScope provides transitionScope
    ) {
        scope.content()
    }
}

/**
 * Remembers a static [AnimatedVisibilityScope] that provides a stable,
 * non-animating transition state.
 *
 * The returned scope will be stable across recompositions (same instance),
 * and its transition will always report [EnterExitState.Visible] as both
 * current and target state.
 *
 * @return A stable [AnimatedVisibilityScope] instance with completed transition state
 */
@Composable
internal fun rememberStaticAnimatedVisibilityScope(): AnimatedVisibilityScope {
    val transitionState = remember {
        MutableTransitionState(EnterExitState.Visible)
    }
    val transition = rememberTransition(transitionState, label = "StaticVisibility")

    return remember(transition) {
        StaticAnimatedVisibilityScopeImpl(transition)
    }
}

/**
 * Static implementation of [AnimatedVisibilityScope].
 *
 * Always reports [EnterExitState.Visible] with no animation.
 * This is used when content needs an [AnimatedVisibilityScope] but
 * is rendered outside of `AnimatedContent`, such as during predictive
 * back gesture rendering.
 *
 * @property transition The transition providing enter/exit state.
 *                      For this static implementation, always in [EnterExitState.Visible].
 */
private class StaticAnimatedVisibilityScopeImpl(
    override val transition: Transition<EnterExitState>
) : AnimatedVisibilityScope
