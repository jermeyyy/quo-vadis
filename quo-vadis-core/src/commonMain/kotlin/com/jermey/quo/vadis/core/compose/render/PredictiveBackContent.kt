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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.jermey.quo.vadis.core.navigation.NavNode

/**
 * Renders overlapping content during predictive back gestures.
 *
 * This composable handles the visual presentation of predictive back gestures by
 * rendering both the current (exiting) content and previous (incoming) content
 * simultaneously with appropriate transforms based on gesture progress.
 *
 * ## Visual Behavior
 *
 * During a predictive back gesture:
 * - **Previous content**: Rendered statically behind current content with a parallax
 *   effect. As the gesture progresses, the previous content slides in from the left
 *   at a slower rate than the current content exits.
 * - **Current content**: Slides to the right and scales down based on gesture progress,
 *   creating a natural "peeling away" effect.
 *
 * ## Transform Details
 *
 * - **Parallax Factor (0.3)**: Previous content moves at 30% of the gesture distance,
 *   creating depth perception.
 * - **Scale Factor (0.1)**: Current content scales down by up to 10% at full gesture
 *   progress (scale ranges from 1.0 to 0.9).
 *
 * ## State Preservation
 *
 * Both current and previous content are rendered through [ComposableCache.CachedEntry],
 * ensuring that composable state is preserved during the gesture. Each content uses
 * [StaticAnimatedVisibilityScope] since the content is rendered outside of
 * `AnimatedContent` during gestures.
 *
 * ## Usage
 *
 * This is an internal component used by hierarchical renderers (e.g., StackRenderer)
 * when predictive back gestures are active:
 *
 * ```kotlin
 * if (predictiveBackController.isActive) {
 *     PredictiveBackContent(
 *         current = currentNode,
 *         previous = previousNode,
 *         progress = predictiveBackController.progress,
 *         scope = renderScope
 *     ) { node ->
 *         screenRegistry.Content(node.destination, navigator)
 *     }
 * }
 * ```
 *
 * @param T The type of NavNode being rendered (must extend [NavNode])
 * @param current The current (exiting) node being dismissed by the gesture
 * @param previous The previous (incoming) node being revealed by the gesture, or null
 *                 if there is no previous destination
 * @param progress The gesture progress from 0 (gesture started) to 1 (gesture complete).
 *                 Values outside this range are clamped internally by transform calculations.
 * @param scope The [NavRenderScope] providing access to cache, state holder, and other
 *              rendering dependencies
 * @param content The composable content to render for each node, receiving an
 *                [AnimatedVisibilityScope] for compatibility with animated visibility modifiers
 *
 * @see NavRenderScope
 * @see StaticAnimatedVisibilityScope
 * @see com.jermey.quo.vadis.core.navigation.compose.navback.PredictiveBackController
 */
@Composable
internal fun <T : NavNode> PredictiveBackContent(
    current: T,
    previous: T?,
    progress: Float,
    scope: NavRenderScope,
    content: @Composable AnimatedVisibilityScope.(T) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Previous (incoming) content - static behind current with parallax effect
        // Guard: Only render previous if it exists AND has a different key than current
        if (previous != null && previous.key != current.key) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // Parallax effect: previous content moves slower than current
                        // At progress=0: fully offset left by PARALLAX_FACTOR * width
                        // At progress=1: at original position (no offset)
                        translationX = -size.width * PARALLAX_FACTOR * (1f - progress)
                    }
            ) {
                // Note: Do NOT use CachedEntry here - the content lambda goes through
                // NavNodeRenderer → ScreenRenderer which already handles caching.
                // Double-caching would cause "Key used multiple times" crashes.
                StaticAnimatedVisibilityScope {
                    content(previous)
                }
            }
        }

        // Current (exiting) content - transforms based on gesture progress
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    // Slide right: at progress=0 no translation, at progress=1 fully off-screen right
                    translationX = size.width * progress
                    // Scale down: from 1.0 at progress=0 to (1-SCALE_FACTOR) at progress=1
                    val scale = 1f - (progress * SCALE_FACTOR)
                    scaleX = scale
                    scaleY = scale
                }
        ) {
            // Note: Do NOT use CachedEntry here - the content lambda goes through
            // NavNodeRenderer → ScreenRenderer which already handles caching.
            // Double-caching would cause "Key used multiple times" crashes.
            StaticAnimatedVisibilityScope {
                content(current)
            }
        }
    }
}

/**
 * Parallax factor for the previous (incoming) content.
 *
 * The previous content moves at this fraction of the gesture distance,
 * creating a depth effect where it appears to be "behind" the current content.
 * A value of 0.3 means the previous content moves at 30% of the speed of the
 * current content's translation.
 */
private const val PARALLAX_FACTOR = 0.1f

/**
 * Scale factor for the current (exiting) content.
 *
 * The current content scales down by this factor at full gesture progress.
 * A value of 0.1 means the content scales from 1.0 (100%) to 0.9 (90%)
 * as the gesture progresses from 0 to 1, creating a subtle "receding" effect.
 */
private const val SCALE_FACTOR = 0.1f
