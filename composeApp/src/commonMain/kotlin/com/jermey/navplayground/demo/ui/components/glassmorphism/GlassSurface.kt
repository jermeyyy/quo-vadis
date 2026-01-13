package com.jermey.navplayground.demo.ui.components.glassmorphism

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials

/**
 * A surface with glassmorphism (frosted glass) effect using Haze.
 *
 * Must be used with a [HazeState] that has a source set via `Modifier.hazeSource()`.
 *
 * @param hazeState The shared HazeState from the blur source
 * @param modifier Modifier for the surface
 * @param style Glass blur intensity preset
 * @param shape Shape of the glass surface
 * @param content Content inside the glass surface
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun GlassSurface(
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    style: GlassStyle = GlassStyle.REGULAR,
    shape: Shape = RoundedCornerShape(16.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val hazeStyle = when (style) {
        GlassStyle.THIN -> HazeMaterials.thin()
        GlassStyle.REGULAR -> HazeMaterials.regular()
        GlassStyle.THICK -> HazeMaterials.thick()
    }

    Box(
        modifier = modifier
            .clip(shape)
            .hazeEffect(state = hazeState) {
                this.style = hazeStyle
            },
        content = content
    )
}

/**
 * A card-like glass surface with elevation shadow.
 *
 * @param hazeState The shared HazeState from the blur source
 * @param modifier Modifier for the card
 * @param style Glass blur intensity preset
 * @param cornerRadius Corner radius of the card
 * @param content Content inside the glass card
 */
@Composable
fun GlassCard(
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    style: GlassStyle = GlassStyle.REGULAR,
    cornerRadius: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(cornerRadius),
        color = Color.Transparent,
        shadowElevation = 4.dp
    ) {
        GlassSurface(
            hazeState = hazeState,
            style = style,
            shape = RoundedCornerShape(cornerRadius),
            content = content
        )
    }
}
