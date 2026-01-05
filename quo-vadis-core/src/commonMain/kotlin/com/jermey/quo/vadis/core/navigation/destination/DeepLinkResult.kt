/*
 * Copyright (c) 2025 Jermey
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
package com.jermey.quo.vadis.core.navigation.destination

/**
 * Result of handling a deep link through a deep link handler.
 *
 * This sealed class represents the two possible outcomes of attempting to handle
 * a deep link URI:
 * - [Matched]: The URI matched a registered route pattern
 * - [NotMatched]: The URI did not match any registered route pattern
 *
 * ## Usage
 *
 * ```kotlin
 * when (val result = handler.handleDeepLink(uri)) {
 *     is DeepLinkResult.Matched -> navigator.navigate(result.destination)
 *     is DeepLinkResult.NotMatched -> showError("Unknown deep link")
 * }
 * ```
 *
 * @see com.jermey.quo.vadis.core.registry.internal.CompositeDeepLinkRegistry
 */
public sealed class DeepLinkResult {
    /**
     * The deep link matched a route pattern and produced a destination.
     *
     * This result contains the fully constructed destination instance with
     * all path parameters extracted from the URI and converted to the
     * appropriate types.
     *
     * @property destination The destination instance created from the deep link.
     */
    public data class Matched(val destination: NavDestination) : DeepLinkResult()

    /**
     * The deep link did not match any registered route pattern.
     *
     * This can occur when:
     * - The URI scheme doesn't match the expected scheme
     * - The URI path doesn't match any registered route patterns
     * - The URI is malformed or invalid
     *
     * Applications should handle this case gracefully, typically by showing
     * an error message or navigating to a default screen.
     */
    public data object NotMatched : DeepLinkResult()
}
