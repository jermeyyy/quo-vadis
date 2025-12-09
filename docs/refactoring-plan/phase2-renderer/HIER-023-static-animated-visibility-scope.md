# HIER-023: StaticAnimatedVisibilityScope

## Task Metadata

| Property | Value |
|----------|-------|
| **Task ID** | HIER-023 |
| **Task Name** | Create StaticAnimatedVisibilityScope |
| **Phase** | Phase 3: Renderer Implementation |
| **Complexity** | Small |
| **Estimated Time** | 0.5 day |
| **Dependencies** | None |
| **Blocked By** | - |
| **Blocks** | HIER-020 |

---

## Overview

`StaticAnimatedVisibilityScope` is a fake implementation of `AnimatedVisibilityScope` for use during predictive back rendering. When content is rendered outside of `AnimatedContent` (like during gesture preview), this provides stable/completed transition values.

---

## File Location

```
quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/hierarchical/StaticAnimatedVisibilityScope.kt
```

---

## Implementation

```kotlin
package com.jermey.quo.vadis.core.navigation.compose.hierarchical

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.rememberTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Provides a static [AnimatedVisibilityScope] for content rendered
 * outside of [AnimatedContent].
 *
 * Used during predictive back gestures when both current and previous
 * content need an AnimatedVisibilityScope but aren't actually animating
 * via AnimatedContent.
 *
 * ## Behavior
 * - Transition state is always [EnterExitState.Visible]
 * - No actual animation occurs
 * - Safe to use with shared element modifiers
 *
 * @param content The composable content requiring a scope
 */
@Composable
internal fun StaticAnimatedVisibilityScope(
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    val scope = rememberStaticAnimatedVisibilityScope()
    scope.content()
}

/**
 * Remembers a static [AnimatedVisibilityScope].
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
 */
private class StaticAnimatedVisibilityScopeImpl(
    override val transition: Transition<EnterExitState>
) : AnimatedVisibilityScope {
    // AnimatedVisibilityScope interface implementation
    // The transition property is all that's needed
}
```

---

## Integration Points

- **PredictiveBackContent**: Uses for rendering previous/current content
- **AnimatedNavContent**: May use when gesture is active
- **Shared element transitions**: Content can still use sharedElement modifier

---

## Testing Requirements

```kotlin
class StaticAnimatedVisibilityScopeTest {
    
    @Test
    fun `scope provides Visible state`() = runComposeTest {
        setContent {
            StaticAnimatedVisibilityScope {
                assertEquals(EnterExitState.Visible, transition.currentState)
            }
        }
    }
    
    @Test
    fun `scope is stable across recompositions`() = runComposeTest {
        var scope1: AnimatedVisibilityScope? = null
        var scope2: AnimatedVisibilityScope? = null
        var recompose by mutableStateOf(0)
        
        setContent {
            val _ = recompose // Observe
            val scope = rememberStaticAnimatedVisibilityScope()
            if (scope1 == null) scope1 = scope else scope2 = scope
        }
        
        recompose++
        
        assertSame(scope1, scope2)
    }
}
```

---

## Acceptance Criteria

- [ ] `StaticAnimatedVisibilityScope` composable wrapper
- [ ] `rememberStaticAnimatedVisibilityScope()` factory function
- [ ] `StaticAnimatedVisibilityScopeImpl` internal class
- [ ] Transition always in `EnterExitState.Visible`
- [ ] Scope is stable (same instance across recompositions)
- [ ] KDoc documentation
- [ ] Unit tests pass
