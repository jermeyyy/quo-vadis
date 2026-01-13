package com.jermey.navplayground.demo.ui.screens.explore

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope.OverlayClip
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.jermey.navplayground.demo.destinations.MainTabs
import com.jermey.navplayground.demo.ui.components.explore.GlassOverlayBox
import com.jermey.quo.vadis.annotations.Screen
import com.jermey.quo.vadis.core.compose.transition.rememberTransitionScope
import com.jermey.quo.vadis.flowmvi.rememberContainer
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.qualifier
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.compose.dsl.subscribe

// Image dimensions now standardized in ExploreItem.imageUrlForId()
private const val MAX_RATING = 5
private val EXPANDED_HEIGHT = 280.dp
private val COLLAPSED_HEIGHT = 56.dp
private const val OVERLAY_HEIGHT_FRACTION = 0.35f

@Suppress("MagicNumber")
private val GOLD_COLOR = Color(0xFFFFD700)

/**
 * Detail screen for an explore item.
 *
 * Features:
 * - Collapsing hero image with parallax effect
 * - Smooth scroll behavior with system insets support
 * - Animated content sections
 * - Back navigation
 * - Uses NavigationContainer for proper lifecycle integration
 *
 * @param destination The Detail destination containing itemId
 * @param modifier Modifier for the screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Screen(MainTabs.ExploreTab.Detail::class)
@Composable
fun ExploreDetailScreen(
    destination: MainTabs.ExploreTab.Detail,
    modifier: Modifier = Modifier,
    container: Store<ExploreDetailState, ExploreDetailIntent, ExploreDetailAction> = rememberContainer(
        qualifier = qualifier<ExploreDetailContainer>(),
        params = { parametersOf(destination.itemId) }
    )
) {
    val state by container.subscribe { action ->
        when (action) {
            is ExploreDetailAction.ShowError -> {
                // Handle error action if needed (e.g., show snackbar)
            }
        }
    }

    when (val currentState = state) {
        is ExploreDetailState.Error -> {
            DetailErrorState(
                message = currentState.message,
                onBack = { container.intent(ExploreDetailIntent.NavigateBack) }
            )
        }
        is ExploreDetailState.Content -> {
            ExploreDetailContent(
                item = currentState.item,
                itemId = destination.itemId,
                onBack = { container.intent(ExploreDetailIntent.NavigateBack) },
                modifier = modifier
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExploreDetailContent(
    item: ExploreItem,
    itemId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Use rememberTransitionScope() to get a fresh TransitionScope that always uses
    // the current LocalAnimatedVisibilityScope
    val transitionScope = rememberTransitionScope()

    val density = LocalDensity.current
    val statusBarHeight = with(density) {
        WindowInsets.statusBars.getTop(this).toDp()
    }

    // Total header height including status bar
    val totalExpandedHeight = EXPANDED_HEIGHT + statusBarHeight

    val listState = rememberLazyListState()

    // Calculate collapse progress based on scroll position (0 = expanded, 1 = collapsed)
    val collapseProgress by remember {
        derivedStateOf {
            val scrollOffset = listState.firstVisibleItemScrollOffset.toFloat()
            val firstIndex = listState.firstVisibleItemIndex

            if (firstIndex > 0) {
                1f // Fully collapsed if scrolled past first item
            } else {
                // Calculate progress based on scroll within the collapse range
                val collapseRange = with(density) {
                    (EXPANDED_HEIGHT - COLLAPSED_HEIGHT).toPx()
                }
                (scrollOffset / collapseRange).coerceIn(0f, 1f)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Main scrollable content
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize()
        ) {
            // Spacer for header area - matches total expanded height
            item {
                Spacer(modifier = Modifier.height(totalExpandedHeight))
            }

            // Content
            item {
                DetailContent(item = item, itemId = itemId)
            }
        }

        // Pinned collapsing header
        CollapsingImageHeader(
            item = item,
            itemId = itemId,
            collapseProgress = collapseProgress,
            statusBarHeight = statusBarHeight,
            onBack = onBack
        )
    }
}

/**
 * Collapsing header with hero image that pins at top.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun CollapsingImageHeader(
    item: ExploreItem,
    itemId: String,
    collapseProgress: Float,
    statusBarHeight: Dp,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalPlatformContext.current
    var imageState by remember { mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty) }
    val transitionScope = rememberTransitionScope()
    val hazeState = remember { HazeState() }
    
    // Calculate current content height (excluding status bar)
    val currentContentHeight = EXPANDED_HEIGHT - (EXPANDED_HEIGHT - COLLAPSED_HEIGHT) * collapseProgress
    // Total header height including status bar
    val totalHeight = currentContentHeight + statusBarHeight
    
    val surfaceColor = MaterialTheme.colorScheme.surface
    val overlayHeight = (EXPANDED_HEIGHT.value * OVERLAY_HEIGHT_FRACTION).dp

    // Single box that spans full height including status bar
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(totalHeight)
            .clip(RoundedCornerShape(0.dp)) // Clip children to this box
    ) {
        // Shared bounds container for image + glass overlay
        val sharedContentModifier = Modifier
            .fillMaxWidth()
            .height(EXPANDED_HEIGHT + statusBarHeight)
        val finalSharedModifier = if (transitionScope != null) {
            with(transitionScope.sharedTransitionScope) {
                sharedContentModifier.sharedBounds(
                    sharedContentState = rememberSharedContentState(key = "explore-card-content-$itemId"),
                    animatedVisibilityScope = transitionScope.animatedVisibilityScope,
                    clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(0.dp))
                )
            }
        } else {
            sharedContentModifier
        }
        
        Box(modifier = finalSharedModifier) {
            // Hero Image - acts as haze source
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(ExploreItem.imageUrlForId(item.id))
                    .crossfade(true)
                    .build(),
                contentDescription = item.title,
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(state = hazeState),
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                onState = { imageState = it }
            )

            // Loading state
            if (imageState is AsyncImagePainter.State.Loading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(40.dp))
                }
            }

            // Glassmorphic overlay at bottom with title and rating
            GlassOverlayBox(
                hazeState = hazeState,
                height = overlayHeight,
                backgroundAlpha = 0.3f,
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                // Content overlay with title and rating
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
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

                    // Rating display
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        repeat(MAX_RATING) { index ->
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (index < item.rating.toInt()) GOLD_COLOR else GOLD_COLOR.copy(alpha = 0.3f)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = formatRating(item.rating),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
            
            // Category badge - inside shared element for transition
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = overlayHeight + 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = item.category.icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = item.category.displayName,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Surface color overlay when collapsed - covers entire header including status bar
        // Outside shared bounds so it fades independently
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(surfaceColor.copy(alpha = collapseProgress * 0.95f))
        )

        // Top bar content - positioned below status bar
        // Use renderInSharedTransitionScopeOverlay for fade animation during transition
        val topBarModifier = Modifier
            .fillMaxWidth()
            .padding(top = statusBarHeight)
            .height(COLLAPSED_HEIGHT)
            .padding(horizontal = 4.dp)
        val finalTopBarModifier = if (transitionScope != null) {
            with(transitionScope.sharedTransitionScope) {
                topBarModifier.renderInSharedTransitionScopeOverlay(
                    zIndexInOverlay = 1f
                )
            }
        } else {
            topBarModifier
        }
        
        // Animated visibility for top bar icons during shared element transition
        val topBarAlpha = if (transitionScope != null) {
            with(transitionScope.sharedTransitionScope) {
                animateFloatAsState(
                    targetValue = if (transitionScope.animatedVisibilityScope.transition.isRunning) 0f else 1f,
                    animationSpec = tween(durationMillis = 300),
                    label = "topBarAlpha"
                ).value
            }
        } else {
            1f
        }
        
        Row(
            modifier = finalTopBarModifier.alpha(topBarAlpha),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button with background when expanded
            IconButton(onClick = onBack) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = if (collapseProgress < 0.5f) {
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                            } else {
                                Color.Transparent
                            },
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Title - fades in as toolbar collapses
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
                    .alpha(collapseProgress)
            )

            // Action buttons with background when expanded
            IconButton(onClick = { /* Bookmark */ }) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = if (collapseProgress < 0.5f) {
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                            } else {
                                Color.Transparent
                            },
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = "Bookmark",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            IconButton(onClick = { /* Share */ }) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = if (collapseProgress < 0.5f) {
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                            } else {
                                Color.Transparent
                            },
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailContent(item: ExploreItem, itemId: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        // Description Section
        DetailSection(title = "About") {
            Text(
                text = buildDetailDescription(item),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Specifications Section
        DetailSection(title = "Details") {
            DetailSpecRow(label = "Category", value = item.category.displayName)
            DetailSpecRow(label = "Rating", value = "${formatRating(item.rating)} / 5.0")
            DetailSpecRow(label = "Status", value = if (item.isFeatured) "Featured" else "Standard")
            DetailSpecRow(label = "ID", value = item.id)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Features Section
        DetailSection(title = "Key Features") {
            FeatureBullet("High-quality content curated by experts")
            FeatureBullet("Regular updates with new information")
            FeatureBullet("Community ratings and reviews")
            FeatureBullet("Bookmark for offline access")
            FeatureBullet("Share with friends and colleagues")
            FeatureBullet("Cross-platform availability")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Extended Description
        DetailSection(title = "Overview") {
            Text(
                text = buildExtendedDescription(item),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Statistics Section
        DetailSection(title = "Statistics") {
            DetailSpecRow(label = "Views", value = "${(item.id.hashCode().toLong() and 0xFFFFL) + 1000}")
            DetailSpecRow(label = "Bookmarks", value = "${(item.id.hashCode().toLong() and 0xFFL) + 50}")
            DetailSpecRow(label = "Shares", value = "${(item.id.hashCode().toLong() and 0x7FL) + 25}")
            DetailSpecRow(label = "Comments", value = "${(item.id.hashCode().toLong() and 0x3FL) + 10}")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Tags Section
        DetailSection(title = "Tags") {
            Text(
                text = generateTags(item),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Related Items Section
        DetailSection(title = "Related") {
            Text(
                text = "Explore more ${item.category.displayName.lowercase()} items...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "• Similar items in this category\n• Popular picks this week\n• Editor's recommendations\n• Trending content",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Additional Info
        DetailSection(title = "Additional Information") {
            Text(
                text = "This content is part of our curated collection. We continuously update our " +
                        "catalog to bring you the best and most relevant information. If you have any " +
                        "feedback or suggestions, please don't hesitate to reach out through our support channels.\n\n" +
                        "Last updated: January 2026\n" +
                        "Content version: 2.0\n" +
                        "Available languages: English, Spanish, French, German, Japanese",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Bottom spacing for scroll overscroll
        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
private fun FeatureBullet(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun DetailSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        content()
    }
}

@Composable
private fun DetailSpecRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun DetailErrorState(
    message: String,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
            Surface(
                onClick = onBack,
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = "Go Back",
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

/**
 * Builds a detailed description for the item.
 */
private fun buildDetailDescription(item: ExploreItem): String {
    return "Welcome to ${item.title}! ${item.subtitle}. " +
           "This exceptional ${item.category.displayName.lowercase()} item has garnered " +
           "a rating of ${formatRating(item.rating)} out of 5 stars from our community. " +
           if (item.isFeatured) {
               "As a featured item, it represents the best in its category. "
           } else {
               ""
           } +
           "Explore all the details and discover what makes this item special."
}

/**
 * Builds an extended description for the item.
 */
private fun buildExtendedDescription(item: ExploreItem): String {
    return "Dive deeper into ${item.title} and discover everything it has to offer. " +
           "Our team of experts has carefully curated this ${item.category.displayName.lowercase()} content " +
           "to ensure you get the most comprehensive and up-to-date information available.\n\n" +
           "Whether you're a beginner looking to learn the basics or an expert seeking advanced insights, " +
           "${item.title} provides valuable knowledge that caters to all skill levels. " +
           "The content is regularly updated to reflect the latest trends and developments in the field.\n\n" +
           "Join thousands of other users who have already explored this content and found it valuable. " +
           "With a rating of ${formatRating(item.rating)} stars, ${item.title} stands out as one of the " +
           "top picks in our ${item.category.displayName.lowercase()} collection."
}

/**
 * Generates tags for the item based on its properties.
 */
private fun generateTags(item: ExploreItem): String {
    val baseTags = listOf(
        "#${item.category.displayName.lowercase().replace(" ", "")}",
        "#explore",
        "#discover",
        "#learn"
    )
    val conditionalTags = if (item.isFeatured) listOf("#featured", "#topPick") else listOf("#trending")
    val ratingTags = if (item.rating >= 4.5f) listOf("#highlyRated", "#mustSee") else listOf("#recommended")
    
    return (baseTags + conditionalTags + ratingTags).joinToString("  ")
}

/**
 * Formats rating to one decimal place.
 */
private fun formatRating(rating: Float): String {
    val rounded = (rating * 10).toInt() / 10.0
    return if (rounded == rounded.toInt().toDouble()) {
        "${rounded.toInt()}.0"
    } else {
        rounded.toString()
    }
}
