# HIER-026: FakeNavRenderScope

## Task Metadata

| Property | Value |
|----------|-------|
| **Task ID** | HIER-026 |
| **Task Name** | Create FakeNavRenderScope for Testing |
| **Phase** | Phase 4: Integration |
| **Complexity** | Small |
| **Estimated Time** | 0.5-1 day |
| **Dependencies** | HIER-001 |
| **Blocked By** | HIER-001 |
| **Blocks** | HIER-027 |

---

## Overview

`FakeNavRenderScope` provides a test-friendly implementation of `NavRenderScope` for unit and integration testing of renderers without requiring a full navigation setup.

---

## File Location

```
quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/testing/FakeNavRenderScope.kt
```

---

## Implementation

```kotlin
package com.jermey.quo.vadis.core.navigation.testing

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import com.jermey.quo.vadis.core.navigation.Navigator
import com.jermey.quo.vadis.core.navigation.compose.animation.AnimationCoordinator
import com.jermey.quo.vadis.core.navigation.compose.cache.ComposableCache
import com.jermey.quo.vadis.core.navigation.compose.gesture.PredictiveBackController
import com.jermey.quo.vadis.core.navigation.compose.hierarchical.NavRenderScope
import com.jermey.quo.vadis.core.navigation.compose.registry.DefaultTransitionRegistry
import com.jermey.quo.vadis.core.navigation.compose.registry.DefaultWrapperRegistry
import com.jermey.quo.vadis.core.navigation.compose.registry.ScreenRegistry
import com.jermey.quo.vadis.core.navigation.compose.registry.WrapperRegistry

/**
 * Fake [NavRenderScope] for testing.
 *
 * Provides test doubles for all dependencies, making it easy
 * to test renderers in isolation.
 *
 * ## Usage
 *
 * ```kotlin
 * @Test
 * fun `ScreenRenderer uses cache`() = runComposeTest {
 *     val fakeScope = FakeNavRenderScope()
 *     
 *     setContent {
 *         ScreenRenderer(
 *             node = testScreenNode,
 *             scope = fakeScope
 *         )
 *     }
 *     
 *     assertTrue(fakeScope.cache.contains(testScreenNode.key))
 * }
 * ```
 *
 * @param navigator Test navigator (defaults to FakeNavigator)
 * @param cache Test cache (defaults to real cache)
 * @param animationCoordinator Test coordinator
 * @param predictiveBackController Test controller
 * @param screenRegistry Test screen registry
 * @param wrapperRegistry Test wrapper registry
 */
@Stable
class FakeNavRenderScope(
    override val navigator: Navigator = FakeNavigator(),
    override val cache: ComposableCache = ComposableCache(),
    override val animationCoordinator: AnimationCoordinator = AnimationCoordinator(DefaultTransitionRegistry),
    override val predictiveBackController: PredictiveBackController = PredictiveBackController(),
    override val screenRegistry: ScreenRegistry = FakeScreenRegistry(),
    override val wrapperRegistry: WrapperRegistry = DefaultWrapperRegistry
) : NavRenderScope {
    
    // SharedTransitionScope is set during test execution
    private var _sharedTransitionScope: SharedTransitionScope? = null
    
    override val sharedTransitionScope: SharedTransitionScope
        get() = _sharedTransitionScope 
            ?: error("SharedTransitionScope not set. Use withSharedTransitionScope {} in test.")
    
    /**
     * Sets the shared transition scope for testing.
     */
    fun setSharedTransitionScope(scope: SharedTransitionScope) {
        _sharedTransitionScope = scope
    }
    
    @Composable
    override fun withAnimatedVisibilityScope(
        visibilityScope: AnimatedVisibilityScope,
        content: @Composable () -> Unit
    ) {
        // In tests, just invoke content directly
        content()
    }
    
    // Tracking for assertions
    val cacheAccessKeys = mutableListOf<String>()
    val transitionRequests = mutableListOf<TransitionRequest>()
    val backGestureActivations = mutableListOf<Unit>()
    
    /**
     * Records for transition request tracking.
     */
    data class TransitionRequest(
        val from: Any?,
        val to: Any,
        val isBack: Boolean
    )
}

/**
 * Fake screen registry for testing.
 */
class FakeScreenRegistry : ScreenRegistry {
    
    var contentToRender: (@Composable () -> Unit)? = null
    val renderedDestinations = mutableListOf<Destination>()
    
    @Composable
    override fun Content(
        destination: Destination,
        navigator: Navigator,
        sharedTransitionScope: SharedTransitionScope,
        animatedVisibilityScope: AnimatedVisibilityScope?
    ) {
        renderedDestinations.add(destination)
        contentToRender?.invoke()
    }
}

/**
 * Extension to run tests with SharedTransitionScope.
 */
@Composable
fun FakeNavRenderScope.withSharedTransitionScope(
    content: @Composable () -> Unit
) {
    SharedTransitionLayout {
        setSharedTransitionScope(this)
        content()
    }
}
```

---

## Acceptance Criteria

- [ ] `FakeNavRenderScope` class with all properties
- [ ] Defaults to fake/test implementations
- [ ] `setSharedTransitionScope` for test setup
- [ ] Tracking lists for assertions
- [ ] `FakeScreenRegistry` with recording
- [ ] `withSharedTransitionScope` test helper
- [ ] KDoc documentation with usage example
