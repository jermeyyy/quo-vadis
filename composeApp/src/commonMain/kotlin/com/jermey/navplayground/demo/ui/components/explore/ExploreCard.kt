package com.jermey.navplayground.demo.ui.components.explore

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope.OverlayClip
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.jermey.quo.vadis.core.compose.transition.rememberTransitionScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.jermey.navplayground.demo.ui.screens.explore.ExploreItem
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials

private const val CARD_HEIGHT = 200
private const val PRESSED_SCALE = 0.96f
private const val DEFAULT_SCALE = 1.0f
private const val CORNER_RADIUS = 16
private const val OVERLAY_HEIGHT_FRACTION = 0.45f
private const val MAX_RATING = 5
private const val RATING_DECIMAL_MULTIPLIER = 10
private const val RATING_DECIMAL_DIVISOR = 10.0

@Suppress("MagicNumber") // Standard color hex value
private val GOLD_COLOR = Color(0xFFFFD700)

/**
 * A glassmorphic explore card with Coil image loading and Haze frosted glass overlay.
 *
 * Features:
 * - AsyncImage for background image loading with crossfade
 * - Haze glassmorphic overlay for text area
 * - Category badge with icon
 * - Rating display with stars
 * - Press animation (scale down on press)
 * - Long-press callback for quick preview
 *
 * @param item The explore item to display
 * @param onClick Callback when card is tapped
 * @param onLongClick Callback when card is long-pressed
 * @param modifier Modifier for the card container
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ExploreCard(
    item: ExploreItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val hazeState = remember { HazeState() }
    // Use rememberTransitionScope() to get a fresh TransitionScope that always uses
    // the current LocalAnimatedVisibilityScope - this ensures shared elements animate
    // correctly even for exit content during navigation transitions
    val transitionScope = rememberTransitionScope()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) PRESSED_SCALE else DEFAULT_SCALE,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "cardScale"
    )

    Surface(
        modifier = modifier
            .height(CARD_HEIGHT.dp)
            .scale(scale)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClick() },
                    onLongPress = { onLongClick() }
                )
            },
        shape = RoundedCornerShape(CORNER_RADIUS.dp),
        shadowElevation = 4.dp,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        // Apply shared bounds to entire card content (image + overlay)
        val contentModifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(CORNER_RADIUS.dp))
        val finalContentModifier = if (transitionScope != null) {
            with(transitionScope.sharedTransitionScope) {
                contentModifier.sharedBounds(
                    sharedContentState = rememberSharedContentState(key = "explore-card-content-${item.id}"),
                    animatedVisibilityScope = transitionScope.animatedVisibilityScope,
                    clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(CORNER_RADIUS.dp))
                )
            }
        } else {
            contentModifier
        }
        
        Box(modifier = finalContentModifier) {
            CardBackgroundImage(
                imageUrl = item.imageUrl,
                contentDescription = item.title,
                hazeState = hazeState
            )

            GlassOverlay(
                hazeState = hazeState,
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                CardContent(item = item)
            }
            
            // Category badge positioned above glass overlay (same as detail screen)
            CategoryBadge(
                category = item.category,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 8.dp, bottom = (CARD_HEIGHT * OVERLAY_HEIGHT_FRACTION + 4).dp)
            )
        }
    }
}

@Composable
private fun CardBackgroundImage(
    imageUrl: String,
    contentDescription: String,
    hazeState: HazeState
) {
    val context = LocalPlatformContext.current
    var imageState by remember { mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .hazeSource(state = hazeState)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageUrl)
                .diskCacheKey(imageUrl)
                .memoryCacheKey(imageUrl)
                .diskCachePolicy(CachePolicy.ENABLED)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .crossfade(true)
                .build(),
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            onState = { imageState = it }
        )

        when (imageState) {
            is AsyncImagePainter.State.Loading -> LoadingPlaceholder()
            is AsyncImagePainter.State.Error -> ErrorPlaceholder()
            else -> { /* Success or Empty - no overlay needed */ }
        }
    }
}

@Composable
private fun LoadingPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun ErrorPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.errorContainer,
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Failed to load",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

@Composable
private fun GlassOverlay(
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    GlassOverlayBox(
        hazeState = hazeState,
        height = (CARD_HEIGHT * OVERLAY_HEIGHT_FRACTION).dp,
        cornerRadius = RoundedCornerShape(bottomStart = CORNER_RADIUS.dp, bottomEnd = CORNER_RADIUS.dp),
        modifier = modifier,
        content = content
    )
}

/**
 * Reusable glassmorphic overlay box with blur effect.
 * Used for consistent styling in ExploreCard and ExploreDetailScreen.
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun GlassOverlayBox(
    hazeState: HazeState,
    height: Dp,
    modifier: Modifier = Modifier,
    cornerRadius: RoundedCornerShape = RoundedCornerShape(0.dp),
    backgroundAlpha: Float = 0.2f,
    content: @Composable () -> Unit
) {
    val thinStyle = HazeMaterials.thin()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(cornerRadius)
            .hazeEffect(state = hazeState) {
                style = thinStyle
            }
            .background(
                Color.Black.copy(alpha = backgroundAlpha)
            )
    ) {
        content()
    }
}

@Composable
private fun CardContent(item: ExploreItem) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        RatingDisplay(rating = item.rating)
    }
}

@Composable
private fun RatingDisplay(rating: Float) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        repeat(MAX_RATING) { index ->
            val isFilled = index < rating.toInt()
            val isHalfFilled = !isFilled && index < rating

            Icon(
                imageVector = if (isFilled || isHalfFilled) Icons.Filled.Star else Icons.Outlined.StarOutline,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = if (isFilled || isHalfFilled) {
                    GOLD_COLOR
                } else {
                    Color.White.copy(alpha = 0.4f)
                }
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = formatRating(rating),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.9f)
        )
    }
}

/**
 * Formats a rating float to one decimal place (KMP-compatible).
 */
private fun formatRating(rating: Float): String {
    val rounded = (rating * RATING_DECIMAL_MULTIPLIER).toInt() / RATING_DECIMAL_DIVISOR
    return if (rounded == rounded.toInt().toDouble()) {
        "${rounded.toInt()}.0"
    } else {
        rounded.toString()
    }
}

@Composable
private fun CategoryBadge(
    category: com.jermey.navplayground.demo.ui.screens.explore.ExploreCategory,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = category.icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = category.displayName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}
