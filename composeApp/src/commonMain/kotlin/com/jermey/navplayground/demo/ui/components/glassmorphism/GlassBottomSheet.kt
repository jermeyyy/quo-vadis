package com.jermey.navplayground.demo.ui.components.glassmorphism

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials

private const val CORNER_RADIUS = 28

/**
 * A glassmorphic modal bottom sheet with blur effect.
 *
 * The sheet has a semi-transparent background with a frosted glass blur effect,
 * allowing content behind it to be visible but blurred.
 *
 * @param onDismissRequest Callback when the sheet is dismissed
 * @param hazeState The HazeState from the parent content to blur
 * @param modifier Modifier for the sheet
 * @param sheetState The state of the bottom sheet
 * @param dragHandle Custom drag handle composable
 * @param content The content of the bottom sheet
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeMaterialsApi::class)
@Composable
fun GlassBottomSheet(
    onDismissRequest: () -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    dragHandle: @Composable (() -> Unit)? = { GlassBottomSheetDragHandle() },
    content: @Composable ColumnScope.() -> Unit
) {
    val hazeStyle = HazeMaterials.ultraThin()
    
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        scrimColor = Color.Black.copy(alpha = 0.32f),
        shape = RoundedCornerShape(topStart = CORNER_RADIUS.dp, topEnd = CORNER_RADIUS.dp),
        dragHandle = null, // We'll add drag handle inside our glass container
        modifier = modifier
    ) {
        // Glass effect container - this is where haze is applied
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = CORNER_RADIUS.dp, topEnd = CORNER_RADIUS.dp))
                .hazeEffect(state = hazeState) {
                    style = hazeStyle
                }
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Drag handle inside glass container
            if (dragHandle != null) {
                dragHandle()
            }
            // Sheet content
            content()
        }
    }
}

/**
 * Default drag handle for glassmorphic bottom sheet.
 */
@Composable
fun GlassBottomSheetDragHandle() {
    Box(
        modifier = Modifier
            .padding(vertical = 12.dp)
            .width(32.dp)
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
    )
}
