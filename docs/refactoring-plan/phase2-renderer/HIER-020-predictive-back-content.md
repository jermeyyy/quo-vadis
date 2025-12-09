# HIER-020: PredictiveBackContent

## Task Metadata

| Property | Value |
|----------|-------|
| **Task ID** | HIER-020 |
| **Task Name** | Create PredictiveBackContent |
| **Phase** | Phase 3: Renderer Implementation |
| **Complexity** | Medium |
| **Estimated Time** | 2 days |
| **Dependencies** | HIER-006 (PredictiveBackController), HIER-007 (ComposableCache) |
| **Blocked By** | HIER-006, HIER-007 |
| **Blocks** | HIER-019 |

---

## Overview

`PredictiveBackContent` renders overlapping current and previous content during a predictive back gesture. The previous content is static behind, while the current content transforms based on gesture progress with parallax and scale effects.

---

## File Location

```
quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/hierarchical/PredictiveBackContent.kt
```

---

## Implementation

```kotlin
package com.jermey.quo.vadis.core.navigation.compose.hierarchical

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.jermey.quo.vadis.core.navigation.compose.gesture.PredictiveBackController
import com.jermey.quo.vadis.core.navigation.core.NavNode

/**
 * Renders content during a predictive back gesture.
 *
 * Shows previous content statically behind the current content,
 * which transforms based on gesture progress.
 *
 * ## Visual Effect
 * - Previous: Static with slight parallax effect
 * - Current: Slides right, scales down based on progress
 *
 * @param T NavNode type
 * @param current Current node being exited
 * @param previous Previous node being revealed
 * @param progress Gesture progress (0.0 to 0.25 visual)
 * @param scope Rendering context
 * @param content Composable renderer for each node
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
        // Previous (incoming) - static behind current with parallax
        if (previous != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // Parallax: moves forward as current exits
                        translationX = -size.width * PARALLAX_FACTOR * (1f - progress / PROGRESS_VISUAL_MAX)
                    }
            ) {
                scope.cache.CachedEntry(key = previous.key) {
                    StaticAnimatedVisibilityScope {
                        content(previous)
                    }
                }
            }
        }
        
        // Current (exiting) - transforms based on gesture
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    // Slide right based on progress
                    translationX = size.width * (progress / PROGRESS_VISUAL_MAX)
                    
                    // Scale down slightly
                    val scale = 1f - (progress / PROGRESS_VISUAL_MAX * SCALE_FACTOR)
                    scaleX = scale
                    scaleY = scale
                    
                    // Slight shadow effect via alpha
                    alpha = 1f - (progress / PROGRESS_VISUAL_MAX * 0.1f)
                }
        ) {
            scope.cache.CachedEntry(key = current.key) {
                StaticAnimatedVisibilityScope {
                    content(current)
                }
            }
        }
    }
}

private const val PARALLAX_FACTOR = 0.3f
private const val SCALE_FACTOR = 0.1f
private const val PROGRESS_VISUAL_MAX = PredictiveBackController.PROGRESS_VISUAL_CLAMP
```

---

## Integration Points

- **AnimatedNavContent**: Switches to PredictiveBackContent when gesture active
- **ComposableCache**: Locks both entries during gesture
- **PredictiveBackController**: Provides progress value
- **StaticAnimatedVisibilityScope**: Provides fake scope for content

---

## Acceptance Criteria

- [ ] `PredictiveBackContent` composable with current, previous, progress, scope, content
- [ ] Previous content renders with parallax transform
- [ ] Current content transforms with translation, scale, alpha
- [ ] Uses `CachedEntry` for both current and previous
- [ ] Uses `StaticAnimatedVisibilityScope` for content
- [ ] Constants for PARALLAX_FACTOR, SCALE_FACTOR match controller
- [ ] KDoc documentation
