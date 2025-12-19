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

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.runtime.compositionLocalOf
import com.jermey.quo.vadis.core.navigation.core.ScreenNode

/**
 * Provides access to the current [ScreenNode] within the navigation hierarchy.
 *
 * This composition local is provided by [ScreenRenderer] and allows screen content
 * to access metadata about the current navigation context, including:
 * - The unique key for this screen instance
 * - The parent key for navigation context
 * - The destination being rendered
 *
 * ## Usage
 *
 * ```kotlin
 * @Composable
 * fun MyScreen() {
 *     val screenNode = LocalScreenNode.current
 *     // Use screenNode.key, screenNode.destination, etc.
 * }
 * ```
 *
 * ## Availability
 *
 * This local is only available within screen content rendered through the
 * hierarchical rendering system. Accessing it outside of this context will
 * return `null`.
 *
 * @see ScreenNode
 * @see ScreenRenderer
 */
val LocalScreenNode = compositionLocalOf<ScreenNode?> { null }

/**
 * Provides access to the current [AnimatedVisibilityScope] within the navigation hierarchy.
 *
 * This composition local is provided by [AnimatedNavContent] (in StackRenderer, TabRenderer, etc.)
 * when rendering animated transitions. It enables screen content to use animated visibility
 * modifiers like `animateEnterExit`.
 *
 * ## Usage
 *
 * ```kotlin
 * @Composable
 * fun MyScreen() {
 *     val animatedVisibilityScope = LocalAnimatedVisibilityScope.current
 *     Box(
 *         modifier = Modifier
 *             .then(
 *                 if (animatedVisibilityScope != null) {
 *                     with(animatedVisibilityScope) {
 *                         Modifier.animateEnterExit(
 *                             enter = fadeIn(),
 *                             exit = fadeOut()
 *                         )
 *                     }
 *                 } else Modifier
 *             )
 *     ) {
 *         // Content
 *     }
 * }
 * ```
 *
 * ## Availability
 *
 * This local is available when:
 * - Content is rendered within `AnimatedContent` (standard transitions)
 * - Content is rendered with `StaticAnimatedVisibilityScope` (predictive back)
 *
 * It may be `null` in edge cases where animation context is not available.
 *
 * @see AnimatedVisibilityScope
 * @see StaticAnimatedVisibilityScope
 */
val LocalAnimatedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> { null }
