package com.jermey.navplayground.demo.ui.components.glassmorphism

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

/**
 * CompositionLocal providing the current [HazeState] for glassmorphism effects.
 *
 * Access via `LocalHazeState.current` within child composables.
 * Returns null if not inside a [HazeScaffold].
 */
val LocalHazeState = compositionLocalOf<HazeState?> { null }

/**
 * A scaffold that sets up Haze blur source and provides [HazeState] via CompositionLocal.
 *
 * Content marked with [Modifier.hazeSource] becomes the blur source.
 * Child composables can access the state via [LocalHazeState] to apply effects.
 *
 * Example usage:
 * ```kotlin
 * HazeScaffold { hazeState ->
 *     // Background content (blur source)
 *     LazyColumn(modifier = Modifier.fillMaxSize()) { ... }
 *
 *     // Glassmorphic overlay (blur effect)
 *     GlassSurface(hazeState = hazeState, modifier = Modifier.align(Alignment.TopCenter)) {
 *         SearchBar(...)
 *     }
 * }
 * ```
 *
 * @param modifier Modifier for the scaffold
 * @param content Content that will be the blur source. Receives [HazeState] for manual usage.
 */
@Composable
fun HazeScaffold(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.(HazeState) -> Unit
) {
    val hazeState = remember { HazeState() }

    CompositionLocalProvider(LocalHazeState provides hazeState) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .hazeSource(state = hazeState),
            content = { content(hazeState) }
        )
    }
}
