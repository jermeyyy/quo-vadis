package com.jermey.navplayground.demo.ui.components.explore

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** Maximum number of items that receive a staggered delay. Items beyond this appear instantly. */
private const val MAX_STAGGER_ITEMS = 6

/** Base delay between each staggered item in milliseconds. */
private const val STAGGER_DELAY_MS = 50

/** Vertical slide distance for entry animation. */
private const val SLIDE_OFFSET_DP = 24f

/** Duration of each item's entry animation in milliseconds. */
private const val ANIMATION_DURATION_MS = 300

/**
 * Tracks the current stagger animation generation.
 *
 * Each call to [triggerAnimation] increments the [generation] counter, which causes
 * all items using [staggeredItemAnimation] to replay their entry animation with
 * per-index stagger delays.
 */
@Stable
class StaggerAnimationState {
    /**
     * Current animation generation. Incremented by [triggerAnimation] to re-trigger
     * all staggered item animations.
     */
    var generation by mutableIntStateOf(0)
        private set

    /**
     * Triggers a new stagger animation cycle for all items observing this state.
     */
    fun triggerAnimation() {
        generation++
    }
}

/**
 * Creates and remembers a [StaggerAnimationState] instance.
 *
 * @return A remembered instance of StaggerAnimationState
 */
@Composable
fun rememberStaggerAnimationState(): StaggerAnimationState {
    return remember { StaggerAnimationState() }
}

/**
 * Applies a staggered entry animation (fade + slide up) to a grid item.
 *
 * Items with [index] < [MAX_STAGGER_ITEMS] receive a delayed fade-in and slide-up
 * animation. Items beyond that threshold appear instantly. The animation replays
 * whenever [state]'s generation changes.
 *
 * Uses [graphicsLayer] for GPU-accelerated alpha and translationY transforms.
 *
 * @param index The item's position in the grid
 * @param state The shared stagger animation state that controls animation generations
 * @return Modified [Modifier] with stagger animation applied
 */
fun Modifier.staggeredItemAnimation(
    index: Int,
    state: StaggerAnimationState
): Modifier = composed {
    if (index >= MAX_STAGGER_ITEMS) return@composed this

    val animatable = remember { Animatable(0f) }

    LaunchedEffect(state.generation) {
        animatable.snapTo(0f)
        delay(index.toLong() * STAGGER_DELAY_MS)
        animatable.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = ANIMATION_DURATION_MS,
                easing = EaseOutCubic
            )
        )
    }

    val density = LocalDensity.current
    val offsetPx = with(density) { SLIDE_OFFSET_DP.dp.toPx() }

    this.graphicsLayer {
        alpha = animatable.value
        translationY = (1f - animatable.value) * offsetPx
    }
}
