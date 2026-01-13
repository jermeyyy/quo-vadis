package com.jermey.navplayground.demo.ui.components.explore

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.jermey.navplayground.demo.ui.components.glassmorphism.GlassBottomSheet
import com.jermey.navplayground.demo.ui.screens.explore.ExploreItem
import com.jermey.quo.vadis.core.compose.transition.rememberTransitionScope
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials

// Image dimensions now standardized in ExploreItem.imageUrlForId()
private const val HERO_IMAGE_HEIGHT_DP = 220
private const val CORNER_RADIUS = 16
private const val MAX_RATING = 5
private const val CONTENT_REVEAL_DELAY_MS = 100L

@Suppress("MagicNumber")
private val GOLD_COLOR = Color(0xFFFFD700)

/**
 * A modal bottom sheet for quick item preview with glassmorphic design.
 *
 * Features:
 * - High-resolution hero image via AsyncImage
 * - Animated content reveal
 * - Glassmorphic overlay using Haze
 * - "View Full Details" navigation button
 * - Swipe to dismiss support
 *
 * @param item The explore item to preview (null hides the sheet)
 * @param onDismiss Callback when sheet is dismissed
 * @param onViewDetails Callback when "View Full Details" is clicked
 * @param hazeState The shared HazeState for glassmorphic effects
 * @param modifier Modifier for the sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickPreviewSheet(
    item: ExploreItem?,
    onDismiss: () -> Unit,
    onViewDetails: (ExploreItem) -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier
) {
    if (item != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        GlassBottomSheet(
            onDismissRequest = onDismiss,
            hazeState = hazeState,
            sheetState = sheetState,
            modifier = modifier
        ) {
            QuickPreviewContent(
                item = item,
                hazeState = hazeState,
                onViewDetails = { onViewDetails(item) }
            )
        }
    }
}

@Composable
private fun QuickPreviewContent(
    item: ExploreItem,
    hazeState: HazeState,
    onViewDetails: () -> Unit
) {
    var contentVisible by remember { mutableStateOf(false) }

    LaunchedEffect(item) {
        contentVisible = false
        kotlinx.coroutines.delay(CONTENT_REVEAL_DELAY_MS)
        contentVisible = true
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
    ) {
        PreviewHeroImage(
            item = item,
            hazeState = hazeState
        )

        AnimatedVisibility(
            visible = contentVisible,
            enter = fadeIn(spring(stiffness = Spring.StiffnessLow)) +
                    slideInVertically(
                        initialOffsetY = { it / 4 },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
        ) {
            PreviewDetails(
                item = item,
                onViewDetails = onViewDetails
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalHazeMaterialsApi::class)
@Composable
private fun PreviewHeroImage(
    item: ExploreItem,
    hazeState: HazeState
) {
    val context = LocalPlatformContext.current
    var imageState by remember { mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty) }
    val localHazeState = remember { HazeState() }
    val thinStyle = HazeMaterials.thin()
    // Use rememberTransitionScope() for proper animation during navigation
    val transitionScope = rememberTransitionScope()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(HERO_IMAGE_HEIGHT_DP.dp)
            .clip(RoundedCornerShape(CORNER_RADIUS.dp))
            .hazeSource(state = localHazeState)
    ) {
        // Apply shared element for image transition from card
        val imageModifier = Modifier.matchParentSize()
        val finalImageModifier = if (transitionScope != null) {
            with(transitionScope.sharedTransitionScope) {
                imageModifier.sharedElement(
                    sharedContentState = rememberSharedContentState(key = "explore-preview-image-${item.id}"),
                    animatedVisibilityScope = transitionScope.animatedVisibilityScope
                )
            }
        } else {
            imageModifier
        }

        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(ExploreItem.imageUrlForId(item.id))
                .crossfade(true)
                .build(),
            contentDescription = item.title,
            modifier = finalImageModifier,
            contentScale = ContentScale.Crop,
            onState = { imageState = it }
        )

        when (imageState) {
            is AsyncImagePainter.State.Loading -> {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 3.dp
                    )
                }
            }
            else -> { /* Success, Error, or Empty */ }
        }

        // Glassmorphic category badge overlay
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .clip(RoundedCornerShape(12.dp))
                .hazeEffect(state = localHazeState) {
                    style = thinStyle
                }
                .background(Color.Black.copy(alpha = 0.3f))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = item.category.icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.White
                )
                Text(
                    text = item.category.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun PreviewDetails(
    item: ExploreItem,
    onViewDetails: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Text(
            text = item.title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = item.subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(12.dp))

        PreviewRating(rating = item.rating)

        Spacer(modifier = Modifier.height(16.dp))

        // Description placeholder
        Text(
            text = buildPreviewDescription(item),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onViewDetails,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("View Full Details")
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun PreviewRating(rating: Float) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(MAX_RATING) { index ->
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (index < rating.toInt()) GOLD_COLOR else GOLD_COLOR.copy(alpha = 0.3f)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = formatRatingDisplay(rating),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Builds a preview description based on the item properties.
 */
private fun buildPreviewDescription(item: ExploreItem): String {
    return "Discover more about ${item.title.lowercase()}. " +
           "This ${item.category.displayName.lowercase()} item has been rated " +
           "${formatRatingDisplay(item.rating)} out of 5 stars. " +
           "Tap 'View Full Details' to explore everything this has to offer."
}

/**
 * Formats rating to one decimal place.
 */
private fun formatRatingDisplay(rating: Float): String {
    val rounded = (rating * 10).toInt() / 10.0
    return if (rounded == rounded.toInt().toDouble()) {
        "${rounded.toInt()}.0"
    } else {
        rounded.toString()
    }
}
